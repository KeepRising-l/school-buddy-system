package com.buddy.common.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 辅助功能存储 —— Redis 实现（可选）
 *
 * <p>把配置 {@code app.aux.store} 改为 {@code redis} 即启用（需自行启动 Redis）。
 * 数据量大或高并发场景下性能优于 MySQL 实现；切换无需改动任何业务代码，
 * 因此如觉得 MySQL 版响应偏慢，可无痛换回 Redis。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.aux.store", havingValue = "redis")
public class RedisAuxStore implements AuxStore {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Long get(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? null : Long.parseLong(value);
    }

    @Override
    public boolean setIfAbsent(String key, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", ttl));
    }

    @Override
    public long increment(String key, Duration ttl) {
        Long count = redisTemplate.opsForValue().increment(key);
        long value = count != null ? count : 1L;
        if (value == 1L) {
            redisTemplate.expire(key, ttl);
        }
        return value;
    }

    @Override
    public void expire(String key, Duration ttl) {
        redisTemplate.expire(key, ttl);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
