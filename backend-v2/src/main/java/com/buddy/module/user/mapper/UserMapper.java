package com.buddy.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buddy.module.user.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
