package com.buddy.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.PostStatus;
import com.buddy.entity.Notification;
import com.buddy.entity.Post;
import com.buddy.entity.PostApply;
import com.buddy.mapper.NotificationMapper;
import com.buddy.mapper.PostApplyMapper;
import com.buddy.mapper.PostMapper;
import com.buddy.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动提醒定时任务（V1.1）
 * 活动开始前 30 分钟，通知发布者与已通过报名者
 */
@Slf4j
@Component
public class ActivityRemindTask {

    /** 提醒提前量（分钟） */
    private static final long REMIND_BEFORE_MINUTES = 30;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostApplyMapper postApplyMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private NotificationService notificationService;

    /**
     * 每5分钟执行：扫描 [now, now+30min] 开始且状态为 RECRUITING/FULL/ONGOING 的帖子
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void checkActivityRemind() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime remindDeadline = now.plusMinutes(REMIND_BEFORE_MINUTES);

        // 查询即将开始的帖子
        List<Post> posts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .in(Post::getStatus, PostStatus.RECRUITING, PostStatus.FULL, PostStatus.ONGOING)
                        .gt(Post::getStartTime, now)
                        .le(Post::getStartTime, remindDeadline)
        );
        if (posts.isEmpty()) {
            return;
        }

        int remindCount = 0;
        for (Post post : posts) {
            // 发布者提醒
            if (sendRemindIfNotSent(post, post.getPublisherId())) {
                remindCount++;
            }

            // 已通过报名者提醒
            List<PostApply> applies = postApplyMapper.selectList(
                    new LambdaQueryWrapper<PostApply>()
                            .eq(PostApply::getPostId, post.getId())
                            .eq(PostApply::getStatus, ApplyStatus.APPROVED)
            );
            for (PostApply apply : applies) {
                if (sendRemindIfNotSent(post, apply.getApplicantId())) {
                    remindCount++;
                }
            }
        }

        if (remindCount > 0) {
            log.info("活动提醒任务：共发送 {} 条活动提醒", remindCount);
        }
    }

    /**
     * 发送提醒并做幂等去重：同一用户对同一帖子只提醒一次
     * （通过查询该用户该帖是否已存在 ACTIVITY_REMIND 通知判断）
     *
     * @return 是否实际发送
     */
    private boolean sendRemindIfNotSent(Post post, Long userId) {
        try {
            // 幂等：已存在该用户该帖的活动提醒则跳过
            Long sentCount = notificationMapper.selectCount(
                    new LambdaQueryWrapper<Notification>()
                            .eq(Notification::getUserId, userId)
                            .eq(Notification::getBizId, post.getId())
                            .eq(Notification::getType, Notification.TYPE_ACTIVITY_REMIND)
            );
            if (sentCount > 0) {
                return false;
            }

            notificationService.send(userId, Notification.TYPE_ACTIVITY_REMIND,
                    "活动即将开始",
                    "你参加的组局「" + post.getTitle() + "」将于 " + formatTime(post.getStartTime()) + " 开始，地点：" + post.getLocation(),
                    "/post/" + post.getId(), post.getId());
            return true;
        } catch (Exception e) {
            log.error("活动提醒发送失败: postId={}, userId={}", post.getId(), userId, e);
            return false;
        }
    }

    /**
     * 格式化开始时间为可读文本
     */
    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.getMonthValue() + "月" + time.getDayOfMonth() + "日 "
                + String.format("%02d", time.getHour()) + ":" + String.format("%02d", time.getMinute());
    }
}
