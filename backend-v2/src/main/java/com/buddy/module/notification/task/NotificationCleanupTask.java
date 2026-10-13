package com.buddy.module.notification.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buddy.module.notification.entity.Notification;
import com.buddy.module.notification.mapper.NotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 通知清理定时任务（缺口#4）：删除超过 90 天的历史通知，防止表无限增长。
 */
@Slf4j
@Component
public class NotificationCleanupTask {

    /** 通知保留天数 */
    private static final int KEEP_DAYS = 90;

    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 每天凌晨 4:30 清理过期通知
     */
    @Scheduled(cron = "0 30 4 * * ?")
    public void cleanupExpired() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(KEEP_DAYS);
        int removed = notificationMapper.delete(
                new LambdaQueryWrapper<Notification>().lt(Notification::getCreatedTime, deadline)
        );
        if (removed > 0) {
            log.info("通知清理任务：删除 {} 条超过 {} 天的历史通知", removed, KEEP_DAYS);
        }
    }
}
