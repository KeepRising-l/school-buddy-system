package com.buddy.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String account;  // 用户名、学号或手机号

    @NotBlank(message = "密码不能为空")
    private String password;
}
