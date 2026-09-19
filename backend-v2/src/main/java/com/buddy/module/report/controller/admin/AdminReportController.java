package com.buddy.module.report.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.context.UserContext;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import com.buddy.common.result.ResultCode;
import com.buddy.module.report.dto.ReportVO;
import com.buddy.module.report.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端-举报中心控制器（等价迁移自旧版，D-009 归属 report 模块）
 */
@RestController
@RequestMapping("/api/admin/report")
public class AdminReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 举报列表（支持状态筛选）
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
     * body: {valid: true/false, remark: "处理备注"}
     */
    @RequireAdmin
    @PostMapping("/handle/{id}")
    public R<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long adminId = UserContext.getUserId();
        Object validObj = body == null ? null : body.get("valid");
        if (validObj == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少处理结果参数");
        }
        // 严格解析：Boolean.parseBoolean("abc") 会静默返回 false，把举报误判为无效
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
