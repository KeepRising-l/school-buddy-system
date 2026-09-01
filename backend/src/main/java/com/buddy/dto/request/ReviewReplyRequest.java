package com.buddy.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 评价回复请求
 */
@Data
public class ReviewReplyRequest {

    /** 评价ID（要回复的那条评价） */
    @NotNull
    private Long reviewId;

    /** 回复内容 */
    @Size(max = 500)
    private String content;
}