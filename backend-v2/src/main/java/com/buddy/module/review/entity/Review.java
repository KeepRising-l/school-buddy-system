package com.buddy.module.review.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体
 * S2 先迁移实体/Mapper 供信誉分计算使用；接口层在 S5 评价切片补齐
 */
@TableName("review")
@Data
public class Review {

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

    /** 逻辑删除：0正常 1已删（D-007） */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
