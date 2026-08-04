package com.buddy.controller.admin;

import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.result.R;
import com.buddy.dto.response.StatsVO;
import com.buddy.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端-数据统计控制器
 */
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    @Autowired
    private StatsService statsService;

    /**
     * 数据概览
     */
    @RequireAdmin
    @GetMapping("/overview")
    public R<StatsVO> getOverview() {
        return R.ok(statsService.getOverview());
    }

    /**
     * 发帖量趋势
     */
    @RequireAdmin
    @GetMapping("/posts")
    public R<List<Map<String, Object>>> getPostTrend(
            @RequestParam(defaultValue = "30") int days) {
        return R.ok(statsService.getPostTrend(days));
    }

    /**
     * 成功组局数
     */
    @RequireAdmin
    @GetMapping("/success")
    public R<List<Map<String, Object>>> getSuccessMatchTrend(
            @RequestParam(defaultValue = "30") int days) {
        return R.ok(statsService.getSuccessMatchTrend(days));
    }

    /**
     * 活跃用户数
     */
    @RequireAdmin
    @GetMapping("/active")
    public R<List<Map<String, Object>>> getActiveUserTrend(
            @RequestParam(defaultValue = "30") int days) {
        return R.ok(statsService.getActiveUserTrend(days));
    }

    /**
     * 热门分类
     */
    @RequireAdmin
    @GetMapping("/category")
    public R<List<StatsVO.CategoryStat>> getCategoryTop5() {
        return R.ok(statsService.getCategoryTop5());
    }

    /**
     * 信誉分分布
     */
    @RequireAdmin
    @GetMapping("/credit")
    public R<List<Map<String, Object>>> getCreditDistribution() {
        return R.ok(statsService.getCreditDistribution());
    }

    /**
     * 用户增长趋势
     */
    @RequireAdmin
    @GetMapping("/user-growth")
    public R<List<Map<String, Object>>> getUserGrowthTrend(
            @RequestParam(defaultValue = "30") int days) {
        return R.ok(statsService.getUserGrowthTrend(days));
    }
}
