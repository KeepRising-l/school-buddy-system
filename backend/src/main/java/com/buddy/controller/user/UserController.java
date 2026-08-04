package com.buddy.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.dto.request.ProfileUpdateRequest;
import com.buddy.dto.response.ApplyVO;
import com.buddy.dto.response.PostVO;
import com.buddy.dto.response.UserTagVO;
import com.buddy.dto.response.UserVO;
import com.buddy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取个人资料
     */
    @GetMapping("/profile")
    public R<UserVO> getProfile() {
        Long userId = UserContext.getUserId();
        return R.ok(userService.getProfile(userId));
    }

    /**
     * 编辑个人资料
     */
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody @Valid ProfileUpdateRequest request) {
        Long userId = UserContext.getUserId();
        userService.updateProfile(userId, request);
        return R.ok();
    }

    /**
     * 获取我的标签
     */
    @GetMapping("/tags")
    public R<List<UserTagVO>> getMyTags() {
        Long userId = UserContext.getUserId();
        return R.ok(userService.getUserTags(userId));
    }

    /**
     * 设置兴趣标签
     * body: {tagIds: [1,2,3], levels: [3,2,5]}
     */
    @PutMapping("/tags")
    public R<Void> setUserTags(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        @SuppressWarnings("unchecked")
        List<Long> tagIds = (List<Long>) body.get("tagIds");
        @SuppressWarnings("unchecked")
        List<Integer> levels = (List<Integer>) body.get("levels");
        userService.setUserTags(userId, tagIds, levels);
        return R.ok();
    }

    /**
     * 我的发布
     */
    @GetMapping("/posts")
    public R<IPage<PostVO>> getMyPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Long userId = UserContext.getUserId();
        return R.ok(userService.getMyPosts(userId, page, size, status));
    }

    /**
     * 我的预约
     */
    @GetMapping("/applies")
    public R<IPage<ApplyVO>> getMyApplies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Long userId = UserContext.getUserId();
        return R.ok(userService.getMyApplies(userId, page, size, status));
    }
}
