package com.buddy.module.post.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import com.buddy.common.result.ResultCode;
import com.buddy.module.post.dto.PostVO;
import com.buddy.module.post.service.AdminPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端-帖子管理控制器（等价迁移自旧版，D-009 归属 post 模块）
 */
@RestController
@RequestMapping("/api/admin/post")
public class AdminPostController {

    @Autowired
    private AdminPostService adminPostService;

    /**
     * 帖子列表（支持状态/分类/审核态筛选）
     */
    @RequireAdmin
    @GetMapping("/list")
    public R<IPage<PostVO>> getPostList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer isAuditPass) {
        return R.ok(adminPostService.getPostList(page, size, status, category, isAuditPass));
    }

    /**
     * 下架帖子
     */
    @RequireAdmin
    @PostMapping("/takedown/{id}")
    public R<Void> takedownPost(@PathVariable Long id) {
        adminPostService.takedownPost(id);
        return R.ok();
    }

    /**
     * 审核帖子（兜底接口，body: {pass: true/false, reason: "可选原因"}）
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
        // 拒绝原因（拒绝时可选，用于通知发布者明确违规点）
        String reason = body == null ? null : (String) body.get("reason");
        adminPostService.auditPost(id, pass, reason);
        return R.ok();
    }

    /**
     * 删除帖子（D-007：逻辑删除+级联）
     */
    @RequireAdmin
    @DeleteMapping("/delete/{id}")
    public R<Void> deletePost(@PathVariable Long id) {
        adminPostService.deletePost(id);
        return R.ok();
    }
}
