package com.buddy.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子列表视图对象
 */
@Data
public class PostVO {

    private Long id;

    private String title;

    private String category;

    private String tagIds;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private Integer maxPeople;

    private Integer currentPeople;

    private String status;

    private Integer viewCount;

    /** 发布者ID */
    private Long publisherId;

    /** 发布者昵称 */
    private String publisherNickname;

    /** 发布者头像 */
    private String publisherAvatar;

    /** 发布者信誉星级 */
    private Integer publisherCreditLevel;

    /** 帖子描述（列表页截断展示） */
    private String description;

    /** 标签名称（逗号分隔，用于列表展示） */
    private String tagName;

    /** 审核状态：0待审核 1已通过 2已拒绝（V1.1 先审后发） */
    private Integer isAuditPass;

    private LocalDateTime createdTime;
}
