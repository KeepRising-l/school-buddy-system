package com.buddy.module.credit.service;

/**
 * 信誉分服务
 */
public interface CreditService {

    /**
     * 重算单个用户信誉分：100 + Σ评价影响 + Σ台账变动，限幅 [0,100]
     */
    void recalculateUserCredit(Long userId);

    /**
     * 重算所有正常状态用户的信誉分（每日定时任务调用）
     */
    void recalculateAllCredit();

    /**
     * 记录台账变动并立即重算（惩罚实时生效，且与每日重算结果一致）
     *
     * @return 重算后的信誉分
     */
    int recordPenalty(Long userId, int changeValue, String reason, String bizType, Long bizId);

    /**
     * 统计某用户收到的全部评价对信誉分的影响总和
     */
    int getReviewImpact(Long userId);

    /**
     * 信誉分 → 五档等级：≥90→5 / ≥75→4 / ≥60→3 / ≥40→2 / 其余→1；null 按 1
     */
    int getCreditLevel(Integer creditScore);

    /**
     * 信誉分 → 五档标签：靠谱队友/优质队友/普通队友/需改善/失信用户
     */
    String getCreditLabel(Integer creditScore);

    /**
     * 是否低于惩罚阈值（60 分）
     */
    boolean isLowCredit(Integer creditScore);
}
