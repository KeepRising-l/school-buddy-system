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
     * 已报名用户信息
     */
    @Data
    public static class ApplyUserVO {
        private Long userId;
        private String nickname;
        private String avatar;
        private Integer creditLevel;
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
