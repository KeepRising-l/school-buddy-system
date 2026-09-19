package com.buddy.module.post.service;

import com.buddy.module.post.dto.request.PostCreateRequest;
import com.buddy.module.post.dto.PostDetailVO;

/**
 * 帖子服务接口
 * 说明：旧版 PostService.getPostList 无任何调用方（列表走 square/推荐与管理端接口），等价迁移时裁剪
 */
public interface PostService {

    /**
     * 创建帖子
     */
    Long createPost(Long userId, PostCreateRequest request);

    /**
     * 获取帖子详情
     */
    PostDetailVO getPostDetail(Long userId, Long postId);

    /**
     * 取消帖子
     */
    void cancelPost(Long userId, Long postId);

    /**
     * 增加浏览量（Redis去重）
     */
    void incrementViewCount(Long postId, Long userId);
}
