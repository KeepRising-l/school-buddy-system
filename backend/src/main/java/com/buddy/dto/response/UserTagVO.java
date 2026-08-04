package com.buddy.dto.response;

import lombok.Data;

/**
 * 用户标签VO（包含段位信息）
 */
@Data
public class UserTagVO {
    private Long id;
    private String category;
    private String name;
    private String description;
    private Integer sortOrder;
    private Integer selfLevel;  // 用户自评段位 1-5
}
