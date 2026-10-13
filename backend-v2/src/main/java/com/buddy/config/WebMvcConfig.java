package com.buddy.config;

import com.buddy.interceptor.JwtInterceptor;
import com.buddy.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web MVC 配置
 * 处理跨域、JWT 拦截器注册等（等价迁移自旧版）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    /**
     * 注册 JWT 拦截器，排除登录注册等公开接口
     * 拦截器为"可选登录"：游客放行、带 Token 时解析身份
     *
     * 注册限流拦截器：晚于 JWT（order 靠后），确保能拿到已解析的 userId。
     * 仅对标注 @RateLimit 的方法生效，未标注的方法直接放行。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/post/comments/**",
                        "/api/review/list/**",
                        "/api/review/user/**",
                        "/api/user/profile/*"
                );

        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }

    /**
     * CORS：仅放行配置中明确列出的来源（逗号分隔，见 app.cors.allowed-origins）。
     * 未配置时不放行任何跨域来源（生产域名确定后通过配置注入，不预置通配符）。
     */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源映射（上传的图片访问）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
