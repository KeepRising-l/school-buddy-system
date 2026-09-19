package com.buddy.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@TableName("sys_user")
@Data
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学号 */
    private String studentNo;

    /** 手机号 */
    private String phone;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 个性签名 */
    private String signature;

    /** 学院 */
    private String college;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 信誉分 0-100 */
    private Integer creditScore;

    /** 状态：1正常 0封禁 */
    private Integer status;

    /** 是否管理员：0否 1是 */
    private Integer isAdmin;

    /** 主页隐私：0公开 1隐藏历史组局 */
    private Integer profilePrivacy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
