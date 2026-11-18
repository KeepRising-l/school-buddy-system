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
        // 优先恢复曾取消收藏（deleted=1）的记录，规避 uk_user_post 唯一索引冲突
        int restored = favoriteMapper.restoreDeleted(userId, postId);
        if (restored > 0) {
            return;
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
        // SQL 层关联分页查询（过滤已下架/已删除帖子，保留已取消帖子以便用户识别并清理收藏），
        // 保证返回记录数与分页总数严格一致，避免内存过滤导致前端提前终止翻页
        IPage<Post> postPage = favoriteMapper.selectFavoritePostsPage(new Page<>(page, size), userId);
        if (postPage == null || postPage.getRecords() == null || postPage.getRecords().isEmpty()) {
            Page<PostVO> emptyPage = new Page<>(page, size, 0);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }
        IPage<PostVO> voPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        voPage.setRecords(postConverter.toVOList(postPage.getRecords()));
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
