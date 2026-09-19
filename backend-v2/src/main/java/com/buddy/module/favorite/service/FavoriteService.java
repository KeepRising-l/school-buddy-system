package com.buddy.module.favorite.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.module.post.dto.PostVO;

/**
 * 收藏服务接口
 */
public interface FavoriteService {

    void add(Long userId, Long postId);

    void cancel(Long userId, Long postId);

    IPage<PostVO> getMyFavorites(Long userId, int page, int size);

    boolean isFavorite(Long userId, Long postId);

    long getFavoriteCount(Long postId);
}
