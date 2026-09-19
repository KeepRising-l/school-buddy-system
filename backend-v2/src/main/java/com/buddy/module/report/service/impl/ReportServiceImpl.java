package com.buddy.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.report.dto.ReportVO;
import com.buddy.module.report.entity.Report;
import com.buddy.module.report.mapper.ReportMapper;
import com.buddy.module.report.service.ReportService;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 举报服务实现类（等价迁移自旧版，S7 范围：用户端提交与进度查询）
 */
@Service
public class ReportServiceImpl implements ReportService {

    /** 24小时重复举报窗口（毫秒） */
    private static final long DUPLICATE_WINDOW_MS = 24 * 60 * 60 * 1000L;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

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

        // 24小时内重复举报校验
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

    /**
     * 分页实体转VO（批量：一次 IN 查询出所有举报人/被举报对象，解决 N+1）
     */
    private IPage<ReportVO> convertPage(IPage<Report> reportPage) {
        List<Report> records = reportPage.getRecords();
        IPage<ReportVO> voPage = new Page<>(reportPage.getCurrent(), reportPage.getSize(), reportPage.getTotal());
        if (records == null || records.isEmpty()) {
            voPage.setRecords(new java.util.ArrayList<>());
            return voPage;
        }

        // 收集所有需要查询的用户ID（举报人、被举报的用户）与帖子ID（被举报的帖子）
        java.util.Set<Long> userIds = new java.util.HashSet<>();
        java.util.Set<Long> postIds = new java.util.HashSet<>();
        for (Report r : records) {
            if (r.getReporterId() != null) {
                userIds.add(r.getReporterId());
            }
            if (Report.TARGET_POST.equals(r.getTargetType())) {
                if (r.getTargetId() != null) {
                    postIds.add(r.getTargetId());
                }
            } else if (r.getTargetId() != null) {
                userIds.add(r.getTargetId());
            }
        }

        // 人和帖各查一次即可
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : toMapById(userMapper.selectBatchIds(userIds), User::getId);
        Map<Long, Post> postMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : toMapById(postMapper.selectBatchIds(postIds), Post::getId);

        List<ReportVO> voList = new java.util.ArrayList<>(records.size());
        for (Report r : records) {
            ReportVO vo = baseReportVO(r);
            User reporter = userMap.get(r.getReporterId());
            if (reporter != null) {
                vo.setReporterNickname(reporter.getNickname());
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
     * 基础字段拷贝（不含关联昵称/标题）
     */
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
        return vo;
    }

    private static <T> Map<Long, T> toMapById(List<T> items, Function<T, Long> idFn) {
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
