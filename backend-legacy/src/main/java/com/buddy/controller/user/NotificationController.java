package com.buddy.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.dto.response.NotificationVO;
import com.buddy.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知中心控制器（V1.1）
 */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 通知列表（分页）
     */
    @GetMapping("/list")
    public R<IPage<NotificationVO>> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        return R.ok(notificationService.getNotifications(userId, page, size));
    }

    /**
     * 未读通知数（角标）
     */
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
    @PutMapping("/read/{id}")
    public R<Void> markRead(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        notificationService.markRead(userId, id);
        return R.ok();
    }

    /**
     * 全部已读
     */
    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        Long userId = UserContext.getUserId();
        notificationService.markAllRead(userId);
        return R.ok();
    }
}
