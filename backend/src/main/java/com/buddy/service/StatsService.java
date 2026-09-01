package com.buddy.service;

import com.buddy.dto.response.StatsVO;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * 统计服务接口
 */
public interface StatsService {

    /**
     * 获取总览统计数据
     */
    StatsVO getOverview();

    /**
     * 获取帖子趋势数据
     */
    List<Map<String, Object>> getPostTrend(int days);

    /**
     * 获取成功组局趋势数据
     */
    List<Map<String, Object>> getSuccessMatchTrend(int days);

    /**
     * 获取活跃用户趋势数据
     */
    List<Map<String, Object>> getActiveUserTrend(int days);

    /**
     * 获取分类TOP5
     */
    List<StatsVO.CategoryStat> getCategoryTop5();

    /**
     * 获取信誉分分布
     */
    List<Map<String, Object>> getCreditDistribution();

    /**
     * 获取用户增长趋势
     */
    List<Map<String, Object>> getUserGrowthTrend(int days);

    /**
     * 用户留存分析（次日/7日/30日留存率）
     */
    List<Map<String, Object>> getRetention(int days);

    /**
     * 转化漏斗分析（浏览→报名→通过→出勤）
     */
    List<Map<String, Object>> getFunnel(int days);

    /**
     * 导出用户列表CSV
     */
    void exportUsers(Writer writer) throws IOException;

    /**
     * 导出帖子列表CSV
     */
    void exportPosts(Writer writer) throws IOException;

    /**
     * 导出统计数据CSV（type: trend/retention/funnel）
     */
    void exportStats(int days, String type, Writer writer) throws IOException;
}
