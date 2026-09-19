package com.buddy.module.admin.dto;

import lombok.Data;

import java.util.List;

/**
 * 统计视图对象
 */
@Data
public class StatsVO {

    /** 总用户数 */
    private Long totalUsers;

    /** 总帖子数 */
    private Long totalPosts;

    /** 今日发帖 */
    private Long todayPosts;

    /** 成功组局数 */
    private Long successMatches;

    /** 活跃用户数 */
    private Long activeUsers;

    /** 分类统计 */
    private List<CategoryStat> categoryStats;

    /**
     * 分类统计项
     */
    @Data
    public static class CategoryStat {
        private String category;
        private Long count;
    }
}
