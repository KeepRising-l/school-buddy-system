package com.buddy.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.entity.Announcement;
import com.buddy.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端-系统公告控制器
 */
@RestController
@RequestMapping("/api/admin/announcement")
public class AdminAnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 公告列表
     */
    @RequireAdmin
    @GetMapping("/list")
    public R<IPage<Announcement>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return R.ok(announcementService.getList(page, size, status));
    }

    /**
     * 新增公告
     */
    @RequireAdmin
    @PostMapping("/add")
    public R<Long> add(@RequestBody Announcement announcement) {
        Long adminId = UserContext.getUserId();
        return R.ok(announcementService.add(announcement, adminId));
    }

    /**
     * 编辑公告
     */
    @RequireAdmin
    @PutMapping("/update")
    public R<Void> update(@RequestBody Announcement announcement) {
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
