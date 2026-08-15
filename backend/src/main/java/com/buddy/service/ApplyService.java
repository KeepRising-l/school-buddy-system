package com.buddy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.dto.response.ApplyVO;

import java.util.List;

/**
 * 报名服务接口
 */
public interface ApplyService {

    void apply(Long userId, Long postId);

    /**
     * 候补报名（V1.1）：帖子满员时进入候补队列
     */
    void waitlist(Long userId, Long postId);

    void cancelApply(Long userId, Long applyId);

    void approve(Long publisherId, Long applyId);

    void reject(Long publisherId, Long applyId);

    /**
     * 获取帖子的报名列表（含申请人信息）
     * 仅帖子发布者本人可以查看
     */
    List<ApplyVO> getApplyList(Long userId, Long postId);

    IPage<ApplyVO> getReceivedApplies(Long userId, int page, int size);

    void markAttendance(Long publisherId, Long applyId, boolean attended);
}
