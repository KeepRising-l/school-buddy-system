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
import com.buddy.dto.response.PostVO;
import com.buddy.dto.response.UserVO;
import com.buddy.entity.*;
import com.buddy.entity.Notification;
import com.buddy.mapper.*;
import com.buddy.service.AdminService;
import com.buddy.service.CreditService;
import com.buddy.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台服务实现类
 */
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostApplyMapper postApplyMapper;

    @Autowired
    private PostCommentMapper postCommentMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private CreditLogMapper creditLogMapper;

    @Autowired
    private CreditService creditService;

    @Autowired
    private NotificationService notificationService;

    @Override
    public IPage<UserVO> getUserList(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(User::getNickname, keyword)
                    .or().like(User::getStudentNo, keyword)
                    .or().like(User::getPhone, keyword)
            );
        }
        wrapper.orderByDesc(User::getCreatedTime);

        IPage<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);

        // 转换为UserVO
        IPage<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream()
                .map(this::convertToUserVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public UserVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToUserVO(user);
    }

    @Override
    public IPage<PostVO> getUserPosts(Long userId, int page, int size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getPublisherId, userId)
                .orderByDesc(Post::getCreatedTime);
        IPage<Post> postPage = postMapper.selectPage(new Page<>(page, size), wrapper);

        IPage<PostVO> voPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        voPage.setRecords(postPage.getRecords().stream()
                .map(this::convertToPostVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public IPage<Map<String, Object>> getUserApplies(Long userId, int page, int size) {
        LambdaQueryWrapper<PostApply> wrapper = new LambdaQueryWrapper<PostApply>()
                .eq(PostApply::getApplicantId, userId)
                .orderByDesc(PostApply::getApplyTime);
        IPage<PostApply> applyPage = postApplyMapper.selectPage(new Page<>(page, size), wrapper);

        IPage<Map<String, Object>> resultPage = new Page<>(applyPage.getCurrent(), applyPage.getSize(), applyPage.getTotal());
        resultPage.setRecords(applyPage.getRecords().stream().map(apply -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", apply.getId());
            map.put("postId", apply.getPostId());
            map.put("status", apply.getStatus());
            map.put("applyTime", apply.getApplyTime());
            map.put("handleTime", apply.getHandleTime());
            // 查询帖子标题
            Post post = postMapper.selectById(apply.getPostId());
            if (post != null) {
                map.put("postTitle", post.getTitle());
            }
            return map;
        }).collect(Collectors.toList()));
        return resultPage;
    }

    @Override
    public IPage<Map<String, Object>> getUserReviews(Long userId, int page, int size) {
        // 查询该用户收到的评价
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(Review::getTargetUserId, userId)
                .orderByDesc(Review::getCreatedTime);
        IPage<Review> reviewPage = reviewMapper.selectPage(new Page<>(page, size), wrapper);

        IPage<Map<String, Object>> resultPage = new Page<>(reviewPage.getCurrent(), reviewPage.getSize(), reviewPage.getTotal());
        resultPage.setRecords(reviewPage.getRecords().stream().map(review -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", review.getId());
            map.put("postId", review.getPostId());
            map.put("reviewerId", review.getReviewerId());
            map.put("rating", review.getRating());
            map.put("tag", review.getTag());
            map.put("content", review.getContent());
            map.put("createdTime", review.getCreatedTime());
            // 查询评价人昵称
            User reviewer = userMapper.selectById(review.getReviewerId());
            if (reviewer != null) {
                map.put("reviewerNickname", reviewer.getNickname());
            }
            // 查询帖子标题
            Post post = postMapper.selectById(review.getPostId());
            if (post != null) {
                map.put("postTitle", post.getTitle());
            }
            return map;
        }).collect(Collectors.toList()));
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // V1.2 安全防护：禁止封禁管理员账号，防止后台被锁死
        if (user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            throw new BusinessException("不能封禁管理员账号");
        }
        user.setStatus(0);
        userMapper.updateById(user);

        // V1.2 联动：封禁用户其名下未完结帖子一律下架（违规内容不随封禁继续展示）
        List<Post> activePosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getPublisherId, userId)
                        .in(Post::getStatus, PostStatus.RECRUITING, PostStatus.FULL, PostStatus.ONGOING)
        );
        for (Post post : activePosts) {
            takedownPost(post.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbanUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(1);
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetCredit(Long userId, Integer creditScore) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // V1.2 信誉分台账：清空旧台账，按目标值换算成一条 ADMIN_RESET 记录，
        // 重算公式 = 100 + Σ评价 + Σ台账 = 目标值，且不会被每日重算冲掉
        creditLogMapper.delete(
                new LambdaQueryWrapper<CreditLog>().eq(CreditLog::getUserId, userId)
        );
        int change = creditScore - CreditConstant.INITIAL_SCORE - creditService.getReviewImpact(userId);
        creditService.recordPenalty(userId, change, CreditLog.REASON_ADMIN_RESET, CreditLog.BIZ_TYPE_ADMIN, userId);
    }

    @Override
    public IPage<PostVO> getPostList(int page, int size, String status, String category, Integer isAuditPass) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Post::getStatus, status);
        }
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(Post::getCategory, category);
        }
        if (isAuditPass != null) {
            wrapper.eq(Post::getIsAuditPass, isAuditPass);
        }
        wrapper.orderByDesc(Post::getCreatedTime);

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
    public void takedownPost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        // 下架即将帖子状态设为CANCELLED
        post.setStatus(PostStatus.CANCELLED);
        postMapper.updateById(post);

        // 联动：取消该帖所有未完结报名（修复：否则报名记录悬挂"已通过"，用户卡死）
        List<PostApply> applies = postApplyMapper.selectList(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .in(PostApply::getStatus, ApplyStatus.PENDING, ApplyStatus.APPROVED, ApplyStatus.WAITLIST)
        );
        for (PostApply apply : applies) {
            // V1.2 恢复名额：已通过（APPROVED）的报名取消后 current_people -1，保持口径一致
            if (ApplyStatus.APPROVED.equals(apply.getStatus())) {
                postMapper.decrementCurrentPeople(postId);
            }
            apply.setStatus(ApplyStatus.CANCELLED);
            apply.setHandleTime(LocalDateTime.now());
            postApplyMapper.updateById(apply);

            // V1.2 通知被取消报名的用户：不能"悄悄被取消"，让用户知道组局被下架
            notificationService.send(apply.getApplicantId(), Notification.TYPE_POST_TAKEDOWN,
                    "组局已下架",
                    "你报名的组局「" + post.getTitle() + "」已被管理员下架，报名已取消",
                    "/post/" + postId, postId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPost(Long postId, boolean pass, String reason) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        // V1.3 先发后审：保留审核接口作为兜底（1=正常 2=已下架）
        // 一般通过下架/举报处理，此接口保留供特殊场景使用
        post.setIsAuditPass(pass ? 1 : 2);
        postMapper.updateById(post);

        // V1.1 通知发布者审核结果；V1.2 拒绝时携带具体原因，便于用户修改后重新发布
        String content;
        if (pass) {
            content = "你的组局「" + post.getTitle() + "」已审核通过，现在可在广场看到";
        } else if (StrUtil.isNotBlank(reason)) {
            content = "你的组局「" + post.getTitle() + "」未通过审核，原因：" + reason + "。请修改后重新发布";
        } else {
            content = "你的组局「" + post.getTitle() + "」未通过审核，请修改后重新发布";
        }
        notificationService.send(post.getPublisherId(), Notification.TYPE_POST_AUDIT,
                pass ? "帖子审核通过" : "帖子审核未通过",
                content,
                "/post/" + postId, postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        // 物理删除：先删关联数据，再删帖子
        postApplyMapper.delete(new LambdaQueryWrapper<PostApply>().eq(PostApply::getPostId, postId));
        postCommentMapper.delete(new LambdaQueryWrapper<PostComment>().eq(PostComment::getPostId, postId));
        reviewMapper.delete(new LambdaQueryWrapper<Review>().eq(Review::getPostId, postId));
        // 修复：级联删除收藏记录，避免收藏列表出现幽灵数据/分页错乱
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getPostId, postId));
        postMapper.deleteById(postId);
    }

    /**
     * 将User实体转换为UserVO
     */
    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setStudentNo(user.getStudentNo());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setSignature(user.getSignature());
        vo.setCollege(user.getCollege());
        vo.setGender(user.getGender());
        vo.setCreditScore(user.getCreditScore());
        vo.setCreditLevel(creditService.getCreditLevel(user.getCreditScore()));
        vo.setCreditLabel(creditService.getCreditLabel(user.getCreditScore()));
        vo.setStatus(user.getStatus());
        vo.setCreatedTime(user.getCreatedTime());
        return vo;
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
        vo.setIsAuditPass(post.getIsAuditPass());
        vo.setRequireLevel(post.getRequireLevel());
        vo.setRequireGender(post.getRequireGender());

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
