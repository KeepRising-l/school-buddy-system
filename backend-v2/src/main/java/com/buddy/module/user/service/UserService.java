package com.buddy.module.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.module.apply.dto.ApplyVO;
import com.buddy.module.auth.dto.request.LoginRequest;
import com.buddy.module.auth.dto.request.RegisterRequest;
import com.buddy.module.post.dto.PostVO;
import com.buddy.module.user.dto.request.ProfileUpdateRequest;
import com.buddy.module.user.dto.UserProfileVO;
import com.buddy.module.user.dto.UserTagVO;
import com.buddy.module.user.dto.UserVO;

import java.util.List;

/**
 * 用户服务
 */
public interface UserService {

    /**
     * 用户注册
     */
    void register(RegisterRequest request);

    /**
     * 用户登录，返回 JWT Token
     */
    String login(LoginRequest request);

    /**
     * 查询本人资料
     */
    UserVO getProfile(Long userId);

    /**
     * 更新用户个人信息
     */
    void updateProfile(Long userId, ProfileUpdateRequest request);

    /**
     * 获取用户的兴趣标签列表
     */
    List<UserTagVO> getUserTags(Long userId);

    /**
     * 设置用户的兴趣标签（全量替换）
     */
    void setUserTags(Long userId, List<Long> tagIds, List<Integer> levels);

    /**
     * 获取我发布的帖子列表
     */
    IPage<PostVO> getMyPosts(Long userId, int page, int size, String status);

    /**
     * 获取我的报名记录
     */
    IPage<ApplyVO> getMyApplies(Long userId, int page, int size, String status);

    /**
     * 获取个人主页聚合信息（他人主页，脱敏）
     */
    UserProfileVO getUserProfile(Long userId);

    /**
     * 设置主页隐私：0公开 1隐藏历史组局
     */
    void setProfilePrivacy(Long userId, Integer privacy);
}
