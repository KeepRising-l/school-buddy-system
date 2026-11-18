package com.buddy.module.ai.dto;

import lombok.Data;

/**
 * AI 一键组局自然语言解析结果 VO
 */
@Data
public class AiPostDraftVO {

    private String title;

    private String category;

    private Long tagId;

    private String tagName;

    private Boolean hasLevel;

    private String startTime;

    private String endTime;

    private String location;

    private Integer maxPeople;

    private Integer requireLevel;

    private Integer requireGender;

    private String description;

    /** 解析引擎来源：LLM（大模型API）或 SMART_ENGINE（内置校园语义引擎） */
    private String engine;
}
