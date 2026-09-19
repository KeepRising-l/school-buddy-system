package com.buddy.module.apply.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名实体（D-007：逻辑删除）
 * S3 先迁移实体/Mapper 供帖子详情/取消联动使用；接口层在 S4 报名切片补齐
 */
@TableName("post_apply")
@Data
public class PostApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 帖子ID */
    private Long postId;

    /** 申请人ID */
    private Long applicantId;

    /** 状态：PENDING/APPROVED/REJECTED/CANCELLED/ATTENDED/NOSHOW/WAITLIST */
    private String status;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 备注 */
    private String remark;

    /** 逻辑删除：0正常 1已删（D-007） */
    @TableLogic
    private Integer deleted;
}
