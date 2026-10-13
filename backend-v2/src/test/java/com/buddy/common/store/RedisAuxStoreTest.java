package com.buddy.common.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 辅助存储测试（可选实现，保证切换后行为与 MySQL 版一致）
 */
@ExtendWith(MockitoExtension.class)
class RedisAuxStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private RedisAuxStore store;

    @BeforeEach
    void setUp() {
        store = new RedisAuxStore();
        ReflectionTestUtils.setField(store, "redisTemplate", redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void setIfAbsentMapsToSetNxWithTtl() {
        when(valueOps.setIfAbsent(eq("k1"), eq("1"), any(Duration.class))).thenReturn(true);

        assertThat(store.setIfAbsent("k1", Duration.ofMinutes(10))).isTrue();
    }

    @Test
    void incrementSetsTtlOnlyOnFirstWrite() {
        when(valueOps.increment("k1")).thenReturn(1L);

        assertThat(store.increment("k1", Duration.ofHours(1))).isEqualTo(1L);
        verify(redisTemplate).expire(eq("k1"), any(Duration.class));
    }

    @Test
    void getParsesStoredValue() {
        when(valueOps.get("k1")).thenReturn("7");

        assertThat(store.get("k1")).isEqualTo(7L);
    }
}
