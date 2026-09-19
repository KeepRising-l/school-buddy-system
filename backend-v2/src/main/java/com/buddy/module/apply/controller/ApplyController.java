package com.buddy.module.apply.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import com.buddy.common.result.ResultCode;
import com.buddy.module.apply.dto.ApplyVO;
import com.buddy.module.apply.service.ApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 报名控制器（等价迁移自旧版）
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
        Long postId = parsePostId(body);
        applyService.apply(userId, postId);
        return R.ok();
    }

    /**
     * 候补报名：帖子满员时进入候补队列
     * body: {postId: 1}
     */
    @PostMapping("/waitlist")
    public R<Void> waitlist(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        Long postId = parsePostId(body);
        applyService.waitlist(userId, postId);
        return R.ok();
    }

    /**
     * 解析请求体中的 postId，参数缺失/不合法时给出明确提示（防 NPE 500）
     */
    private Long parsePostId(Map<String, Object> body) {
        Object postIdObj = body == null ? null : body.get("postId");
        if (postIdObj == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少帖子ID");
        }
        try {
            return Long.valueOf(postIdObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "帖子ID不合法");
        }
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
        return R.ok();
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
        Object attendedObj = body == null ? null : body.get("attended");
        if (attendedObj == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少出勤标记参数");
        }
        // 严格校验布尔值：parseBoolean("abc") 会静默返回 false（语义反转）
        boolean attended;
        if (attendedObj instanceof Boolean) {
            attended = (Boolean) attendedObj;
        } else if ("true".equalsIgnoreCase(attendedObj.toString())) {
            attended = true;
        } else if ("false".equalsIgnoreCase(attendedObj.toString())) {
            attended = false;
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "出勤标记参数不合法");
        }
        applyService.markAttendance(userId, id, attended);
        return R.ok();
    }
}
