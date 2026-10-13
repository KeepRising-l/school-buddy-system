package com.buddy.module.post.task;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.buddy.module.apply.entity.PostApply;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.notification.entity.Notification;
import com.buddy.module.notification.mapper.NotificationMapper;
import com.buddy.module.notification.service.NotificationService;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 活动提醒任务测试（缺口#1）：发布者+已通过报名者收到提醒；已提醒过则跳过
 */
@ExtendWith(MockitoExtension.class)
class ActivityRemindTaskTest {

    @Mock
    private PostMapper postMapper;
    @Mock
    private PostApplyMapper postApplyMapper;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private NotificationService notificationService;

    private ActivityRemindTask task;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Post.class);
        TableInfoHelper.initTableInfo(assistant, PostApply.class);
        TableInfoHelper.initTableInfo(assistant, Notification.class);
    }

    @BeforeEach
    void setUp() {
        task = new ActivityRemindTask();
        ReflectionTestUtils.setField(task, "postMapper", postMapper);
        ReflectionTestUtils.setField(task, "postApplyMapper", postApplyMapper);
        ReflectionTestUtils.setField(task, "notificationMapper", notificationMapper);
        ReflectionTestUtils.setField(task, "notificationService", notificationService);
    }

    private static Post post() {
        Post post = new Post();
        post.setId(1L);
        post.setTitle("羽毛球双打");
        post.setLocation("体育馆");
        post.setPublisherId(5L);
        post.setStartTime(LocalDateTime.now().plusMinutes(20));
        return post;
    }

    @Test
    void sendsRemindToPublisherAndApprovedApplicants() {
        when(postMapper.selectList(any())).thenReturn(List.of(post()));
        PostApply apply = new PostApply();
        apply.setId(11L);
        apply.setPostId(1L);
        apply.setApplicantId(7L);
        apply.setStatus("APPROVED");
        when(postApplyMapper.selectList(any())).thenReturn(List.of(apply));
        when(notificationMapper.selectCount(any())).thenReturn(0L);

        task.checkActivityRemind();

        verify(notificationService).send(eq(5L), eq(Notification.TYPE_ACTIVITY_REMIND), any(), any(), any(), eq(1L));
        verify(notificationService).send(eq(7L), eq(Notification.TYPE_ACTIVITY_REMIND), any(), any(), any(), eq(1L));
    }

    @Test
    void skipsWhenAlreadyReminded() {
        when(postMapper.selectList(any())).thenReturn(List.of(post()));
        when(postApplyMapper.selectList(any())).thenReturn(List.of());
        when(notificationMapper.selectCount(any())).thenReturn(1L); // 已有提醒记录

        task.checkActivityRemind();

        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    void doesNothingWhenNoUpcomingPosts() {
        when(postMapper.selectList(any())).thenReturn(List.of());

        task.checkActivityRemind();

        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }
}
