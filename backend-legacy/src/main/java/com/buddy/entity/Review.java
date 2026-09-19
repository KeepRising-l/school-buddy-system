package com.buddy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体
 */
@TableName("review")
@Data
public class Review extends Model<Review> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 帖子ID */
    private Long postId;

    /** 评价人ID */
    private Long reviewerId;

    /** 被评价用户ID */
    private Long targetUserId;

    /** 评分 1-5星 */
    private Integer rating;

    /** 标签：靠谱/爽约/迟到 */
    private String tag;

    /** 评价内容 */
    private String content;

    /** 被评价人的回复（互评解释） */
    private String replyContent;

    /** 回复时间 */
    private LocalDateTime replyTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
