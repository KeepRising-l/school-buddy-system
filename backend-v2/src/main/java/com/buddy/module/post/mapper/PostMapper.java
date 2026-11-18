package com.buddy.module.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buddy.module.post.entity.Post;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 帖子 Mapper 接口
 */
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

    /**
     * 原子增加浏览量（消除先 selectById 再 updateById 的并发丢失与多余查询）
     */
    @Update("UPDATE post SET view_count = view_count + 1 WHERE id = #{postId} AND deleted = 0")
    int incrementViewCount(@Param("postId") Long postId);

    /**
     * 按天聚合发帖数（替代循环 N 次按天查询）
     */
    @Select("SELECT DATE(created_time) AS d, COUNT(*) AS c FROM post " +
            "WHERE deleted = 0 AND created_time >= #{start} " +
            "GROUP BY DATE(created_time) ORDER BY d")
    List<Map<String, Object>> countDailyPostsSince(@Param("start") LocalDateTime start);

    /**
     * 按天聚合成功成局数（状态为 FINISHED）
     */
    @Select("SELECT DATE(updated_time) AS d, COUNT(*) AS c FROM post " +
            "WHERE deleted = 0 AND status = 'FINISHED' AND updated_time >= #{start} " +
            "GROUP BY DATE(updated_time) ORDER BY d")
    List<Map<String, Object>> countDailyFinishedSince(@Param("start") LocalDateTime start);

    /**
     * 查询指定起始时间以来每天发帖的用户（用于活跃用户趋势单次聚合去重）
     */
    @Select("SELECT DISTINCT DATE(created_time) AS d, publisher_id AS uid FROM post " +
            "WHERE deleted = 0 AND created_time >= #{start}")
    List<Map<String, Object>> selectDailyPublishersSince(@Param("start") LocalDateTime start);
}
