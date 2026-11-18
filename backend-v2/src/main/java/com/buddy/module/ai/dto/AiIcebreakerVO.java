package com.buddy.module.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 成局破冰小秘书返回 VO
 */
@Data
public class AiIcebreakerVO {

    private Long postId;

    /** 成员共同点与破冰话题 */
    private List<String> commonTopics;

    /** 一键复制打招呼模板 */
    private String greetingTemplate;

    /** 活动行前小贴士 */
    private List<String> activityTips;
}
