package com.buddy.module.report.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.store.AuxStore;
import com.buddy.module.notification.entity.Notification;
import com.buddy.module.notification.service.NotificationService;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.post.service.AdminPostService;
import com.buddy.module.report.entity.Report;
import com.buddy.module.report.mapper.ReportMapper;
import com.buddy.module.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 举报服务测试：24小时查重（DB + Redis 门闩）、处置并发防重、副作用后置
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final Long REPORTER_ID = 1L;
    private static final Long PUBLISHER_ID = 9L;
    private static final Long ADMIN_ID = 99L;
    private static final Long TARGET_ID = 5L;
    private static final Long REPORT_ID = 66L;

    @Mock
    private ReportMapper reportMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private AdminPostService adminPostService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuxStore auxStore;

    private ReportServiceImpl reportService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Report.class);
    }

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl();
        ReflectionTestUtils.setField(reportService, "reportMapper", reportMapper);
        ReflectionTestUtils.setField(reportService, "userMapper", userMapper);
        ReflectionTestUtils.setField(reportService, "postMapper", postMapper);
        ReflectionTestUtils.setField(reportService, "adminPostService", adminPostService);
        ReflectionTestUtils.setField(reportService, "notificationService", notificationService);
        ReflectionTestUtils.setField(reportService, "auxStore", auxStore);
    }

    private static Report report(Integer status) {
        Report report = new Report();
        report.setId(REPORT_ID);
        report.setReporterId(REPORTER_ID);
        report.setTargetType(Report.TARGET_POST);
        report.setTargetId(TARGET_ID);
        report.setStatus(status);
        return report;
    }

    // ===== 提交举报 =====

    @Test
    void submitRejectsDuplicateByDbCheck() {
        when(postMapper.selectById(TARGET_ID)).thenReturn(new Post());
        when(reportMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> reportService.submit(REPORTER_ID, Report.TARGET_POST, TARGET_ID, "违规内容", "描述"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已举报过该对象");
        verify(reportMapper, never()).insert(any(Report.class));
    }

    @Test
    void submitRejectsByDedupGate() {
        when(postMapper.selectById(TARGET_ID)).thenReturn(new Post());
        when(reportMapper.selectCount(any())).thenReturn(0L);
        when(auxStore.setIfAbsent(anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> reportService.submit(REPORTER_ID, Report.TARGET_POST, TARGET_ID, "违规内容", "描述"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已举报过该对象");
        verify(reportMapper, never()).insert(any(Report.class));
    }

    @Test
    void submitSuccessInsertsReport() {
        when(postMapper.selectById(TARGET_ID)).thenReturn(new Post());
        when(reportMapper.selectCount(any())).thenReturn(0L);
        when(auxStore.setIfAbsent(anyString(), any())).thenReturn(true);

        reportService.submit(REPORTER_ID, Report.TARGET_POST, TARGET_ID, "违规内容", "描述");

        verify(reportMapper).insert(any(Report.class));
    }

    @Test
    void submitProceedsWhenDedupStoreUnavailable() {
        when(postMapper.selectById(TARGET_ID)).thenReturn(new Post());
        when(reportMapper.selectCount(any())).thenReturn(0L);
        when(auxStore.setIfAbsent(anyString(), any())).thenThrow(new RuntimeException("store down"));

        reportService.submit(REPORTER_ID, Report.TARGET_POST, TARGET_ID, "违规内容", "描述");

        verify(reportMapper).insert(any(Report.class));
    }

    // ===== 处置举报 =====

    @Test
    void handleRejectsAlreadyHandledReport() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(report(Report.STATUS_VALID));

        assertThatThrownBy(() -> reportService.handle(ADMIN_ID, REPORT_ID, true, "ok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已处理");
    }

    @Test
    void handleFailsWhenLostConcurrentRaceWithoutSideEffects() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(report(Report.STATUS_PENDING));
        when(reportMapper.update(isNull(), any())).thenReturn(0); // 并发下已被其他管理员处理

        assertThatThrownBy(() -> reportService.handle(ADMIN_ID, REPORT_ID, true, "ok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被处理");
        // 状态转换失败时不得下架/封禁/通知
        verify(adminPostService, never()).takedownPost(any(), any());
        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    void handleInvalidOnlyNotifiesReporter() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(report(Report.STATUS_PENDING));
        when(reportMapper.update(isNull(), any())).thenReturn(1);

        reportService.handle(ADMIN_ID, REPORT_ID, false, "证据不足");

        verify(adminPostService, never()).takedownPost(any(), any());
        verify(notificationService).send(eq(REPORTER_ID), eq(Notification.TYPE_SYSTEM),
                eq("举报处理结果"), any(), isNull(), isNull());
    }

    @Test
    void handleValidTakesDownPostAndNotifiesReporter() {
        when(reportMapper.selectById(REPORT_ID)).thenReturn(report(Report.STATUS_PENDING));
        when(reportMapper.update(isNull(), any())).thenReturn(1);
        Post post = new Post();
        post.setId(TARGET_ID);
        post.setPublisherId(PUBLISHER_ID);
        post.setTitle("被举报帖子");
        post.setStatus("RECRUITING");
        when(postMapper.selectById(TARGET_ID)).thenReturn(post);
        when(reportMapper.selectCount(any())).thenReturn(1L); // 未达自动风控阈值

        reportService.handle(ADMIN_ID, REPORT_ID, true, "违规属实");

        // 下架走统一入口并附带原因；发布者通知由 takedownPost 内部负责（断言见 AdminPostServiceImplTest）
        verify(adminPostService).takedownPost(eq(TARGET_ID), contains("举报"));
        verify(notificationService).send(eq(REPORTER_ID), eq(Notification.TYPE_SYSTEM),
                eq("举报处理结果"), any(), isNull(), isNull());
    }
}
