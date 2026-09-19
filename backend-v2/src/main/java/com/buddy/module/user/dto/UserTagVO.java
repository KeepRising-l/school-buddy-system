package com.buddy.module.user.dto;

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
    /** 用户自评段位 1-5（无段位概念的项目为 0/null） */
    private Integer selfLevel;
    /** 该项目是否有段位体系 */
    private Boolean hasLevel;
}
