package com.buddy.common.constant;

/**
 * 信誉分常量（等价迁移自旧版）
 */
public class CreditConstant {

    /** 初始信誉分 */
    public static final int INITIAL_SCORE = 100;
    /** 最低信誉分 */
    public static final int MIN_SCORE = 0;
    /** 最高信誉分 */
    public static final int MAX_SCORE = 100;
    /** 惩罚阈值，低于此分数将受限 */
    public static final int PUNISH_THRESHOLD = 60;
    /** 惩罚系数 */
    public static final double PUNISH_FACTOR = 0.3;

    /** 靠谱评价 +2 */
    public static final int RELIABLE_BONUS = 2;
    /** 4星评价 +1 */
    public static final int FOUR_STAR_BONUS = 1;
    /** 迟到 -3 */
    public static final int LATE_PENALTY = -3;
    /** 爽约 -10 */
    public static final int NOSHOW_PENALTY = -10;
    /** 爽约额外 -5 */
    public static final int NOSHOW_EXTRA_PENALTY = -5;
    /** 临场取消 -5 */
    public static final int CANCEL_LAST_MINUTE = -5;

    private CreditConstant() {
    }
}
