package com.buddy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.CreditConstant;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.dto.request.ReviewRequest;
import com.buddy.entity.Post;
import com.buddy.entity.PostApply;
import com.buddy.entity.Review;
import com.buddy.mapper.PostApplyMapper;
import com.buddy.mapper.PostMapper;
import com.buddy.mapper.ReviewMapper;
import com.buddy.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 评价服务实现类
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    /** 评价窗口期（天） */
    private static final int REVIEW_WINDOW_DAYS = 7;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostApplyMapper postApplyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReview(Long reviewerId, ReviewRequest request) {
        // 校验帖子状态为FINISHED
        Post post = postMapper.selectById(request.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!PostStatus.FINISHED.equals(post.getStatus())) {
            throw new BusinessException("只能评价已结束的活动");
        }

        // 校验评价人是否参与了该活动（是发布者或已出勤的报名者）
        boolean isParticipant = post.getPublisherId().equals(reviewerId);
        if (!isParticipant) {
            Long attendedCount = postApplyMapper.selectCount(
                    new LambdaQueryWrapper<PostApply>()
                            .eq(PostApply::getPostId, request.getPostId())
                            .eq(PostApply::getApplicantId, reviewerId)
                            .eq(PostApply::getStatus, ApplyStatus.ATTENDED)
            );
            if (attendedCount == 0) {
                throw new BusinessException("您未参加该活动，无法评价");
            }
        }

        // 校验被评价用户也参与了该活动（发布者或已出勤的报名者）
        boolean targetIsParticipant = post.getPublisherId().equals(request.getTargetUserId());
        if (!targetIsParticipant) {
            Long targetAttendedCount = postApplyMapper.selectCount(
                    new LambdaQueryWrapper<PostApply>()
                            .eq(PostApply::getPostId, request.getPostId())
                            .eq(PostApply::getApplicantId, request.getTargetUserId())
                            .eq(PostApply::getStatus, ApplyStatus.ATTENDED)
            );
            if (targetAttendedCount == 0) {
                throw new BusinessException("被评价用户未参加该活动");
            }
        }

        // 校验不能评价自己
        if (reviewerId.equals(request.getTargetUserId())) {
            throw new BusinessException("不能评价自己");
        }

        // 校验未重复评价
        Long reviewCount = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getPostId, request.getPostId())
                        .eq(Review::getReviewerId, reviewerId)
                        .eq(Review::getTargetUserId, request.getTargetUserId())
        );
        if (reviewCount > 0) {
            throw new BusinessException(ResultCode.REVIEW_DUPLICATE);
        }

        // 校验7天评价窗口（从活动结束时间 endTime 算起，REV-04）
        // 注意：不能用 updatedTime，帖子被编辑会重置窗口，且可能为 null 导致校验被跳过
        if (post.getEndTime() != null) {
            long daysSinceFinished = ChronoUnit.DAYS.between(post.getEndTime(), LocalDateTime.now());
            if (daysSinceFinished > REVIEW_WINDOW_DAYS) {
                throw new BusinessException(ResultCode.REVIEW_WINDOW_EXPIRED);
            }
        }

        // 插入评价
        Review review = new Review();
        review.setPostId(request.getPostId());
        review.setReviewerId(reviewerId);
        review.setTargetUserId(request.getTargetUserId());
        review.setRating(request.getRating());
        review.setTag(request.getTag());
        review.setContent(request.getContent());

        reviewMapper.insert(review);
    }

    @Override
    public List<Review> getPostReviews(Long postId) {
        return reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getPostId, postId)
                        .orderByDesc(Review::getCreatedTime)
        );
    }

    @Override
    public List<Review> getUserReviews(Long targetUserId) {
        return reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTargetUserId, targetUserId)
                        .orderByDesc(Review::getCreatedTime)
        );
    }
}
