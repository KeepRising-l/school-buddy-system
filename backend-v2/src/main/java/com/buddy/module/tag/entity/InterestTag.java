package com.buddy.module.tag.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 兴趣标签实体
 * S3 先迁移实体/Mapper 供帖子 VO 转换使用；接口层在 S7 标签切片补齐
 */
@TableName("interest_tag")
@Data
public class InterestTag {

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
     * 由 TagService 根据分类规则填充，true 表示用户需为该项目自评段位。
     */
    @TableField(exist = false)
    private Boolean hasLevel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
