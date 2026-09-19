package com.buddy.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import com.buddy.common.result.ResultCode;
import com.buddy.dto.response.PostVO;
import com.buddy.dto.response.UserVO;
import com.buddy.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端-用户管理控制器
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private AdminService adminService;

    /**
     * 用户列表
     */
    @RequireAdmin
    @GetMapping("/list")
    public R<IPage<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(adminService.getUserList(page, size, keyword));
    }

    /**
     * 用户详情
     */
    @RequireAdmin
    @GetMapping("/detail/{id}")
    public R<UserVO> getUserDetail(@PathVariable Long id) {
        return R.ok(adminService.getUserDetail(id));
    }

    /**
     * 用户发布历史
     */
    @RequireAdmin
    @GetMapping("/posts/{id}")
    public R<IPage<PostVO>> getUserPosts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return R.ok(adminService.getUserPosts(id, page, size));
    }

    /**
     * 用户报名历史
     */
    @RequireAdmin
    @GetMapping("/applies/{id}")
    public R<IPage<Map<String, Object>>> getUserApplies(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return R.ok(adminService.getUserApplies(id, page, size));
    }

    /**
     * 用户评价记录
     */
    @RequireAdmin
    @GetMapping("/reviews/{id}")
    public R<IPage<Map<String, Object>>> getUserReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return R.ok(adminService.getUserReviews(id, page, size));
    }

    /**
     * 封禁用户
     */
    @RequireAdmin
    @PostMapping("/ban/{id}")
    public R<Void> banUser(@PathVariable Long id) {
        adminService.banUser(id);
        return R.ok();
    }

    /**
     * 解封用户
     */
    @RequireAdmin
    @PostMapping("/unban/{id}")
    public R<Void> unbanUser(@PathVariable Long id) {
        adminService.unbanUser(id);
        return R.ok();
    }

    /**
     * 重置信誉分
     * body: {userId: 1, creditScore: 80}
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
        adminService.resetCredit(userId, creditScore);
        return R.ok();
    }
}
