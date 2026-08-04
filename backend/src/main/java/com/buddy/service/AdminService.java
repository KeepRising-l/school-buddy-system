package com.buddy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.dto.response.PostVO;
import com.buddy.dto.response.UserVO;

import java.util.List;
import java.util.Map;

/**
 * 管理后台服务接口
 */
public interface AdminService {

    /**
     * 获取用户列表（支持关键词搜索）
     */
    IPage<UserVO> getUserList(int page, int size, String keyword);

    /**
     * 获取用户详情（含发布历史、报名历史、评价记录）
     */
    UserVO getUserDetail(Long userId);

    /**
     * 获取用户发布历史
     */
    IPage<PostVO> getUserPosts(Long userId, int page, int size);

    /**
     * 获取用户报名历史
     */
    IPage<Map<String, Object>> getUserApplies(Long userId, int page, int size);

    /**
     * 获取用户评价记录
     */
    IPage<Map<String, Object>> getUserReviews(Long userId, int page, int size);

    /**
     * 封禁用户
     */
    void banUser(Long userId);

    /**
     * 解封用户
     */
    void unbanUser(Long userId);

    /**
     * 重置用户信誉分
     */
    void resetCredit(Long userId, Integer creditScore);

    /**
     * 获取帖子列表（管理后台，支持状态和分类筛选）
     */
    IPage<PostVO> getPostList(int page, int size, String status, String category, Integer isAuditPass);

    /**
     * 下架帖子
     */
    void takedownPost(Long postId);

    /**
     * 审核帖子
     */
    void auditPost(Long postId, boolean pass);

    /**
     * 物理删除帖子
     */
    void deletePost(Long postId);
}
