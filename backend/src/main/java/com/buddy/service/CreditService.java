package com.buddy.service;

/**
 * 信誉分服务接口
 */
public interface CreditService {

    /**
     * 重新计算用户信誉分
     */
    void recalculateUserCredit(Long userId);

    /**
     * 重新计算所有用户的信誉分
     */
    void recalculateAllCredit();

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
