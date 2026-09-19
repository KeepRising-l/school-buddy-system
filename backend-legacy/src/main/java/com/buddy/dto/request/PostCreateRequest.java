package com.buddy.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 发帖请求
 */
@Data
public class PostCreateRequest {

    @NotBlank
    @Size(min = 5, max = 30)
    private String title;

    @NotBlank
    @Size(max = 64)
    private String category;

    /** 逗号分隔的标签ID */
    private String tagIds;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @NotBlank
    @Size(max = 128)
    private String location;

    @NotNull
    @Min(2)
    @Max(20)
    private Integer maxPeople;

    /** 要求最低段位，null不限 */
    private Integer requireLevel;

    /** 要求性别：0不限 1男 2女 */
    private Integer requireGender;

    @Size(max = 500)
    private String description;
}
