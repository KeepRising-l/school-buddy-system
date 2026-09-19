package com.buddy.module.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.module.report.dto.ReportVO;

/**
 * 举报服务接口
 * S7 迁移用户端（提交/我的举报）；管理端处理（handle/列表/详情，联动下架与自动风控）在 S8 管理端切片补齐
 */
public interface ReportService {

    void submit(Long reporterId, String targetType, Long targetId, String reasonType, String description);

    IPage<ReportVO> getMyReports(Long reporterId, int page, int size);
}
