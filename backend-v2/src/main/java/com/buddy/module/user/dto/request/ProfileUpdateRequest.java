package com.buddy.module.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人信息更新请求
 */
@Data
public class ProfileUpdateRequest {

    @Size(min = 2, max = 20, message = "昵称2-20字符")
    private String nickname;

    private String avatar;

    @Size(max = 100, message = "签名最长100字符")
    private String signature;

    private String college;

    /** 性别：0未知 1男 2女 */
    private Integer gender;
}
