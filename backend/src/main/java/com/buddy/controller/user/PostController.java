package com.buddy.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.dto.request.PostCreateRequest;
import com.buddy.dto.response.CommentVO;
import com.buddy.dto.response.PostDetailVO;
import com.buddy.service.CommentService;
import com.buddy.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 帖子控制器
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
        // 浏览量 +1（同一用户 10 分钟内只计一次，DET-01）
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
        Long postId = Long.valueOf(body.get("postId").toString());
        String content = (String) body.get("content");
        commentService.addComment(userId, postId, content);
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
