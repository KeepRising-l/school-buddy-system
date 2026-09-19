package com.buddy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体
 */
@TableName("post_comment")
@Data
public class PostComment extends Model<PostComment> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 帖子ID */
    private Long postId;

    /** 评论用户ID */
    private Long userId;

    /** 评论内容 */
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
