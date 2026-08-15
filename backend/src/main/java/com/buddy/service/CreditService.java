package com.buddy.service;

/**
 * 信誉分服务接口
 */
public interface CreditService {

    /**
     * 重新计算用户信誉分
     * 公式 = clamp(100 + Σ评价影响 + Σ台账变动, 0, 100)
     */
    void recalculateUserCredit(Long userId);

    /**
     * 重新计算所有用户的信誉分
     */
    void recalculateAllCredit();

    /**
     * 记录一条信誉分惩罚/重置台账（V1.2），并立即重算该用户信誉分，
     * 保证惩罚实时生效且不被每日重算冲掉
     *
     * @return 重算后的信誉分
     */
    int recordPenalty(Long userId, int changeValue, String reason, String bizType, Long bizId);

    /**
     * 计算用户收到的全部评价对其信誉分的影响（V1.2，供管理员重置时换算）
     */
    int getReviewImpact(Long userId);

    /**
     * 根据信誉分获取星级
     */
    int getCreditLevel(Integer creditScore);

    /**
     * 根据信誉分获取标签文字
     */
    String getCreditLabel(Integer creditScore);

    /**
     * 判断信誉分是否过低
     */
    boolean isLowCredit(Integer creditScore);
}
