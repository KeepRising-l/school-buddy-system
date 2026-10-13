package com.buddy.module.admin.service;

import com.buddy.module.admin.dto.ReportStatsVO;
import com.buddy.module.admin.dto.StatsVO;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * 统计服务接口（D-009：跨模块统计聚合收敛到 module/admin）
 */
public interface StatsService {

    StatsVO getOverview();

    List<Map<String, Object>> getPostTrend(int days);

    List<Map<String, Object>> getSuccessMatchTrend(int days);

    List<Map<String, Object>> getActiveUserTrend(int days);

    List<StatsVO.CategoryStat> getCategoryTop5();

    List<Map<String, Object>> getCreditDistribution();

    List<Map<String, Object>> getUserGrowthTrend(int days);

    List<Map<String, Object>> getRetention(int days);

    List<Map<String, Object>> getFunnel(int days);

    /**
     * 举报处理统计（缺口#4）：总量/已处理/有效/待处理、处理率、平均处理时长、近 N 天趋势
     */
    ReportStatsVO getReportStats(int days);

    void exportUsers(Writer writer) throws IOException;

    void exportPosts(Writer writer) throws IOException;

    void exportStats(int days, String type, Writer writer) throws IOException;
}
