package com.buddy.module.post.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.buddy.common.cache.CacheEpoch;
import com.buddy.common.exception.BusinessException;
import com.buddy.module.apply.entity.PostApply;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.favorite.mapper.FavoriteMapper;
import com.buddy.module.notification.entity.Notification;
import com.buddy.module.notification.service.NotificationService;
import com.buddy.module.post.converter.PostConverter;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostCommentMapper;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.review.mapper.ReviewMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端下架测试：幂等下架（重复/并发处置只生效一次，副作用只执行一次）
 */
@ExtendWith(MockitoExtension.class)
class AdminPostServiceImplTest {

    private static final Long POST_ID = 1L;
    private static final Long APPLICANT_ID = 8L;

    @Mock
    private PostMapper postMapper;
    @Mock
    private PostApplyMapper postApplyMapper;
    @Mock
    private PostCommentMapper postCommentMapper;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private PostConverter postConverter;
    @Mock
    private NotificationService notificationService;

    private AdminPostServiceImpl adminPostService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Post.class);
        TableInfoHelper.initTableInfo(assistant, PostApply.class);
    }

    @BeforeEach
    void setUp() {
        adminPostService = new AdminPostServiceImpl();
        ReflectionTestUtils.setField(adminPostService, "postMapper", postMapper);
        ReflectionTestUtils.setField(adminPostService, "postApplyMapper", postApplyMapper);
        ReflectionTestUtils.setField(adminPostService, "postCommentMapper", postCommentMapper);
        ReflectionTestUtils.setField(adminPostService, "reviewMapper", reviewMapper);
        ReflectionTestUtils.setField(adminPostService, "favoriteMapper", favoriteMapper);
        ReflectionTestUtils.setField(adminPostService, "postConverter", postConverter);
        ReflectionTestUtils.setField(adminPostService, "notificationService", notificationService);
        ReflectionTestUtils.setField(adminPostService, "cacheEpoch", new CacheEpoch());
    }

    private static Post post(String status) {
        Post post = new Post();
        post.setId(POST_ID);
        post.setTitle("被举报的组局");
        post.setStatus(status);
        return post;
    }

    @Test
    void takedownReturnsEarlyWhenAlreadyCancelled() {
        when(postMapper.selectById(POST_ID)).thenReturn(post("CANCELLED"));
        when(postMapper.update(isNull(), any())).thenReturn(0); // 条件更新未命中：已被并发下架

        adminPostService.takedownPost(POST_ID);

        verify(postApplyMapper, never()).selectList(any());
        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    void takedownRejectsMissingPost() {
        when(postMapper.selectById(POST_ID)).thenReturn(null);

        assertThatThrownBy(() -> adminPostService.takedownPost(POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帖子不存在");
    }

    @Test
    void takedownCancelsAppliesAndNotifiesOnce() {
        when(postMapper.selectById(POST_ID)).thenReturn(post("RECRUITING"));
        when(postMapper.update(isNull(), any())).thenReturn(1);
        PostApply apply = new PostApply();
        apply.setId(100L);
        apply.setPostId(POST_ID);
        apply.setApplicantId(APPLICANT_ID);
        apply.setStatus("APPROVED");
        when(postApplyMapper.selectList(any())).thenReturn(List.of(apply));
        when(postApplyMapper.update(isNull(), any())).thenReturn(1);

        adminPostService.takedownPost(POST_ID);

        verify(postMapper).decrementCurrentPeople(POST_ID);
        verify(notificationService).send(eq(APPLICANT_ID), eq(Notification.TYPE_POST_TAKEDOWN),
                any(), any(), any(), eq(POST_ID));
    }

    @Test
    void takedownSkipsApplyHandledConcurrently() {
        when(postMapper.selectById(POST_ID)).thenReturn(post("RECRUITING"));
        when(postMapper.update(isNull(), any())).thenReturn(1);
        PostApply apply = new PostApply();
        apply.setId(100L);
        apply.setPostId(POST_ID);
        apply.setApplicantId(APPLICANT_ID);
        apply.setStatus("APPROVED");
        when(postApplyMapper.selectList(any())).thenReturn(List.of(apply));
        when(postApplyMapper.update(isNull(), any())).thenReturn(0);

        adminPostService.takedownPost(POST_ID);

        verify(postMapper, never()).decrementCurrentPeople(POST_ID);
        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }
}
