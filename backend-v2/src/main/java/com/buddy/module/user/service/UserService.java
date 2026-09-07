package com.buddy.module.user.service;

import com.buddy.module.auth.dto.request.LoginRequest;
import com.buddy.module.auth.dto.request.RegisterRequest;
import com.buddy.module.user.dto.UserVO;

/**
 * 用户服务
 * Demo 范围：注册 / 登录 / 本人资料（等价迁移自旧版 UserServiceImpl 的对应方法）
 */
public interface UserService {

    /**
     * 用户注册
     */
    void register(RegisterRequest request);

    /**
     * 用户登录，返回 JWT Token
     */
    String login(LoginRequest request);

    /**
     * 查询本人资料
     */
    UserVO getProfile(Long userId);
}
