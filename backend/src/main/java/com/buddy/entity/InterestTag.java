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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
