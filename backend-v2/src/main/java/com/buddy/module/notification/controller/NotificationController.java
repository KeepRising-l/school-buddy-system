package com.buddy.module.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.module.notification.dto.NotificationVO;
import com.buddy.module.notification.service.NotificationService;
import com.buddy.common.annotation.RequireLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知中心控制器（用户端，等价迁移自旧版）
 */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 通知列表（分页）
     *
     * @param type 可选，按通知类型筛选（支持逗号分隔多类型，如 APPLY_NEW,APPLY_RESULT）
     */
    @RequireLogin
    @GetMapping("/list")
    public R<IPage<NotificationVO>> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        Long userId = UserContext.getUserId();
        return R.ok(notificationService.getNotifications(userId, page, size, type));
    }

    /**
     * 未读通知数（角标）
     */
    @RequireLogin
    @GetMapping("/unread-count")
    public R<Map<String, Object>> getUnreadCount() {
        Long userId = UserContext.getUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("count", notificationService.getUnreadCount(userId));
        return R.ok(data);
    }

    /**
     * 标记单条已读
     */
    @RequireLogin
    @PutMapping("/read/{id}")
    public R<Void> markRead(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        notificationService.markRead(userId, id);
        return R.ok();
    }

    /**
     * 全部已读
     */
    @RequireLogin
    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        Long userId = UserContext.getUserId();
        notificationService.markAllRead(userId);
        return R.ok();
    }
}
