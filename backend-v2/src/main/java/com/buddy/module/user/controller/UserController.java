package com.buddy.module.user.controller;

import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.module.user.dto.UserVO;
import com.buddy.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器（用户端）
 * Demo 范围：本人资料查询——需登录（由 JWT 拦截器校验），作为鉴权冒烟端点
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 查询本人资料（需登录，Token 有效时 UserContext 中有 userId）
     */
    @GetMapping("/profile")
    public R<UserVO> profile() {
        return R.ok(userService.getProfile(UserContext.getUserId()));
    }
}
