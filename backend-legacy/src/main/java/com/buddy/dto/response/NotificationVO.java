package com.buddy.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知视图对象
 */
@Data
public class NotificationVO {

    private Long id;

    /** 通知类型 */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 跳转路径 */
    private String link;

    /** 是否已读：0未读 1已读 */
    private Integer isRead;

    /** 关联对象ID */
    private Long bizId;

    private LocalDateTime createdTime;
}
