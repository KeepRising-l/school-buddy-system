package com.buddy.module.apply.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.module.apply.entity.PostApply;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.credit.service.CreditService;
import com.buddy.module.notification.service.NotificationService;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.tag.entity.UserTag;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报名服务单元测试：资格三重校验、并发防护（条件更新/原子人数）、取消扣分窗口、出勤规则
 */
@ExtendWith(MockitoExtension.class)
class ApplyServiceImplTest {

    private static final Long PUBLISHER_ID = 1L;
    private static final Long APPLICANT_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final Long APPLY_ID = 100L;

    @Mock
    private PostApplyMapper postApplyMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserTagMapper userTagMapper;

    @Mock
    private CreditService creditService;

    @Mock
    private NotificationService notificationService;

    private ApplyServiceImpl applyService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, PostApply.class);
        TableInfoHelper.initTableInfo(assistant, Post.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, UserTag.class);
    }

    @BeforeEach
    void setUp() {
        applyService = new ApplyServiceImpl();
        ReflectionTestUtils.setField(applyService, "postApplyMapper", postApplyMapper);
        ReflectionTestUtils.setField(applyService, "postMapper", postMapper);
        ReflectionTestUtils.setField(applyService, "userMapper", userMapper);
        ReflectionTestUtils.setField(applyService, "userTagMapper", userTagMapper);
        ReflectionTestUtils.setField(applyService, "creditService", creditService);
        ReflectionTestUtils.setField(applyService, "notificationService", notificationService);
    }

    // ===== 构造辅助 =====

    private static Post post(String status) {
        Post post = new Post();
        post.setId(POST_ID);
        post.setPublisherId(PUBLISHER_ID);
        post.setTitle("测试组局");
        post.setStatus(status);
        post.setIsAuditPass(1);
        post.setMaxPeople(4);
        post.setCurrentPeople(1);
        post.setStartTime(LocalDateTime.now().plusDays(1));
        post.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        return post;
    }

    private static PostApply apply(String status) {
        PostApply apply = new PostApply();
        apply.setId(APPLY_ID);
        apply.setPostId(POST_ID);
        apply.setApplicantId(APPLICANT_ID);
        apply.setStatus(status);
        apply.setApplyTime(LocalDateTime.now().minusHours(1));
        return apply;
    }

    private static User applicant(Integer gender, Integer credit) {
        User user = new User();
        user.setId(APPLICANT_ID);
        user.setNickname("测试报名者");
        user.setGender(gender);
        user.setCreditScore(credit);
        return user;
    }

    private void stubQualificationOk(Post post) {
        lenient().when(userMapper.selectById(APPLICANT_ID)).thenReturn(applicant(0, 100));
        lenient().when(userTagMapper.selectList(any())).thenReturn(ListOf.userTag(3));
    }

    /** 小助手：构造用户标签列表 */
    static class ListOf {
        static java.util.List<UserTag> userTag(int level) {
            UserTag ut = new UserTag();
            ut.setUserId(APPLICANT_ID);
            ut.setTagId(1L);
            ut.setSelfLevel(level);
            return java.util.List.of(ut);
        }
    }

    // ===== 报名资格校验 =====

    @Test
    void applyRejectsMissingPost() {
        when(postMapper.selectById(POST_ID)).thenReturn(null);
        assertThatThrownBy(() -> applyService.apply(APPLICANT_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帖子不存在");
    }

    @Test
    void applyRejectsNonRecruitingPost() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.ONGOING));
        assertThatThrownBy(() -> applyService.apply(APPLICANT_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("招募");
    }

    @Test
    void applyRejectsOwnPost() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        assertThatThrownBy(() -> applyService.apply(PUBLISHER_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能报名自己发布的活动");
    }

    @Test
    void applyRejectsTakenDownPost() {
        Post post = post(PostStatus.RECRUITING);
        post.setIsAuditPass(2);
        when(postMapper.selectById(POST_ID)).thenReturn(post);
        assertThatThrownBy(() -> applyService.apply(APPLICANT_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("下架");
    }

    @Test
    void applyRejectsDuplicate() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        when(postApplyMapper.selectOne(any())).thenReturn(apply("PENDING"));
        assertThatThrownBy(() -> applyService.apply(APPLICANT_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已报名");
    }

    @Test
    void applyAllowsReapplyAfterCancel() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        PostApply cancelled = apply("CANCELLED");
        when(postApplyMapper.selectOne(any())).thenReturn(cancelled);
        stubQualificationOk(post(PostStatus.RECRUITING));

        applyService.apply(APPLICANT_ID, POST_ID);

        // 复用旧记录行，规避唯一键冲突
        verify(postApplyMapper, never()).insert(any(PostApply.class));
        verify(postApplyMapper).updateById(cancelled);
        assertThat(cancelled.getStatus()).isEqualTo("PENDING");
        verify(notificationService).send(eq(PUBLISHER_ID), eq("APPLY_NEW"), any(), any(), any(), eq(POST_ID));
    }

    @Test
    void applyRejectsLowCredit() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        when(postApplyMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectById(APPLICANT_ID)).thenReturn(applicant(0, 59));
        // creditService 为 mock，低分判定按桩返回（判定逻辑本身由 CreditServiceImplTest 覆盖）
        when(creditService.isLowCredit(59)).thenReturn(true);
        assertThatThrownBy(() -> applyService.apply(APPLICANT_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("信誉分");
    }

    @Test
    void applyRejectsLevelNotMet() {
        Post post = post(PostStatus.RECRUITING);
        post.setRequireLevel(4);
        post.setTagIds("1,2");
        when(postMapper.selectById(POST_ID)).thenReturn(post);
        when(postApplyMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectById(APPLICANT_ID)).thenReturn(applicant(0, 100));
        when(userTagMapper.selectList(any())).thenReturn(ListOf.userTag(2));

        assertThatThrownBy(() -> applyService.apply(APPLICANT_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("段位不满足要求");
    }

    @Test
    void applyRejectsGenderMismatch() {
        Post post = post(PostStatus.RECRUITING);
        post.setRequireGender(1); // 仅男
        when(postMapper.selectById(POST_ID)).thenReturn(post);
        when(postApplyMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectById(APPLICANT_ID)).thenReturn(applicant(2, 100)); // 女

        assertThatThrownBy(() -> applyService.apply(APPLICANT_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("性别不满足要求");
    }

    @Test
    void applySuccessInsertsAndNotifiesPublisher() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        when(postApplyMapper.selectOne(any())).thenReturn(null);
        stubQualificationOk(post(PostStatus.RECRUITING));

        applyService.apply(APPLICANT_ID, POST_ID);

        verify(postApplyMapper).insert(any(PostApply.class));
        verify(notificationService).send(eq(PUBLISHER_ID), eq("APPLY_NEW"), any(), any(), any(), eq(POST_ID));
    }

    // ===== 候补 =====

    @Test
    void waitlistRejectsWhenNotFull() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        assertThatThrownBy(() -> applyService.waitlist(APPLICANT_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未满员");
    }

    @Test
    void waitlistSuccessNotifiesPosition() {
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.FULL));
        when(postApplyMapper.selectOne(any())).thenReturn(null);
        lenient().when(userMapper.selectById(APPLICANT_ID)).thenReturn(applicant(0, 100));
        lenient().when(userTagMapper.selectList(any())).thenReturn(ListOf.userTag(1));
        when(postApplyMapper.selectCount(any())).thenReturn(2L);

        applyService.waitlist(APPLICANT_ID, POST_ID);

        verify(postApplyMapper).insert(any(PostApply.class));
        verify(notificationService).send(eq(APPLICANT_ID), eq("WAITLIST_PROMOTED"),
                any(), org.mockito.ArgumentMatchers.contains("候补第 2 位"), any(), eq(POST_ID));
    }

    // ===== 取消报名 =====

    @Test
    void cancelRejectsNotOwner() {
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("PENDING"));
        assertThatThrownBy(() -> applyService.cancelApply(99L, APPLY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能取消自己的报名");
    }

    @Test
    void cancelRejectsFinishedApply() {
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("FINISHED"));
        assertThatThrownBy(() -> applyService.cancelApply(APPLICANT_ID, APPLY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可取消");
    }

    @Test
    void cancelRejectsAfterActivityStarts() {
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("APPROVED"));
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.ONGOING));
        assertThatThrownBy(() -> applyService.cancelApply(APPLICANT_ID, APPLY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法取消报名");
    }

    @Test
    void cancelRejectsConcurrentDuplicateCancel() {
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("APPROVED"));
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        // 条件更新命中 0 行：并发下已被取消
        when(postApplyMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> applyService.cancelApply(APPLICANT_ID, APPLY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可取消");
    }

    @Test
    void cancelApprovedDecrementsPeople() {
        Post post = post(PostStatus.RECRUITING);
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("APPROVED"));
        when(postMapper.selectById(POST_ID)).thenReturn(post);
        when(postApplyMapper.update(isNull(), any())).thenReturn(1);
        when(postMapper.selectById(post.getId())).thenReturn(post); // 取消后重查

        applyService.cancelApply(APPLICANT_ID, APPLY_ID);

        verify(postMapper).decrementCurrentPeople(POST_ID);
        // 未到临场取消窗口（开始前1小时以外），不扣分
        verify(creditService, never()).recordPenalty(anyLong(), anyInt(), any(), any(), anyLong());
    }

    @Test
    void cancelWithinLastHourPenalizesFive() {
        Post post = post(PostStatus.RECRUITING);
        post.setStartTime(LocalDateTime.now().plusMinutes(30)); // 临场窗口内
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("APPROVED"));
        when(postMapper.selectById(POST_ID)).thenReturn(post);
        when(postApplyMapper.update(isNull(), any())).thenReturn(1);
        when(postMapper.selectById(post.getId())).thenReturn(post);
        when(creditService.recordPenalty(eq(APPLICANT_ID), eq(-5), eq("CANCEL_LAST_MINUTE"), eq("APPLY"), eq(APPLY_ID)))
                .thenReturn(95);

        applyService.cancelApply(APPLICANT_ID, APPLY_ID);

        verify(creditService).recordPenalty(eq(APPLICANT_ID), eq(-5), eq("CANCEL_LAST_MINUTE"), eq("APPLY"), eq(APPLY_ID));
    }

    @Test
    void cancelPromotesFirstWaitlistWhenFull() {
        Post full = post(PostStatus.FULL);
        PostApply approved = apply("APPROVED");
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(approved);
        when(postMapper.selectById(POST_ID)).thenReturn(full);
        when(postApplyMapper.update(isNull(), any())).thenReturn(1);
        // 第一次重查：仍满员；第二次重查（递补后）：人数回落
        Post afterDecrement = post(PostStatus.FULL);
        when(postMapper.selectById(full.getId())).thenReturn(full, afterDecrement);
        // 候补队列第一位存在
        PostApply firstWait = apply("WAITLIST");
        when(postApplyMapper.selectOne(any())).thenReturn(firstWait);

        applyService.cancelApply(APPLICANT_ID, APPLY_ID);

        verify(postMapper).decrementCurrentPeople(POST_ID);
        verify(postApplyMapper).updateById(firstWait);
        assertThat(firstWait.getStatus()).isEqualTo("PENDING");
        verify(notificationService).send(eq(firstWait.getApplicantId()), eq("WAITLIST_PROMOTED"),
                eq("有空位啦！"), any(), any(), eq(POST_ID));
    }

    // ===== 审批 =====

    @Test
    void approveRejectsNonPublisher() {
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("PENDING"));
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        assertThatThrownBy(() -> applyService.approve(99L, APPLY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有发布者");
    }

    @Test
    void approveRejectsFinishedPost() {
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("PENDING"));
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.FINISHED));
        assertThatThrownBy(() -> applyService.approve(PUBLISHER_ID, APPLY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可审批");
    }

    @Test
    void approveRejectsNonPendingApply() {
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("CANCELLED"));
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        assertThatThrownBy(() -> applyService.approve(PUBLISHER_ID, APPLY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("待审核");
    }

    @Test
    void approveRejectsConcurrentDuplicateApprove() {
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("PENDING"));
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        when(postApplyMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> applyService.approve(PUBLISHER_ID, APPLY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被处理");
    }

    @Test
    void approveRejectsWhenFull() {
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("PENDING"));
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.RECRUITING));
        when(postApplyMapper.update(isNull(), any())).thenReturn(1);
        when(postMapper.incrementCurrentPeople(POST_ID)).thenReturn(0); // 原子自增失败=已满

        assertThatThrownBy(() -> applyService.approve(PUBLISHER_ID, APPLY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("名额已满");
    }

    @Test
    void approveSuccessTransitionsToFullAtCapacity() {
        Post post = post(PostStatus.RECRUITING);
        post.setCurrentPeople(3);
        Post afterIncrement = post(PostStatus.RECRUITING);
        afterIncrement.setCurrentPeople(4); // 达到 maxPeople=4
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("PENDING"));
        when(postMapper.selectById(POST_ID)).thenReturn(post).thenReturn(afterIncrement);
        when(postApplyMapper.update(isNull(), any())).thenReturn(1);
        when(postMapper.incrementCurrentPeople(POST_ID)).thenReturn(1);
        when(userMapper.selectById(APPLICANT_ID)).thenReturn(applicant(0, 100));

        applyService.approve(PUBLISHER_ID, APPLY_ID);

        assertThat(afterIncrement.getStatus()).isEqualTo(PostStatus.FULL);
        verify(postMapper).updateById(afterIncrement);
        verify(notificationService).send(eq(APPLICANT_ID), eq("APPLY_RESULT"),
                eq("报名已通过"), any(), any(), eq(POST_ID));
    }

    // ===== 出勤标记 =====

    @Test
    void attendanceRejectsNoshowWhileOngoing() {
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("APPROVED"));
        when(postMapper.selectById(POST_ID)).thenReturn(post(PostStatus.ONGOING));
        assertThatThrownBy(() -> applyService.markAttendance(PUBLISHER_ID, APPLY_ID, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("暂不能标记未到场");
    }

    @Test
    void attendanceRejectsBeyond24hWindow() {
        Post post = post(PostStatus.FINISHED);
        post.setEndTime(LocalDateTime.now().minusHours(25));
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("APPROVED"));
        when(postMapper.selectById(POST_ID)).thenReturn(post);
        assertThatThrownBy(() -> applyService.markAttendance(PUBLISHER_ID, APPLY_ID, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("24小时");
    }

    @Test
    void attendanceRejectsNonApprovedApply() {
        Post post = post(PostStatus.FINISHED);
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("PENDING"));
        when(postMapper.selectById(POST_ID)).thenReturn(post);
        assertThatThrownBy(() -> applyService.markAttendance(PUBLISHER_ID, APPLY_ID, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能标记已通过");
    }

    @Test
    void noshowPenalizesTenAndNotifies() {
        Post post = post(PostStatus.FINISHED);
        post.setEndTime(LocalDateTime.now().minusHours(1));
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("APPROVED"));
        when(postMapper.selectById(POST_ID)).thenReturn(post);
        when(postApplyMapper.update(isNull(), any())).thenReturn(1);
        when(creditService.recordPenalty(eq(APPLICANT_ID), eq(-10), eq("NOSHOW"), eq("APPLY"), eq(APPLY_ID)))
                .thenReturn(90);

        applyService.markAttendance(PUBLISHER_ID, APPLY_ID, false);

        verify(creditService).recordPenalty(APPLICANT_ID, -10, "NOSHOW", "APPLY", APPLY_ID);
        verify(notificationService).send(eq(APPLICANT_ID), eq("SYSTEM"),
                eq("信誉分扣减通知"), org.mockito.ArgumentMatchers.contains("-10"), any(), eq(POST_ID));
    }

    @Test
    void attendedMarkDoesNotPenalize() {
        Post post = post(PostStatus.FINISHED);
        post.setEndTime(LocalDateTime.now().minusHours(1));
        when(postApplyMapper.selectById(APPLY_ID)).thenReturn(apply("APPROVED"));
        when(postMapper.selectById(POST_ID)).thenReturn(post);
        when(postApplyMapper.update(isNull(), any())).thenReturn(1);

        applyService.markAttendance(PUBLISHER_ID, APPLY_ID, true);

        verify(creditService, never()).recordPenalty(anyLong(), anyInt(), any(), any(), anyLong());
        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }
}
