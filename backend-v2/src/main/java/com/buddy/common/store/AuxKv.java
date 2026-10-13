package com.buddy.common.store;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 辅助键值表实体（登录锁定 / 浏览去重 / 上传限频 / 举报去重；默认替代 Redis）
 */
@TableName("aux_kv")
@Data
public class AuxKv {

    /** 键 */
    @TableId(type = IdType.INPUT)
    private String k;

    /** 值（计数） */
    private Long v;

    /** 过期时间 */
    private LocalDateTime expireTime;
}
