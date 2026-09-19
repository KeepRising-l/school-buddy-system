package com.buddy.module.post.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子实体（D-007：逻辑删除）
 */
@TableName("post")
@Data
public class Post {

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

    /** 1正常 2已下架（先发后审，0废弃不用） */
    private Integer isAuditPass;

    /** 逻辑删除：0正常 1已删（D-007） */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
