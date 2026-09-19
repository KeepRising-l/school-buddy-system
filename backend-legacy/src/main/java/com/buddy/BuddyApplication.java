package com.buddy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园搭子系统启动类
 */
@SpringBootApplication
@MapperScan("com.buddy.mapper")
public class BuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuddyApplication.class, args);
    }
}
