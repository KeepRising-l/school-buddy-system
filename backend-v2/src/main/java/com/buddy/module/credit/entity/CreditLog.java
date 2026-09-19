package com.buddy.module.credit.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信誉分变动台账
 *
 * <p>记录非评价类的信誉分变动（爽约/临场取消/管理员重置），
 * 每日重算公式 = 100 + Σ评价影响 + Σ台账变动，保证运行期惩罚不被重算冲掉。</p>
 */
@TableName("credit_log")
@Data
public class CreditLog {

    /** 原因：标记未到场 */
    public static final String REASON_NOSHOW = "NOSHOW";
    /** 原因：临场取消 */
    public static final String REASON_CANCEL_LAST_MINUTE = "CANCEL_LAST_MINUTE";
    /** 原因：管理员重置 */
    public static final String REASON_ADMIN_RESET = "ADMIN_RESET";

    /** 业务类型：报名相关 */
    public static final String BIZ_TYPE_APPLY = "APPLY";
    /** 业务类型：管理员操作 */
    public static final String BIZ_TYPE_ADMIN = "ADMIN";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 变动值（正加负减） */
    private Integer changeValue;

    /** 原因：NOSHOW/CANCEL_LAST_MINUTE/ADMIN_RESET */
    private String reason;

    /** 业务类型：APPLY/ADMIN */
    private String bizType;

    /** 业务对象ID */
    private Long bizId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
