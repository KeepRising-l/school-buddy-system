package com.buddy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.dto.response.ReportVO;
import com.buddy.entity.Notification;
import com.buddy.entity.Post;
import com.buddy.entity.Report;
import com.buddy.entity.User;
import com.buddy.mapper.PostMapper;
import com.buddy.mapper.ReportMapper;
import com.buddy.mapper.UserMapper;
import com.buddy.service.AdminService;
import com.buddy.service.NotificationService;
import com.buddy.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 举报服务实现类（V1.1）
 */
@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    /** 24小时重复举报窗口（毫秒） */
    private static final long DUPLICATE_WINDOW_MS = 24 * 60 * 60 * 1000L;

    /** 自动风控阈值：同一对象累计有效举报次数 */
    private static final int AUTO_BAN_THRESHOLD = 3;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AdminService adminService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long reporterId, String targetType, Long targetId, String reasonType, String description) {
        // 校验参数
        if (targetId == null || (description != null && description.length() > 200)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "举报参数不合法");
        }

        // 校验对象存在
        if (Report.TARGET_POST.equals(targetType)) {
            Post post = postMapper.selectById(targetId);
            if (post == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "被举报的帖子不存在");
            }
        } else if (Report.TARGET_USER.equals(targetType)) {
            User user = userMapper.selectById(targetId);
            if (user == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "被举报的用户不存在");
            }
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "举报对象类型不合法");
        }

        // 不能举报自己
        if (Report.TARGET_USER.equals(targetType) && reporterId.equals(targetId)) {
            throw new BusinessException("不能举报自己");
        }

        // 24小时内重复举报校验（REP-02）
        LocalDateTime windowStart = LocalDateTime.now().minusNanos(DUPLICATE_WINDOW_MS * 1_000_000);
        Long duplicateCount = reportMapper.selectCount(
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getReporterId, reporterId)
                        .eq(Report::getTargetType, targetType)
                        .eq(Report::getTargetId, targetId)
                        .ge(Report::getCreatedTime, windowStart)
        );
        if (duplicateCount > 0) {
            throw new BusinessException("已举报过该对象，请勿重复举报");
        }

        // 插入举报
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReasonType(reasonType);
        report.setDescription(description);
        report.setStatus(Report.STATUS_PENDING);
        reportMapper.insert(report);
    }

    @Override
    public IPage<ReportVO> getMyReports(Long reporterId, int page, int size) {
        IPage<Report> reportPage = reportMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getReporterId, reporterId)
                        .orderByDesc(Report::getCreatedTime)
        );
        return convertPage(reportPage);
    }

    @Override
    public IPage<ReportVO> getReportList(int page, int size, Integer status) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByDesc(Report::getCreatedTime);
        IPage<Report> reportPage = reportMapper.selectPage(new Page<>(page, size), wrapper);
        return convertPage(reportPage);
    }

    @Override
    public ReportVO getReportDetail(Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }
        return convertToVO(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long adminId, Long reportId, boolean valid, String remark) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }
        if (report.getStatus() != Report.STATUS_PENDING) {
            throw new BusinessException("该举报已处理");
        }

        // 更新举报状态
        report.setStatus(valid ? Report.STATUS_VALID : Report.STATUS_INVALID);
        report.setHandleBy(adminId);
        report.setHandleTime(LocalDateTime.now());
        report.setHandleRemark(remark);
        reportMapper.updateById(report);

        // 有效举报：联动处理（REP-05）
        if (valid) {
            String targetDesc;
            if (Report.TARGET_POST.equals(report.getTargetType())) {
                // 下架帖子（V1.2 复用 takedownPost：级联取消未完结报名+恢复人数+通知报名用户，
                // 修复：原来只置状态导致参与者报名记录悬挂卡死）
                Post post = postMapper.selectById(report.getTargetId());
                if (post != null && !PostStatus.CANCELLED.equals(post.getStatus())) {
                    adminService.takedownPost(report.getTargetId());
                    // 通知发布者：帖子因违规被下架
                    notificationService.send(post.getPublisherId(), Notification.TYPE_POST_TAKEDOWN,
                            "帖子已被下架",
                            "你的组局「" + post.getTitle() + "」因违规被下架，如有疑问请联系管理员",
                            "/post/" + post.getId(), post.getId());
                    targetDesc = post.getTitle();
                } else {
                    targetDesc = "帖子#" + report.getTargetId();
                }
            } else {
                // 封禁用户
                User user = userMapper.selectById(report.getTargetId());
                if (user != null && user.getStatus() == 1) {
                    user.setStatus(0);
                    userMapper.updateById(user);
                    // 通知被封禁用户
                    notificationService.send(user.getId(), Notification.TYPE_SYSTEM,
                            "账号已被封禁",
                            "你的账号因违规行为被封禁，如有疑问请联系管理员",
                            null, null);
                    targetDesc = user.getNickname();
                } else {
                    targetDesc = "用户#" + report.getTargetId();
                }
            }

            // 自动风控（V1.2 落地）：同一对象累计3次有效举报（REP-05）触发兜底处置。
            // 单次有效举报已即时下架/封禁，此处对"多次违规"对象追加风控确认：
            // 若对象仍存活（例如此前举报的是另一维度/对象被恢复）则再次处置并追加警告通知
            Long validCount = reportMapper.selectCount(
                    new LambdaQueryWrapper<Report>()
                            .eq(Report::getTargetType, report.getTargetType())
                            .eq(Report::getTargetId, report.getTargetId())
                            .eq(Report::getStatus, Report.STATUS_VALID)
            );
            if (validCount >= AUTO_BAN_THRESHOLD) {
                log.warn("自动风控触发：对象 {}-{} 有效举报已达 {} 次", report.getTargetType(), report.getTargetId(), validCount);
                if (Report.TARGET_POST.equals(report.getTargetType())) {
                    Post targetPost = postMapper.selectById(report.getTargetId());
                    if (targetPost != null && !PostStatus.CANCELLED.equals(targetPost.getStatus())) {
                        adminService.takedownPost(targetPost.getId());
                        notificationService.send(targetPost.getPublisherId(), Notification.TYPE_POST_TAKEDOWN,
                                "多次违规，组局已被下架",
                                "你的组局「" + targetPost.getTitle() + "」因多次被举报核实违规，已被系统自动下架",
                                "/post/" + targetPost.getId(), targetPost.getId());
                    }
                } else {
                    User targetUser = userMapper.selectById(report.getTargetId());
                    if (targetUser != null && targetUser.getStatus() == 1) {
                        targetUser.setStatus(0);
                        userMapper.updateById(targetUser);
                        notificationService.send(targetUser.getId(), Notification.TYPE_SYSTEM,
                                "多次违规，账号已被封禁",
                                "你的账号因多次违规行为已被系统自动封禁，如有疑问请联系管理员",
                                null, null);
                    }
                }
            }
        }

        // 通知举报人处理结果
        String resultText = valid ? "举报已受理，感谢你的反馈" : "举报经核实不成立";
        notificationService.send(report.getReporterId(), Notification.TYPE_SYSTEM,
                "举报处理结果",
                resultText,
                null, null);
    }

    /**
     * 分页实体转VO（批量：一次 IN 查询出所有举报人/处理人/被举报对象，解决 N+1）
     */
    private IPage<ReportVO> convertPage(IPage<Report> reportPage) {
        List<Report> records = reportPage.getRecords();
        IPage<ReportVO> voPage = new Page<>(reportPage.getCurrent(), reportPage.getSize(), reportPage.getTotal());
        if (records == null || records.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        // 收集所有需要查询的用户ID（举报人、处理人、被举报的用户）与帖子ID（被举报的帖子）
        Set<Long> userIds = new HashSet<>();
        Set<Long> postIds = new HashSet<>();
        for (Report r : records) {
            if (r.getReporterId() != null) {
                userIds.add(r.getReporterId());
            }
            if (r.getHandleBy() != null) {
                userIds.add(r.getHandleBy());
            }
            if (Report.TARGET_POST.equals(r.getTargetType())) {
                if (r.getTargetId() != null) {
                    postIds.add(r.getTargetId());
                }
            } else if (r.getTargetId() != null) {
                userIds.add(r.getTargetId());
            }
        }

        // 三次变两次：人和帖各查一次即可
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : toMapById(userMapper.selectBatchIds(userIds), User::getId);
        Map<Long, Post> postMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : toMapById(postMapper.selectBatchIds(postIds), Post::getId);

        List<ReportVO> voList = new ArrayList<>(records.size());
        for (Report r : records) {
            ReportVO vo = baseReportVO(r);
            User reporter = userMap.get(r.getReporterId());
            if (reporter != null) {
                vo.setReporterNickname(reporter.getNickname());
            }
            if (r.getHandleBy() != null) {
                User handler = userMap.get(r.getHandleBy());
                if (handler != null) {
                    vo.setHandleByName(handler.getNickname());
                }
            }
            if (Report.TARGET_POST.equals(r.getTargetType())) {
                Post post = postMap.get(r.getTargetId());
                if (post != null) {
                    vo.setTargetTitle(post.getTitle());
                }
            } else {
                User user = userMap.get(r.getTargetId());
                if (user != null) {
                    vo.setTargetTitle(user.getNickname());
                }
            }
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 实体转VO（单条，详情页使用）
     */
    private ReportVO convertToVO(Report report) {
        ReportVO vo = baseReportVO(report);
        User reporter = report.getReporterId() != null ? userMapper.selectById(report.getReporterId()) : null;
        if (reporter != null) {
            vo.setReporterNickname(reporter.getNickname());
        }
        if (report.getHandleBy() != null) {
            User handler = userMapper.selectById(report.getHandleBy());
            if (handler != null) {
                vo.setHandleByName(handler.getNickname());
            }
        }
        if (Report.TARGET_POST.equals(report.getTargetType())) {
            Post post = report.getTargetId() != null ? postMapper.selectById(report.getTargetId()) : null;
            if (post != null) {
                vo.setTargetTitle(post.getTitle());
            }
        } else if (report.getTargetId() != null) {
            User user = userMapper.selectById(report.getTargetId());
            if (user != null) {
                vo.setTargetTitle(user.getNickname());
            }
        }
        return vo;
    }

    /** 基础字段拷贝（不含关联查询） */
    private ReportVO baseReportVO(Report report) {
        ReportVO vo = new ReportVO();
        vo.setId(report.getId());
        vo.setReporterId(report.getReporterId());
        vo.setTargetType(report.getTargetType());
        vo.setTargetId(report.getTargetId());
        vo.setReasonType(report.getReasonType());
        vo.setDescription(report.getDescription());
        vo.setStatus(report.getStatus());
        vo.setHandleRemark(report.getHandleRemark());
        vo.setHandleTime(report.getHandleTime());
        vo.setCreatedTime(report.getCreatedTime());
        return vo;
    }

    private static <T> Map<Long, T> toMapById(List<T> items, java.util.function.Function<T, Long> idFn) {
        Map<Long, T> map = new HashMap<>();
        if (items != null) {
            for (T item : items) {
                Long key = idFn.apply(item);
                if (key != null) {
                    map.put(key, item);
                }
            }
        }
        return map;
    }
}
