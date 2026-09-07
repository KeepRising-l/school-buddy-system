package com.buddy.module.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知实体（等价迁移自旧版 V1.1 通知中心）
 * 通知类型 7 类（D-011：去除「帖子审核」类型，人工审核已取消）
 */
@TableName("notification")
@Data
public class Notification {

    /** 通知类型：APPLY_NEW 新的报名申请 */
    public static final String TYPE_APPLY_NEW = "APPLY_NEW";
    /** 通知类型：APPLY_RESULT 报名审核结果 */
    public static final String TYPE_APPLY_RESULT = "APPLY_RESULT";
    /** 通知类型：WAITLIST_PROMOTED 候补递补 */
    public static final String TYPE_WAITLIST_PROMOTED = "WAITLIST_PROMOTED";
    /** 通知类型：POST_TAKEDOWN 帖子下架 */
    public static final String TYPE_POST_TAKEDOWN = "POST_TAKEDOWN";
    /** 通知类型：POST_CANCEL 组局取消 */
    public static final String TYPE_POST_CANCEL = "POST_CANCEL";
    /** 通知类型：ACTIVITY_REMIND 活动提醒 */
    public static final String TYPE_ACTIVITY_REMIND = "ACTIVITY_REMIND";
    /** 通知类型：SYSTEM 系统通知 */
    public static final String TYPE_SYSTEM = "SYSTEM";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人用户ID */
    private Long userId;

    /** 通知类型 */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 跳转路径（如 /post/12） */
    private String link;

    /** 是否已读：0未读 1已读 */
    private Integer isRead;

    /** 关联对象ID（帖子ID/报名ID等，便于业务查询） */
    private Long bizId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
