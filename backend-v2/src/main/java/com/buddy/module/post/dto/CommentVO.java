package com.buddy.module.post.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论视图对象（包含用户信息）
 */
@Data
public class CommentVO {

    private Long id;

    private Long postId;

    private Long userId;

    private String content;

    /** 评论者昵称 */
    private String nickname;

    /** 评论者头像 */
    private String avatar;

    private LocalDateTime createdTime;
}
