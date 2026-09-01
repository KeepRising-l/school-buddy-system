package com.buddy.controller.admin;

import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.result.R;
import com.buddy.dto.response.StatsVO;
import com.buddy.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
        statsService.exportUsers(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
    }

    /**
     * 导出帖子列表（CSV）
     */
    @RequireAdmin
    @GetMapping("/export/posts")
    public void exportPosts(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=posts.csv");
        statsService.exportPosts(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
    }

    /**
     * 导出统计数据（CSV）- trend/retention/funnel
     */
    @RequireAdmin
    @GetMapping("/export/stats")
    public void exportStats(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "trend") String type,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=stats_" + type + ".csv");
        statsService.exportStats(days, type, new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
    }
}
