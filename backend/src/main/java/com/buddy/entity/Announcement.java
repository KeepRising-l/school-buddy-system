package com.buddy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告实体
 */
@TableName("sys_announcement")
@Data
public class Announcement extends Model<Announcement> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 类型：1=公告 2=弹窗 */
    private Integer type;

    /** 状态：0=关闭 1=启用 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 过期时间（null 表示永不过期） */
    private LocalDateTime expireTime;

    /** 创建人（管理员ID） */
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
