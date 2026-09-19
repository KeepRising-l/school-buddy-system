package com.buddy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名实体
 */
@TableName("post_apply")
@Data
public class PostApply extends Model<PostApply> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 帖子ID */
    private Long postId;

    /** 申请人ID */
    private Long applicantId;

    /** 状态：PENDING/APPROVED/REJECTED/CANCELLED/ATTENDED/NOSHOW */
    private String status;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 备注 */
    private String remark;
}
