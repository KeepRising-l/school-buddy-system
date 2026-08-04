package com.buddy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.dto.request.PostCreateRequest;
import com.buddy.dto.response.PostDetailVO;
import com.buddy.dto.response.PostVO;

/**
 * 帖子服务接口
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
     * 获取帖子列表（分页）
     */
    IPage<PostVO> getPostList(int page, int size, String category, String sort);

    /**
     * 取消帖子
     */
    void cancelPost(Long userId, Long postId);

    /**
     * 增加浏览量（Redis去重）
     */
    void incrementViewCount(Long postId, Long userId);
}
