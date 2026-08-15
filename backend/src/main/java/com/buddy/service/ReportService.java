package com.buddy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.dto.response.ReportVO;

/**
 * 举报服务接口（V1.1）
 */
public interface ReportService {

    /**
     * 提交举报
     *
     * @param reporterId  举报人
     * @param targetType  对象类型：POST/USER
     * @param targetId    对象ID
     * @param reasonType  举报类型
     * @param description 说明
     */
    void submit(Long reporterId, String targetType, Long targetId, String reasonType, String description);

    /**
     * 我的举报记录（分页）
     */
    IPage<ReportVO> getMyReports(Long reporterId, int page, int size);

    /**
     * 管理端：举报列表（分页，可按状态筛选）
     */
    IPage<ReportVO> getReportList(int page, int size, Integer status);

    /**
     * 管理端：举报详情
     */
    ReportVO getReportDetail(Long reportId);

    /**
     * 管理端：处理举报
     *
     * @param adminId  处理人
     * @param reportId 举报ID
     * @param valid    是否有效
     * @param remark   处理备注
     */
    void handle(Long adminId, Long reportId, boolean valid, String remark);
}
