package com.buddy.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象（不含密码）
 */
@Data
public class UserVO {

    private Long id;

    private String studentNo;

    private String phone;

    private String username;

    private String nickname;

    private String avatar;

    private String signature;

    private String college;

    private Integer gender;

    private Integer creditScore;

    /** 计算后的星级 1-5 */
    private Integer creditLevel;

    /** 信誉标签：靠谱队友/优质队友/... */
    private String creditLabel;

    private Integer status;

    private LocalDateTime createdTime;
}
