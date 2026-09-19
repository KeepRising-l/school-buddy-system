package com.buddy.controller.user;

import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.dto.request.ReviewReplyRequest;
import com.buddy.dto.request.ReviewRequest;
import com.buddy.entity.Review;
import com.buddy.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 评价控制器
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
