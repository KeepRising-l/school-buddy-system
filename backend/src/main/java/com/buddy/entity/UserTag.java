package com.buddy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户标签关联实体
 */
@TableName("user_tag")
@Data
public class UserTag extends Model<UserTag> {

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
