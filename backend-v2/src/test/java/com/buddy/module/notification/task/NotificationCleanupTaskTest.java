package com.buddy.module.notification.task;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知清理任务测试（缺口#4）：按保留天数删除历史通知
 */
@ExtendWith(MockitoExtension.class)
class NotificationCleanupTaskTest {

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationCleanupTask task;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Notification.class);
    }

    @BeforeEach
    void setUp() {
        task = new NotificationCleanupTask();
        ReflectionTestUtils.setField(task, "notificationMapper", notificationMapper);
    }

    @Test
    void deletesNotificationsOlderThanKeepDays() {
        when(notificationMapper.delete(any())).thenReturn(3);

        task.cleanupExpired();

        verify(notificationMapper).delete(any());
    }
}
