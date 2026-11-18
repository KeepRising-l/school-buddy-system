package com.buddy.module.post.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.buddy.common.cache.CacheEpoch;
import com.buddy.common.constant.PostStatus;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 帖子状态定时任务（等价迁移自旧版）
 * 自动推进帖子生命周期：招募中/已满 -> 进行中 -> 已结束
 */
@Slf4j
@Component
public class PostStatusTask {

    @Autowired
    private PostMapper postMapper;

    @Autowired(required = false)
    private com.buddy.module.apply.mapper.PostApplyMapper postApplyMapper;

    @Autowired(required = false)
    private CacheEpoch cacheEpoch;

    /**
     * 每5分钟执行：检查到达start_time的帖子 -> RECRUITING/FULL -> ONGOING
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void checkPostToOngoing() {
        log.info("定时任务：检查帖子状态 RECRUITING/FULL -> ONGOING");
        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Post::getStatus, Arrays.asList(PostStatus.RECRUITING, PostStatus.FULL))
                // 只推进已审核通过的帖子
                .eq(Post::getIsAuditPass, 1)
                .le(Post::getStartTime, LocalDateTime.now())
                .set(Post::getStatus, PostStatus.ONGOING);
        int count = postMapper.update(null, wrapper);
        if (count > 0) {
            if (cacheEpoch != null) {
                cacheEpoch.bump();
            }
            log.info("已将 {} 条帖子状态更新为 ONGOING", count);
        }
    }

    /**
     * 每5分钟执行：检查到达end_time的帖子 -> ONGOING -> FINISHED
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void checkPostToFinished() {
        log.info("定时任务：检查帖子状态 ONGOING -> FINISHED");
        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Post::getStatus, PostStatus.ONGOING)
                .le(Post::getEndTime, LocalDateTime.now())
                .set(Post::getStatus, PostStatus.FINISHED);
        int count = postMapper.update(null, wrapper);
        if (count > 0) {
            if (cacheEpoch != null) {
                cacheEpoch.bump();
            }
            log.info("已将 {} 条帖子状态更新为 FINISHED", count);
        }
    }

    /**
     * 每10分钟执行：活动结束超过24小时且发布者未标记出勤的 APPROVED 报名，自动默认转为 ATTENDED（守约兜底），
     * 解决发布者忘记标记出勤导致参与者永远无法评价的闭环死锁
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void settleDefaultAttendance() {
        if (postApplyMapper == null) {
            return;
        }
        LocalDateTime deadline = LocalDateTime.now().minusHours(24);
        java.util.List<Post> expiredFinishedPosts = postMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Post>()
                        .select(Post::getId)
                        .eq(Post::getStatus, PostStatus.FINISHED)
                        .le(Post::getEndTime, deadline)
        );
        if (expiredFinishedPosts == null || expiredFinishedPosts.isEmpty()) {
            return;
        }
        java.util.List<Long> postIds = new java.util.ArrayList<>(expiredFinishedPosts.size());
        for (Post p : expiredFinishedPosts) {
            if (p != null && p.getId() != null) {
                postIds.add(p.getId());
            }
        }
        if (postIds.isEmpty()) {
            return;
        }
        int settled = postApplyMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.buddy.module.apply.entity.PostApply>()
                        .in(com.buddy.module.apply.entity.PostApply::getPostId, postIds)
                        .eq(com.buddy.module.apply.entity.PostApply::getStatus, com.buddy.common.constant.ApplyStatus.APPROVED)
                        .set(com.buddy.module.apply.entity.PostApply::getStatus, com.buddy.common.constant.ApplyStatus.ATTENDED)
                        .set(com.buddy.module.apply.entity.PostApply::getHandleTime, LocalDateTime.now()));
        if (settled > 0) {
            log.info("超时24h未标记出勤自动守约兜底：已将 {} 条报名更新为 ATTENDED", settled);
        }
    }
}
