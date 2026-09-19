package com.buddy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 兴趣标签实体
 */
@TableName("interest_tag")
@Data
public class InterestTag extends Model<InterestTag> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类 */
    private String category;

    /** 标签名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 排序序号 */
    private Integer sortOrder;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /**
     * 是否有段位体系（非表字段，接口返回用）：
     * 球类/电竞等竞技项目有段位概念；跑步/桌游/学习等没有。
     * 由 TagServiceImpl 根据分类规则填充，true 表示用户需为该项目自评段位。
     */
    @TableField(exist = false)
    private Boolean hasLevel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
