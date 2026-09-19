package com.buddy.module.favorite.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import com.buddy.common.result.ResultCode;
import com.buddy.module.favorite.service.FavoriteService;
import com.buddy.module.post.dto.PostVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 收藏控制器（等价迁移自旧版）
 */
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 收藏帖子
     * body: {postId: 1}
     */
    @PostMapping("/add")
    public R<Void> add(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        favoriteService.add(userId, parsePostId(body));
        return R.ok();
    }

    /**
     * 取消收藏
     * body: {postId: 1}
     */
    @PostMapping("/cancel")
    public R<Void> cancel(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        favoriteService.cancel(userId, parsePostId(body));
        return R.ok();
    }

    /**
     * 解析请求体中的 postId（防 NPE 500）
     */
    private Long parsePostId(Map<String, Object> body) {
        Object postIdObj = body == null ? null : body.get("postId");
        if (postIdObj == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少帖子ID");
        }
        try {
            return Long.valueOf(postIdObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "帖子ID不合法");
        }
    }

    /**
     * 我的收藏（分页）
     */
    @GetMapping("/list")
    public R<IPage<PostVO>> getMyFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        return R.ok(favoriteService.getMyFavorites(userId, page, size));
    }

    /**
     * 是否已收藏 + 收藏数
     */
    @GetMapping("/status/{postId}")
    public R<Map<String, Object>> getStatus(@PathVariable Long postId) {
        Long userId = UserContext.getUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("favorited", favoriteService.isFavorite(userId, postId));
        data.put("count", favoriteService.getFavoriteCount(postId));
        return R.ok(data);
    }
}
