package com.buddy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.PostStatus;
import com.buddy.dto.response.StatsVO;
import com.buddy.entity.Post;
import com.buddy.entity.PostApply;
import com.buddy.entity.User;
import com.buddy.mapper.PostApplyMapper;
import com.buddy.mapper.PostMapper;
import com.buddy.mapper.UserMapper;
import com.buddy.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务实现类
 */
@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostApplyMapper postApplyMapper;

    @Override
    public StatsVO getOverview() {
        StatsVO vo = new StatsVO();

        // 总用户数
        vo.setTotalUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>()
        ));

        // 总帖子数
        vo.setTotalPosts(postMapper.selectCount(
                new LambdaQueryWrapper<Post>()
        ));

        // 今日发帖数
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        vo.setTodayPosts(postMapper.selectCount(
                new LambdaQueryWrapper<Post>().ge(Post::getCreatedTime, todayStart)
        ));

        // 成功组局数（状态为FINISHED的帖子数）
        vo.setSuccessMatches(postMapper.selectCount(
                new LambdaQueryWrapper<Post>().eq(Post::getStatus, PostStatus.FINISHED)
        ));

        // 活跃用户数（近7天有登录/发帖/报名的用户，此处简化为近7天有发帖或报名的用户）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Long activePostUsers = postMapper.selectCount(
                new QueryWrapper<Post>()
                        .ge("created_time", sevenDaysAgo)
                        .select("DISTINCT publisher_id")
        );
        Long activeApplyUsers = postApplyMapper.selectCount(
                new QueryWrapper<PostApply>()
                        .ge("apply_time", sevenDaysAgo)
                        .select("DISTINCT applicant_id")
        );
        vo.setActiveUsers(activePostUsers + activeApplyUsers);

        // 分类统计TOP5
        vo.setCategoryStats(getCategoryTop5());

        return vo;
    }

    @Override
    public List<Map<String, Object>> getPostTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            Long count = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>()
                            .ge(Post::getCreatedTime, dayStart)
                            .le(Post::getCreatedTime, dayEnd)
            );

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", count);
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getSuccessMatchTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            Long count = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getStatus, PostStatus.FINISHED)
                            .ge(Post::getUpdatedTime, dayStart)
                            .le(Post::getUpdatedTime, dayEnd)
            );

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", count);
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getActiveUserTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            // 当天发帖的用户数
            Long postUserCount = postMapper.selectCount(
                    new QueryWrapper<Post>()
                            .ge("created_time", dayStart)
                            .le("created_time", dayEnd)
                            .select("DISTINCT publisher_id")
            );

            // 当天报名的用户数
            Long applyUserCount = postApplyMapper.selectCount(
                    new QueryWrapper<PostApply>()
                            .ge("apply_time", dayStart)
                            .le("apply_time", dayEnd)
                            .select("DISTINCT applicant_id")
            );

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", postUserCount + applyUserCount);
            result.add(item);
        }

        return result;
    }

    @Override
    public List<StatsVO.CategoryStat> getCategoryTop5() {
        // 使用MyBatis-Plus的groupBy查询分类统计
        List<Map<String, Object>> maps = postMapper.selectMaps(
                new QueryWrapper<Post>()
                        .select("category, COUNT(*) as count")
                        .groupBy("category")
                        .orderByDesc("count")
                        .last("LIMIT 5")
        );

        List<StatsVO.CategoryStat> stats = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            StatsVO.CategoryStat stat = new StatsVO.CategoryStat();
            stat.setCategory((String) map.get("category"));
            stat.setCount(((Number) map.get("count")).longValue());
            stats.add(stat);
        }
        return stats;
    }

    @Override
    public List<Map<String, Object>> getCreditDistribution() {
        // 按信誉分区间统计用户分布
        List<Map<String, Object>> result = new ArrayList<>();

        // 90-100 靠谱队友
        addCreditRange(result, "靠谱队友(90-100)", 90, 100);
        // 75-89 优质队友
        addCreditRange(result, "优质队友(75-89)", 75, 89);
        // 60-74 普通队友
        addCreditRange(result, "普通队友(60-74)", 60, 74);
        // 40-59 风险队友
        addCreditRange(result, "风险队友(40-59)", 40, 59);
        // 0-39 失信队友
        addCreditRange(result, "失信队友(0-39)", 0, 39);

        return result;
    }

    /**
     * 添加信誉分区间统计
     */
    private void addCreditRange(List<Map<String, Object>> result, String label, int min, int max) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .ge(User::getCreditScore, min)
                        .le(User::getCreditScore, max)
        );
        Map<String, Object> item = new HashMap<>();
        item.put("label", label);
        item.put("count", count);
        result.add(item);
    }

    @Override
    public List<Map<String, Object>> getUserGrowthTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            // 累计注册用户数：注册时间 <= 当天结束
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .le(User::getCreatedTime, dayEnd)
            );

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", count);
            result.add(item);
        }

        return result;
    }
}
