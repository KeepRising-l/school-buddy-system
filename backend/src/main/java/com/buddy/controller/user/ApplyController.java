package com.buddy.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.dto.response.ApplyVO;
import com.buddy.service.ApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 报名控制器
 */
@RestController
@RequestMapping("/api/apply")
public class ApplyController {

    @Autowired
    private ApplyService applyService;

    /**
     * 报名
     * body: {postId: 1}
     */
    @PostMapping("/join")
    public R<Void> apply(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        Long postId = Long.valueOf(body.get("postId").toString());
        applyService.apply(userId, postId);
        return R.ok();
    }

    /**
     * 取消报名
     */
    @PostMapping("/cancel/{id}")
    public R<Void> cancelApply(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        applyService.cancelApply(userId, id);
        return R.ok();
    }

    /**
     * 报名列表（发布者查看）
     */
    @GetMapping("/list/{postId}")
    public R<List<ApplyVO>> getApplyList(@PathVariable Long postId) {
        Long userId = UserContext.getUserId();
        return R.ok(applyService.getApplyList(userId, postId));
    }

    /**
     * 同意报名
     */
    @PostMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        applyService.approve(userId, id);
        return R.ok();
    }

    /**
     * 拒绝报名
     */
    @PostMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        applyService.reject(userId, id);
        return com.buddy.common.result.R.ok();
    }

    /**
     * 收到的报名申请
     */
    @GetMapping("/received")
    public R<IPage<ApplyVO>> getReceivedApplies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        return R.ok(applyService.getReceivedApplies(userId, page, size));
    }

    /**
     * 标记出勤状态
     * body: {attended: true/false}
     */
    @PostMapping("/attendance/{id}")
    public R<Void> markAttendance(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        boolean attended = Boolean.parseBoolean(body.get("attended").toString());
        applyService.markAttendance(userId, id, attended);
        return R.ok();
    }
}
