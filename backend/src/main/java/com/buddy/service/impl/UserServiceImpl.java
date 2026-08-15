package com.buddy.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.CreditConstant;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.jwt.JwtUtils;
import com.buddy.common.result.ResultCode;
import com.buddy.dto.request.LoginRequest;
import com.buddy.dto.request.ProfileUpdateRequest;
import com.buddy.dto.request.RegisterRequest;
import com.buddy.dto.response.ApplyVO;
import com.buddy.dto.response.PostVO;
import com.buddy.dto.response.ReviewVO;
import com.buddy.dto.response.UserProfileVO;
import com.buddy.dto.response.UserTagVO;
import com.buddy.dto.response.UserVO;
import com.buddy.entity.InterestTag;
import com.buddy.entity.Post;
import com.buddy.entity.PostApply;
import com.buddy.entity.Review;
import com.buddy.entity.User;
import com.buddy.entity.UserTag;
import com.buddy.mapper.InterestTagMapper;
import com.buddy.mapper.PostApplyMapper;
import com.buddy.mapper.PostMapper;
import com.buddy.mapper.ReviewMapper;
import com.buddy.mapper.UserMapper;
import com.buddy.mapper.UserTagMapper;
import com.buddy.service.CreditService;
import com.buddy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserTagMapper userTagMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private InterestTagMapper interestTagMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostApplyMapper postApplyMapper;

    @Autowired
    private CreditService creditService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 登录失败计数缓存前缀 */
    private static final String LOGIN_FAIL_KEY = "login:fail:";
    /** 最大登录失败次数 */
    private static final int MAX_LOGIN_FAIL = 5;
    /** 锁定时间（分钟） */
    private static final int LOCK_MINUTES = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        boolean isPhoneRegister = "phone".equals(request.getRegisterType());

        if (isPhoneRegister) {
            // 手机号注册：手机号必填
            if (StrUtil.isBlank(request.getPhone())) {
                throw new BusinessException("手机号不能为空");
            }
            // 校验手机号是否已存在
            Long countByPhone = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
            );
            if (countByPhone > 0) {
                throw new BusinessException(ResultCode.USER_EXISTS, "该手机号已注册");
            }
            // 学号选填，如果填了也校验唯一性
            if (StrUtil.isNotBlank(request.getStudentNo())) {
                Long countByStudentNo = userMapper.selectCount(
                        new LambdaQueryWrapper<User>().eq(User::getStudentNo, request.getStudentNo())
                );
                if (countByStudentNo > 0) {
                    throw new BusinessException(ResultCode.USER_EXISTS, "该学号已注册");
                }
            }
        } else {
            // 学号注册：学号必填
            if (StrUtil.isBlank(request.getStudentNo())) {
                throw new BusinessException("学号不能为空");
            }
            // 校验学号是否已存在
            Long countByStudentNo = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getStudentNo, request.getStudentNo())
            );
            if (countByStudentNo > 0) {
                throw new BusinessException(ResultCode.USER_EXISTS, "该学号已注册");
            }
            // 手机号选填，如果填了也校验唯一性
            if (StrUtil.isNotBlank(request.getPhone())) {
                Long countByPhone = userMapper.selectCount(
                        new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
                );
                if (countByPhone > 0) {
                    throw new BusinessException(ResultCode.USER_EXISTS, "该手机号已注册");
                }
            }
        }

        // 构建用户实体
        User user = new User();
        user.setStudentNo(request.getStudentNo());
        user.setPhone(request.getPhone());
        // 默认用户名 = 学号（注册后可在个人中心修改）
        user.setUsername(request.getStudentNo());
        // BCrypt加密密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getRealName());
        user.setCreditScore(CreditConstant.INITIAL_SCORE);
        user.setStatus(1);
        user.setIsAdmin(0);
        user.setGender(0);

        userMapper.insert(user);
    }

    @Override
    public String login(LoginRequest request) {
        // 检查账号是否被锁定（Redis不可用时跳过）
        String lockKey = LOGIN_FAIL_KEY + request.getAccount();
        try {
            String failCountStr = redisTemplate.opsForValue().get(lockKey);
            if (failCountStr != null && Integer.parseInt(failCountStr) >= MAX_LOGIN_FAIL) {
                throw new BusinessException("登录失败次数过多，账号已锁定" + LOCK_MINUTES + "分钟，请稍后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis 不可用时采取 fail-closed：拒绝登录而不是跳过锁定检查
            log.error("Redis不可用，登录安全检查异常", e);
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        // 根据用户名、学号或手机号查询用户（昵称可重复，不作为登录凭证）
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getAccount())
                        .or()
                        .eq(User::getStudentNo, request.getAccount())
                        .or()
                        .eq(User::getPhone, request.getAccount())
        );
        if (user == null) {
            incrementLoginFail(lockKey);
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 校验用户是否被封禁
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被封禁");
        }

        // BCrypt校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            incrementLoginFail(lockKey);
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 登录成功，清除失败计数
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            // Redis不可用，跳过
        }

        // 生成JWT Token
        return jwtUtils.generateToken(user.getId(), user.getStudentNo());
    }

    /**
     * 增加登录失败计数（Redis不可用时静默跳过）
     */
    private void incrementLoginFail(String lockKey) {
        try {
            Long count = redisTemplate.opsForValue().increment(lockKey);
            if (count != null && count == 1) {
                redisTemplate.expire(lockKey, LOCK_MINUTES, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            // 计数失败影响较小（锁定检查已在 login() 中 fail-closed），仅记录日志
            log.error("Redis不可用，登录失败计数异常", e);
        }
    }

    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 更新非空字段
        if (StrUtil.isNotBlank(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (StrUtil.isNotBlank(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getSignature() != null) {
            user.setSignature(request.getSignature());
        }
        if (StrUtil.isNotBlank(request.getCollege())) {
            user.setCollege(request.getCollege());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        userMapper.updateById(user);
    }

    @Override
    public List<UserTagVO> getUserTags(Long userId) {
        // 查询用户的标签关联
        List<UserTag> userTags = userTagMapper.selectList(
                new LambdaQueryWrapper<UserTag>().eq(UserTag::getUserId, userId)
        );
        if (userTags.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建 tagId -> selfLevel 映射
        Map<Long, Integer> levelMap = userTags.stream()
                .collect(Collectors.toMap(UserTag::getTagId, UserTag::getSelfLevel, (a, b) -> a));

        // 查询标签详情，组装为 UserTagVO
        List<InterestTag> tags = interestTagMapper.selectBatchIds(levelMap.keySet());
        return tags.stream().map(tag -> {
            UserTagVO vo = new UserTagVO();
            vo.setId(tag.getId());
            vo.setCategory(tag.getCategory());
            vo.setName(tag.getName());
            vo.setDescription(tag.getDescription());
            vo.setSortOrder(tag.getSortOrder());
            vo.setSelfLevel(levelMap.getOrDefault(tag.getId(), 1));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserTags(Long userId, List<Long> tagIds, List<Integer> levels) {
        // 先删除旧的关联
        userTagMapper.delete(
                new LambdaQueryWrapper<UserTag>().eq(UserTag::getUserId, userId)
        );

        // 批量插入新关联
        for (int i = 0; i < tagIds.size(); i++) {
            UserTag userTag = new UserTag();
            userTag.setUserId(userId);
            userTag.setTagId(tagIds.get(i));
            userTag.setSelfLevel(levels.get(i));
            userTagMapper.insert(userTag);
        }
    }

    @Override
    public IPage<PostVO> getMyPosts(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getPublisherId, userId)
                .orderByDesc(Post::getCreatedTime);

        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Post::getStatus, status);
        }

        IPage<Post> postPage = postMapper.selectPage(new Page<>(page, size), wrapper);

        // 转换为PostVO
        IPage<PostVO> voPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        voPage.setRecords(postPage.getRecords().stream()
                .map(this::convertToPostVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public IPage<ApplyVO> getMyApplies(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<PostApply> wrapper = new LambdaQueryWrapper<PostApply>()
                .eq(PostApply::getApplicantId, userId)
                .orderByDesc(PostApply::getApplyTime);

        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(PostApply::getStatus, status);
        }

        IPage<PostApply> applyPage = postApplyMapper.selectPage(new Page<>(page, size), wrapper);

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

            // 查询帖子标题
            Post post = postMapper.selectById(apply.getPostId());
            if (post != null) {
                vo.setPostTitle(post.getTitle());
            }

            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }

    /**
     * 将User实体转换为UserVO
     */
    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setStudentNo(user.getStudentNo());
        vo.setPhone(user.getPhone());
        vo.setUsername(user.getUsername());
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

    /**
     * 将Post实体转换为PostVO（不含发布者信息）
     */
    private PostVO convertToPostVO(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setCategory(post.getCategory());
        vo.setTagIds(post.getTagIds());
        vo.setStartTime(post.getStartTime());
        vo.setEndTime(post.getEndTime());
        vo.setLocation(post.getLocation());
        vo.setMaxPeople(post.getMaxPeople());
        vo.setCurrentPeople(post.getCurrentPeople());
        vo.setStatus(post.getStatus());
        vo.setViewCount(post.getViewCount());
        vo.setPublisherId(post.getPublisherId());
        vo.setCreatedTime(post.getCreatedTime());
        vo.setIsAuditPass(post.getIsAuditPass());

        // 查询发布者信息
        User publisher = userMapper.selectById(post.getPublisherId());
        if (publisher != null) {
            vo.setPublisherNickname(publisher.getNickname());
            vo.setPublisherAvatar(publisher.getAvatar());
            vo.setPublisherCreditLevel(creditService.getCreditLevel(publisher.getCreditScore()));
        }
        return vo;
    }

    @Override
    public UserProfileVO getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        UserProfileVO vo = new UserProfileVO();
        // 基本信息（不含手机号/学号，敏感信息脱敏 PRO-02）
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setSignature(user.getSignature());
        vo.setCollege(user.getCollege());
        vo.setGender(user.getGender());
        vo.setCreditScore(user.getCreditScore());
        vo.setCreditLevel(creditService.getCreditLevel(user.getCreditScore()));
        vo.setCreditLabel(creditService.getCreditLabel(user.getCreditScore()));
        vo.setIsLowCredit(creditService.isLowCredit(user.getCreditScore()));
        vo.setProfilePrivacy(user.getProfilePrivacy() == null ? 0 : user.getProfilePrivacy());

        // 兴趣标签（含段位）
        vo.setTags(getUserTags(userId));

        // 评价标签云统计
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getTargetUserId, userId)
        );
        Map<String, Long> tagCounts = reviews.stream()
                .filter(r -> r.getTag() != null && !r.getTag().isEmpty())
                .map(r -> r.getTag().split(",")[0].trim())
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        List<UserProfileVO.TagStat> tagStats = tagCounts.entrySet().stream()
                .map(e -> {
                    UserProfileVO.TagStat stat = new UserProfileVO.TagStat();
                    stat.setTag(e.getKey());
                    stat.setCount(e.getValue());
                    return stat;
                })
                .collect(Collectors.toList());
        vo.setReviewTagStats(tagStats);

        // 历史组局（发布 + 参加，最多20条；隐私隐藏时不返回 PRO-03）
        if (vo.getProfilePrivacy() != 1) {
            List<PostVO> historyPosts = new ArrayList<>();
            List<Post> published = postMapper.selectList(
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getPublisherId, userId)
                            .orderByDesc(Post::getCreatedTime)
                            .last("LIMIT 20")
            );
            historyPosts.addAll(published.stream().map(this::convertToPostVO).collect(Collectors.toList()));
            vo.setHistoryPosts(historyPosts);
        }

        // 收到的评价（最多10条）
        List<ReviewVO> reviewVOs = reviews.stream()
                .sorted((a, b) -> b.getCreatedTime().compareTo(a.getCreatedTime()))
                .limit(10)
                .map(this::convertToReviewVO)
                .collect(Collectors.toList());
        vo.setRecentReviews(reviewVOs);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setProfilePrivacy(Long userId, Integer privacy) {
        if (privacy == null || (privacy != 0 && privacy != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "隐私设置不合法");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setProfilePrivacy(privacy);
        userMapper.updateById(user);
    }

    /**
     * 评价实体转VO
     */
    private ReviewVO convertToReviewVO(Review review) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setPostId(review.getPostId());
        vo.setReviewerId(review.getReviewerId());
        vo.setTargetUserId(review.getTargetUserId());
        vo.setRating(review.getRating());
        vo.setTag(review.getTag());
        vo.setContent(review.getContent());
        vo.setCreatedTime(review.getCreatedTime());

        // 评价人昵称
        User reviewer = userMapper.selectById(review.getReviewerId());
        if (reviewer != null) {
            vo.setReviewerNickname(reviewer.getNickname());
        }
        // 帖子标题
        Post post = postMapper.selectById(review.getPostId());
        if (post != null) {
            vo.setPostTitle(post.getTitle());
        }
        return vo;
    }
}
