package com.buddy.interceptor;

import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.context.UserContext;
import com.buddy.common.jwt.JwtUtils;
import com.buddy.entity.User;
import com.buddy.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 认证拦截器
 * 校验请求头中的 Token，验证用户身份，设置用户上下文
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从请求头获取 Token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // Token 为空，返回401
        if (token == null || token.isEmpty()) {
            log.warn("请求缺少Token: {} {}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        // 校验 Token 有效性
        if (!jwtUtils.validateToken(token)) {
            log.warn("Token无效或已过期: {} {}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        // 解析 Token，提取用户ID
        Long userId;
        try {
            userId = jwtUtils.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("Token解析异常: {}", e.getMessage());
            writeUnauthorized(response, "登录状态异常，请重新登录");
            return false;
        }

        // 校验用户是否存在且未被封禁（封禁用户即使 Token 未过期也不能访问）
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            log.warn("被封禁或不存在用户访问: userId={}, uri={}", userId, request.getRequestURI());
            writeForbidden(response, "账号已被封禁或不存在");
            return false;
        }

        // 存入上下文
        UserContext.setUserId(userId);

        // 检查 @RequireAdmin 注解，校验管理员权限
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
            if (requireAdmin != null) {
                if (user.getIsAdmin() == null || user.getIsAdmin() != 1) {
                    log.warn("非管理员访问管理接口: userId={}, uri={}", userId, request.getRequestURI());
                    UserContext.clear();
                    writeForbidden(response, "无管理员权限");
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清除ThreadLocal，防止内存泄漏
        UserContext.clear();
    }

    /**
     * 写入401未授权响应
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", message);
        result.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 写入403禁止访问响应
     */
    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("message", message);
        result.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
