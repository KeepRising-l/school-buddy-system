package com.buddy.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
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
        String targetType = (String) body.get("targetType");
        Long targetId = Long.valueOf(body.get("targetId").toString());
        String reasonType = (String) body.get("reasonType");
        String description = (String) body.get("description");
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
