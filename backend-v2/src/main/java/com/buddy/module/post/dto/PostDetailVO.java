package com.buddy.module.post.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 帖子详情视图对象（继承 PostVO 并扩展）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostDetailVO extends PostVO {

    /** 发布者个性签名 */
    private String publisherSignature;

    /** 发布者学院 */
    private String publisherCollege;

    /** 发布者兴趣标签列表 */
    private List<PublisherTagVO> publisherTags;

    /** 已报名用户列表 */
    private List<ApplyUserVO> applyUsers;

    /** 可评价用户列表（已出勤的参与者，发布者可评价这些人） */
    private List<ApplyUserVO> reviewTargets;

    /** 当前用户是否已报名 */
    private Boolean hasApplied;

    /** 当前用户的报名状态 */
    private String applyStatus;

    /** 当前用户的报名记录ID（用于取消报名） */
    private Long myApplyId;

    /**
     * 发布者联系方式（手机号）。
     * 仅当查看者是发布者本人，或其报名已被通过（APPROVED/ATTENDED）时才返回，
     * 其余情况为 null（前端提示"报名通过后可查看"）。
     */
    private String publisherContact;

    /** 联系方式是否已解锁（查看者是否为发布者/已通过报名者） */
    private Boolean contactVisible;

    /**
     * 4位数字活动签到码（仅当查看者为发布者本人时返回，供线下向参与者出示以自助核销出勤）
     */
    private String checkinCode;

    /**
     * 已报名用户信息
     */
    @Data
    public static class ApplyUserVO {
        private Long userId;
        private String nickname;
        private String avatar;
        private Integer creditLevel;
        /**
         * 联系方式（手机号）。仅对"发布者"和"同为已通过报名的参与者"返回，其余为 null。
         */
        private String contact;
    }

    /**
     * 发布者兴趣标签信息
     */
    @Data
    public static class PublisherTagVO {
        private Long tagId;
        private String tagName;
        private String category;
        private Integer selfLevel;
    }
}
