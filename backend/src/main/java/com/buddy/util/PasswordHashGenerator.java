package com.buddy.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt密码哈希生成工具（一次性使用）
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456";
        String encoded = encoder.encode(rawPassword);
        System.out.println("原始密码: " + rawPassword);
        System.out.println("BCrypt哈希: " + encoded);
        System.out.println("验证结果: " + encoder.matches(rawPassword, encoded));
    }
}
