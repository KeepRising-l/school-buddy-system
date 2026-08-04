package com.buddy.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名视图对象（包含申请人信息）
 */
@Data
public class ApplyVO {

    private Long id;

    private Long postId;

    /** 帖子标题 */
    private String postTitle;

    private Long applicantId;

    /** 申请人昵称 */
    private String applicantNickname;

    /** 申请人头像 */
    private String applicantAvatar;

    /** 申请人信誉星级 */
    private Integer applicantCreditLevel;

    /** 状态：PENDING/APPROVED/REJECTED/CANCELLED/ATTENDED/NOSHOW */
    private String status;

    private LocalDateTime applyTime;

    private LocalDateTime handleTime;

    private String remark;
}
