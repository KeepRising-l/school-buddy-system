package com.buddy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏实体（V1.1 组局收藏）
 */
@TableName("favorite")
@Data
public class Favorite extends Model<Favorite> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收藏用户ID */
    private Long userId;

    /** 帖子ID */
    private Long postId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
