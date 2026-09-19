package com.buddy.module.user.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import com.buddy.common.result.ResultCode;
import com.buddy.module.post.dto.PostVO;
import com.buddy.module.user.dto.UserVO;
import com.buddy.module.user.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端-用户管理控制器（等价迁移自旧版，D-009 归属 user 模块）
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * 用户列表（支持关键词搜索）
     */
    @RequireAdmin
    @GetMapping("/list")
    public R<IPage<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(adminUserService.getUserList(page, size, keyword));
    }

    /**
     * 用户详情
     */
    @RequireAdmin
    @GetMapping("/detail/{id}")
    public R<UserVO> getUserDetail(@PathVariable Long id) {
        return R.ok(adminUserService.getUserDetail(id));
    }

    /**
     * 用户发布历史
     */
    @RequireAdmin
    @GetMapping("/posts/{id}")
    public R<IPage<PostVO>> getUserPosts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(adminUserService.getUserPosts(id, page, size));
    }

    /**
     * 用户报名历史
     */
    @RequireAdmin
    @GetMapping("/applies/{id}")
    public R<IPage<Map<String, Object>>> getUserApplies(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(adminUserService.getUserApplies(id, page, size));
    }

    /**
     * 用户评价记录
     */
    @RequireAdmin
    @GetMapping("/reviews/{id}")
    public R<IPage<Map<String, Object>>> getUserReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(adminUserService.getUserReviews(id, page, size));
    }

    /**
     * 封禁用户
     */
    @RequireAdmin
    @PostMapping("/ban/{id}")
    public R<Void> banUser(@PathVariable Long id) {
        adminUserService.banUser(id);
        return R.ok();
    }

    /**
     * 解封用户
     */
    @RequireAdmin
    @PostMapping("/unban/{id}")
    public R<Void> unbanUser(@PathVariable Long id) {
        adminUserService.unbanUser(id);
        return R.ok();
    }

    /**
     * 重置用户信誉分
     * body: {userId: 2, creditScore: 80}
     */
    @RequireAdmin
    @PutMapping("/credit")
    public R<Void> resetCredit(@RequestBody Map<String, Object> body) {
        Object userIdObj = body == null ? null : body.get("userId");
        Object creditObj = body == null ? null : body.get("creditScore");
        if (userIdObj == null || creditObj == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "参数不完整");
        }
        Long userId;
        Integer creditScore;
        try {
            userId = Long.valueOf(userIdObj.toString());
            creditScore = Integer.valueOf(creditObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "参数格式不正确");
        }
        // 范围校验：信誉分 0-100
        if (creditScore < 0 || creditScore > 100) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "信誉分必须在0-100之间");
        }
        adminUserService.resetCredit(userId, creditScore);
        return R.ok();
    }
}
