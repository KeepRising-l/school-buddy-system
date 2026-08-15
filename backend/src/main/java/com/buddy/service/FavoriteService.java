package com.buddy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.dto.response.PostVO;

/**
 * 收藏服务接口（V1.1）
 */
public interface FavoriteService {

    /**
     * 收藏帖子
     */
    void add(Long userId, Long postId);

    /**
     * 取消收藏
     */
    void cancel(Long userId, Long postId);

    /**
     * 我的收藏列表（分页，含帖子信息）
     */
    IPage<PostVO> getMyFavorites(Long userId, int page, int size);

    /**
     * 是否已收藏
     */
    boolean isFavorite(Long userId, Long postId);

    /**
     * 帖子收藏数
     */
    long getFavoriteCount(Long postId);
}
