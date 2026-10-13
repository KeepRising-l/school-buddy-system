package com.buddy.module.post.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.store.AuxStore;
import com.buddy.module.apply.entity.PostApply;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.credit.service.CreditService;
import com.buddy.module.notification.entity.Notification;
import com.buddy.module.notification.service.NotificationService;
import com.buddy.module.post.dto.PostDetailVO;
import com.buddy.module.post.dto.request.PostCreateRequest;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.post.service.SensitiveWordService;
import com.buddy.module.tag.mapper.InterestTagMapper;
import com.buddy.module.tag.mapper.UserTagMapper;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 帖子服务测试：发帖时长边界、活跃帖上限（用户行锁）、取消帖子条件更新与副作用
 */
@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    private static final Long USER_ID = 2L;
    private static final Long POST_ID = 10L;

    @Mock
    private PostMapper postMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PostApplyMapper postApplyMapper;
    @Mock
    private UserTagMapper userTagMapper;
    @Mock
    private InterestTagMapper interestTagMapper;
    @Mock
    private CreditService creditService;
    @Mock
    private AuxStore auxStore;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SensitiveWordService sensitiveWordService;

    private PostServiceImpl postService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Post.class);
        TableInfoHelper.initTableInfo(assistant, PostApply.class);
    }

    @BeforeEach
    void setUp() {
        postService = new PostServiceImpl();
        ReflectionTestUtils.setField(postService, "postMapper", postMapper);
        ReflectionTestUtils.setField(postService, "userMapper", userMapper);
        ReflectionTestUtils.setField(postService, "postApplyMapper", postApplyMapper);
        ReflectionTestUtils.setField(postService, "userTagMapper", userTagMapper);
        ReflectionTestUtils.setField(postService, "interestTagMapper", interestTagMapper);
        ReflectionTestUtils.setField(postService, "creditService", creditService);
        ReflectionTestUtils.setField(postService, "auxStore", auxStore);
        ReflectionTestUtils.setField(postService, "notificationService", notificationService);
        ReflectionTestUtils.setField(postService, "sensitiveWordService", sensitiveWordService);
    }

    // ===== 发帖 =====

    private PostCreateRequest request(LocalDateTime start, LocalDateTime end) {
        PostCreateRequest req = new PostCreateRequest();
        req.setTitle("测试组局活动");
        req.setCategory("球类");
        req.setLocation("东区篮球场");
        req.setMaxPeople(4);
        req.setStartTime(start);
        req.setEndTime(end);
        return req;
    }

    private void stubCreateCommon(int activeCount) {
        User user = new User();
        user.setId(USER_ID);
        user.setCreditScore(100);
        lenient().when(userMapper.selectById(USER_ID)).thenReturn(user);
        lenient().when(creditService.isLowCredit(any())).thenReturn(false);
        lenient().when(sensitiveWordService.check(any(), any(), any())).thenReturn(List.of());
        lenient().when(postMapper.selectCount(any())).thenReturn((long) activeCount);
    }

    @Test
    void createRejectsOver24Hours() {
        stubCreateCommon(0);
        LocalDateTime start = LocalDateTime.now().plusHours(1);

        assertThatThrownBy(() -> postService.createPost(USER_ID,
                request(start, start.plusHours(24).plusMinutes(1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("24小时");
        verify(postMapper, never()).insert(any(Post.class));
    }

    @Test
    void createAllowsExactly24Hours() {
        stubCreateCommon(0);
        LocalDateTime start = LocalDateTime.now().plusHours(1);

        postService.createPost(USER_ID, request(start, start.plusHours(24)));

        verify(postMapper).insert(any(Post.class));
    }

    @Test
    void createRejectsWhenActiveLimitReached() {
        stubCreateCommon(5);
        LocalDateTime start = LocalDateTime.now().plusHours(1);

        assertThatThrownBy(() -> postService.createPost(USER_ID, request(start, start.plusHours(1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过5个");
        verify(userMapper).lockById(USER_ID);
        verify(postMapper, never()).insert(any(Post.class));
    }

    // ===== 取消帖子 =====

    private static Post post(String status) {
        Post post = new Post();
        post.setId(POST_ID);
        post.setPublisherId(USER_ID);
        post.setTitle("测试组局");
        post.setStatus(status);
        return post;
    }

    @Test
    void cancelRejectsNonOwner() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));

        assertThatThrownBy(() -> postService.cancelPost(99L, POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能取消自己发布的帖子");
    }

    @Test
    void cancelFailsWhenStatusChangedConcurrently() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        when(postMapper.update(isNull(), any())).thenReturn(0); // 并发下状态已被他人改变

        assertThatThrownBy(() -> postService.cancelPost(USER_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态已变化");
        // 状态转换失败时不得执行任何副作用
        verify(postApplyMapper, never()).selectList(any());
        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelSuccessCancelsAppliesAndNotifies() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        when(postMapper.update(isNull(), any())).thenReturn(1);
        PostApply apply = new PostApply();
        apply.setId(100L);
        apply.setPostId(POST_ID);
        apply.setApplicantId(8L);
        apply.setStatus("APPROVED");
        when(postApplyMapper.selectList(any())).thenReturn(List.of(apply));
        when(postApplyMapper.update(isNull(), any())).thenReturn(1);

        postService.cancelPost(USER_ID, POST_ID);

        verify(postMapper).decrementCurrentPeople(POST_ID);
        verify(notificationService).send(8L, Notification.TYPE_POST_CANCEL, "组局已取消",
                "你参加的组局「测试组局」已被取消", "/post/" + POST_ID, POST_ID);
    }

    @Test
    void cancelSkipsApplyHandledConcurrently() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        when(postMapper.update(isNull(), any())).thenReturn(1);
        PostApply apply = new PostApply();
        apply.setId(100L);
        apply.setPostId(POST_ID);
        apply.setApplicantId(8L);
        apply.setStatus("APPROVED");
        when(postApplyMapper.selectList(any())).thenReturn(List.of(apply));
        when(postApplyMapper.update(isNull(), any())).thenReturn(0); // 该报名已被并发处理

        postService.cancelPost(USER_ID, POST_ID);

        verify(postMapper, never()).decrementCurrentPeople(POST_ID);
        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }

    // ===== 联系方式可见性（缺口#5）=====

    /** 详情页公共桩：1 个已通过报名者、发布者与参与者都填了手机号 */
    private void stubDetailCommon() {
        Post p = post(PostStatus.RECRUITING);
        p.setIsAuditPass(1);
        when(postMapper.selectById(POST_ID)).thenReturn(p);

        User publisher = new User();
        publisher.setId(USER_ID);
        publisher.setNickname("发布者");
        publisher.setPhone("13800000001");
        when(userMapper.selectById(USER_ID)).thenReturn(publisher);

        PostApply approved = new PostApply();
        approved.setId(100L);
        approved.setPostId(POST_ID);
        approved.setApplicantId(8L);
        approved.setStatus("APPROVED");
        // 第一次查 APPROVED 列表，第二次查 ATTENDED 列表
        when(postApplyMapper.selectList(any())).thenReturn(List.of(approved), List.of());

        User applicant = new User();
        applicant.setId(8L);
        applicant.setNickname("参与者");
        applicant.setPhone("13800000002");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(applicant));

        when(userTagMapper.selectList(any())).thenReturn(List.of());
        when(creditService.getCreditLevel(any())).thenReturn(4);
    }

    @Test
    void detailHidesContactFromUnauthorizedViewer() {
        stubDetailCommon();
        when(postApplyMapper.selectOne(any())).thenReturn(null); // 查看者未报名

        PostDetailVO vo = postService.getPostDetail(99L, POST_ID);

        assertThat(vo.getContactVisible()).isFalse();
        // 未授权者拿不到手机号（服务端不填充，而不是靠前端隐藏）
        assertThat(vo.getPublisherContact()).isNull();
        assertThat(vo.getApplyUsers()).hasSize(1);
        assertThat(vo.getApplyUsers().get(0).getContact()).isNull();
    }

    @Test
    void detailHidesContactFromPendingApplicant() {
        stubDetailCommon();
        PostApply myApply = new PostApply();
        myApply.setId(200L);
        myApply.setStatus("PENDING"); // 待审核：仍未解锁
        when(postApplyMapper.selectOne(any())).thenReturn(myApply);

        PostDetailVO vo = postService.getPostDetail(8L, POST_ID);

        assertThat(vo.getContactVisible()).isFalse();
        assertThat(vo.getPublisherContact()).isNull();
    }

    @Test
    void detailShowsContactToApprovedParticipant() {
        stubDetailCommon();
        PostApply myApply = new PostApply();
        myApply.setId(200L);
        myApply.setStatus("APPROVED");
        when(postApplyMapper.selectOne(any())).thenReturn(myApply);

        PostDetailVO vo = postService.getPostDetail(8L, POST_ID);

        assertThat(vo.getContactVisible()).isTrue();
        assertThat(vo.getPublisherContact()).isEqualTo("13800000001");
        assertThat(vo.getApplyUsers().get(0).getContact()).isEqualTo("13800000002");
    }

    @Test
    void detailShowsContactToPublisher() {
        stubDetailCommon();
        when(postApplyMapper.selectOne(any())).thenReturn(null); // 发布者本人不报名

        PostDetailVO vo = postService.getPostDetail(USER_ID, POST_ID);

        assertThat(vo.getContactVisible()).isTrue();
        assertThat(vo.getApplyUsers().get(0).getContact()).isEqualTo("13800000002");
    }
}
