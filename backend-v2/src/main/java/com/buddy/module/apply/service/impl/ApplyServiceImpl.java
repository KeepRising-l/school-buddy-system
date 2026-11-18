package com.buddy.module.apply.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.cache.CacheEpoch;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.CreditConstant;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.common.statemachine.PostStateMachine;
import com.buddy.module.apply.dto.ApplyVO;
import com.buddy.module.apply.entity.PostApply;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.apply.service.ApplyService;
import com.buddy.module.credit.entity.CreditLog;
import com.buddy.module.credit.service.CreditService;
import com.buddy.module.notification.entity.Notification;
import com.buddy.module.notification.service.NotificationService;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.tag.entity.UserTag;
import com.buddy.module.tag.mapper.UserTagMapper;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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
import java.util.stream.Collectors;

/**
 * 报名服务实现类（等价迁移自旧版）
 */
@Service
public class ApplyServiceImpl implements ApplyService {

    @Autowired
    private PostApplyMapper postApplyMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserTagMapper userTagMapper;

    @Autowired
    private CreditService creditService;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private CacheEpoch cacheEpoch;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long userId, Long postId) {
        // 校验帖子存在且状态为RECRUITING/FULL（FULL 时走候补逻辑）
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (PostStatus.FULL.equals(post.getStatus())) {
            // 候补机制：满员时调用候补报名
            doWaitlist(userId, post);
            return;
        }
        if (!PostStatus.RECRUITING.equals(post.getStatus())) {
            throw new BusinessException(ResultCode.POST_NOT_RECRUITING);
        }

        // 先发后审：已下架帖子不可报名
        if (post.getIsAuditPass() != null && post.getIsAuditPass() == 2) {
            throw new BusinessException(ResultCode.POST_NOT_RECRUITING, "帖子已被下架，暂不可报名");
        }

        // 校验不能报名自己发布的帖子
        if (post.getPublisherId().equals(userId)) {
            throw new BusinessException("不能报名自己发布的活动");
        }

        // 校验未重复报名（允许取消/被拒后重新报名：复用旧记录行，避免唯一键冲突）
        PostApply existing = postApplyMapper.selectOne(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .eq(PostApply::getApplicantId, userId)
                        .last("LIMIT 1")
        );
        if (existing != null
                && !ApplyStatus.CANCELLED.equals(existing.getStatus())
                && !ApplyStatus.REJECTED.equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.APPLY_DUPLICATE);
        }

        // 校验报名资格（信誉分+段位+性别），候补报名共用同一套规则
        User applicant = validateQualification(userId, post);

        // 插入或复用旧报名记录（取消/被拒后再次报名复用旧行，规避唯一键冲突）
        PostApply apply;
        if (existing != null) {
            // 条件更新：并发下只有一条"取消/被拒"记录能被重新激活为待审核
            int reactivated = postApplyMapper.update(null, new LambdaUpdateWrapper<PostApply>()
                    .eq(PostApply::getId, existing.getId())
                    .in(PostApply::getStatus, ApplyStatus.CANCELLED, ApplyStatus.REJECTED)
                    .set(PostApply::getStatus, ApplyStatus.PENDING)
                    .set(PostApply::getApplyTime, LocalDateTime.now())
                    .set(PostApply::getHandleTime, null)
                    .set(PostApply::getRemark, null));
            if (reactivated == 0) {
                throw new BusinessException(ResultCode.APPLY_DUPLICATE, "报名状态已变化，请刷新后重试");
            }
            apply = existing;
        } else {
            apply = new PostApply();
            apply.setPostId(postId);
            apply.setApplicantId(userId);
            apply.setStatus(ApplyStatus.PENDING);
            apply.setApplyTime(LocalDateTime.now());
            try {
                postApplyMapper.insert(apply);
            } catch (DuplicateKeyException e) {
                // 并发双击/多端同时报名：由唯一索引 uk_post_applicant 兜底，返回明确业务响应
                throw new BusinessException(ResultCode.APPLY_DUPLICATE, "已报名该活动，请勿重复提交");
            }
        }

        // 通知发布者：有新的报名申请（与业务同一事务）
        notificationService.send(post.getPublisherId(), Notification.TYPE_APPLY_NEW,
                "新的报名申请",
                applicant.getNickname() + " 报名了你的组局「" + post.getTitle() + "」",
                "/post/" + postId, postId);
    }

    /**
     * 校验报名资格：信誉分 + 段位 + 性别（报名与候补共用）
     *
     * <p>段位校验说明：检查用户在帖子任一标签上的最高段位；
     * 提示语说明具体原因（未设置标签 / 段位不足）。</p>
     *
     * @return 报名用户实体
     */
    private User validateQualification(Long userId, Post post) {
        // 校验信誉分
        User applicant = userMapper.selectById(userId);
        if (applicant == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (creditService.isLowCredit(applicant.getCreditScore())) {
            throw new BusinessException(ResultCode.CREDIT_TOO_LOW);
        }

        // 校验段位要求（requireLevel为0表示不限）
        if (post.getRequireLevel() != null && post.getRequireLevel() > 0) {
            List<Long> tagIdList = parseTagIds(post.getTagIds());
            boolean levelQualified = false;
            if (!tagIdList.isEmpty()) {
                List<UserTag> userTags = userTagMapper.selectList(
                        new LambdaQueryWrapper<UserTag>()
                                .eq(UserTag::getUserId, userId)
                                .in(UserTag::getTagId, tagIdList)
                );
                levelQualified = userTags.stream()
                        .anyMatch(ut -> ut.getSelfLevel() != null && ut.getSelfLevel() >= post.getRequireLevel());
            }
            if (!levelQualified) {
                if (tagIdList.isEmpty()) {
                    throw new BusinessException(ResultCode.APPLY_NOT_QUALIFIED, "帖子未设置标签，无法校验段位");
                }
                throw new BusinessException(ResultCode.APPLY_NOT_QUALIFIED,
                        "段位不满足要求：该组局要求" + levelName(post.getRequireLevel()) + "以上，请先到「兴趣标签」中设置对应项目的段位");
            }
        }

        // 校验性别要求
        if (post.getRequireGender() != null && post.getRequireGender() != 0) {
            if (!post.getRequireGender().equals(applicant.getGender())) {
                throw new BusinessException(ResultCode.APPLY_NOT_QUALIFIED, "性别不满足要求");
            }
        }

        return applicant;
    }

    /**
     * 候补报名：帖子已满员时进入候补队列
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void waitlist(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        // 仅满员状态允许候补
        if (!PostStatus.FULL.equals(post.getStatus())) {
            throw new BusinessException("帖子未满员，可直接报名");
        }
        doWaitlist(userId, post);
    }

    /**
     * 候补报名内部实现
     */
    private void doWaitlist(Long userId, Post post) {
        // 先发后审：已下架帖子不可候补
        if (post.getIsAuditPass() != null && post.getIsAuditPass() == 2) {
            throw new BusinessException("帖子已被下架，暂不可候补");
        }

        // 校验不能报名自己发布的帖子
        if (post.getPublisherId().equals(userId)) {
            throw new BusinessException("不能报名自己发布的活动");
        }

        // 校验未重复报名/候补（允许取消/被拒后重新候补，复用旧记录行避免唯一键冲突）
        PostApply existing = postApplyMapper.selectOne(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, post.getId())
                        .eq(PostApply::getApplicantId, userId)
                        .last("LIMIT 1")
        );
        if (existing != null
                && !ApplyStatus.CANCELLED.equals(existing.getStatus())
                && !ApplyStatus.REJECTED.equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.APPLY_DUPLICATE);
        }

        // 校验报名资格（信誉分+段位+性别），与正常报名同一套规则，避免候补绕过门槛
        validateQualification(userId, post);

        // 插入或复用旧记录（取消/被拒后再次候补复用旧行，规避唯一键冲突）
        PostApply apply;
        if (existing != null) {
            // 条件更新：并发下只有一条"取消/被拒"记录能被重新激活为候补
            LocalDateTime now = LocalDateTime.now();
            int reactivated = postApplyMapper.update(null, new LambdaUpdateWrapper<PostApply>()
                    .eq(PostApply::getId, existing.getId())
                    .in(PostApply::getStatus, ApplyStatus.CANCELLED, ApplyStatus.REJECTED)
                    .set(PostApply::getStatus, ApplyStatus.WAITLIST)
                    .set(PostApply::getApplyTime, now)
                    .set(PostApply::getHandleTime, null)
                    .set(PostApply::getRemark, null));
            if (reactivated == 0) {
                throw new BusinessException(ResultCode.APPLY_DUPLICATE, "报名状态已变化，请刷新后重试");
            }
            apply = existing;
            apply.setStatus(ApplyStatus.WAITLIST);
            apply.setApplyTime(now);
            apply.setHandleTime(null);
            apply.setRemark(null);
        } else {
            apply = new PostApply();
            apply.setPostId(post.getId());
            apply.setApplicantId(userId);
            apply.setStatus(ApplyStatus.WAITLIST);
            apply.setApplyTime(LocalDateTime.now());
            try {
                postApplyMapper.insert(apply);
            } catch (DuplicateKeyException e) {
                // 并发双击/多端同时候补：由唯一索引 uk_post_applicant 兜底，返回明确业务响应
                throw new BusinessException(ResultCode.APPLY_DUPLICATE, "已报名过该活动，请勿重复提交");
            }
        }

        // 查询候补位置
        Long position = postApplyMapper.selectCount(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, post.getId())
                        .eq(PostApply::getStatus, ApplyStatus.WAITLIST)
                        .le(PostApply::getApplyTime, apply.getApplyTime())
        );

        // 通知候补者
        notificationService.send(userId, Notification.TYPE_WAITLIST_PROMOTED,
                "已进入候补队列",
                "组局「" + post.getTitle() + "」已满员，你当前是候补第 " + position + " 位，有人取消将自动递补",
                "/post/" + post.getId(), post.getId());
    }

    /**
     * 自动递补：APPROVED 报名取消或递补被拒后，候补队列第一位转 PENDING 并通知
     *
     * @param post 相关帖子
     * @return true=成功递补了一位候补者；false=无候补者可递补
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean promoteWaitlist(Post post) {
        // 查询候补队列第一位（按候补时间升序）
        PostApply firstWait = postApplyMapper.selectOne(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, post.getId())
                        .eq(PostApply::getStatus, ApplyStatus.WAITLIST)
                        .orderByAsc(PostApply::getApplyTime)
                        .last("LIMIT 1")
        );
        if (firstWait == null) {
            return false;
        }

        // 递补为待审核（条件更新防并发重复递补：只有一个请求能把候补置为待审核）
        int promoted = postApplyMapper.update(null, new LambdaUpdateWrapper<PostApply>()
                .eq(PostApply::getId, firstWait.getId())
                .eq(PostApply::getStatus, ApplyStatus.WAITLIST)
                .set(PostApply::getStatus, ApplyStatus.PENDING)
                .set(PostApply::getHandleTime, LocalDateTime.now()));
        if (promoted == 0) {
            return false;
        }
        firstWait.setStatus(ApplyStatus.PENDING);

        // 通知候补者：有空位了
        notificationService.send(firstWait.getApplicantId(), Notification.TYPE_WAITLIST_PROMOTED,
                "有空位啦！",
                "你候补的组局「" + post.getTitle() + "」有空位了，等待发布者确认",
                "/post/" + post.getId(), post.getId());

        // 通知发布者：有递补申请
        notificationService.send(post.getPublisherId(), Notification.TYPE_APPLY_NEW,
                "有递补申请",
                "候补用户已递补，请及时审核",
                "/post/" + post.getId(), post.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelApply(Long userId, Long applyId) {
        PostApply apply = postApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }

        // 校验是报名者本人
        if (!apply.getApplicantId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能取消自己的报名");
        }

        // 校验状态可取消（PENDING/APPROVED/WAITLIST 才能取消，支持候补取消）
        String originalStatus = apply.getStatus();
        if (!ApplyStatus.PENDING.equals(originalStatus)
                && !ApplyStatus.APPROVED.equals(originalStatus)
                && !ApplyStatus.WAITLIST.equals(originalStatus)) {
            throw new BusinessException("当前报名状态不可取消");
        }

        // 校验帖子状态：只有招募中/已满员才能取消；进行中/已结束不可取消
        Post post = postMapper.selectById(apply.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!PostStatus.RECRUITING.equals(post.getStatus()) && !PostStatus.FULL.equals(post.getStatus())) {
            throw new BusinessException("活动已开始或已结束，无法取消报名");
        }

        // 条件更新防并发重复取消：只有仍处于可取消状态的记录才能置 CANCELLED，
        // 双击/多端同时取消时仅一个请求成功，避免人数被多减、候补被重复递补
        int updatedCount = postApplyMapper.update(null, new LambdaUpdateWrapper<PostApply>()
                .eq(PostApply::getId, applyId)
                .in(PostApply::getStatus, ApplyStatus.PENDING, ApplyStatus.APPROVED, ApplyStatus.WAITLIST)
                .set(PostApply::getStatus, ApplyStatus.CANCELLED)
                .set(PostApply::getHandleTime, LocalDateTime.now()));
        if (updatedCount == 0) {
            throw new BusinessException("当前报名状态不可取消");
        }

        // 已通过（APPROVED）的报名取消后：人数-1，若有候补则优先递补，无候补时回退为招募中
        if (ApplyStatus.APPROVED.equals(originalStatus)) {
            postMapper.decrementCurrentPeople(post.getId());
            post = postMapper.selectById(post.getId());

            // 候补递补：满员时有候补则先递补（递补后该名额留给候补队首审核，不立即退回 RECRUITING，防路人插队）
            if (post != null && PostStatus.FULL.equals(post.getStatus())) {
                boolean promoted = promoteWaitlist(post);
                if (!promoted) {
                    post = postMapper.selectById(post.getId());
                    if (post != null && post.getCurrentPeople() < post.getMaxPeople()) {
                        // 无人候补时，仅在仍为满员时回退为招募中，避免覆盖并发的取消/下架
                        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                                .eq(Post::getId, post.getId())
                                .eq(Post::getStatus, PostStatus.FULL)
                                .set(Post::getStatus, PostStatus.RECRUITING));
                    }
                }
            }
            if (cacheEpoch != null) {
                cacheEpoch.bump();
            }
        }

        // 活动开始前1小时内取消报名，扣除信誉分5分（窗口为 [开始前1小时, 开始时间]）
        // 仅 APPROVED（已占名额）取消才扣分；PENDING/WAITLIST 未占名额不扣
        // 写台账+即时重算，惩罚实时生效且不被每日重算冲掉
        if (ApplyStatus.APPROVED.equals(originalStatus)
                && post != null && post.getStartTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = post.getStartTime();
            if (now.isAfter(startTime.minusHours(1)) && now.isBefore(startTime)) {
                creditService.recordPenalty(userId,
                        CreditConstant.CANCEL_LAST_MINUTE, CreditLog.REASON_CANCEL_LAST_MINUTE,
                        CreditLog.BIZ_TYPE_APPLY, applyId);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long publisherId, Long applyId) {
        PostApply apply = postApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }

        // 校验是发布者
        Post post = postMapper.selectById(apply.getPostId());
        if (post == null || !post.getPublisherId().equals(publisherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有发布者才能审批报名");
        }

        // 校验帖子状态：只有招募中/已满员才能审批（活动开始后不可再加人）
        if (!PostStatus.RECRUITING.equals(post.getStatus()) && !PostStatus.FULL.equals(post.getStatus())) {
            throw new BusinessException("当前帖子状态不可审批报名");
        }

        // 先发后审：已下架帖子不可审批报名
        if (post.getIsAuditPass() != null && post.getIsAuditPass() == 2) {
            throw new BusinessException("帖子已被下架，暂不可审批报名");
        }

        // 校验报名状态为PENDING
        if (!ApplyStatus.PENDING.equals(apply.getStatus())) {
            throw new BusinessException("只能审批待审核的报名");
        }

        // 条件更新防并发重复审批：只有仍为 PENDING 的记录才能置 APPROVED，
        // 并发双击/双端审批时第二个请求更新 0 行直接失败，避免同一申请人被计入两次名额
        int updatedCount = postApplyMapper.update(null, new LambdaUpdateWrapper<PostApply>()
                .eq(PostApply::getId, applyId)
                .eq(PostApply::getStatus, ApplyStatus.PENDING)
                .set(PostApply::getStatus, ApplyStatus.APPROVED)
                .set(PostApply::getHandleTime, LocalDateTime.now()));
        if (updatedCount == 0) {
            throw new BusinessException("该报名已被处理，请刷新后重试");
        }

        // 原子增加人数（仅当未满员时），防止并发超卖
        int updated = postMapper.incrementCurrentPeople(post.getId());
        if (updated == 0) {
            throw new BusinessException("名额已满，无法通过报名");
        }

        // 重新查询，检查是否满员并流转状态（条件更新，避免覆盖并发的取消/下架）
        post = postMapper.selectById(post.getId());
        if (post.getCurrentPeople() >= post.getMaxPeople()) {
            PostStateMachine.checkTransition(post.getStatus(), PostStatus.FULL);
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, post.getId())
                    .in(Post::getStatus, PostStatus.RECRUITING, PostStatus.FULL)
                    .set(Post::getStatus, PostStatus.FULL));
        }
        if (cacheEpoch != null) {
            cacheEpoch.bump();
        }

        // 通知报名者：审核通过
        User applicant = userMapper.selectById(apply.getApplicantId());
        if (applicant != null) {
            notificationService.send(apply.getApplicantId(), Notification.TYPE_APPLY_RESULT,
                    "报名已通过",
                    "你报名「" + post.getTitle() + "」已通过，记得按时参加！",
                    "/post/" + post.getId(), post.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long publisherId, Long applyId) {
        PostApply apply = postApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }

        // 校验是发布者
        Post post = postMapper.selectById(apply.getPostId());
        if (post == null || !post.getPublisherId().equals(publisherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有发布者才能审批报名");
        }

        // 先发后审：已下架帖子不可审批报名
        if (post.getIsAuditPass() != null && post.getIsAuditPass() == 2) {
            throw new BusinessException("帖子已被下架，暂不可审批报名");
        }

        // 校验报名状态为PENDING
        if (!ApplyStatus.PENDING.equals(apply.getStatus())) {
            throw new BusinessException("只能拒绝待审核的报名");
        }

        // 条件更新防并发重复拒绝
        int updatedCount = postApplyMapper.update(null, new LambdaUpdateWrapper<PostApply>()
                .eq(PostApply::getId, applyId)
                .eq(PostApply::getStatus, ApplyStatus.PENDING)
                .set(PostApply::getStatus, ApplyStatus.REJECTED)
                .set(PostApply::getHandleTime, LocalDateTime.now()));
        if (updatedCount == 0) {
            throw new BusinessException("该报名已被处理，请刷新后重试");
        }

        // 通知报名者：审核被拒
        User applicant = userMapper.selectById(apply.getApplicantId());
        if (applicant != null) {
            notificationService.send(apply.getApplicantId(), Notification.TYPE_APPLY_RESULT,
                    "报名未通过",
                    "很遗憾，你报名「" + post.getTitle() + "」未通过审核",
                    "/post/" + post.getId(), post.getId());
        }

        // 若被拒的是满员后递补上来的申请，继续尝试递补下一位候补者；若已无候补且未满员则回退为招募中
        if (PostStatus.FULL.equals(post.getStatus()) && post.getCurrentPeople() < post.getMaxPeople()) {
            boolean promoted = promoteWaitlist(post);
            if (!promoted) {
                postMapper.update(null, new LambdaUpdateWrapper<Post>()
                        .eq(Post::getId, post.getId())
                        .eq(Post::getStatus, PostStatus.FULL)
                        .set(Post::getStatus, PostStatus.RECRUITING));
                if (cacheEpoch != null) {
                    cacheEpoch.bump();
                }
            }
        }
    }

    @Override
    public List<ApplyVO> getApplyList(Long userId, Long postId) {
        // 校验：只有帖子发布者才能查看报名列表（防止越权查看他人帖子的报名信息）
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!post.getPublisherId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有发布者才能查看报名列表");
        }

        List<PostApply> applies = postApplyMapper.selectList(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .orderByDesc(PostApply::getApplyTime)
        );
        if (applies == null || applies.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询申请人信息（消除 N+1）
        Set<Long> applicantIds = new HashSet<>();
        for (PostApply apply : applies) {
            if (apply.getApplicantId() != null) {
                applicantIds.add(apply.getApplicantId());
            }
        }
        Map<Long, User> userMap = new HashMap<>();
        if (!applicantIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(applicantIds);
            if (users != null) {
                for (User u : users) {
                    if (u != null && u.getId() != null) {
                        userMap.put(u.getId(), u);
                    }
                }
            }
        }

        return applies.stream().map(apply -> {
            ApplyVO vo = toApplyVO(apply);
            User applicant = userMap.get(apply.getApplicantId());
            if (applicant != null) {
                vo.setApplicantNickname(applicant.getNickname());
                vo.setApplicantAvatar(applicant.getAvatar());
                vo.setApplicantCreditLevel(creditService.getCreditLevel(applicant.getCreditScore()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public IPage<ApplyVO> getReceivedApplies(Long userId, int page, int size) {
        // 查询当前用户发布的帖子列表
        List<Post> myPosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>().eq(Post::getPublisherId, userId)
        );
        if (myPosts.isEmpty()) {
            return new Page<>(page, size);
        }

        Map<Long, Post> postMap = new HashMap<>();
        List<Long> postIds = new ArrayList<>(myPosts.size());
        for (Post p : myPosts) {
            if (p != null && p.getId() != null) {
                postIds.add(p.getId());
                postMap.put(p.getId(), p);
            }
        }

        // 查询这些帖子的报名记录
        IPage<PostApply> applyPage = postApplyMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PostApply>()
                        .in(PostApply::getPostId, postIds)
                        .orderByDesc(PostApply::getApplyTime)
        );

        List<PostApply> records = applyPage.getRecords() != null ? applyPage.getRecords() : Collections.emptyList();
        Set<Long> applicantIds = new HashSet<>();
        for (PostApply apply : records) {
            if (apply.getApplicantId() != null) {
                applicantIds.add(apply.getApplicantId());
            }
        }
        Map<Long, User> userMap = new HashMap<>();
        if (!applicantIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(applicantIds);
            if (users != null) {
                for (User u : users) {
                    if (u != null && u.getId() != null) {
                        userMap.put(u.getId(), u);
                    }
                }
            }
        }

        // 转换为ApplyVO（复用 postMap 与批量 userMap，消除 N+1）
        IPage<ApplyVO> voPage = new Page<>(applyPage.getCurrent(), applyPage.getSize(), applyPage.getTotal());
        voPage.setRecords(records.stream().map(apply -> {
            ApplyVO vo = toApplyVO(apply);

            User applicant = userMap.get(apply.getApplicantId());
            if (applicant != null) {
                vo.setApplicantNickname(applicant.getNickname());
                vo.setApplicantAvatar(applicant.getAvatar());
                vo.setApplicantCreditLevel(creditService.getCreditLevel(applicant.getCreditScore()));
            }

            Post post = postMap.get(apply.getPostId());
            if (post != null) {
                vo.setPostTitle(post.getTitle());
            }

            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAttendance(Long publisherId, Long applyId, boolean attended) {
        PostApply apply = postApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }

        // 校验是帖子发布者
        Post post = postMapper.selectById(apply.getPostId());
        if (post == null || !post.getPublisherId().equals(publisherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有发布者才能标记出勤");
        }

        // 校验帖子状态为ONGOING或FINISHED
        if (!PostStatus.ONGOING.equals(post.getStatus()) && !PostStatus.FINISHED.equals(post.getStatus())) {
            throw new BusinessException("活动进行中或已结束时才能标记出勤");
        }

        // 出勤标记时间窗：
        // 1) 活动进行中不允许标记"未到场"（参与者可能仍在路上），只能标记已出勤
        // 2) 活动结束后 24 小时内必须完成标记，避免发布者事后无限期追溯扣分
        if (PostStatus.ONGOING.equals(post.getStatus()) && !attended) {
            throw new BusinessException("活动进行中，暂不能标记未到场，请活动结束后再操作");
        }
        if (PostStatus.FINISHED.equals(post.getStatus())) {
            LocalDateTime markDeadline = post.getEndTime().plusHours(24);
            if (LocalDateTime.now().isAfter(markDeadline)) {
                throw new BusinessException("活动结束超过24小时，无法再标记出勤状态");
            }
        }

        // 校验报名状态为APPROVED
        if (!ApplyStatus.APPROVED.equals(apply.getStatus())) {
            throw new BusinessException("只能标记已通过的报名出勤状态");
        }

        // 更新出勤状态（条件更新防并发重复标记）
        int updatedCount = postApplyMapper.update(null, new LambdaUpdateWrapper<PostApply>()
                .eq(PostApply::getId, applyId)
                .eq(PostApply::getStatus, ApplyStatus.APPROVED)
                .set(PostApply::getStatus, attended ? ApplyStatus.ATTENDED : ApplyStatus.NOSHOW)
                .set(PostApply::getHandleTime, LocalDateTime.now()));
        if (updatedCount == 0) {
            throw new BusinessException("该报名状态已变更，请刷新后重试");
        }

        // 未到场扣信誉分10分（写台账+即时重算：实时生效且不被每日重算冲掉）
        if (!attended) {
            int newScore = creditService.recordPenalty(apply.getApplicantId(),
                    CreditConstant.NOSHOW_PENALTY, CreditLog.REASON_NOSHOW, CreditLog.BIZ_TYPE_APPLY, applyId);
            // 通知被扣分用户：信誉分变化必须可感知，避免用户莫名掉分
            notificationService.send(apply.getApplicantId(), Notification.TYPE_SYSTEM,
                    "信誉分扣减通知",
                    "你在组局「" + post.getTitle() + "」中被标记为未到场，信誉分 -10（当前 " + newScore + " 分）",
                    "/post/" + post.getId(), post.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkinByCode(Long userId, Long postId, String code) {
        if (code == null || !code.trim().matches("^\\d{4}$")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请输入4位数字签到码");
        }
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        // 允许在活动开始前30分钟内、进行中(ONGOING)或结束后24小时内自助签到
        LocalDateTime now = LocalDateTime.now();
        boolean inTimeWindow = PostStatus.ONGOING.equals(post.getStatus())
                || (PostStatus.FINISHED.equals(post.getStatus())
                        && post.getEndTime() != null && !now.isAfter(post.getEndTime().plusHours(24)))
                || ((PostStatus.RECRUITING.equals(post.getStatus()) || PostStatus.FULL.equals(post.getStatus()))
                        && post.getStartTime() != null && !now.isBefore(post.getStartTime().minusMinutes(30)));
        if (!inTimeWindow) {
            throw new BusinessException("仅可在活动开始前30分钟至活动结束后24小时内签到");
        }

        String expectedCode = PostStateMachine.generateCheckinCode(post.getId(), post.getPublisherId(), post.getStartTime());
        if (!expectedCode.equals(code.trim())) {
            throw new BusinessException("签到码不正确，请向发布者确认4位活动签到码");
        }

        PostApply myApply = postApplyMapper.selectOne(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .eq(PostApply::getApplicantId, userId)
                        .last("LIMIT 1")
        );
        if (myApply == null) {
            throw new BusinessException("您未报名该活动");
        }
        if (ApplyStatus.ATTENDED.equals(myApply.getStatus())) {
            return; // 幂等：已签到直接成功
        }
        if (!ApplyStatus.APPROVED.equals(myApply.getStatus())) {
            throw new BusinessException("仅已通过审核的报名者可签到");
        }

        int updated = postApplyMapper.update(null, new LambdaUpdateWrapper<PostApply>()
                .eq(PostApply::getId, myApply.getId())
                .eq(PostApply::getStatus, ApplyStatus.APPROVED)
                .set(PostApply::getStatus, ApplyStatus.ATTENDED)
                .set(PostApply::getHandleTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new BusinessException("报名状态已变更，请刷新后重试");
        }
    }

    /** ApplyVO 基础字段拷贝 */
    private ApplyVO toApplyVO(PostApply apply) {
        ApplyVO vo = new ApplyVO();
        vo.setId(apply.getId());
        vo.setPostId(apply.getPostId());
        vo.setApplicantId(apply.getApplicantId());
        vo.setStatus(apply.getStatus());
        vo.setApplyTime(apply.getApplyTime());
        vo.setHandleTime(apply.getHandleTime());
        vo.setRemark(apply.getRemark());
        return vo;
    }

    /**
     * 解析帖子tagIds为标签ID列表（逗号分隔）
     */
    private List<Long> parseTagIds(String tagIds) {
        List<Long> ids = new ArrayList<>();
        if (tagIds == null || tagIds.isEmpty()) {
            return ids;
        }
        for (String idStr : tagIds.split(",")) {
            try {
                ids.add(Long.parseLong(idStr.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    /**
     * 段位数字转名称
     */
    private String levelName(Integer level) {
        if (level == null) {
            return "不限";
        }
        switch (level) {
            case 1: return "萌新";
            case 2: return "小白";
            case 3: return "熟手";
            case 4: return "高手";
            case 5: return "宗师";
            default: return String.valueOf(level);
        }
    }
}
