package com.buddy.module.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求（等价迁移自旧版，javax -> jakarta）
 */
@Data
public class RegisterRequest {

    /** 注册方式：student学号注册 phone手机号注册 */
    private String registerType;

    /** 学号（学号注册时必填，手机号注册时选填） */
    @Pattern(regexp = "\\d{8,12}", message = "学号格式不正确")
    private String studentNo;

    /** 手机号（手机号注册时必填，学号注册时选填） */
    @Pattern(regexp = "1[3-9]\\d{9}", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,20}$", message = "密码需8-20位，包含字母和数字")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名最长64字符")
    private String realName;
}
