package com.buddy.module.apply.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buddy.module.apply.entity.PostApply;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报名 Mapper（S3 随帖子模块先行迁移，接口层 S4 补齐）
 */
public interface PostApplyMapper extends BaseMapper<PostApply> {

    /**
     * 查询指定起始时间以来每天报名的用户（用于活跃用户趋势单次聚合去重）
     */
    @Select("SELECT DISTINCT DATE(apply_time) AS d, applicant_id AS uid FROM post_apply " +
            "WHERE deleted = 0 AND apply_time >= #{start}")
    List<Map<String, Object>> selectDailyApplicantsSince(@Param("start") LocalDateTime start);
}
