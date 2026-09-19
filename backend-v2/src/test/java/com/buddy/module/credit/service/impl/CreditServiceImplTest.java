package com.buddy.module.credit.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.buddy.module.credit.entity.CreditLog;
import com.buddy.module.credit.mapper.CreditLogMapper;
import com.buddy.module.review.entity.Review;
import com.buddy.module.review.mapper.ReviewMapper;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 信誉分服务单元测试：五档映射边界、标签→分值映射、重算公式与限幅、台账即时生效
 */
@ExtendWith(MockitoExtension.class)
class CreditServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CreditLogMapper creditLogMapper;

    private CreditServiceImpl creditService;

    @BeforeAll
    static void initTableInfo() {
        // 单测环境下手工初始化 MyBatis-Plus 实体元数据（LambdaQueryWrapper 依赖）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoInit.init(assistant);
    }

    /** 集中初始化，避免每个用例重复 */
    static class TableInfoInit {
        static void init(MapperBuilderAssistant assistant) {
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, Review.class);
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, User.class);
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, CreditLog.class);
        }
    }

    @BeforeEach
    void setUp() {
        creditService = new CreditServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(creditService, "reviewMapper", reviewMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(creditService, "userMapper", userMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(creditService, "creditLogMapper", creditLogMapper);
    }

    private static Review review(String tag, Integer rating) {
        Review r = new Review();
        r.setTag(tag);
        r.setRating(rating);
        return r;
    }

    // ===== 五档映射（逐档 + 边界值） =====

    @Test
    void levelBoundaries() {
        assertThat(creditService.getCreditLevel(100)).isEqualTo(5);
        assertThat(creditService.getCreditLevel(90)).isEqualTo(5);
        assertThat(creditService.getCreditLevel(89)).isEqualTo(4);
        assertThat(creditService.getCreditLevel(75)).isEqualTo(4);
        assertThat(creditService.getCreditLevel(74)).isEqualTo(3);
        assertThat(creditService.getCreditLevel(60)).isEqualTo(3);
        assertThat(creditService.getCreditLevel(59)).isEqualTo(2);
        assertThat(creditService.getCreditLevel(40)).isEqualTo(2);
        assertThat(creditService.getCreditLevel(39)).isEqualTo(1);
        assertThat(creditService.getCreditLevel(0)).isEqualTo(1);
        assertThat(creditService.getCreditLevel(null)).isEqualTo(1);
    }

    @Test
    void labelBoundaries() {
        assertThat(creditService.getCreditLabel(95)).isEqualTo("靠谱队友");
        assertThat(creditService.getCreditLabel(80)).isEqualTo("优质队友");
        assertThat(creditService.getCreditLabel(65)).isEqualTo("普通队友");
        assertThat(creditService.getCreditLabel(50)).isEqualTo("需改善");
        assertThat(creditService.getCreditLabel(10)).isEqualTo("失信用户");
        assertThat(creditService.getCreditLabel(null)).isEqualTo("失信用户");
    }

    @Test
    void lowCreditThreshold() {
        assertThat(creditService.isLowCredit(60)).isFalse();
        assertThat(creditService.isLowCredit(59)).isTrue();
        assertThat(creditService.isLowCredit(null)).isFalse();
    }

    // ===== 评价 → 分值映射 =====

    @Test
    void positiveTagsMapToTwoPoints() {
        when(reviewMapper.selectList(any())).thenReturn(List.of(
                review("靠谱", 5), review("友好", null), review("技术好", 4)));
        assertThat(creditService.getReviewImpact(1L)).isEqualTo(6);
    }

    @Test
    void punctualPlusOneLateMinusThree() {
        when(reviewMapper.selectList(any())).thenReturn(List.of(review("准时", null), review("迟到", 2)));
        assertThat(creditService.getReviewImpact(1L)).isEqualTo(-2);
    }

    @Test
    void noshowTagMinusTen() {
        when(reviewMapper.selectList(any())).thenReturn(List.of(review("爽约", 1)));
        assertThat(creditService.getReviewImpact(1L)).isEqualTo(-10);
    }

    @Test
    void multiTagTakesFirst() {
        // 多标签逗号分隔时只取第一个有效标签，避免重复累加
        when(reviewMapper.selectList(any())).thenReturn(List.of(review("靠谱,准时", null)));
        assertThat(creditService.getReviewImpact(1L)).isEqualTo(2);
    }

    @Test
    void ratingFallbackWithoutTag() {
        when(reviewMapper.selectList(any())).thenReturn(List.of(
                review(null, 5), review(null, 4), review(null, 2), review(null, 3), review("", null)));
        // 5星+2、4星+1、≤2星-3、3星0、无标签无星0
        assertThat(creditService.getReviewImpact(1L)).isEqualTo(0);
    }

    // ===== 重算公式：100 + Σ评价 + Σ台账，限幅 [0,100] =====

    @Test
    void recalculateCombinesReviewAndLogs() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(reviewMapper.selectList(any())).thenReturn(List.of(review("靠谱", 5)));        // +2
        when(creditLogMapper.selectList(any())).thenReturn(List.of(
                creditLog(-10), creditLog(-5)));                                            // -15

        creditService.recalculateUserCredit(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getCreditScore()).isEqualTo(87);
    }

    @Test
    void recalculateClampsToZeroAndHundred() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(reviewMapper.selectList(any())).thenReturn(List.of(review("爽约", 1)));        // -10
        when(creditLogMapper.selectList(any())).thenReturn(List.of(creditLog(-10)));       // -10
        // 100 - 10 - 10 = 80，不触限幅；再造一个触底场景
        creditService.recalculateUserCredit(1L);
        verify(userMapper).updateById(user);

        when(reviewMapper.selectList(any())).thenReturn(List.of());
        when(creditLogMapper.selectList(any())).thenReturn(List.of(creditLog(-500)));
        creditService.recalculateUserCredit(1L);
        assertThat(user.getCreditScore()).isZero();
    }

    @Test
    void recalculateSkipsMissingUser() {
        when(userMapper.selectById(99L)).thenReturn(null);
        creditService.recalculateUserCredit(99L);
        verify(userMapper, never()).updateById(any(User.class));
    }

    // ===== recordPenalty：写台账 + 即时重算，返回新分值 =====

    @Test
    void recordPenaltyWritesLogAndRecalculates() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user, user);
        when(reviewMapper.selectList(any())).thenReturn(List.of());
        // 重算时台账里已包含本次插入的 -10（真实流程：insert 先于重算查询）
        when(creditLogMapper.selectList(any())).thenReturn(List.of(creditLog(-10)));

        int newScore = creditService.recordPenalty(1L, -10, CreditLog.REASON_NOSHOW,
                CreditLog.BIZ_TYPE_APPLY, 100L);

        assertThat(newScore).isEqualTo(90);
        ArgumentCaptor<CreditLog> captor = ArgumentCaptor.forClass(CreditLog.class);
        verify(creditLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getChangeValue()).isEqualTo(-10);
        assertThat(captor.getValue().getReason()).isEqualTo("NOSHOW");
        assertThat(captor.getValue().getBizId()).isEqualTo(100L);
    }

    private static CreditLog creditLog(int change) {
        CreditLog log = new CreditLog();
        log.setUserId(1L);
        log.setChangeValue(change);
        return log;
    }
}
