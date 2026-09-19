package com.buddy.module.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

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

    /** 标签：靠谱/爽约/迟到（限长，防止超列宽入库 500） */
    @Size(max = 64)
    private String tag;

    @Size(max = 500)
    private String content;
}
