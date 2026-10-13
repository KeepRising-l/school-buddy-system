package com.buddy.common.cache;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 全局缓存纪元（缓存失效用的轻量机制）
 *
 * <p>问题：推荐列表按「用户 + 筛选条件」做了 5 分钟进程内缓存，好处是翻页不重算；
 * 但帖子集合发生变化（发布 / 取消 / 下架）后，缓存里还是旧的帖子列表——
 * 用户刚发完帖回到广场，5 分钟内看不到自己发的帖子。</p>
 *
 * <p>做法：读缓存时把当前纪元拼进缓存键；任何写操作调用 {@link #bump()} 让纪元 +1，
 * 旧键随即全部失效（无需遍历清理，旧条目会由缓存自身的容量上限与过期逻辑淘汰）。
 * 相比"让帖子模块直接调用推荐模块的清理方法"，这样两边都只依赖 common 包，不产生跨模块耦合。</p>
 */
@Component
public class CacheEpoch {

    private final AtomicLong epoch = new AtomicLong();

    /** 当前纪元（拼进缓存键） */
    public long current() {
        return epoch.get();
    }

    /** 帖子等公共列表数据发生变化后调用，使既有缓存立即失效 */
    public void bump() {
        epoch.incrementAndGet();
    }
}
