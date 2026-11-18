package com.buddy.module.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * 管理端举报 AI 风控研判建议 VO
 */
@Data
public class AiReportAdviceVO {

    private Long reportId;

    /** 风险等级：HIGH / MEDIUM / LOW */
    private String riskLevel;

    /** 综合风险评分 0-100 */
    private Integer riskScore;

    /** AI 研判结论摘要 */
    private String summary;

    /** 建议判定结果：true=有效（联动处置），false=无效 */
    private Boolean suggestedValid;

    /** 建议填写的处理备注 */
    private String suggestedRemark;

    /** 研判依据列表 */
    private List<String> evidences;
}
