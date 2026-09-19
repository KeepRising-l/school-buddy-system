package com.buddy.common.statemachine;

import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 帖子状态机校验器（等价迁移自旧版）
 * 定义帖子状态的合法流转规则
 */
public class PostStateMachine {

    /** 合法的状态流转映射 */
    private static final Map<String, Set<String>> TRANSITIONS = new HashMap<>();

    static {
        // RECRUITING -> FULL, ONGOING, CANCELLED
        TRANSITIONS.put(PostStatus.RECRUITING, new HashSet<>(Arrays.asList(PostStatus.FULL, PostStatus.ONGOING, PostStatus.CANCELLED)));
        // FULL -> RECRUITING, ONGOING, CANCELLED
        TRANSITIONS.put(PostStatus.FULL, new HashSet<>(Arrays.asList(PostStatus.RECRUITING, PostStatus.ONGOING, PostStatus.CANCELLED)));
        // ONGOING -> FINISHED, CANCELLED（管理员可下架进行中的帖子）
        TRANSITIONS.put(PostStatus.ONGOING, new HashSet<>(Arrays.asList(PostStatus.FINISHED, PostStatus.CANCELLED)));
        // FINISHED 和 CANCELLED 是终态，不可流转
    }

    /**
     * 校验状态流转是否合法，不合法则抛出业务异常
     *
     * @param from 当前状态
     * @param to   目标状态
     */
    public static void checkTransition(String from, String to) {
        Set<String> allowedTargets = TRANSITIONS.get(from);
        if (allowedTargets == null || !allowedTargets.contains(to)) {
            throw new BusinessException(ResultCode.STATUS_TRANSIT_ERROR);
        }
    }

    /**
     * 判断是否可以流转到目标状态
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 是否可以流转
     */
    public static boolean canTransit(String from, String to) {
        Set<String> allowedTargets = TRANSITIONS.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }
}
