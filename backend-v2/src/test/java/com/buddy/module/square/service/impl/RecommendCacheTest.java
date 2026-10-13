package com.buddy.module.square.service.impl;

import com.buddy.module.post.entity.Post;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 推荐进程内缓存测试（缺口#2）：命中 / 未命中 / 过期 / 覆盖
 */
class RecommendCacheTest {

    private static Post post(long id) {
        Post p = new Post();
        p.setId(id);
        return p;
    }

    @Test
    void returnsCachedListBeforeExpiry() {
        RecommendCache cache = new RecommendCache(5);
        cache.put("k", List.of(post(1), post(2)));

        assertThat(cache.get("k")).hasSize(2);
    }

    @Test
    void returnsNullForUnknownKey() {
        assertThat(new RecommendCache(5).get("nope")).isNull();
    }

    @Test
    void expiresAfterTtl() throws Exception {
        RecommendCache cache = new RecommendCache(0);
        cache.put("k", List.of(post(1)));
        Thread.sleep(5);

        assertThat(cache.get("k")).isNull();
    }

    @Test
    void overwritesSameKey() {
        RecommendCache cache = new RecommendCache(5);
        cache.put("k", List.of(post(1)));
        cache.put("k", List.of(post(1), post(2), post(3)));

        assertThat(cache.get("k")).hasSize(3);
    }
}
