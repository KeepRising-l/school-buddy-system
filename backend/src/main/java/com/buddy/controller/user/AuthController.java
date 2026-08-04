package com.buddy.controller.user;

import com.buddy.common.jwt.JwtUtils;
import com.buddy.common.result.R;
import com.buddy.dto.request.LoginRequest;
import com.buddy.dto.request.RegisterRequest;
import com.buddy.dto.response.UserVO;
import com.buddy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public R<Void> register(@RequestBody @Valid RegisterRequest request) {
        userService.register(request);
        return R.ok();
    }

    /**
     * 用户登录，返回token和用户信息
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody @Valid LoginRequest request) {
        String token = userService.login(request);
        // 从token中解析userId（登录接口不走JWT拦截器，不能从UserContext取）
        Long userId = jwtUtils.getUserIdFromToken(token);
        UserVO userVO = userService.getProfile(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", userVO);
        return R.ok(data);
    }
}
