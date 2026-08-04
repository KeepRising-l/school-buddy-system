package com.buddy.dto.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 评价请求
 */
@Data
public class ReviewRequest {

    @NotNull
    private Long postId;

    @NotNull
    private Long targetUserId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    /** 标签：靠谱/爽约/迟到 */
    private String tag;

    @Size(max = 500)
    private String content;
}
