package com.buddy.module.admin.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 举报处理统计（缺口#4）
 *
 * <p>说明：原"审核时效统计"随人工审核环节取消（先发后审）不再适用，
 * 改为"举报处理时效"口径：平均处理时长 + 待处理积压量。</p>
 */
@Data
public class ReportStatsVO {

    /** 举报总数 */
    private Long totalReports;

    /** 已处理数（有效 + 无效） */
    private Long handledReports;

    /** 有效举报数 */
    private Long validReports;

    /** 待处理数 */
    private Long pendingReports;

    /** 处理率（%，已处理/总数） */
    private Double handledRate;

    /** 平均处理时长（小时，无已处理数据时为 null） */
    private Double avgHandleHours;

    /** 近 N 天每日举报量 [{date, count}] */
    private List<Map<String, Object>> trend;
}
