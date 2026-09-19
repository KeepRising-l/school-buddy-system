package com.buddy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buddy.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏 Mapper（V1.1）
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
