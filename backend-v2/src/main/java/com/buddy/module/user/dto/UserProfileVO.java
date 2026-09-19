package com.buddy.module.user.dto;

import com.buddy.module.post.dto.PostVO;
import com.buddy.module.review.dto.ReviewVO;
import lombok.Data;

import java.util.List;

/**
 * 个人主页聚合视图对象
 */
@Data
public class UserProfileVO {

    /** 用户基本信息（脱敏） */
    private Long id;
    private String nickname;
    private String avatar;
    private String signature;
    private String college;
    private Integer gender;
    private Integer creditScore;
    private Integer creditLevel;
    private String creditLabel;
    /** 是否失信用户（<60） */
    private Boolean isLowCredit;

    /** 兴趣标签（含段位） */
    private List<UserTagVO> tags;

    /** 评价标签云统计：tag -> 次数 */
    private List<TagStat> reviewTagStats;

    /** 历史组局（发布 + 参加，最多20条） */
    private List<PostVO> historyPosts;

    /** 收到的评价（最多10条） */
    private List<ReviewVO> recentReviews;

    /** 隐私设置：0公开 1隐藏历史组局 */
    private Integer profilePrivacy;

    /**
     * 评价标签统计
     */
    @Data
    public static class TagStat {
        private String tag;
        private Long count;
    }
}
