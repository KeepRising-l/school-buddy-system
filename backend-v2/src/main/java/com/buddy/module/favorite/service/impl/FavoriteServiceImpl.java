package com.buddy.module.favorite.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.module.favorite.entity.Favorite;
import com.buddy.module.favorite.mapper.FavoriteMapper;
import com.buddy.module.favorite.service.FavoriteService;
import com.buddy.module.post.converter.PostConverter;
import com.buddy.module.post.dto.PostVO;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏服务实现类（等价迁移自旧版）
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostConverter postConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, Long postId) {
        // 校验帖子存在
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        // 防重复收藏（唯一约束兜底）
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getPostId, postId)
        );
        if (count > 0) {
            throw new BusinessException("已收藏该组局");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setPostId(postId);
        favoriteMapper.insert(favorite);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long postId) {
        favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getPostId, postId)
        );
    }

    @Override
    public IPage<PostVO> getMyFavorites(Long userId, int page, int size) {
        IPage<Favorite> favoritePage = favoriteMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedTime)
        );

        List<Long> postIds = favoritePage.getRecords().stream()
                .map(Favorite::getPostId)
                .collect(Collectors.toList());

        IPage<PostVO> voPage = new Page<>(favoritePage.getCurrent(), favoritePage.getSize(), favoritePage.getTotal());
        if (postIds.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        // 批量查询帖子（避免N+1），并过滤下架/已取消的（避免收藏列表出现幽灵数据）
        List<Post> posts = postMapper.selectBatchIds(postIds).stream()
                .filter(p -> p != null
                        && p.getIsAuditPass() != null && p.getIsAuditPass() == 1
                        && !"CANCELLED".equals(p.getStatus()))
                .collect(Collectors.toList());
        voPage.setRecords(postConverter.toVOList(posts));
        return voPage;
    }

    @Override
    public boolean isFavorite(Long userId, Long postId) {
        if (userId == null) {
            return false;
        }
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getPostId, postId)
        );
        return count > 0;
    }

    @Override
    public long getFavoriteCount(Long postId) {
        return favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getPostId, postId)
        );
    }
}
