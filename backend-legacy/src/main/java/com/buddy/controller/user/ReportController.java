package com.buddy.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import com.buddy.common.result.ResultCode;
import com.buddy.dto.response.ReportVO;
import com.buddy.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 举报控制器（V1.1 用户端）
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 提交举报
     * body: {targetType: "POST"/"USER", targetId: 1, reasonType: "违规内容", description: "..."}
     */
    @PostMapping("/submit")
    public R<Void> submit(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        // V1.2 防御解析：缺失/畸形参数直接 400，避免 NPE/NFE 落 500
        Object targetTypeObj = body == null ? null : body.get("targetType");
        Object targetIdObj = body == null ? null : body.get("targetId");
        if (targetTypeObj == null || targetIdObj == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "举报参数不完整");
        }
        String targetType = targetTypeObj.toString();
        Long targetId;
        try {
            targetId = Long.valueOf(targetIdObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "举报对象参数不合法");
        }
        String reasonType = body.get("reasonType") == null ? null : body.get("reasonType").toString();
        String description = body.get("description") == null ? null : body.get("description").toString();
        reportService.submit(userId, targetType, targetId, reasonType, description);
        return R.ok();
    }

    /**
     * 我的举报记录（分页）
     */
    @GetMapping("/my")
    public R<IPage<ReportVO>> getMyReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        return R.ok(reportService.getMyReports(userId, page, size));
    }
}
