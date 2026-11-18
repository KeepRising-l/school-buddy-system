package com.buddy.module.ai.service;

import com.buddy.module.ai.dto.AiIcebreakerVO;
import com.buddy.module.ai.dto.AiPostDraftVO;
import com.buddy.module.ai.dto.AiReportAdviceVO;

import java.util.Map;

/**
 * AI 智能搭子服务接口
 */
public interface AiBuddyService {

    /**
     * 自然语言一句话解析生成组局表单草稿
     *
     * @param prompt 用户输入的一句话描述
     * @return 结构化组局草稿
     */
    AiPostDraftVO parsePostFromText(String prompt);

    /**
     * 根据表单要素智能扩写/润色组局招募文案
     *
     * @param params 包含 category/tagName/location/startTime/maxPeople/requireLevel/draft 等
     * @return 润色后的招募描述
     */
    String polishDescription(Map<String, Object> params);

    /**
     * 为已成行/报名通过的组局成员生成破冰话题与打招呼话术
     */
    AiIcebreakerVO generateIcebreaker(Long userId, Long postId);

    /**
     * 管理端：为举报单生成 AI 风险研判与处置建议
     */
    AiReportAdviceVO analyzeReport(Long reportId);
}
