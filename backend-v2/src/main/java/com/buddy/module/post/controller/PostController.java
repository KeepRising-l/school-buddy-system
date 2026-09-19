package com.buddy.module.post.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import com.buddy.common.result.ResultCode;
import com.buddy.module.post.dto.CommentVO;
import com.buddy.module.post.dto.PostDetailVO;
import com.buddy.module.post.dto.request.PostCreateRequest;
import com.buddy.module.post.service.CommentService;
import com.buddy.module.post.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 帖子控制器（等价迁移自旧版）
 */
@RestController
@RequestMapping("/api/post")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    /**
     * 发布组局
     */
    @PostMapping("/create")
    public R<Long> createPost(@RequestBody @Valid PostCreateRequest request) {
        Long userId = UserContext.getUserId();
        return R.ok(postService.createPost(userId, request));
    }

    /**
     * 帖子详情
     */
    @GetMapping("/detail/{id}")
    public R<PostDetailVO> getDetail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        // 浏览量 +1（同一用户 10 分钟内只计一次）
        postService.incrementViewCount(id, userId);
        return R.ok(postService.getPostDetail(userId, id));
    }

    /**
     * 取消组局
     */
    @PostMapping("/cancel/{id}")
    public R<Void> cancelPost(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        postService.cancelPost(userId, id);
        return R.ok();
    }

    /**
     * 发表评论
     * body: {postId: 1, content: "xxx"}
     */
    @PostMapping("/comment")
    public R<Void> addComment(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        Object postIdObj = body.get("postId");
        Object contentObj = body.get("content");
        // 防御：参数缺失时给明确提示，而不是 NPE 500
        if (postIdObj == null || contentObj == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论参数不完整");
        }
        Long postId;
        try {
            postId = Long.valueOf(postIdObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "帖子ID不合法");
        }
        commentService.addComment(userId, postId, (String) contentObj);
        return R.ok();
    }

    /**
     * 评论列表
     */
    @GetMapping("/comments/{postId}")
    public R<IPage<CommentVO>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(commentService.getComments(postId, page, size));
    }
}
