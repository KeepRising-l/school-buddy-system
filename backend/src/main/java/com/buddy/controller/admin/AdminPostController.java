package com.buddy.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import com.buddy.common.result.ResultCode;
import com.buddy.dto.response.PostVO;
import com.buddy.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端-帖子管理控制器
 */
@RestController
@RequestMapping("/api/admin/post")
public class AdminPostController {

    @Autowired
    private AdminService adminService;

    /**
     * 帖子列表（支持审核状态筛选）
     */
    @RequireAdmin
    @GetMapping("/list")
    public R<IPage<PostVO>> getPostList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer isAuditPass) {
        return R.ok(adminService.getPostList(page, size, status, category, isAuditPass));
    }

    /**
     * 下架帖子
     */
    @RequireAdmin
    @PostMapping("/takedown/{id}")
    public R<Void> takedownPost(@PathVariable Long id) {
        adminService.takedownPost(id);
        return R.ok();
    }

    /**
     * 审核帖子
     * body: {pass: true}
     */
    @RequireAdmin
    @PostMapping("/audit/{id}")
    public R<Void> auditPost(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object passObj = body == null ? null : body.get("pass");
        if (passObj == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少审核结果参数");
        }
        boolean pass;
        if (passObj instanceof Boolean) {
            pass = (Boolean) passObj;
        } else if ("true".equalsIgnoreCase(passObj.toString())) {
            pass = true;
        } else if ("false".equalsIgnoreCase(passObj.toString())) {
            pass = false;
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "审核结果参数不合法");
        }
        adminService.auditPost(id, pass);
        return R.ok();
    }

    /**
     * 物理删除帖子
     */
    @RequireAdmin
    @DeleteMapping("/delete/{id}")
    public R<Void> deletePost(@PathVariable Long id) {
        adminService.deletePost(id);
        return R.ok();
    }
}
