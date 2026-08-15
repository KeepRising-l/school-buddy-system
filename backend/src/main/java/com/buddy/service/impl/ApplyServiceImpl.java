package com.buddy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.CreditConstant;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.common.statemachine.PostStateMachine;
import com.buddy.dto.response.ApplyVO;
import com.buddy.entity.Post;
import com.buddy.entity.PostApply;
import com.buddy.entity.User;
import com.buddy.entity.UserTag;
import com.buddy.entity.Notification;
import com.buddy.mapper.PostApplyMapper;
import com.buddy.mapper.PostMapper;
import com.buddy.mapper.UserMapper;
import com.buddy.mapper.UserTagMapper;
import com.buddy.service.ApplyService;
import com.buddy.service.CreditService;
import com.buddy.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 报名服务实现类
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long userId, Long postId) {
        // 校验帖子存在且状态为RECRUITING/FULL（FULL 时走候补逻辑）
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (PostStatus.FULL.equals(post.getStatus())) {
            // V1.1 候补机制：满员时调用候补报名
            doWaitlist(userId, post);
            return;
        }
        if (!PostStatus.RECRUITING.equals(post.getStatus())) {
            throw new BusinessException(ResultCode.POST_NOT_RECRUITING);
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

        // 插入或复用旧报名记录（修复：取消/被拒后再次报名撞唯一键 uk_post_applicant 报 500）
        PostApply apply;
        if (existing != null) {
            apply = existing;
            apply.setStatus(ApplyStatus.PENDING);
            apply.setApplyTime(LocalDateTime.now());
            apply.setHandleTime(null);
            apply.setRemark(null);
            postApplyMapper.updateById(apply);
        } else {
            apply = new PostApply();
            apply.setPostId(postId);
            apply.setApplicantId(userId);
            apply.setStatus(ApplyStatus.PENDING);
            apply.setApplyTime(LocalDateTime.now());
            postApplyMapper.insert(apply);
        }

        // 通知发布者：有新的报名申请（V1.1 通知中心，与业务同一事务）
        notificationService.send(post.getPublisherId(), Notification.TYPE_APPLY_NEW,
                "新的报名申请",
                applicant.getNickname() + " 报名了你的组局「" + post.getTitle() + "」",
                "/post/" + postId, postId);
    }

    /**
     * 校验报名资格：信誉分 + 段位 + 性别（报名与候补共用）
     *
     * <p>段位校验说明：不再只检查第一个标签，而是检查用户在帖子任一标签上的最高段位；
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
     * 候补报名（V1.1）：帖子已满员时进入候补队列
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

        // 插入或复用旧记录（修复：取消/被拒后再次候补撞唯一键报 500）
        PostApply apply;
        if (existing != null) {
            apply = existing;
            apply.setStatus(ApplyStatus.WAITLIST);
            apply.setApplyTime(LocalDateTime.now());
            apply.setHandleTime(null);
            apply.setRemark(null);
            postApplyMapper.updateById(apply);
        } else {
            apply = new PostApply();
            apply.setPostId(post.getId());
            apply.setApplicantId(userId);
            apply.setStatus(ApplyStatus.WAITLIST);
            apply.setApplyTime(LocalDateTime.now());
            postApplyMapper.insert(apply);
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
     * 自动递补（V1.1）：APPROVED 报名取消后，候补队列第一位转 PENDING 并通知
     *
     * @param post 相关帖子
     */
    @Transactional(rollbackFor = Exception.class)
    public void promoteWaitlist(Post post) {
        // 查询候补队列第一位（按候补时间升序）
        PostApply firstWait = postApplyMapper.selectOne(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, post.getId())
                        .eq(PostApply::getStatus, ApplyStatus.WAITLIST)
                        .orderByAsc(PostApply::getApplyTime)
                        .last("LIMIT 1")
        );
        if (firstWait == null) {
            return;
        }

        // 递补为待审核
        firstWait.setStatus(ApplyStatus.PENDING);
        firstWait.setHandleTime(LocalDateTime.now());
        postApplyMapper.updateById(firstWait);

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

        // 校验状态可取消（PENDING/APPROVED/WAITLIST 才能取消，V1.1 支持候补取消）
        String originalStatus = apply.getStatus();
        if (!ApplyStatus.PENDING.equals(originalStatus)
                && !ApplyStatus.APPROVED.equals(originalStatus)
                && !ApplyStatus.WAITLIST.equals(originalStatus)) {
            throw new BusinessException("当前报名状态不可取消");
        }

        // 校验帖子状态：只有招募中/已满员才能取消（CAN-01；进行中/已结束不可取消 CAN-04）
        Post post = postMapper.selectById(apply.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!PostStatus.RECRUITING.equals(post.getStatus()) && !PostStatus.FULL.equals(post.getStatus())) {
            throw new BusinessException("活动已开始或已结束，无法取消报名");
        }

        // 更新报名状态
        apply.setStatus(ApplyStatus.CANCELLED);
        apply.setHandleTime(LocalDateTime.now());
        postApplyMapper.updateById(apply);

        // 已通过（APPROVED）的报名取消后：人数-1，若满员则回退为招募中（原子操作）
        if (ApplyStatus.APPROVED.equals(originalStatus)) {
            postMapper.decrementCurrentPeople(post.getId());
            post = postMapper.selectById(post.getId());

            // V1.1 候补递补：满员时有候补则先递补（递补后仍是 PENDING，人数不变）
            if (PostStatus.FULL.equals(post.getStatus())) {
                promoteWaitlist(post);
                // 递补后重新查询（递补不改变 current_people，若无人候补则回退）
                post = postMapper.selectById(post.getId());
                if (post.getCurrentPeople() < post.getMaxPeople()) {
                    post.setStatus(PostStatus.RECRUITING);
                    postMapper.updateById(post);
                }
            }
        }

        // 活动开始前1小时内取消报名，扣除信誉分5分（CAN-03，窗口为 [开始前1小时, 开始时间]）
        if (post.getStartTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = post.getStartTime();
            if (now.isAfter(startTime.minusHours(1)) && now.isBefore(startTime)) {
                User applicant = userMapper.selectById(userId);
                if (applicant != null) {
                    int newScore = Math.max(CreditConstant.MIN_SCORE,
                            applicant.getCreditScore() + CreditConstant.CANCEL_LAST_MINUTE);
                    applicant.setCreditScore(newScore);
                    userMapper.updateById(applicant);
                }
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

        // 校验报名状态为PENDING
        if (!ApplyStatus.PENDING.equals(apply.getStatus())) {
            throw new BusinessException("只能审批待审核的报名");
        }

        // 更新报名状态为APPROVED
        apply.setStatus(ApplyStatus.APPROVED);
        apply.setHandleTime(LocalDateTime.now());
        postApplyMapper.updateById(apply);

        // 原子增加人数（仅当未满员时），防止并发超卖
        int updated = postMapper.incrementCurrentPeople(post.getId());
        if (updated == 0) {
            throw new BusinessException("名额已满，无法通过报名");
        }

        // 重新查询，检查是否满员并流转状态
        post = postMapper.selectById(post.getId());
        if (post.getCurrentPeople() >= post.getMaxPeople()) {
            PostStateMachine.checkTransition(post.getStatus(), PostStatus.FULL);
            post.setStatus(PostStatus.FULL);
            postMapper.updateById(post);
        }

        // 通知报名者：审核通过（V1.1 通知中心）
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

        // 校验报名状态为PENDING
        if (!ApplyStatus.PENDING.equals(apply.getStatus())) {
            throw new BusinessException("只能拒绝待审核的报名");
        }

        // 更新报名状态为REJECTED
        apply.setStatus(ApplyStatus.REJECTED);
        apply.setHandleTime(LocalDateTime.now());
        postApplyMapper.updateById(apply);

        // 通知报名者：审核被拒（V1.1 通知中心）
        User applicant = userMapper.selectById(apply.getApplicantId());
        if (applicant != null) {
            notificationService.send(apply.getApplicantId(), Notification.TYPE_APPLY_RESULT,
                    "报名未通过",
                    "很遗憾，你报名「" + post.getTitle() + "」未通过审核",
                    "/post/" + post.getId(), post.getId());
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
        return applies.stream().map(apply -> {
            ApplyVO vo = new ApplyVO();
            vo.setId(apply.getId());
            vo.setPostId(apply.getPostId());
            vo.setApplicantId(apply.getApplicantId());
            vo.setStatus(apply.getStatus());
            vo.setApplyTime(apply.getApplyTime());
            vo.setHandleTime(apply.getHandleTime());
            vo.setRemark(apply.getRemark());

            // 查询申请人信息
            User applicant = userMapper.selectById(apply.getApplicantId());
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
        // 查询当前用户发布的帖子ID列表
        List<Post> myPosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>().eq(Post::getPublisherId, userId)
        );
        if (myPosts.isEmpty()) {
            return new Page<>(page, size);
        }

        List<Long> postIds = myPosts.stream().map(Post::getId).collect(java.util.stream.Collectors.toList());

        // 查询这些帖子的报名记录
        IPage<PostApply> applyPage = postApplyMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PostApply>()
                        .in(PostApply::getPostId, postIds)
                        .orderByDesc(PostApply::getApplyTime)
        );

        // 转换为ApplyVO
        IPage<ApplyVO> voPage = new Page<>(applyPage.getCurrent(), applyPage.getSize(), applyPage.getTotal());
        voPage.setRecords(applyPage.getRecords().stream().map(apply -> {
            ApplyVO vo = new ApplyVO();
            vo.setId(apply.getId());
            vo.setPostId(apply.getPostId());
            vo.setApplicantId(apply.getApplicantId());
            vo.setStatus(apply.getStatus());
            vo.setApplyTime(apply.getApplyTime());
            vo.setHandleTime(apply.getHandleTime());
            vo.setRemark(apply.getRemark());

            // 查询申请人信息
            User applicant = userMapper.selectById(apply.getApplicantId());
            if (applicant != null) {
                vo.setApplicantNickname(applicant.getNickname());
                vo.setApplicantAvatar(applicant.getAvatar());
                vo.setApplicantCreditLevel(creditService.getCreditLevel(applicant.getCreditScore()));
            }

            // 查询帖子标题
            Post post = postMapper.selectById(apply.getPostId());
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

        // 校验报名状态为APPROVED
        if (!ApplyStatus.APPROVED.equals(apply.getStatus())) {
            throw new BusinessException("只能标记已通过的报名出勤状态");
        }

        // 更新出勤状态
        apply.setStatus(attended ? ApplyStatus.ATTENDED : ApplyStatus.NOSHOW);
        apply.setHandleTime(LocalDateTime.now());
        postApplyMapper.updateById(apply);

        // 未到场扣信誉分10分
        if (!attended) {
            User applicant = userMapper.selectById(apply.getApplicantId());
            if (applicant != null) {
                int newScore = Math.max(CreditConstant.MIN_SCORE,
                        applicant.getCreditScore() + CreditConstant.NOSHOW_PENALTY);
                applicant.setCreditScore(newScore);
                userMapper.updateById(applicant);

                // V1.2 通知被扣分用户：信誉分变化必须可感知，避免用户莫名掉分
                notificationService.send(applicant.getId(), Notification.TYPE_SYSTEM,
                        "信誉分扣减通知",
                        "你在组局「" + post.getTitle() + "」中被标记为未到场，信誉分 -10（当前 " + newScore + " 分）",
                        "/post/" + post.getId(), post.getId());
            }
        }
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
            case 1: return "青铜";
            case 2: return "白银";
            case 3: return "黄金";
            case 4: return "钻石";
            case 5: return "王者";
            default: return String.valueOf(level);
        }
    }
}
