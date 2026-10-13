package com.buddy.common.store;

import java.time.Duration;

/**
 * 辅助功能存储（登录失败锁定 / 浏览量去重 / 上传频率限制 / 举报去重）。
 *
 * <p>提供两套实现，通过配置 {@code app.aux.store} 切换，业务代码零改动：</p>
 * <ul>
 *   <li>{@code mysql}（默认）：单表 aux_kv 实现，零外部依赖，任何电脑装好 MySQL 即可运行；</li>
 *   <li>{@code redis}：Redis 实现，数据量大或高并发场景可切换，性能更好（需自行启动 Redis）。</li>
 * </ul>
 */
public interface AuxStore {

    /**
     * 读取值
     *
     * @return 不存在或已过期返回 null
     */
    Long get(String key);

    /**
     * 仅当键不存在（或已过期）时写入
     *
     * @return true=写入成功（首次）；false=键仍然有效（已存在）
     */
    boolean setIfAbsent(String key, Duration ttl);

    /**
     * 计数 +1：键不存在时创建并设置过期时间；已存在时仅自增（不改变原过期时间）
     *
     * @return 自增后的值
     */
    long increment(String key, Duration ttl);

    /**
     * 刷新过期时间（用于"每次失败都续期"的登录锁定语义）
     */
    void expire(String key, Duration ttl);

    /**
     * 删除键
     */
    void delete(String key);
}
