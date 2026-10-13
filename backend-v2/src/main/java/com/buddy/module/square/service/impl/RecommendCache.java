package com.buddy.module.square.service.impl;

import com.buddy.module.post.entity.Post;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推荐结果进程内缓存（缺口#2）
 *
 * <p>按"用户 + 筛选条件"缓存打分排序后的帖子列表，默认 TTL 5 分钟。
 * 单实例部署下的轻量实现；如改为多实例部署，可替换为 Redis 实现（对外形态不变）。</p>
 */
class RecommendCache {

    /** 容量上限：超出时先清过期项，仍超则移除一项（防内存无限增长） */
    private static final int MAX_ENTRIES = 200;

    private final long ttlMillis;
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    /**
     * @param ttlMinutes 缓存有效期（分钟）
     */
    RecommendCache(int ttlMinutes) {
        this.ttlMillis = ttlMinutes * 60_000L;
    }

    /**
     * 读取缓存；过期返回 null 并清除该键
     */
    List<Post> get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            store.remove(key, entry);
            return null;
        }
        return entry.posts;
    }

    /**
     * 写入缓存
     */
    void put(String key, List<Post> posts) {
        if (store.size() >= MAX_ENTRIES) {
            store.entrySet().removeIf(e -> System.currentTimeMillis() > e.getValue().expireAt);
            if (store.size() >= MAX_ENTRIES) {
                store.keySet().stream().findFirst().ifPresent(store::remove);
            }
        }
        store.put(key, new Entry(posts, System.currentTimeMillis() + ttlMillis));
    }

    private static final class Entry {
        final List<Post> posts;
        final long expireAt;

        Entry(List<Post> posts, long expireAt) {
            this.posts = posts;
            this.expireAt = expireAt;
        }
    }
}
