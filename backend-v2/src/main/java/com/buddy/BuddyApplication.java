package com.buddy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园搭子系统启动类（重构版）
 * 包结构：按业务功能模块划分（module/auth、module/user、module/post...）
 */
@SpringBootApplication
@EnableScheduling
@MapperScan({"com.buddy.module.user.mapper", "com.buddy.module.notification.mapper", "com.buddy.module.credit.mapper", "com.buddy.module.review.mapper", "com.buddy.module.post.mapper", "com.buddy.module.apply.mapper", "com.buddy.module.tag.mapper"})
public class BuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuddyApplication.class, args);
    }
}
