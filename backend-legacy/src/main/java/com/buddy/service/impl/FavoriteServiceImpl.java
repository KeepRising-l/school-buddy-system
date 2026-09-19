package com.buddy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.dto.response.PostVO;
import com.buddy.entity.Favorite;
import com.buddy.entity.Post;
import com.buddy.entity.User;
import com.buddy.mapper.FavoriteMapper;
import com.buddy.mapper.PostMapper;
import com.buddy.mapper.UserMapper;
import com.buddy.service.CreditService;
import com.buddy.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏服务实现类（V1.1）
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CreditService creditService;

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
            voPage.setRecords(new java.util.ArrayList<>());
            return voPage;
        }

        // 批量查询帖子（避免N+1），并过滤下架/未过审的（修复：收藏列表出现幽灵数据）
        List<Post> posts = postMapper.selectBatchIds(postIds).stream()
                .filter(p -> p != null
                        && p.getIsAuditPass() != null && p.getIsAuditPass() == 1
                        && !"CANCELLED".equals(p.getStatus()))
                .collect(Collectors.toList());
        voPage.setRecords(posts.stream()
                .map(this::convertToPostVO)
                .collect(Collectors.toList()));
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

    /**
     * 帖子实体转VO（含发布者信息）
     */
    private PostVO convertToPostVO(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setCategory(post.getCategory());
        vo.setTagIds(post.getTagIds());
        vo.setStartTime(post.getStartTime());
        vo.setEndTime(post.getEndTime());
        vo.setLocation(post.getLocation());
        vo.setMaxPeople(post.getMaxPeople());
        vo.setCurrentPeople(post.getCurrentPeople());
        vo.setStatus(post.getStatus());
        vo.setViewCount(post.getViewCount());
        vo.setPublisherId(post.getPublisherId());
        vo.setCreatedTime(post.getCreatedTime());
        vo.setDescription(post.getDescription());
        vo.setIsAuditPass(post.getIsAuditPass());
        vo.setRequireLevel(post.getRequireLevel());
        vo.setRequireGender(post.getRequireGender());

        User publisher = userMapper.selectById(post.getPublisherId());
        if (publisher != null) {
            vo.setPublisherNickname(publisher.getNickname());
            vo.setPublisherAvatar(publisher.getAvatar());
            vo.setPublisherCreditLevel(creditService.getCreditLevel(publisher.getCreditScore()));
        }
        return vo;
    }
}
