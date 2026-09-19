package com.buddy.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充
 *
 * <p>实体上标注 {@code @TableField(fill = FieldFill.INSERT/INSERT_UPDATE)} 的时间字段
 * 由此统一填充。旧版缺失该 Handler，导致 fill 字段被显式写入 NULL、绕过数据库默认值，
 * 引发举报 24h 去重失效（created_time 为 NULL 无法命中窗口条件）与列表排序异常，
 * 属基础设施修复（对应《现状说明》已知问题）。</p>
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
    }
}
