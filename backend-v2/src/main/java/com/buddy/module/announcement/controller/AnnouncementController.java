package com.buddy.module.announcement.controller;

import com.buddy.common.result.R;
import com.buddy.module.announcement.entity.SysAnnouncement;
import com.buddy.module.announcement.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端公告控制器（等价迁移自旧版）
 */
@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 获取最新弹窗公告（用户端首页加载时调用）
     */
    @GetMapping("/latest-popup")
    public R<SysAnnouncement> getLatestPopup() {
        return R.ok(announcementService.getLatestPopup());
    }
}
