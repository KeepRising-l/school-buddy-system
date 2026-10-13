package com.buddy.module.notification.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.module.notification.entity.Notification;
import com.buddy.module.notification.mapper.NotificationMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 通知服务测试（缺口#4）：按类型筛选
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Notification.class);
    }

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl();
        ReflectionTestUtils.setField(service, "notificationMapper", notificationMapper);
    }

    @Test
    void getNotificationsAppliesTypeFilter() {
        when(notificationMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Wrapper<Notification> wrapper = invocation.getArgument(1);
            assertThat(wrapper.getTargetSql()).contains("type");
            return new Page<Notification>(1, 20, 0);
        });

        service.getNotifications(1L, 1, 20, "SYSTEM");
    }

    @Test
    void getNotificationsWithoutTypeReturnsAll() {
        when(notificationMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Wrapper<Notification> wrapper = invocation.getArgument(1);
            // 不传类型时不应拼接 type 条件（保留原有"全部通知"行为）
            assertThat(wrapper.getTargetSql()).doesNotContain("type");
            return new Page<Notification>(1, 20, 0);
        });

        service.getNotifications(1L, 1, 20, null);
    }

    @Test
    void getNotificationsSupportsCommaSeparatedTypes() {
        when(notificationMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<Notification> wrapper = invocation.getArgument(1);
            // MyBatis-Plus 的占位符参数是懒加载的：先求值一次才会写入 paramNameValuePairs
            wrapper.getSqlSegment();
            // user_id + 2 个类型参数
            assertThat(wrapper.getParamNameValuePairs().values())
                    .containsExactlyInAnyOrder(1L, "APPLY_NEW", "APPLY_RESULT");
            assertThat(wrapper.getTargetSql()).contains("type").contains("IN");
            return new Page<Notification>(1, 20, 0);
        });

        service.getNotifications(1L, 1, 20, "APPLY_NEW, APPLY_RESULT");
    }

    @Test
    void getNotificationsIgnoresBlankTypeList() {
        when(notificationMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<Notification> wrapper = invocation.getArgument(1);
            // 只有逗号/空格等无意义输入时退化为不筛选，而不是拼出 type IN ()
            assertThat(wrapper.getTargetSql()).doesNotContain("type");
            return new Page<Notification>(1, 20, 0);
        });

        service.getNotifications(1L, 1, 20, " , ");
    }
}
