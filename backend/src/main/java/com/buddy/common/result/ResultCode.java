package com.buddy.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 返回码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 通用码
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    // 业务码
    USER_EXISTS(1001, "用户已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    CREDIT_TOO_LOW(1004, "信誉分过低，无法操作"),
    POST_NOT_RECRUITING(1005, "帖子不在招募中"),
    APPLY_DUPLICATE(1006, "已报名该活动"),
    APPLY_NOT_QUALIFIED(1007, "不满足报名要求"),
    STATUS_TRANSIT_ERROR(1008, "状态流转不合法"),
    REVIEW_DUPLICATE(1009, "已评价过该用户"),
    REVIEW_WINDOW_EXPIRED(1010, "评价窗口已过期");

    private final int code;
    private final String message;
}
