package com.buddy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.dto.request.LoginRequest;
import com.buddy.dto.request.ProfileUpdateRequest;
import com.buddy.dto.request.RegisterRequest;
import com.buddy.dto.response.ApplyVO;
import com.buddy.dto.response.PostVO;
import com.buddy.dto.response.UserProfileVO;
import com.buddy.dto.response.UserVO;
import com.buddy.dto.response.UserTagVO;
import com.buddy.entity.InterestTag;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    void register(RegisterRequest request);

    /**
     * 用户登录，返回JWT Token
     */
    String login(LoginRequest request);

    /**
     * 获取用户个人信息
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
     * 获取个人主页聚合信息（V1.1）
     */
    UserProfileVO getUserProfile(Long userId);

    /**
     * 设置主页隐私（V1.1）：0公开 1隐藏历史组局
     */
    void setProfilePrivacy(Long userId, Integer privacy);
}
