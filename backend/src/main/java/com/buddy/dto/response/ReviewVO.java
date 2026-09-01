package com.buddy.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价视图对象
 */
@Data
public class ReviewVO {

    private Long id;

    /** 帖子ID */
    private Long postId;

    /** 帖子标题 */
    private String postTitle;

    /** 评价人ID */
    private Long reviewerId;

    /** 评价人昵称 */
    private String reviewerNickname;

    /** 被评价人ID */
    private Long targetUserId;

    /** 星级 1-5 */
    private Integer rating;

    /** 评价标签 */
    private String tag;

    /** 评价内容 */
    private String content;

    /** 被评价人的回复（互评解释） */
    private String replyContent;

    /** 回复时间 */
    private LocalDateTime replyTime;

    private LocalDateTime createdTime;
}
