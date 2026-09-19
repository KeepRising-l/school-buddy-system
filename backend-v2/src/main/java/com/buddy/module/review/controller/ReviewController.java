package com.buddy.module.review.controller;

import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.module.review.dto.request.ReviewReplyRequest;
import com.buddy.module.review.dto.request.ReviewRequest;
import com.buddy.module.review.entity.Review;
import com.buddy.module.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评价控制器（等价迁移自旧版）
 */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 提交评价
     */
    @PostMapping("/submit")
    public R<Void> submitReview(@RequestBody @Valid ReviewRequest request) {
        Long userId = UserContext.getUserId();
        reviewService.submitReview(userId, request);
        return R.ok();
    }

    /**
     * 帖子评价列表
     */
    @GetMapping("/list/{postId}")
    public R<List<Review>> getPostReviews(@PathVariable Long postId) {
        return R.ok(reviewService.getPostReviews(postId));
    }

    /**
     * 被评价人对评价进行回复/解释（互评双向）
     */
    @PostMapping("/reply")
    public R<Void> replyReview(@RequestBody @Valid ReviewReplyRequest request) {
        Long userId = UserContext.getUserId();
        reviewService.replyReview(userId, request.getReviewId(), request.getContent());
        return R.ok();
    }

    /**
     * 用户收到的评价
     */
    @GetMapping("/user/{userId}")
    public R<List<Review>> getUserReviews(@PathVariable Long userId) {
        return R.ok(reviewService.getUserReviews(userId));
    }
}
