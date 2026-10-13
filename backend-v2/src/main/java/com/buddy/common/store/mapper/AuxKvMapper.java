package com.buddy.common.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buddy.common.store.AuxKv;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 辅助键值表 Mapper（MySQL 版 AuxStore 的底层访问）
 *
 * <p>注意：本接口刻意放在 {@code mapper} 子包，而不是和 {@code AuxStore} 同包。
 * 因为 {@code @MapperScan} 是按包扫描接口的——若扫到 {@code com.buddy.common.store}，
 * {@code AuxStore} 自己也会被注册成 MyBatis Mapper（且 Bean 名恰为 {@code auxStore}，
 * 会按字段名优先注入），运行时会抛 "Invalid bound statement (not found): ...AuxStore.get"。
 * 同类问题在单元测试里发现不了（测试直接 mock 了 AuxStore）。</p>
 */
public interface AuxKvMapper extends BaseMapper<AuxKv> {

    /** 读取未过期的值 */
    @Select("SELECT v FROM aux_kv WHERE k = #{key} AND expire_time > NOW()")
    Long selectValue(@Param("key") String key);

    /** 清理指定键的过期记录（过期即视为不存在，可重新创建） */
    @Delete("DELETE FROM aux_kv WHERE k = #{key} AND expire_time <= NOW()")
    int purgeExpired(@Param("key") String key);

    /** 计数：不存在则创建，存在则 +1（不改变原过期时间） */
    @Insert("INSERT INTO aux_kv (k, v, expire_time) VALUES (#{key}, 1, #{expireTime}) " +
            "ON DUPLICATE KEY UPDATE v = v + 1")
    int upsertIncrement(@Param("key") String key, @Param("expireTime") LocalDateTime expireTime);

    /** 刷新过期时间 */
    @Update("UPDATE aux_kv SET expire_time = #{expireTime} WHERE k = #{key}")
    int touch(@Param("key") String key, @Param("expireTime") LocalDateTime expireTime);

    /** 删除键 */
    @Delete("DELETE FROM aux_kv WHERE k = #{key}")
    int deleteByKey(@Param("key") String key);

    /** 清理全部过期键（定时任务调用） */
    @Delete("DELETE FROM aux_kv WHERE expire_time <= NOW()")
    int purgeAllExpired();
}
