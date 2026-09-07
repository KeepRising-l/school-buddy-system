package com.buddy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园搭子系统启动类（重构版）
 * 包结构：按业务功能模块划分（module/auth、module/user、module/post...）
 */
@SpringBootApplication
@MapperScan({"com.buddy.module.user.mapper", "com.buddy.module.notification.mapper"})
public class BuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuddyApplication.class, args);
    }
}
