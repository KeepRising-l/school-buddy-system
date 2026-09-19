package com.buddy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置
 * 关闭 CSRF 和 Session，放行所有请求（JWT 认证由自定义拦截器处理）
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF
                .csrf().disable()
                // 关闭 Session，使用无状态方式
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 放行所有请求，JWT 认证由自定义拦截器处理
                .authorizeRequests()
                .anyRequest().permitAll()
                .and()
                // 禁用 frameOptions，允许 iframe 嵌入
                .headers().frameOptions().disable();

        return http.build();
    }
}
