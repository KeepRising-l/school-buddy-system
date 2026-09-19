package com.buddy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.dto.response.CommentVO;
import com.buddy.entity.PostComment;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 添加评论
     */
    void addComment(Long userId, Long postId, String content);

    /**
     * 获取帖子的评论列表（分页，含用户信息）
     */
    IPage<CommentVO> getComments(Long postId, int page, int size);
}
