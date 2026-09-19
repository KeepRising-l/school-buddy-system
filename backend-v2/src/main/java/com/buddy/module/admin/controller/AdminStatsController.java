package com.buddy.module.admin.controller;

import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.result.R;
import com.buddy.module.admin.dto.StatsVO;
import com.buddy.module.admin.service.StatsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 管理端-数据统计控制器（等价迁移自旧版，D-009 跨模块统计聚合收敛到 module/admin）
 */
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    @Autowired
    private StatsService statsService;

    /**
     * 概览指标
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
     * 成功组局趋势
     */
    @RequireAdmin
    @GetMapping("/success")
    public R<List<Map<String, Object>>> getSuccessMatchTrend(
            @RequestParam(defaultValue = "30") int days) {
        return R.ok(statsService.getSuccessMatchTrend(days));
    }

    /**
     * 活跃用户趋势
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

    /**
     * 用户留存分析（次日/7日/30日留存率）
     */
    @RequireAdmin
    @GetMapping("/retention")
    public R<List<Map<String, Object>>> getRetention(
            @RequestParam(defaultValue = "30") int days) {
        return R.ok(statsService.getRetention(days));
    }

    /**
     * 转化漏斗分析（浏览→报名→通过→出勤）
     */
    @RequireAdmin
    @GetMapping("/funnel")
    public R<List<Map<String, Object>>> getFunnel(
            @RequestParam(defaultValue = "30") int days) {
        return R.ok(statsService.getFunnel(days));
    }

    /**
     * 导出用户列表（CSV）
     */
    @RequireAdmin
    @GetMapping("/export/users")
    public void exportUsers(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=users.csv");
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            statsService.exportUsers(writer);
        }
    }

    /**
     * 导出帖子列表（CSV）
     */
    @RequireAdmin
    @GetMapping("/export/posts")
    public void exportPosts(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=posts.csv");
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            statsService.exportPosts(writer);
        }
    }

    /**
     * 导出统计数据（CSV，type=trend/retention/funnel）
     */
    @RequireAdmin
    @GetMapping("/export/stats")
    public void exportStats(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "trend") String type,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=stats.csv");
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            statsService.exportStats(days, type, writer);
        }
    }
}
