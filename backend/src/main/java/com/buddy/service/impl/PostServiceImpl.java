package com.buddy.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.CreditConstant;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.common.statemachine.PostStateMachine;
import com.buddy.dto.request.PostCreateRequest;
import com.buddy.dto.response.PostDetailVO;
import com.buddy.dto.response.PostVO;
import com.buddy.entity.Post;
import com.buddy.entity.PostApply;
import com.buddy.entity.User;
import com.buddy.entity.UserTag;
import com.buddy.entity.InterestTag;
import com.buddy.entity.Notification;
import com.buddy.mapper.InterestTagMapper;
import com.buddy.mapper.PostApplyMapper;
import com.buddy.mapper.PostMapper;
import com.buddy.mapper.UserMapper;
import com.buddy.mapper.UserTagMapper;
import com.buddy.service.CreditService;
import com.buddy.service.NotificationService;
import com.buddy.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 帖子服务实现类
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
    private StringRedisTemplate redisTemplate;

    @Autowired
    private NotificationService notificationService;

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
        // V1.1 先审后发：发布后默认待审核（0），审核通过后才在广场展示
        post.setIsAuditPass(0);

        postMapper.insert(post);
        return post.getId();
    }

    @Override
    public PostDetailVO getPostDetail(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        // 查询发布者信息
        User publisher = userMapper.selectById(post.getPublisherId());

        // 查询已通过和待审核的报名用户列表
        List<PostApply> applies = postApplyMapper.selectList(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .in(PostApply::getStatus, ApplyStatus.APPROVED, ApplyStatus.PENDING)
        );

        // 组装报名用户信息
        List<PostDetailVO.ApplyUserVO> applyUsers = new ArrayList<>();
        for (PostApply apply : applies) {
            User applyUser = userMapper.selectById(apply.getApplicantId());
            if (applyUser != null) {
                PostDetailVO.ApplyUserVO vo = new PostDetailVO.ApplyUserVO();
                vo.setUserId(applyUser.getId());
                vo.setNickname(applyUser.getNickname());
                vo.setAvatar(applyUser.getAvatar());
                vo.setCreditLevel(creditService.getCreditLevel(applyUser.getCreditScore()));
                applyUsers.add(vo);
            }
        }

        // 判断当前用户是否已报名
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

        if (publisher != null) {
            detailVO.setPublisherNickname(publisher.getNickname());
            detailVO.setPublisherAvatar(publisher.getAvatar());
            detailVO.setPublisherCreditLevel(creditService.getCreditLevel(publisher.getCreditScore()));
            detailVO.setPublisherSignature(publisher.getSignature());
            detailVO.setPublisherCollege(publisher.getCollege());

            // 查询发布者的兴趣标签
            List<UserTag> publisherUserTags = userTagMapper.selectList(
                    new LambdaQueryWrapper<UserTag>().eq(UserTag::getUserId, publisher.getId())
            );
            List<PostDetailVO.PublisherTagVO> publisherTags = new ArrayList<>();
            for (UserTag ut : publisherUserTags) {
                InterestTag tag = interestTagMapper.selectById(ut.getTagId());
                if (tag != null && tag.getStatus() == 1) {
                    PostDetailVO.PublisherTagVO tagVO = new PostDetailVO.PublisherTagVO();
                    tagVO.setTagId(tag.getId());
                    tagVO.setTagName(tag.getName());
                    tagVO.setCategory(tag.getCategory());
                    tagVO.setSelfLevel(ut.getSelfLevel());
                    publisherTags.add(tagVO);
                }
            }
            detailVO.setPublisherTags(publisherTags);
        }

        return detailVO;
    }

    @Override
    public IPage<PostVO> getPostList(int page, int size, String category, String sort) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .ne(Post::getStatus, PostStatus.CANCELLED)
                // 只展示已审核通过的帖子
                .eq(Post::getIsAuditPass, 1);

        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(Post::getCategory, category);
        }

        // 排序方式（热度 = 报名人数，PRD SQ-04）
        if ("hot".equals(sort)) {
            wrapper.orderByDesc(Post::getCurrentPeople);
        } else {
            // 默认按时间倒序
            wrapper.orderByDesc(Post::getCreatedTime);
        }

        IPage<Post> postPage = postMapper.selectPage(new Page<>(page, size), wrapper);

        // 转换为PostVO
        IPage<PostVO> voPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        voPage.setRecords(postPage.getRecords().stream()
                .map(this::convertToPostVO)
                .collect(Collectors.toList()));
        return voPage;
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

        // 状态机校验
        PostStateMachine.checkTransition(post.getStatus(), PostStatus.CANCELLED);

        // 更新状态
        post.setStatus(PostStatus.CANCELLED);
        postMapper.updateById(post);

        // 通知所有已报名用户：组局已取消（V1.1 通知中心）
        List<PostApply> applies = postApplyMapper.selectList(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .in(PostApply::getStatus, ApplyStatus.PENDING, ApplyStatus.APPROVED)
        );
        for (PostApply apply : applies) {
            notificationService.send(apply.getApplicantId(), Notification.TYPE_POST_CANCEL,
                    "组局已取消",
                    "你参加的组局「" + post.getTitle() + "」已被取消",
                    "/post/" + postId, postId);
        }
    }

    @Override
    public void incrementViewCount(Long postId, Long userId) {
        // Redis去重：10分钟内同一用户只计一次
        String key = VIEW_COUNT_KEY + postId + ":" + userId;
        try {
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", VIEW_DEDUP_MINUTES, TimeUnit.MINUTES);
            if (Boolean.TRUE.equals(isNew)) {
                // 浏览量+1
                Post post = postMapper.selectById(postId);
                if (post != null) {
                    post.setViewCount(post.getViewCount() + 1);
                    postMapper.updateById(post);
                }
            }
        } catch (Exception e) {
            // Redis不可用，直接+1浏览量（不去重）
            Post post = postMapper.selectById(postId);
            if (post != null) {
                post.setViewCount(post.getViewCount() + 1);
                postMapper.updateById(post);
            }
        }
    }

    /**
     * 将Post实体转换为PostVO
     */
    private PostVO convertToPostVO(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setCategory(post.getCategory());
        vo.setTagIds(post.getTagIds());
        vo.setStartTime(post.getStartTime());
        vo.setEndTime(post.getEndTime());
        vo.setLocation(post.getLocation());
        vo.setMaxPeople(post.getMaxPeople());
        vo.setCurrentPeople(post.getCurrentPeople());
        vo.setStatus(post.getStatus());
        vo.setViewCount(post.getViewCount());
        vo.setPublisherId(post.getPublisherId());
        vo.setCreatedTime(post.getCreatedTime());
        vo.setDescription(post.getDescription());
        vo.setIsAuditPass(post.getIsAuditPass());

        // 根据tagIds查询标签名称
        if (StrUtil.isNotBlank(post.getTagIds())) {
            List<String> names = new ArrayList<>();
            for (String idStr : post.getTagIds().split(",")) {
                try {
                    Long tagId = Long.valueOf(idStr.trim());
                    InterestTag tag = interestTagMapper.selectById(tagId);
                    if (tag != null) {
                        names.add(tag.getName());
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            vo.setTagName(String.join(",", names));
        }

        // 查询发布者信息
        User publisher = userMapper.selectById(post.getPublisherId());
        if (publisher != null) {
            vo.setPublisherNickname(publisher.getNickname());
            vo.setPublisherAvatar(publisher.getAvatar());
            vo.setPublisherCreditLevel(creditService.getCreditLevel(publisher.getCreditScore()));
        }
        return vo;
    }
}
