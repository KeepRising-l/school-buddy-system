package com.buddy.interceptor;

import com.buddy.common.annotation.RateLimit;
import com.buddy.common.context.UserContext;
import com.buddy.common.store.AuxStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 接口限流拦截器（缺口#4）
 *
 * <p>对标注 {@link RateLimit} 的接口，按"动作 + 登录用户"在窗口内计数，超限返回 429。
 * 计数走辅助存储（默认 MySQL、可切换 Redis）；存储不可用时降级放行（记录告警），保证可用性。</p>
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private AuxStore auxStore;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        RateLimit rateLimit = ((HandlerMethod) handler).getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }
        Long userId = UserContext.getUserId();
        if (userId == null) {
            // 未登录请求由 @RequireLogin 负责拦截，这里不重复处理
            return true;
        }

        String key = "rate:" + rateLimit.value() + ":" + userId;
        try {
            long count = auxStore.increment(key, Duration.ofHours(rateLimit.windowHours()));
            if (count > rateLimit.max()) {
                log.warn("接口限流触发: action={}, userId={}, count={}", rateLimit.value(), userId, count);
                writeTooManyRequests(response);
                return false;
            }
        } catch (Exception e) {
            log.warn("限流存储不可用，本次放行: action={}, error={}", rateLimit.value(), e.getMessage());
        }
        return true;
    }

    /**
     * 写入 429 响应
     */
    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 429);
        result.put("message", "操作过于频繁，请稍后再试");
        result.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
