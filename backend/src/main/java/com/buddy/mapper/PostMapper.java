package com.buddy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buddy.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 帖子 Mapper 接口
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 原子增加当前人数（仅当未满员时），防止并发超卖
     *
     * @param postId 帖子ID
     * @return 受影响行数，0 表示已满员或帖子不存在
     */
    @Update("UPDATE post SET current_people = current_people + 1 " +
            "WHERE id = #{postId} AND current_people < max_people")
    int incrementCurrentPeople(@Param("postId") Long postId);

    /**
     * 原子减少当前人数（不低于 1，发布者本身占 1 个名额）
     *
     * @param postId 帖子ID
     * @return 受影响行数
     */
    @Update("UPDATE post SET current_people = current_people - 1 " +
            "WHERE id = #{postId} AND current_people > 1")
    int decrementCurrentPeople(@Param("postId") Long postId);
}
