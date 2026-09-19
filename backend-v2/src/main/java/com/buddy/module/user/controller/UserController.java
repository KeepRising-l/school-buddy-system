package com.buddy.module.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import com.buddy.common.result.ResultCode;
import com.buddy.module.apply.dto.ApplyVO;
import com.buddy.module.post.dto.PostVO;
import com.buddy.module.user.dto.request.ProfileUpdateRequest;
import com.buddy.module.user.dto.UserProfileVO;
import com.buddy.module.user.dto.UserTagVO;
import com.buddy.module.user.dto.UserVO;
import com.buddy.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器（用户端，等价迁移自旧版）
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
        // JSON 数组数字被反序列化为 Integer，统一按 Number 解析避免 ClassCastException
        Object tagIdsObj = body == null ? null : body.get("tagIds");
        Object levelsObj = body == null ? null : body.get("levels");
        if (!(tagIdsObj instanceof List) || !(levelsObj instanceof List)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标签参数格式不正确");
        }
        List<?> tagIdsRaw = (List<?>) tagIdsObj;
        List<?> levelsRaw = (List<?>) levelsObj;
        if (tagIdsRaw.isEmpty() || tagIdsRaw.size() != levelsRaw.size()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标签与段位数量不一致");
        }
        List<Long> tagIds = new ArrayList<>();
        List<Integer> levels = new ArrayList<>();
        for (int i = 0; i < tagIdsRaw.size(); i++) {
            Object tid = tagIdsRaw.get(i);
            Object lv = levelsRaw.get(i);
            if (!(tid instanceof Number) || !(lv instanceof Number)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "标签参数不合法");
            }
            tagIds.add(((Number) tid).longValue());
            int level = ((Number) lv).intValue();
            // 允许 0：无段位体系的项目（晨跑/剧本杀等）段位为 0
            if (level < 0 || level > 5) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "段位必须在0-5之间");
            }
            levels.add(level);
        }
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

    /**
     * 个人主页：查看任意用户主页聚合信息（脱敏）
     */
    @GetMapping("/profile/{userId}")
    public R<UserProfileVO> getUserProfile(@PathVariable Long userId) {
        return R.ok(userService.getUserProfile(userId));
    }

    /**
     * 设置主页隐私
     * body: {privacy: 0/1}
     */
    @PutMapping("/privacy")
    public R<Void> setProfilePrivacy(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        // 防御解析：缺失/非数字参数直接 400，避免 NPE 500
        Object privacyObj = body == null ? null : body.get("privacy");
        if (privacyObj == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少隐私设置参数");
        }
        Integer privacy;
        try {
            privacy = Integer.valueOf(privacyObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "隐私设置参数不合法");
        }
        userService.setProfilePrivacy(userId, privacy);
        return R.ok();
    }
}
