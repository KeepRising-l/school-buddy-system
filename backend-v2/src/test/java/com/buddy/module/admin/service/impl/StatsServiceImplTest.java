package com.buddy.module.admin.service.impl;

import com.buddy.module.admin.dto.ReportStatsVO;
import com.buddy.module.admin.dto.StatsVO;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.report.mapper.ReportMapper;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 统计服务测试：活跃用户并集去重、趋势天数上限、CSV 公式注入防护
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private PostApplyMapper postApplyMapper;
    @Mock
    private ReportMapper reportMapper;

    private StatsServiceImpl statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsServiceImpl();
        ReflectionTestUtils.setField(statsService, "userMapper", userMapper);
        ReflectionTestUtils.setField(statsService, "postMapper", postMapper);
        ReflectionTestUtils.setField(statsService, "postApplyMapper", postApplyMapper);
        ReflectionTestUtils.setField(statsService, "reportMapper", reportMapper);
    }

    @Test
    void overviewActiveUsersIsUnionOfPostersAndApplicants() {
        lenient().when(postMapper.selectCount(any())).thenReturn(0L);
        lenient().when(userMapper.selectCount(any())).thenReturn(0L);
        lenient().when(postMapper.selectMaps(any())).thenReturn(List.of());
        when(postMapper.selectObjs(any())).thenReturn(List.of((Object) 2L, (Object) 3L));
        when(postApplyMapper.selectObjs(any())).thenReturn(List.of((Object) 3L, (Object) 4L));

        StatsVO vo = statsService.getOverview();

        // 2、3、4 并集去重（用户 3 同时发帖和报名只计一次）
        assertThat(vo.getActiveUsers()).isEqualTo(3L);
    }

    @Test
    void trendDaysIsClampedToUpperBound() {
        lenient().when(postMapper.selectCount(any())).thenReturn(0L);

        List<Map<String, Object>> trend = statsService.getPostTrend(10000);

        assertThat(trend).hasSize(90);
    }

    @Test
    void trendDaysIsClampedToLowerBound() {
        lenient().when(postMapper.selectCount(any())).thenReturn(0L);

        List<Map<String, Object>> trend = statsService.getPostTrend(0);

        assertThat(trend).hasSize(1);
    }

    @Test
    void csvEscapesFormulaInjection() throws Exception {
        User u = new User();
        u.setId(1L);
        u.setStudentNo("=cmd()");
        u.setNickname("+1+1");
        u.setPhone("-2-2");
        u.setCollege("@SUM(1)");
        u.setCreditScore(100);
        u.setStatus(1);
        when(userMapper.selectList(any())).thenReturn(List.of(u));

        StringWriter writer = new StringWriter();
        statsService.exportUsers(writer);
        String csv = writer.toString();

        assertThat(csv).contains("'=cmd()");
        assertThat(csv).contains("'+1+1");
        assertThat(csv).contains("'-2-2");
        assertThat(csv).contains("'@SUM(1)");
        assertThat(csv).contains("正常");
    }

    // ===== 举报处理统计（缺口#4）=====

    @Test
    void reportStatsComputesRateAverageAndTrend() {
        // 按调用顺序：总数、已处理、有效、待处理
        when(reportMapper.selectCount(any())).thenReturn(10L, 6L, 4L, 4L);
        when(reportMapper.avgHandleSeconds()).thenReturn(7200.0);
        String today = LocalDate.now().toString();
        Map<String, Object> row = new HashMap<>();
        row.put("d", today);
        row.put("c", 3L);
        when(reportMapper.countDailySince(any())).thenReturn(List.of(row));

        ReportStatsVO vo = statsService.getReportStats(7);

        assertThat(vo.getTotalReports()).isEqualTo(10L);
        assertThat(vo.getHandledReports()).isEqualTo(6L);
        assertThat(vo.getValidReports()).isEqualTo(4L);
        assertThat(vo.getPendingReports()).isEqualTo(4L);
        assertThat(vo.getHandledRate()).isEqualTo(60.0);
        assertThat(vo.getAvgHandleHours()).isEqualTo(2.0);
        // 近 7 天趋势补齐为 7 条，今天 3 条、其余 0
        assertThat(vo.getTrend()).hasSize(7);
        assertThat(vo.getTrend().get(6)).containsEntry("date", today).containsEntry("count", 3L);
        assertThat(vo.getTrend().get(0)).containsEntry("count", 0L);
    }

    @Test
    void reportStatsHandlesNoData() {
        when(reportMapper.selectCount(any())).thenReturn(0L, 0L, 0L, 0L);
        when(reportMapper.avgHandleSeconds()).thenReturn(null);
        when(reportMapper.countDailySince(any())).thenReturn(List.of());

        ReportStatsVO vo = statsService.getReportStats(1);

        // 无数据时不应除零，平均时长返回 null（前端显示"—"）
        assertThat(vo.getHandledRate()).isEqualTo(0.0);
        assertThat(vo.getAvgHandleHours()).isNull();
        assertThat(vo.getTrend()).hasSize(1);
    }

    @Test
    void reportStatsClampsTrendDays() {
        when(reportMapper.selectCount(any())).thenReturn(0L, 0L, 0L, 0L);
        when(reportMapper.avgHandleSeconds()).thenReturn(null);
        when(reportMapper.countDailySince(any())).thenReturn(List.of());

        assertThat(statsService.getReportStats(10000).getTrend()).hasSize(90);
    }
}
