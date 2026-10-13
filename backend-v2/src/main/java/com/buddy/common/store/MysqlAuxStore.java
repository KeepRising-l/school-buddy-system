package com.buddy.common.store;

import com.buddy.common.store.mapper.AuxKvMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 辅助功能存储 —— MySQL 实现（默认）
 *
 * <p>单表 aux_kv 承载"键值 + 过期时间"；零外部依赖，方便在任何电脑上直接运行。
 * 并发语义：依赖主键唯一约束保证"仅当不存在时写入"，计数使用原子 UPSERT。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.aux.store", havingValue = "mysql", matchIfMissing = true)
public class MysqlAuxStore implements AuxStore {

    @Autowired
    private AuxKvMapper auxKvMapper;

    @Override
    public Long get(String key) {
        return auxKvMapper.selectValue(key);
    }

    @Override
    public boolean setIfAbsent(String key, Duration ttl) {
        // 先清掉已过期的同键记录，再依赖主键唯一约束：并发下只有一个请求能插入成功
        auxKvMapper.purgeExpired(key);
        AuxKv kv = new AuxKv();
        kv.setK(key);
        kv.setV(1L);
        kv.setExpireTime(LocalDateTime.now().plus(ttl));
        try {
            auxKvMapper.insert(kv);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public long increment(String key, Duration ttl) {
        auxKvMapper.purgeExpired(key);
        auxKvMapper.upsertIncrement(key, LocalDateTime.now().plus(ttl));
        Long value = auxKvMapper.selectValue(key);
        return value != null ? value : 1L;
    }

    @Override
    public void expire(String key, Duration ttl) {
        auxKvMapper.touch(key, LocalDateTime.now().plus(ttl));
    }

    @Override
    public void delete(String key) {
        auxKvMapper.deleteByKey(key);
    }

    /** 每 30 分钟清理一次过期键，防止表膨胀 */
    @Scheduled(cron = "0 */30 * * * *")
    public void cleanupExpired() {
        int removed = auxKvMapper.purgeAllExpired();
        if (removed > 0) {
            log.debug("aux_kv 清理过期键 {} 条", removed);
        }
    }
}
