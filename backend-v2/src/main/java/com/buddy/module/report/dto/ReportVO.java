package com.buddy.module.report.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报视图对象
 */
@Data
public class ReportVO {

    private Long id;

    /** 举报人用户ID */
    private Long reporterId;

    /** 举报人昵称（脱敏展示） */
    private String reporterNickname;

    /** 举报对象类型：POST/USER */
    private String targetType;

    /** 被举报对象ID */
    private Long targetId;

    /** 被举报对象标题（帖子标题/用户昵称） */
    private String targetTitle;

    /** 举报类型 */
    private String reasonType;

    /** 举报说明 */
    private String description;

    /** 处理状态：0待处理 1有效 2无效 */
    private Integer status;

    /** 处理人昵称 */
    private String handleByName;

    /** 处理备注 */
    private String handleRemark;

    private LocalDateTime handleTime;

    private LocalDateTime createdTime;
}
