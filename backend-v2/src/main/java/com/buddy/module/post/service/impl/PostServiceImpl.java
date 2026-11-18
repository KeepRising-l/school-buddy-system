package com.buddy.module.post.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.buddy.common.cache.CacheEpoch;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.common.statemachine.PostStateMachine;
import com.buddy.common.store.AuxStore;
import com.buddy.module.apply.entity.PostApply;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.credit.service.CreditService;
import com.buddy.module.notification.entity.Notification;
import com.buddy.module.notification.service.NotificationService;
import com.buddy.module.post.dto.PostDetailVO;
import com.buddy.module.post.dto.request.PostCreateRequest;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.post.service.PostService;
import com.buddy.module.post.service.SensitiveWordService;
import com.buddy.module.tag.entity.InterestTag;
import com.buddy.module.tag.entity.UserTag;
import com.buddy.module.tag.mapper.InterestTagMapper;
import com.buddy.module.tag.mapper.UserTagMapper;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 帖子服务实现类（等价迁移自旧版）
 */
@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostApplyMapper postApplyMapper;

    @Autowired
    private UserTagMapper userTagMapper;

    @Autowired
    private InterestTagMapper interestTagMapper;

    @Autowired
    private CreditService creditService;

    @Autowired
    private AuxStore auxStore;

    /** 全局缓存纪元：帖子集合变化后使推荐缓存失效 */
    @Autowired
    private CacheEpoch cacheEpoch;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    /** 浏览量去重缓存前缀 */
    private static final String VIEW_COUNT_KEY = "post:view:";

    /** 去重时间窗口（分钟） */
    private static final long VIEW_DEDUP_MINUTES = 10;

    /** 同时进行中帖子上限 */
    private static final int MAX_ACTIVE_POSTS = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(Long userId, PostCreateRequest request) {
        // 校验信誉分
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (creditService.isLowCredit(user.getCreditScore())) {
            throw new BusinessException(ResultCode.CREDIT_TOO_LOW);
        }

        // 违禁词检测（先发后审第一道防线）
        List<String> hitWords = sensitiveWordService.check(
                request.getTitle(),
                request.getDescription(),
                request.getLocation()
        );
        if (!hitWords.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "内容包含违禁词：" + String.join("、", hitWords) + "，请修改后重新发布");
        }

        // 校验时间合法性（不校验会导致帖子刚发布就被定时任务置为进行中/已结束）
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始/结束时间不能为空");
        }
        if (!request.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始时间必须晚于当前时间");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        // 用精确比较判断是否超过24小时（toHours() 会截断，24小时59分会漏放）
        if (request.getEndTime().isAfter(request.getStartTime().plusHours(24))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "活动时长不能超过24小时");
        }

        // 校验段位/性别要求范围
        if (request.getRequireLevel() != null && (request.getRequireLevel() < 1 || request.getRequireLevel() > 5)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "段位要求不合法（1-5）");
        }
        if (request.getRequireGender() != null && (request.getRequireGender() < 0 || request.getRequireGender() > 2)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "性别要求不合法（0-2）");
        }

        // 并发防护：锁定用户行，串行化同一用户"计数+插入"，避免并发请求绕过上限
        userMapper.lockById(userId);

        // 校验同时进行中的帖子数量
        Long activeCount = postMapper.selectCount(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getPublisherId, userId)
                        .in(Post::getStatus, PostStatus.RECRUITING, PostStatus.FULL, PostStatus.ONGOING)
        );
        if (activeCount >= MAX_ACTIVE_POSTS) {
            throw new BusinessException("同时进行中的帖子不能超过" + MAX_ACTIVE_POSTS + "个");
        }

        // 构建帖子实体
        Post post = new Post();
        post.setPublisherId(userId);
        post.setTitle(request.getTitle());
        post.setCategory(request.getCategory());
        post.setTagIds(request.getTagIds());
        post.setStartTime(request.getStartTime());
        post.setEndTime(request.getEndTime());
        post.setLocation(request.getLocation());
        post.setMaxPeople(request.getMaxPeople());
        post.setCurrentPeople(1); // 发布者自己算1人
        post.setRequireLevel(request.getRequireLevel());
        post.setRequireGender(request.getRequireGender());
        post.setDescription(request.getDescription());
        post.setStatus(PostStatus.RECRUITING);
        post.setViewCount(0);
        post.setLikeCount(0);
        // 先发后审：通过违禁词检测后直接发布（1），违规内容靠举报兜底
        post.setIsAuditPass(1);

        postMapper.insert(post);
        // 让推荐列表缓存失效：否则用户刚发完帖回到广场，5 分钟内看不到自己发的帖子
        cacheEpoch.bump();
        return post.getId();
    }

    @Override
    public PostDetailVO getPostDetail(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        // 先发后审：被下架的帖子（is_audit_pass=2）仅发布者可查看，他人视为不存在
        if (post.getIsAuditPass() != null && post.getIsAuditPass() == 2) {
            boolean isOwner = userId != null && post.getPublisherId().equals(userId);
            if (!isOwner) {
                throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
            }
        }

        // 查询发布者信息
        User publisher = userMapper.selectById(post.getPublisherId());

        // 查询已通过报名的用户列表（不返回 PENDING 待审核用户，保护隐私且与人数一致）
        List<PostApply> applies = postApplyMapper.selectList(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .eq(PostApply::getStatus, ApplyStatus.APPROVED)
        );

        // 批量查询报名用户信息（避免每条报名一次 selectById）
        Set<Long> applicantIds = new HashSet<>();
        for (PostApply apply : applies) {
            if (apply.getApplicantId() != null) {
                applicantIds.add(apply.getApplicantId());
            }
        }
        Map<Long, User> applyUserMap = applicantIds.isEmpty()
                ? Collections.emptyMap()
                : toUserMap(userMapper.selectBatchIds(applicantIds));

        // 组装报名用户信息
        // 联系方式可见性（缺口#5）：仅"发布者本人"与"已通过报名（APPROVED/ATTENDED）的参与者"
        // 可以看到彼此的联系方式，用于成行后互相联系；游客/未通过者一律看不到。
        boolean isOwner = userId != null && post.getPublisherId().equals(userId);

        // 判断当前用户是否已报名（在组装报名用户前完成，联系方式可见性依赖它）
        boolean hasApplied = false;
        String applyStatus = null;
        Long myApplyId = null;
        if (userId != null) {
            PostApply myApply = postApplyMapper.selectOne(
                    new LambdaQueryWrapper<PostApply>()
                            .eq(PostApply::getPostId, postId)
                            .eq(PostApply::getApplicantId, userId)
                            .last("LIMIT 1")
            );
            if (myApply != null) {
                hasApplied = true;
                applyStatus = myApply.getStatus();
                myApplyId = myApply.getId();
            }
        }
        boolean contactVisible = isOwner
                || ApplyStatus.APPROVED.equals(applyStatus)
                || ApplyStatus.ATTENDED.equals(applyStatus);

        List<PostDetailVO.ApplyUserVO> applyUsers = new ArrayList<>();
        for (PostApply apply : applies) {
            User applyUser = apply.getApplicantId() != null ? applyUserMap.get(apply.getApplicantId()) : null;
            if (applyUser != null) {
                PostDetailVO.ApplyUserVO vo = new PostDetailVO.ApplyUserVO();
                vo.setUserId(applyUser.getId());
                vo.setNickname(applyUser.getNickname());
                vo.setAvatar(applyUser.getAvatar());
                vo.setCreditLevel(creditService.getCreditLevel(applyUser.getCreditScore()));
                // 可见时返回手机号，否则保持 null（前端显示"报名通过后可见"）
                if (contactVisible) {
                    vo.setContact(applyUser.getPhone());
                }
                applyUsers.add(vo);
            }
        }

        // 组装PostDetailVO
        PostDetailVO detailVO = new PostDetailVO();
        detailVO.setId(post.getId());
        detailVO.setTitle(post.getTitle());
        detailVO.setCategory(post.getCategory());
        detailVO.setTagIds(post.getTagIds());
        detailVO.setStartTime(post.getStartTime());
        detailVO.setEndTime(post.getEndTime());
        detailVO.setLocation(post.getLocation());
        detailVO.setMaxPeople(post.getMaxPeople());
        detailVO.setCurrentPeople(post.getCurrentPeople());
        detailVO.setStatus(post.getStatus());
        detailVO.setViewCount(post.getViewCount());
        detailVO.setPublisherId(post.getPublisherId());
        detailVO.setCreatedTime(post.getCreatedTime());
        detailVO.setRequireLevel(post.getRequireLevel());
        detailVO.setRequireGender(post.getRequireGender());
        detailVO.setDescription(post.getDescription());
        detailVO.setIsAuditPass(post.getIsAuditPass());
        detailVO.setApplyUsers(applyUsers);
        detailVO.setHasApplied(hasApplied);
        detailVO.setApplyStatus(applyStatus);
        detailVO.setMyApplyId(myApplyId);
        // 联系方式（缺口#5）：仅发布者与已通过报名者可见；发布者看自己时也返回，便于确认资料
        detailVO.setContactVisible(contactVisible);
        if (contactVisible && publisher != null) {
            detailVO.setPublisherContact(publisher.getPhone());
        }

        if (publisher != null) {
            detailVO.setPublisherNickname(publisher.getNickname());
            detailVO.setPublisherAvatar(publisher.getAvatar());
            detailVO.setPublisherCreditLevel(creditService.getCreditLevel(publisher.getCreditScore()));
            detailVO.setPublisherSignature(publisher.getSignature());
            detailVO.setPublisherCollege(publisher.getCollege());

            // 查询发布者的兴趣标签（批量查询 InterestTag，消除 N+1）
            List<UserTag> publisherUserTags = userTagMapper.selectList(
                    new LambdaQueryWrapper<UserTag>().eq(UserTag::getUserId, publisher.getId())
            );
            List<PostDetailVO.PublisherTagVO> publisherTags = new ArrayList<>();
            if (publisherUserTags != null && !publisherUserTags.isEmpty()) {
                Set<Long> pubTagIds = new HashSet<>();
                for (UserTag ut : publisherUserTags) {
                    if (ut.getTagId() != null) {
                        pubTagIds.add(ut.getTagId());
                    }
                }
                Map<Long, InterestTag> tagMap = new HashMap<>();
                if (!pubTagIds.isEmpty()) {
                    List<InterestTag> tags = interestTagMapper.selectBatchIds(pubTagIds);
                    if (tags != null) {
                        for (InterestTag t : tags) {
                            if (t != null && t.getId() != null) {
                                tagMap.put(t.getId(), t);
                            }
                        }
                    }
                }
                for (UserTag ut : publisherUserTags) {
                    InterestTag tag = tagMap.get(ut.getTagId());
                    if (tag != null && tag.getStatus() != null && tag.getStatus() == 1) {
                        PostDetailVO.PublisherTagVO tagVO = new PostDetailVO.PublisherTagVO();
                        tagVO.setTagId(tag.getId());
                        tagVO.setTagName(tag.getName());
                        tagVO.setCategory(tag.getCategory());
                        tagVO.setSelfLevel(ut.getSelfLevel());
                        publisherTags.add(tagVO);
                    }
                }
            }
            detailVO.setPublisherTags(publisherTags);
        }

        // 可评价用户列表：已出勤（ATTENDED）的参与者，供发布者评价
        List<PostApply> attendedApplies = postApplyMapper.selectList(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .eq(PostApply::getStatus, ApplyStatus.ATTENDED)
        );
        // 批量查询可评价用户信息（避免每条一次 selectById）
        Set<Long> attendeeIds = new HashSet<>();
        for (PostApply apply : attendedApplies) {
            if (apply.getApplicantId() != null) {
                attendeeIds.add(apply.getApplicantId());
            }
        }
        Map<Long, User> attendeeMap = attendeeIds.isEmpty()
                ? Collections.emptyMap()
                : toUserMap(userMapper.selectBatchIds(attendeeIds));
        List<PostDetailVO.ApplyUserVO> reviewTargets = new ArrayList<>();
        for (PostApply apply : attendedApplies) {
            User attendee = apply.getApplicantId() != null ? attendeeMap.get(apply.getApplicantId()) : null;
            if (attendee != null) {
                PostDetailVO.ApplyUserVO vo = new PostDetailVO.ApplyUserVO();
                vo.setUserId(attendee.getId());
                vo.setNickname(attendee.getNickname());
                vo.setAvatar(attendee.getAvatar());
                vo.setCreditLevel(creditService.getCreditLevel(attendee.getCreditScore()));
                reviewTargets.add(vo);
            }
        }
        detailVO.setReviewTargets(reviewTargets);

        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        // 校验是发布者
        if (!post.getPublisherId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能取消自己发布的帖子");
        }

        // 状态机预检（给出明确的错误文案）
        PostStateMachine.checkTransition(post.getStatus(), PostStatus.CANCELLED);

        // 带预期旧状态的条件更新：并发下（如管理员同时下架）只有一次能成功，
        // 避免先查后改覆盖并发状态、重复执行副作用
        int updated = postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .in(Post::getStatus, PostStatus.RECRUITING, PostStatus.FULL, PostStatus.ONGOING)
                .set(Post::getStatus, PostStatus.CANCELLED));
        if (updated == 0) {
            throw new BusinessException(ResultCode.STATUS_TRANSIT_ERROR, "帖子状态已变化，请刷新后重试");
        }

        // 帖子集合已变化：让推荐列表缓存立即失效（否则广场上这条帖子还会停留最多 5 分钟）
        cacheEpoch.bump();

        // 联动（D-008）：取消所有未完结的报名（PENDING/APPROVED/WAITLIST）。
        // 副作用只在状态转换成功后执行；每条报名同样用条件更新防止并发重复处理
        List<PostApply> applies = postApplyMapper.selectList(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .in(PostApply::getStatus, ApplyStatus.PENDING, ApplyStatus.APPROVED, ApplyStatus.WAITLIST)
        );
        for (PostApply apply : applies) {
            int applyUpdated = postApplyMapper.update(null, new LambdaUpdateWrapper<PostApply>()
                    .eq(PostApply::getId, apply.getId())
                    .in(PostApply::getStatus, ApplyStatus.PENDING, ApplyStatus.APPROVED, ApplyStatus.WAITLIST)
                    .set(PostApply::getStatus, ApplyStatus.CANCELLED)
                    .set(PostApply::getHandleTime, LocalDateTime.now()));
            if (applyUpdated == 0) {
                continue; // 已被并发处理（如用户自己取消），跳过
            }
            if (ApplyStatus.APPROVED.equals(apply.getStatus())) {
                postMapper.decrementCurrentPeople(postId);
            }
            // 通知该用户：组局已取消
            notificationService.send(apply.getApplicantId(), Notification.TYPE_POST_CANCEL,
                    "组局已取消",
                    "你参加的组局「" + post.getTitle() + "」已被取消",
                    "/post/" + postId, postId);
        }
    }

    @Override
    public void incrementViewCount(Long postId, Long userId) {
        // 游客未登录时按匿名窗口处理，登录用户按 10 分钟去重（辅助存储，默认 MySQL，可切换 Redis）
        String suffix = userId != null ? String.valueOf(userId) : "guest";
        String key = VIEW_COUNT_KEY + postId + ":" + suffix;
        try {
            boolean isNew = auxStore.setIfAbsent(key, Duration.ofMinutes(VIEW_DEDUP_MINUTES));
            if (isNew) {
                postMapper.incrementViewCount(postId);
            }
        } catch (Exception e) {
            // 辅助存储不可用，直接原子+1浏览量（不去重）
            postMapper.incrementViewCount(postId);
        }
    }

    /** 批量查询 Map<id, User>（避免 N+1） */
    private static Map<Long, User> toUserMap(List<User> users) {
        Map<Long, User> map = new HashMap<>();
        if (users != null) {
            for (User u : users) {
                if (u.getId() != null) {
                    map.put(u.getId(), u);
                }
            }
        }
        return map;
    }
}
