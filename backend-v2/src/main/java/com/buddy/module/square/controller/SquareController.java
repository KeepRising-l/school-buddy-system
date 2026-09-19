package com.buddy.module.square.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.module.post.dto.PostVO;
import com.buddy.module.square.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 广场控制器（等价迁移自旧版）
 */
@RestController
@RequestMapping("/api/square")
public class SquareController {

    @Autowired
    private RecommendService recommendService;

    /**
     * 广场列表（推荐排序）
     * @param timeRange 时间筛选：today/week/month，为空则不限
     */
    @GetMapping("/list")
    public R<IPage<PostVO>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "recommend") String sort,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) String keyword) {
        Long userId = UserContext.getUserId();
        return R.ok(recommendService.getRecommendList(userId, page, size, category, sort, timeRange, keyword));
    }
}
