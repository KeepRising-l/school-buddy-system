package com.buddy.module.announcement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告实体
 */
@TableName("sys_announcement")
@Data
public class SysAnnouncement {

    /** 公告类型：普通公告 */
    public static final int TYPE_NOTICE = 1;
    /** 公告类型：弹窗公告 */
    public static final int TYPE_POPUP = 2;

    /** 状态：关闭 */
    public static final int STATUS_OFF = 0;
    /** 状态：启用 */
    public static final int STATUS_ON = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 类型：1普通 2弹窗 */
    private Integer type;

    /** 状态：0关闭 1启用 */
    private Integer status;

    /** 过期时间（可空，空为永久） */
    private LocalDateTime expireTime;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 创建人（管理员）ID */
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
