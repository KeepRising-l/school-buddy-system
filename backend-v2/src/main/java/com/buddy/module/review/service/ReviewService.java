package com.buddy.module.review.service;

import com.buddy.module.review.dto.request.ReviewRequest;
import com.buddy.module.review.entity.Review;

import java.util.List;

/**
 * 评价服务接口
 */
public interface ReviewService {

    void submitReview(Long reviewerId, ReviewRequest request);

    void replyReview(Long targetUserId, Long reviewId, String content);

    List<Review> getPostReviews(Long postId);

    List<Review> getUserReviews(Long targetUserId);
}
