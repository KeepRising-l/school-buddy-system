package com.buddy.module.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.constant.CreditConstant;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.module.credit.entity.CreditLog;
import com.buddy.module.credit.mapper.CreditLogMapper;
import com.buddy.module.credit.service.CreditService;
import com.buddy.module.apply.entity.PostApply;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.post.converter.PostConverter;
import com.buddy.module.post.dto.PostVO;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.post.service.AdminPostService;
import com.buddy.module.review.entity.Review;
import com.buddy.module.review.mapper.ReviewMapper;
import com.buddy.module.user.dto.UserVO;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import com.buddy.module.user.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端-用户管理服务实现类（等价迁移自旧版 AdminServiceImpl 用户部分）
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostApplyMapper postApplyMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private CreditLogMapper creditLogMapper;

    @Autowired
    private CreditService creditService;

    @Autowired
    private PostConverter postConverter;

    @Autowired
    private AdminPostService adminPostService;

    @Override
    public IPage<UserVO> getUserList(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(User::getNickname, keyword)
                    .or().like(User::getStudentNo, keyword)
                    .or().like(User::getPhone, keyword)
            );
        }
        wrapper.orderByDesc(User::getCreatedTime);

        IPage<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);

        IPage<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream()
                .map(this::convertToUserVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public UserVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToUserVO(user);
    }

    @Override
    public IPage<PostVO> getUserPosts(Long userId, int page, int size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getPublisherId, userId)
                .orderByDesc(Post::getCreatedTime);
        IPage<Post> postPage = postMapper.selectPage(new Page<>(page, size), wrapper);

        IPage<PostVO> voPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        voPage.setRecords(postConverter.toVOList(postPage.getRecords()));
        return voPage;
    }

    @Override
    public IPage<Map<String, Object>> getUserApplies(Long userId, int page, int size) {
        LambdaQueryWrapper<PostApply> wrapper = new LambdaQueryWrapper<PostApply>()
                .eq(PostApply::getApplicantId, userId)
                .orderByDesc(PostApply::getApplyTime);
        IPage<PostApply> applyPage = postApplyMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量查出本页所有帖子标题（避免每条报名一次 selectById）
        Map<Long, Post> postMap = toPostMap(applyPage.getRecords());

        IPage<Map<String, Object>> resultPage = new Page<>(applyPage.getCurrent(), applyPage.getSize(), applyPage.getTotal());
        List<Map<String, Object>> records = new ArrayList<>(applyPage.getRecords().size());
        for (PostApply apply : applyPage.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", apply.getId());
            map.put("postId", apply.getPostId());
            map.put("status", apply.getStatus());
            map.put("applyTime", apply.getApplyTime());
            map.put("handleTime", apply.getHandleTime());
            Post post = apply.getPostId() != null ? postMap.get(apply.getPostId()) : null;
            if (post != null) {
                map.put("postTitle", post.getTitle());
            }
            records.add(map);
        }
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public IPage<Map<String, Object>> getUserReviews(Long userId, int page, int size) {
        // 查询该用户收到的评价
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(Review::getTargetUserId, userId)
                .orderByDesc(Review::getCreatedTime);
        IPage<Review> reviewPage = reviewMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量查出评价人与帖子标题（避免每条评价两次 selectById）
        List<Review> reviews = reviewPage.getRecords();
        Set<Long> reviewerIds = new HashSet<>();
        Set<Long> postIds = new HashSet<>();
        for (Review r : reviews) {
            if (r.getReviewerId() != null) {
                reviewerIds.add(r.getReviewerId());
            }
            if (r.getPostId() != null) {
                postIds.add(r.getPostId());
            }
        }
        Map<Long, User> reviewerMap = reviewerIds.isEmpty() ? Collections.emptyMap()
                : toMapById(userMapper.selectBatchIds(reviewerIds), User::getId);
        Map<Long, Post> postMap = postIds.isEmpty() ? Collections.emptyMap()
                : toMapById(postMapper.selectBatchIds(postIds), Post::getId);

        IPage<Map<String, Object>> resultPage = new Page<>(reviewPage.getCurrent(), reviewPage.getSize(), reviewPage.getTotal());
        List<Map<String, Object>> records = new ArrayList<>(reviews.size());
        for (Review review : reviews) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", review.getId());
            map.put("postId", review.getPostId());
            map.put("reviewerId", review.getReviewerId());
            map.put("rating", review.getRating());
            map.put("tag", review.getTag());
            map.put("content", review.getContent());
            map.put("createdTime", review.getCreatedTime());
            User reviewer = review.getReviewerId() != null ? reviewerMap.get(review.getReviewerId()) : null;
            if (reviewer != null) {
                map.put("reviewerNickname", reviewer.getNickname());
            }
            Post post = review.getPostId() != null ? postMap.get(review.getPostId()) : null;
            if (post != null) {
                map.put("postTitle", post.getTitle());
            }
            records.add(map);
        }
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 禁止封禁管理员账号，防止后台被锁死
        if (user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            throw new BusinessException("不能封禁管理员账号");
        }
        user.setStatus(0);
        userMapper.updateById(user);

        // 联动：封禁用户其名下未完结帖子一律下架（违规内容不随封禁继续展示）
        List<Post> activePosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getPublisherId, userId)
                        .in(Post::getStatus, PostStatus.RECRUITING, PostStatus.FULL, PostStatus.ONGOING)
        );
        for (Post post : activePosts) {
            adminPostService.takedownPost(post.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbanUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(1);
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetCredit(Long userId, Integer creditScore) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 信誉分台账：清空旧台账，按目标值换算成一条 ADMIN_RESET 记录，
        // 重算公式 = 100 + Σ评价 + Σ台账 = 目标值，且不会被每日重算冲掉
        creditLogMapper.delete(
                new LambdaQueryWrapper<CreditLog>().eq(CreditLog::getUserId, userId)
        );
        int change = creditScore - CreditConstant.INITIAL_SCORE - creditService.getReviewImpact(userId);
        creditService.recordPenalty(userId, change, CreditLog.REASON_ADMIN_RESET, CreditLog.BIZ_TYPE_ADMIN, userId);
    }

    /**
     * 将User实体转换为UserVO
     */
    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setStudentNo(user.getStudentNo());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setSignature(user.getSignature());
        vo.setCollege(user.getCollege());
        vo.setGender(user.getGender());
        vo.setCreditScore(user.getCreditScore());
        vo.setCreditLevel(creditService.getCreditLevel(user.getCreditScore()));
        vo.setCreditLabel(creditService.getCreditLabel(user.getCreditScore()));
        vo.setStatus(user.getStatus());
        vo.setCreatedTime(user.getCreatedTime());
        return vo;
    }

    /** 从报名记录批量收集帖子ID并查询出 Map<postId, Post>（避免 N+1） */
    private Map<Long, Post> toPostMap(List<PostApply> applies) {
        Set<Long> postIds = new HashSet<>();
        for (PostApply apply : applies) {
            if (apply.getPostId() != null) {
                postIds.add(apply.getPostId());
            }
        }
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return toMapById(postMapper.selectBatchIds(postIds), Post::getId);
    }

    private static <T> Map<Long, T> toMapById(List<T> items, Function<T, Long> idFn) {
        Map<Long, T> map = new HashMap<>();
        if (items != null) {
            for (T item : items) {
                Long key = idFn.apply(item);
                if (key != null) {
                    map.put(key, item);
                }
            }
        }
        return map;
    }
}
