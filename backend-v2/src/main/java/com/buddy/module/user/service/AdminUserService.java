package com.buddy.module.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.module.post.dto.PostVO;
import com.buddy.module.user.dto.UserVO;

import java.util.Map;

/**
 * 管理端-用户管理服务接口（D-009：管理端接口归属各业务模块）
 */
public interface AdminUserService {

    IPage<UserVO> getUserList(int page, int size, String keyword);

    UserVO getUserDetail(Long userId);

    IPage<PostVO> getUserPosts(Long userId, int page, int size);

    IPage<Map<String, Object>> getUserApplies(Long userId, int page, int size);

    IPage<Map<String, Object>> getUserReviews(Long userId, int page, int size);

    void banUser(Long userId);

    void unbanUser(Long userId);

    void resetCredit(Long userId, Integer creditScore);
}
