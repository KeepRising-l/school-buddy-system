package com.buddy.common.store;

import com.buddy.common.store.mapper.AuxKvMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MySQL 辅助存储测试：首次写入 / 已存在冲突 / 计数 / 清理
 */
@ExtendWith(MockitoExtension.class)
class MysqlAuxStoreTest {

    @Mock
    private AuxKvMapper auxKvMapper;

    private MysqlAuxStore store;

    @BeforeEach
    void setUp() {
        store = new MysqlAuxStore();
        ReflectionTestUtils.setField(store, "auxKvMapper", auxKvMapper);
    }

    @Test
    void setIfAbsentReturnsTrueWhenInserted() {
        assertThat(store.setIfAbsent("k1", Duration.ofMinutes(10))).isTrue();
    }

    @Test
    void setIfAbsentReturnsFalseWhenKeyStillValid() {
        when(auxKvMapper.insert(any(AuxKv.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThat(store.setIfAbsent("k1", Duration.ofMinutes(10))).isFalse();
    }

    @Test
    void incrementReturnsLatestCount() {
        when(auxKvMapper.selectValue("k1")).thenReturn(3L);

        assertThat(store.increment("k1", Duration.ofHours(1))).isEqualTo(3L);
        verify(auxKvMapper).upsertIncrement(anyString(), any());
    }

    @Test
    void getPassesThroughMapperValue() {
        when(auxKvMapper.selectValue("k1")).thenReturn(5L);

        assertThat(store.get("k1")).isEqualTo(5L);
    }

    @Test
    void deleteDelegatesToMapper() {
        store.delete("k1");

        verify(auxKvMapper).deleteByKey("k1");
    }
}
