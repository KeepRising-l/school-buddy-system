package com.buddy.module.post.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.module.apply.entity.PostApply;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.favorite.entity.Favorite;
import com.buddy.module.favorite.mapper.FavoriteMapper;
import com.buddy.module.notification.entity.Notification;
import com.buddy.module.notification.service.NotificationService;
import com.buddy.module.post.converter.PostConverter;
import com.buddy.module.post.dto.PostVO;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.entity.PostComment;
import com.buddy.module.post.mapper.PostCommentMapper;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.post.service.AdminPostService;
import com.buddy.module.review.entity.Review;
import com.buddy.module.review.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端-帖子管理服务实现类（等价迁移自旧版 AdminServiceImpl 帖子部分）
 */
@Service
public class AdminPostServiceImpl implements AdminPostService {

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
    private PostConverter postConverter;

    @Autowired
    private NotificationService notificationService;

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

        IPage<PostVO> voPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        voPage.setRecords(postConverter.toVOList(postPage.getRecords()));
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

        // 联动：取消该帖所有未完结报名（否则报名记录悬挂"已通过"，用户卡死）
        List<PostApply> applies = postApplyMapper.selectList(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .in(PostApply::getStatus, ApplyStatus.PENDING, ApplyStatus.APPROVED, ApplyStatus.WAITLIST)
        );
        for (PostApply apply : applies) {
            // 恢复名额：已通过（APPROVED）的报名取消后 current_people -1，保持口径一致
            if (ApplyStatus.APPROVED.equals(apply.getStatus())) {
                postMapper.decrementCurrentPeople(postId);
            }
            apply.setStatus(ApplyStatus.CANCELLED);
            apply.setHandleTime(LocalDateTime.now());
            postApplyMapper.updateById(apply);

            // 通知被取消报名的用户：不能"悄悄被取消"，让用户知道组局被下架
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
        // 先发后审（D-006）：保留审核接口作为兜底（1=正常 2=已下架）
        post.setIsAuditPass(pass ? 1 : 2);
        postMapper.updateById(post);

        // 通知发布者。说明：旧版使用 TYPE_POST_AUDIT 通知类型，D-011 已将通知收敛为 7 类
        // （不含审核类），故拒绝时改用 POST_TAKEDOWN（语义=下架），通过时使用 SYSTEM。
        String content;
        if (pass) {
            content = "你的组局「" + post.getTitle() + "」已审核通过，现在可在广场看到";
        } else if (StrUtil.isNotBlank(reason)) {
            content = "你的组局「" + post.getTitle() + "」未通过审核，原因：" + reason + "。请修改后重新发布";
        } else {
            content = "你的组局「" + post.getTitle() + "」未通过审核，请修改后重新发布";
        }
        notificationService.send(post.getPublisherId(),
                pass ? Notification.TYPE_SYSTEM : Notification.TYPE_POST_TAKEDOWN,
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
        // D-007 逻辑删除+级联：删除帖子时级联逻辑删除其评论/报名/评价/收藏，数据可追溯。
        // @TableLogic 使 delete 操作自动转为 UPDATE deleted=1
        postApplyMapper.delete(new LambdaQueryWrapper<PostApply>().eq(PostApply::getPostId, postId));
        postCommentMapper.delete(new LambdaQueryWrapper<PostComment>().eq(PostComment::getPostId, postId));
        reviewMapper.delete(new LambdaQueryWrapper<Review>().eq(Review::getPostId, postId));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getPostId, postId));
        postMapper.deleteById(postId);
    }
}
