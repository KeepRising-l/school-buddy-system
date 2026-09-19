package com.buddy.module.square.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.constant.CreditConstant;
import com.buddy.common.constant.PostStatus;
import com.buddy.module.credit.service.CreditService;
import com.buddy.module.post.converter.PostConverter;
import com.buddy.module.post.dto.PostVO;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.square.service.RecommendService;
import com.buddy.module.tag.entity.UserTag;
import com.buddy.module.tag.mapper.UserTagMapper;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 推荐服务实现类（等价迁移自旧版）
 * 核心推荐算法：Score = 0.4×标签匹配度 + 0.3×时间重合度 + 0.2×信誉分/100 + 0.1×热度因子
 * 游客/无标签用户：Score = 0.5×时间重合度 + 0.3×热度因子 + 0.2×标签匹配度
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserTagMapper userTagMapper;

    @Autowired
    private CreditService creditService;

    @Autowired
    private PostConverter postConverter;

    /** 时间重合度计算的最大小时数（7天=168小时） */
    private static final double MAX_HOURS_FOR_TIME = 168.0;

    @Override
    public IPage<PostVO> getRecommendList(Long userId, int page, int size, String category, String sort, String timeRange, String keyword) {
        // 如果sort为latest或hot，走普通排序逻辑
        // 兼容前端 newest/hottest 与后端 latest/hot 两种取值（前端传 newest/hottest）
        if ("latest".equals(sort) || "newest".equals(sort)
                || "hot".equals(sort) || "hottest".equals(sort)) {
            return getSortedList(page, size, category, sort, timeRange, keyword);
        }

        // 查询RECRUITING和FULL状态且已审核通过的帖子
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .in(Post::getStatus, PostStatus.RECRUITING, PostStatus.FULL)
                .eq(Post::getIsAuditPass, 1);

        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(Post::getCategory, category);
        }

        // 关键词搜索
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Post::getTitle, keyword)
                    .or().like(Post::getLocation, keyword)
                    .or().like(Post::getDescription, keyword));
        }

        // 时间筛选
        applyTimeRangeFilter(wrapper, timeRange);

        List<Post> allPosts = postMapper.selectList(wrapper);
        if (allPosts.isEmpty()) {
            return new Page<>(page, size);
        }

        // 获取用户兴趣标签集合
        Set<Long> userTagIds = new HashSet<>();
        if (userId != null) {
            List<UserTag> userTags = userTagMapper.selectList(
                    new LambdaQueryWrapper<UserTag>().eq(UserTag::getUserId, userId)
            );
            userTagIds = userTags.stream()
                    .map(UserTag::getTagId)
                    .collect(Collectors.toSet());
        }

        // 对每条帖子计算权重
        Set<Long> finalUserTagIds = userTagIds;
        List<ScoredPost> scoredPosts = new ArrayList<>();
        for (Post post : allPosts) {
            double score = calculateScore(post, userId, finalUserTagIds);
            scoredPosts.add(new ScoredPost(post, score));
        }

        // 按权重降序排序
        scoredPosts.sort((a, b) -> Double.compare(b.score, a.score));

        // 手动分页
        int total = scoredPosts.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<PostVO> voList;
        if (fromIndex >= total) {
            voList = new ArrayList<>();
        } else {
            voList = scoredPosts.subList(fromIndex, toIndex).stream()
                    .map(sp -> postConverter.toVO(sp.post))
                    .collect(Collectors.toList());
        }

        Page<PostVO> result = new Page<>(page, size, total);
        result.setRecords(voList);
        return result;
    }

    /**
     * 计算帖子推荐权重
     * Score = 0.4×标签匹配度 + 0.3×时间重合度 + 0.2×信誉分/100 + 0.1×热度因子
     */
    private double calculateScore(Post post, Long userId, Set<Long> userTagIds) {
        // 标签匹配度：用户标签集合与帖子tagIds的交集数量 / 帖子标签总数
        double tagMatchScore = calculateTagMatchScore(post, userTagIds);

        // 时间重合度：1 - (距活动开始的小时数 / 168)，超过7天为0
        double timeScore = calculateTimeScore(post);

        // 热度因子：currentPeople / maxPeople
        double hotFactor = post.getMaxPeople() != null && post.getMaxPeople() > 0
                ? (double) post.getCurrentPeople() / post.getMaxPeople() : 0;

        double score;
        if (userId != null && !userTagIds.isEmpty()) {
            // 已登录且有兴趣标签的用户：使用完整公式（含信誉分因子）
            User publisher = userMapper.selectById(post.getPublisherId());
            double creditFactor = publisher != null ? publisher.getCreditScore() / 100.0 : 0.5;
            score = 0.4 * tagMatchScore + 0.3 * timeScore + 0.2 * creditFactor + 0.1 * hotFactor;

            // 如果发布者信誉分<60，权重×0.3（沉底）
            if (publisher != null && creditService.isLowCredit(publisher.getCreditScore())) {
                score *= CreditConstant.PUNISH_FACTOR;
            }
        } else {
            // 游客/无标签用户：不含信誉分因子，仅按时间重合度+热度推荐
            score = 0.5 * timeScore + 0.3 * hotFactor + 0.2 * tagMatchScore;
        }

        return score;
    }

    /**
     * 计算标签匹配度
     */
    private double calculateTagMatchScore(Post post, Set<Long> userTagIds) {
        if (userTagIds.isEmpty() || StrUtil.isBlank(post.getTagIds())) {
            return 0;
        }

        // 解析帖子标签ID（脏数据如 "abc"、"1,2,x" 逐项容错跳过，一条坏帖子不能让整个广场 500）
        Set<Long> postTagIdSet = new HashSet<>();
        for (String idStr : post.getTagIds().split(",")) {
            String trimmed = idStr.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                postTagIdSet.add(Long.parseLong(trimmed));
            } catch (NumberFormatException ignored) {
                // 跳过非法标签ID
            }
        }

        if (postTagIdSet.isEmpty()) {
            return 0;
        }

        // 计算交集数量
        Set<Long> intersection = new HashSet<>(userTagIds);
        intersection.retainAll(postTagIdSet);

        return (double) intersection.size() / postTagIdSet.size();
    }

    /**
     * 计算时间重合度：1 - (距活动开始的小时数 / 168)，超过7天为0
     */
    private double calculateTimeScore(Post post) {
        if (post.getStartTime() == null) {
            return 0;
        }

        long hoursUntilStart = ChronoUnit.HOURS.between(LocalDateTime.now(), post.getStartTime());
        if (hoursUntilStart < 0) {
            // 已开始的活动，给一个较低的固定分
            return 0.1;
        }
        if (hoursUntilStart >= MAX_HOURS_FOR_TIME) {
            return 0;
        }

        return 1.0 - (hoursUntilStart / MAX_HOURS_FOR_TIME);
    }

    /**
     * 按时间或热度排序获取帖子列表
     */
    private IPage<PostVO> getSortedList(int page, int size, String category, String sort, String timeRange, String keyword) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .in(Post::getStatus, PostStatus.RECRUITING, PostStatus.FULL)
                // 只展示已审核通过的帖子
                .eq(Post::getIsAuditPass, 1);

        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(Post::getCategory, category);
        }

        // 关键词搜索
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Post::getTitle, keyword)
                    .or().like(Post::getLocation, keyword)
                    .or().like(Post::getDescription, keyword));
        }

        // 时间筛选
        applyTimeRangeFilter(wrapper, timeRange);

        // 热度 = 报名人数；兼容前端 hottest 与后端 hot 两种取值
        if ("hot".equals(sort) || "hottest".equals(sort)) {
            wrapper.orderByDesc(Post::getCurrentPeople);
        } else {
            wrapper.orderByDesc(Post::getCreatedTime);
        }

        IPage<Post> postPage = postMapper.selectPage(new Page<>(page, size), wrapper);

        IPage<PostVO> voPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        voPage.setRecords(postConverter.toVOList(postPage.getRecords()));
        return voPage;
    }

    /**
     * 应用时间范围筛选条件
     * @param timeRange today/week/month，为空则不限
     * 使用闭区间（原版只加下界，"今天"会把未来无限远的帖子也算进来）
     */
    private void applyTimeRangeFilter(LambdaQueryWrapper<Post> wrapper, String timeRange) {
        if (StrUtil.isBlank(timeRange)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime;
        LocalDateTime endTime;
        switch (timeRange) {
            case "today":
                // 今天：当天 0 点 ~ 明天 0 点开始的活动
                startTime = now.toLocalDate().atStartOfDay();
                endTime = startTime.plusDays(1);
                break;
            case "week":
                // 本周：未来 7 天内开始的活动
                startTime = now;
                endTime = now.plusDays(7);
                break;
            case "month":
                // 本月：未来 30 天内开始的活动
                startTime = now;
                endTime = now.plusDays(30);
                break;
            default:
                return;
        }
        wrapper.ge(Post::getStartTime, startTime)
                .lt(Post::getStartTime, endTime);
    }

    /**
     * 带分数的帖子内部类
     */
    private static class ScoredPost {
        Post post;
        double score;

        ScoredPost(Post post, double score) {
            this.post = post;
            this.score = score;
        }
    }
}
