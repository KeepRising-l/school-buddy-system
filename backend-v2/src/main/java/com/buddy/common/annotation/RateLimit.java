package com.buddy.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流（缺口#4）：按"动作 + 登录用户"在时间窗口内限制调用次数。
 * 计数走辅助存储（默认 MySQL、可切换 Redis）；超出限制返回 HTTP 429。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 动作标识（同一动作共用计数，如报名与候补共用 "apply"） */
    String value();

    /** 窗口内最大调用次数 */
    int max() default 10;

    /** 窗口时长（小时） */
    int windowHours() default 1;
}
