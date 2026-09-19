package com.buddy.module.review.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
