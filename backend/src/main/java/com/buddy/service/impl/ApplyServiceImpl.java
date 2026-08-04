package com.buddy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.CreditConstant;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.common.statemachine.PostStateMachine;
import com.buddy.dto.response.ApplyVO;
import com.buddy.entity.Post;
import com.buddy.entity.PostApply;
import com.buddy.entity.User;
import com.buddy.entity.UserTag;
import com.buddy.mapper.PostApplyMapper;
import com.buddy.mapper.PostMapper;
import com.buddy.mapper.UserMapper;
import com.buddy.mapper.UserTagMapper;
import com.buddy.service.ApplyService;
import com.buddy.service.CreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 报名服务实现类
 */
@Service
public class ApplyServiceImpl implements ApplyService {

    @Autowired
    private PostApplyMapper postApplyMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserTagMapper userTagMapper;

    @Autowired
    private CreditService creditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long userId, Long postId) {
        // 校验帖子存在且状态为RECRUITING
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!PostStatus.RECRUITING.equals(post.getStatus())) {
            throw new BusinessException(ResultCode.POST_NOT_RECRUITING);
        }

        // 校验不能报名自己发布的帖子
        if (post.getPublisherId().equals(userId)) {
            throw new BusinessException("不能报名自己发布的活动");
        }

        // 校验未重复报名
        Long applyCount = postApplyMapper.selectCount(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .eq(PostApply::getApplicantId, userId)
                        .ne(PostApply::getStatus, ApplyStatus.CANCELLED)
        );
        if (applyCount > 0) {
            throw new BusinessException(ResultCode.APPLY_DUPLICATE);
        }

        // 校验信誉分
        User applicant = userMapper.selectById(userId);
        if (applicant == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (creditService.isLowCredit(applicant.getCreditScore())) {
            throw new BusinessException(ResultCode.CREDIT_TOO_LOW);
        }

        // 校验段位要求（requireLevel为0表示不限）
        if (post.getRequireLevel() != null && post.getRequireLevel() > 0) {
            UserTag userTag = userTagMapper.selectOne(
                    new LambdaQueryWrapper<UserTag>()
                            .eq(UserTag::getUserId, userId)
                            .eq(UserTag::getTagId, parseFirstTagId(post.getTagIds()))
                            .last("LIMIT 1")
            );
            if (userTag == null || userTag.getSelfLevel() < post.getRequireLevel()) {
                throw new BusinessException(ResultCode.APPLY_NOT_QUALIFIED, "段位不满足要求");
            }
        }

        // 校验性别要求
        if (post.getRequireGender() != null && post.getRequireGender() != 0) {
            if (!post.getRequireGender().equals(applicant.getGender())) {
                throw new BusinessException(ResultCode.APPLY_NOT_QUALIFIED, "性别不满足要求");
            }
        }

        // 插入报名记录
        PostApply apply = new PostApply();
        apply.setPostId(postId);
        apply.setApplicantId(userId);
        apply.setStatus(ApplyStatus.PENDING);
        apply.setApplyTime(LocalDateTime.now());

        postApplyMapper.insert(apply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelApply(Long userId, Long applyId) {
        PostApply apply = postApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }

        // 校验是报名者本人
        if (!apply.getApplicantId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能取消自己的报名");
        }

        // 校验状态可取消（PENDING或APPROVED才能取消）
        String originalStatus = apply.getStatus();
        if (!ApplyStatus.PENDING.equals(originalStatus) && !ApplyStatus.APPROVED.equals(originalStatus)) {
            throw new BusinessException("当前报名状态不可取消");
        }

        // 校验帖子状态：只有招募中/已满员才能取消（CAN-01；进行中/已结束不可取消 CAN-04）
        Post post = postMapper.selectById(apply.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!PostStatus.RECRUITING.equals(post.getStatus()) && !PostStatus.FULL.equals(post.getStatus())) {
            throw new BusinessException("活动已开始或已结束，无法取消报名");
        }

        // 更新报名状态
        apply.setStatus(ApplyStatus.CANCELLED);
        apply.setHandleTime(LocalDateTime.now());
        postApplyMapper.updateById(apply);

        // 已通过（APPROVED）的报名取消后：人数-1，若满员则回退为招募中（原子操作）
        if (ApplyStatus.APPROVED.equals(originalStatus)) {
            postMapper.decrementCurrentPeople(post.getId());
            post = postMapper.selectById(post.getId());
            if (PostStatus.FULL.equals(post.getStatus())
                    && post.getCurrentPeople() < post.getMaxPeople()) {
                post.setStatus(PostStatus.RECRUITING);
                postMapper.updateById(post);
            }
        }

        // 活动开始前1小时内取消报名，扣除信誉分5分（CAN-03，窗口为 [开始前1小时, 开始时间]）
        if (post.getStartTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = post.getStartTime();
            if (now.isAfter(startTime.minusHours(1)) && now.isBefore(startTime)) {
                User applicant = userMapper.selectById(userId);
                if (applicant != null) {
                    int newScore = Math.max(CreditConstant.MIN_SCORE,
                            applicant.getCreditScore() + CreditConstant.CANCEL_LAST_MINUTE);
                    applicant.setCreditScore(newScore);
                    userMapper.updateById(applicant);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long publisherId, Long applyId) {
        PostApply apply = postApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }

        // 校验是发布者
        Post post = postMapper.selectById(apply.getPostId());
        if (post == null || !post.getPublisherId().equals(publisherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有发布者才能审批报名");
        }

        // 校验帖子状态：只有招募中/已满员才能审批（活动开始后不可再加人）
        if (!PostStatus.RECRUITING.equals(post.getStatus()) && !PostStatus.FULL.equals(post.getStatus())) {
            throw new BusinessException("当前帖子状态不可审批报名");
        }

        // 校验报名状态为PENDING
        if (!ApplyStatus.PENDING.equals(apply.getStatus())) {
            throw new BusinessException("只能审批待审核的报名");
        }

        // 更新报名状态为APPROVED
        apply.setStatus(ApplyStatus.APPROVED);
        apply.setHandleTime(LocalDateTime.now());
        postApplyMapper.updateById(apply);

        // 原子增加人数（仅当未满员时），防止并发超卖
        int updated = postMapper.incrementCurrentPeople(post.getId());
        if (updated == 0) {
            throw new BusinessException("名额已满，无法通过报名");
        }

        // 重新查询，检查是否满员并流转状态
        post = postMapper.selectById(post.getId());
        if (post.getCurrentPeople() >= post.getMaxPeople()) {
            PostStateMachine.checkTransition(post.getStatus(), PostStatus.FULL);
            post.setStatus(PostStatus.FULL);
            postMapper.updateById(post);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long publisherId, Long applyId) {
        PostApply apply = postApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }

        // 校验是发布者
        Post post = postMapper.selectById(apply.getPostId());
        if (post == null || !post.getPublisherId().equals(publisherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有发布者才能审批报名");
        }

        // 校验报名状态为PENDING
        if (!ApplyStatus.PENDING.equals(apply.getStatus())) {
            throw new BusinessException("只能拒绝待审核的报名");
        }

        // 更新报名状态为REJECTED
        apply.setStatus(ApplyStatus.REJECTED);
        apply.setHandleTime(LocalDateTime.now());
        postApplyMapper.updateById(apply);
    }

    @Override
    public List<ApplyVO> getApplyList(Long userId, Long postId) {
        // 校验：只有帖子发布者才能查看报名列表（防止越权查看他人帖子的报名信息）
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!post.getPublisherId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有发布者才能查看报名列表");
        }

        List<PostApply> applies = postApplyMapper.selectList(
                new LambdaQueryWrapper<PostApply>()
                        .eq(PostApply::getPostId, postId)
                        .orderByDesc(PostApply::getApplyTime)
        );
        return applies.stream().map(apply -> {
            ApplyVO vo = new ApplyVO();
            vo.setId(apply.getId());
            vo.setPostId(apply.getPostId());
            vo.setApplicantId(apply.getApplicantId());
            vo.setStatus(apply.getStatus());
            vo.setApplyTime(apply.getApplyTime());
            vo.setHandleTime(apply.getHandleTime());
            vo.setRemark(apply.getRemark());

            // 查询申请人信息
            User applicant = userMapper.selectById(apply.getApplicantId());
            if (applicant != null) {
                vo.setApplicantNickname(applicant.getNickname());
                vo.setApplicantAvatar(applicant.getAvatar());
                vo.setApplicantCreditLevel(creditService.getCreditLevel(applicant.getCreditScore()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public IPage<ApplyVO> getReceivedApplies(Long userId, int page, int size) {
        // 查询当前用户发布的帖子ID列表
        List<Post> myPosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>().eq(Post::getPublisherId, userId)
        );
        if (myPosts.isEmpty()) {
            return new Page<>(page, size);
        }

        List<Long> postIds = myPosts.stream().map(Post::getId).collect(java.util.stream.Collectors.toList());

        // 查询这些帖子的报名记录
        IPage<PostApply> applyPage = postApplyMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PostApply>()
                        .in(PostApply::getPostId, postIds)
                        .orderByDesc(PostApply::getApplyTime)
        );

        // 转换为ApplyVO
        IPage<ApplyVO> voPage = new Page<>(applyPage.getCurrent(), applyPage.getSize(), applyPage.getTotal());
        voPage.setRecords(applyPage.getRecords().stream().map(apply -> {
            ApplyVO vo = new ApplyVO();
            vo.setId(apply.getId());
            vo.setPostId(apply.getPostId());
            vo.setApplicantId(apply.getApplicantId());
            vo.setStatus(apply.getStatus());
            vo.setApplyTime(apply.getApplyTime());
            vo.setHandleTime(apply.getHandleTime());
            vo.setRemark(apply.getRemark());

            // 查询申请人信息
            User applicant = userMapper.selectById(apply.getApplicantId());
            if (applicant != null) {
                vo.setApplicantNickname(applicant.getNickname());
                vo.setApplicantAvatar(applicant.getAvatar());
                vo.setApplicantCreditLevel(creditService.getCreditLevel(applicant.getCreditScore()));
            }

            // 查询帖子标题
            Post post = postMapper.selectById(apply.getPostId());
            if (post != null) {
                vo.setPostTitle(post.getTitle());
            }

            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAttendance(Long publisherId, Long applyId, boolean attended) {
        PostApply apply = postApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }

        // 校验是帖子发布者
        Post post = postMapper.selectById(apply.getPostId());
        if (post == null || !post.getPublisherId().equals(publisherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有发布者才能标记出勤");
        }

        // 校验帖子状态为ONGOING或FINISHED
        if (!PostStatus.ONGOING.equals(post.getStatus()) && !PostStatus.FINISHED.equals(post.getStatus())) {
            throw new BusinessException("活动进行中或已结束时才能标记出勤");
        }

        // 校验报名状态为APPROVED
        if (!ApplyStatus.APPROVED.equals(apply.getStatus())) {
            throw new BusinessException("只能标记已通过的报名出勤状态");
        }

        // 更新出勤状态
        apply.setStatus(attended ? ApplyStatus.ATTENDED : ApplyStatus.NOSHOW);
        apply.setHandleTime(LocalDateTime.now());
        postApplyMapper.updateById(apply);

        // 未到场扣信誉分10分
        if (!attended) {
            User applicant = userMapper.selectById(apply.getApplicantId());
            if (applicant != null) {
                int newScore = Math.max(CreditConstant.MIN_SCORE,
                        applicant.getCreditScore() + CreditConstant.NOSHOW_PENALTY);
                applicant.setCreditScore(newScore);
                userMapper.updateById(applicant);
            }
        }
    }

    /**
     * 解析帖子tagIds中的第一个标签ID
     */
    private Long parseFirstTagId(String tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }
        String[] ids = tagIds.split(",");
        return Long.parseLong(ids[0].trim());
    }
}
