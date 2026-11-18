package com.buddy.module.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.module.favorite.entity.Favorite;
import com.buddy.module.post.entity.Post;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 收藏 Mapper
 */
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 恢复已软删除的收藏记录（解决 @TableLogic 软删除与 uk_user_post 唯一索引冲突，避免取消收藏后无法再次收藏）
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 受影响行数（1=恢复成功，0=无软删记录）
     */
    @Update("UPDATE favorite SET deleted = 0, created_time = NOW() " +
            "WHERE user_id = #{userId} AND post_id = #{postId} AND deleted = 1")
    int restoreDeleted(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 关联查询用户收藏的有效帖子分页列表（在 SQL 层过滤已软删与已下架帖子，避免内存过滤破坏分页 total 与数量）
     */
    @Select("SELECT p.* FROM favorite f " +
            "INNER JOIN post p ON f.post_id = p.id " +
            "WHERE f.user_id = #{userId} AND f.deleted = 0 " +
            "AND p.deleted = 0 AND p.is_audit_pass = 1 " +
            "ORDER BY f.created_time DESC")
    IPage<Post> selectFavoritePostsPage(Page<Post> page, @Param("userId") Long userId);
}
