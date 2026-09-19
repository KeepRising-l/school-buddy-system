package com.buddy.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.annotation.RequireAdmin;
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
 * 管理端-举报处理控制器（V1.1）
 */
@RestController
@RequestMapping("/api/admin/report")
public class AdminReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 举报列表（分页，可按状态筛选）
     */
    @RequireAdmin
    @GetMapping("/list")
    public R<IPage<ReportVO>> getReportList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return R.ok(reportService.getReportList(page, size, status));
    }

    /**
     * 举报详情
     */
    @RequireAdmin
    @GetMapping("/detail/{id}")
    public R<ReportVO> getReportDetail(@PathVariable Long id) {
        return R.ok(reportService.getReportDetail(id));
    }

    /**
     * 处理举报
     * body: {valid: true, remark: "确认违规"}
     */
    @RequireAdmin
    @PostMapping("/handle/{id}")
    public R<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long adminId = UserContext.getUserId();
        Object validObj = body == null ? null : body.get("valid");
        if (validObj == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少处理结果参数");
        }
        // V1.2 严格解析：Boolean.parseBoolean("abc") 会静默返回 false，把举报误判为无效
        boolean valid;
        if (validObj instanceof Boolean) {
            valid = (Boolean) validObj;
        } else if ("true".equalsIgnoreCase(validObj.toString())) {
            valid = true;
        } else if ("false".equalsIgnoreCase(validObj.toString())) {
            valid = false;
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "处理结果参数不合法");
        }
        String remark = body == null ? null : (String) body.get("remark");
        reportService.handle(adminId, id, valid, remark);
        return R.ok();
    }
}
