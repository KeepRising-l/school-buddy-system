package com.buddy.module.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求（等价迁移自旧版，javax -> jakarta）
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String account;  // 用户名、学号或手机号

    @NotBlank(message = "密码不能为空")
    private String password;
}
