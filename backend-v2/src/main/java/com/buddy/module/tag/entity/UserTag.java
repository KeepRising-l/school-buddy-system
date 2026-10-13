package com.buddy.module.tag.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户标签关联实体
 */
@TableName("user_tag")
@Data
public class UserTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 标签ID */
    private Long tagId;

    /** 自评段位：1萌新 2小白 3熟手 4高手 5宗师 */
    private Integer selfLevel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
