package com.buddy.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.buddy.common.constant.PostStatus;
import com.buddy.entity.Post;
import com.buddy.mapper.PostMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 帖子状态定时任务
 * 自动推进帖子生命周期：招募中/已满 -> 进行中 -> 已结束
 */
@Slf4j
@Component
@EnableScheduling
public class PostStatusTask {

    @Autowired
    private PostMapper postMapper;

    /**
     * 每5分钟执行：检查到达start_time的帖子 -> RECRUITING/FULL -> ONGOING
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void checkPostToOngoing() {
        log.info("定时任务：检查帖子状态 RECRUITING/FULL -> ONGOING");
        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Post::getStatus, Arrays.asList(PostStatus.RECRUITING, PostStatus.FULL))
                // 修复：只推进已审核通过的帖子，待审核的到点也不应变成进行中
                .eq(Post::getIsAuditPass, 1)
                .le(Post::getStartTime, LocalDateTime.now())
                .set(Post::getStatus, PostStatus.ONGOING);
        int count = postMapper.update(null, wrapper);
        if (count > 0) {
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
            log.info("已将 {} 条帖子状态更新为 FINISHED", count);
        }
    }
}
