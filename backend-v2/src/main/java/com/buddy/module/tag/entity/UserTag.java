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

    /** 自评段位：1青铜 2白银 3黄金 4钻石 5王者 */
    private Integer selfLevel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
