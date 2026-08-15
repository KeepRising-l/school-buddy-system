package com.buddy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报实体（V1.1 举报闭环）
 */
@TableName("report")
@Data
public class Report extends Model<Report> {

    /** 举报对象类型：帖子 */
    public static final String TARGET_POST = "POST";
    /** 举报对象类型：用户 */
    public static final String TARGET_USER = "USER";

    /** 举报类型：违规内容 */
    public static final String REASON_VIOLATION = "违规内容";
    /** 举报类型：虚假信息 */
    public static final String REASON_FALSE = "虚假信息";
    /** 举报类型：恶意行为 */
    public static final String REASON_ABUSE = "恶意行为";
    /** 举报类型：其他 */
    public static final String REASON_OTHER = "其他";

    /** 处理状态：待处理 */
    public static final int STATUS_PENDING = 0;
    /** 处理状态：有效 */
    public static final int STATUS_VALID = 1;
    /** 处理状态：无效 */
    public static final int STATUS_INVALID = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人用户ID */
    private Long reporterId;

    /** 举报对象类型：POST/USER */
    private String targetType;

    /** 被举报对象ID */
    private Long targetId;

    /** 举报类型 */
    private String reasonType;

    /** 举报说明 */
    private String description;

    /** 处理状态：0待处理 1有效 2无效 */
    private Integer status;

    /** 处理人（管理员）用户ID */
    private Long handleBy;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理备注 */
    private String handleRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
