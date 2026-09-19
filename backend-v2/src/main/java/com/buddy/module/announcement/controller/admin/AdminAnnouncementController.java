package com.buddy.module.announcement.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.module.announcement.entity.SysAnnouncement;
import com.buddy.module.announcement.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-公告管理控制器（等价迁移自旧版，D-009 归属 announcement 模块）
 */
@RestController
@RequestMapping("/api/admin/announcement")
public class AdminAnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 公告列表（支持状态筛选）
     */
    @RequireAdmin
    @GetMapping("/list")
    public R<IPage<SysAnnouncement>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return R.ok(announcementService.getList(page, size, status));
    }

    /**
     * 新增公告（默认关闭态）
     */
    @RequireAdmin
    @PostMapping("/add")
    public R<Long> add(@RequestBody SysAnnouncement announcement) {
        Long adminId = UserContext.getUserId();
        return R.ok(announcementService.add(announcement, adminId));
    }

    /**
     * 修改公告（启用中需先关闭）
     */
    @RequireAdmin
    @PutMapping("/update")
    public R<Void> update(@RequestBody SysAnnouncement announcement) {
        Long adminId = UserContext.getUserId();
        announcementService.update(announcement, adminId);
        return R.ok();
    }

    /**
     * 发布公告
     */
    @RequireAdmin
    @PostMapping("/publish/{id}")
    public R<Void> publish(@PathVariable Long id) {
        Long adminId = UserContext.getUserId();
        announcementService.publish(id, adminId);
        return R.ok();
    }

    /**
     * 关闭公告
     */
    @RequireAdmin
    @PostMapping("/close/{id}")
    public R<Void> close(@PathVariable Long id) {
        Long adminId = UserContext.getUserId();
        announcementService.close(id, adminId);
        return R.ok();
    }

    /**
     * 删除公告
     */
    @RequireAdmin
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return R.ok();
    }
}
