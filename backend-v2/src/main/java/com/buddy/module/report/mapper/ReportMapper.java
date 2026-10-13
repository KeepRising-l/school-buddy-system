package com.buddy.module.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buddy.module.report.entity.Report;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 举报 Mapper
 */
public interface ReportMapper extends BaseMapper<Report> {

    /**
     * 平均处理时长（秒）：对"已处理"（handle_time 非空）的举报求 提交→处理 的时间差均值。
     * 无已处理数据时返回 null。
     */
    @Select("SELECT AVG(TIMESTAMPDIFF(SECOND, created_time, handle_time)) "
            + "FROM report WHERE handle_time IS NOT NULL")
    Double avgHandleSeconds();

    /**
     * 按天统计举报量（缺口#4 举报处理统计的趋势图）
     *
     * @param start 起始时间（含）
     * @return 每行 {d: 日期, c: 数量}
     */
    @Select("SELECT DATE(created_time) AS d, COUNT(*) AS c FROM report "
            + "WHERE created_time >= #{start} "
            + "GROUP BY DATE(created_time) ORDER BY d")
    List<Map<String, Object>> countDailySince(@Param("start") LocalDateTime start);
}
