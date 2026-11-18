package com.buddy.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buddy.module.user.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 锁定用户行（SELECT ... FOR UPDATE），用于串行化同一用户的并发写操作。
     * 必须在事务内调用。
     *
     * @param id 用户ID
     * @return 用户ID（不存在时为 null）
     */
    @Select("SELECT id FROM sys_user WHERE id = #{id} FOR UPDATE")
    Long lockById(@Param("id") Long id);

    /**
     * 按天统计新增注册用户数（用于单次聚合计算累计用户增长趋势，消除 N 次循环查询）
     */
    @Select("SELECT DATE(created_time) AS d, COUNT(*) AS c FROM sys_user " +
            "WHERE created_time >= #{start} " +
            "GROUP BY DATE(created_time) ORDER BY d")
    List<Map<String, Object>> countDailyNewUsersSince(@Param("start") LocalDateTime start);
}
