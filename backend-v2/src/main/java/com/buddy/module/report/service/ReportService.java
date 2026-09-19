package com.buddy.module.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.module.report.dto.ReportVO;

/**
 * 举报服务接口
 */
public interface ReportService {

    void submit(Long reporterId, String targetType, Long targetId, String reasonType, String description);

    IPage<ReportVO> getMyReports(Long reporterId, int page, int size);

    IPage<ReportVO> getReportList(int page, int size, Integer status);

    ReportVO getReportDetail(Long reportId);

    void handle(Long adminId, Long reportId, boolean valid, String remark);
}
