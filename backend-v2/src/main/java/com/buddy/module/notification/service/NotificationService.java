package com.buddy.module.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.module.notification.dto.NotificationVO;

/**
 * 通知中心服务接口（等价迁移自旧版）
 * 供各业务模块调用（模块间只走 Service 接口）
 */
public interface NotificationService {

    /**
     * 发送通知（供各业务模块调用，与业务操作同一事务）
     *
     * @param userId  接收人
     * @param type    通知类型（见 Notification.TYPE_* 常量，7 类）
     * @param title   标题
     * @param content 内容
     * @param link    跳转路径（可空）
     * @param bizId   关联对象ID（可空）
     */
    void send(Long userId, String type, String title, String content, String link, Long bizId);

    /**
     * 获取用户的通知列表（分页，按时间倒序）
     */
    IPage<NotificationVO> getNotifications(Long userId, int page, int size);

    /**
     * 获取用户未读通知数（角标）
     */
    long getUnreadCount(Long userId);

    /**
     * 标记单条通知已读（校验归属）
     */
    void markRead(Long userId, Long notificationId);

    /**
     * 全部标记已读
     */
    void markAllRead(Long userId);
}
