package com.buddy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子实体
 */
@TableName("post")
@Data
public class Post extends Model<Post> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布者ID */
    private Long publisherId;

    /** 标题 */
    private String title;

    /** 分类 */
    private String category;

    /** 逗号分隔的标签ID */
    private String tagIds;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 地点 */
    private String location;

    /** 最大人数 */
    private Integer maxPeople;

    /** 当前人数 */
    private Integer currentPeople;

    /** 要求最低段位，null不限 */
    private Integer requireLevel;

    /** 要求性别：0不限 1男 2女 */
    private Integer requireGender;

    /** 描述 */
    private String description;

    /** 状态：RECRUITING/FULL/ONGOING/FINISHED/CANCELLED */
    private String status;

    /** 浏览量 */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 审核是否通过：1通过 0待审 */
    private Integer isAuditPass;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
