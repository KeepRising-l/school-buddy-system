package com.buddy.common.annotation;

import java.lang.annotation.*;

/**
 * 管理员权限注解
 * 标注在Controller方法上，表示需要管理员权限
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {
}
