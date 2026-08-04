package com.buddy.service;

import com.buddy.dto.request.ReviewRequest;
import com.buddy.entity.Review;

import java.util.List;

/**
 * 评价服务接口
 */
public interface ReviewService {

    /**
     * 提交评价
     */
    void submitReview(Long reviewerId, ReviewRequest request);

    /**
     * 获取帖子的评价列表
     */
    List<Review> getPostReviews(Long postId);

    /**
     * 获取用户收到的评价列表
     */
    List<Review> getUserReviews(Long targetUserId);
}
