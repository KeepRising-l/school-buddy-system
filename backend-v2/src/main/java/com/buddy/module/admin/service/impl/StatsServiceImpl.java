package com.buddy.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.PostStatus;
import com.buddy.module.admin.dto.StatsVO;
import com.buddy.module.admin.service.StatsService;
import com.buddy.module.apply.entity.PostApply;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统计服务实现类（等价迁移自旧版）
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

        // 活跃用户数（近7天有发帖或报名的用户）
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

    @Override
    public List<Map<String, Object>> getRetention(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 遍历每一天作为"注册基准日"，计算该日新增用户的后续留存
        // 注意：距离今天不足 N 天的日期不计算 N 日留存（数据不完整）
        for (int i = days - 1; i >= 0; i--) {
            LocalDate cohortDate = today.minusDays(i);
            LocalDateTime cohortStart = LocalDateTime.of(cohortDate, LocalTime.MIN);
            LocalDateTime cohortEnd = LocalDateTime.of(cohortDate, LocalTime.MAX);

            // 当日新增用户数
            Long newUsers = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .ge(User::getCreatedTime, cohortStart)
                            .le(User::getCreatedTime, cohortEnd)
            );

            Map<String, Object> item = new HashMap<>();
            item.put("date", cohortDate.toString());
            item.put("newUsers", newUsers);

            if (newUsers == 0) {
                item.put("d1Rate", 0.0);
                item.put("d7Rate", 0.0);
                item.put("d30Rate", 0.0);
                result.add(item);
                continue;
            }

            // 取当日新增用户的 ID 列表
            List<User> newUserList = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .select(User::getId)
                            .ge(User::getCreatedTime, cohortStart)
                            .le(User::getCreatedTime, cohortEnd)
            );
            List<Long> newUserIds = newUserList.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            // 计算 D+1 / D+7 / D+30 留存：对应日期有发帖或报名行为
            double d1Rate = i >= 1 ? calculateRetentionRate(newUserIds, cohortDate.plusDays(1)) : 0.0;
            double d7Rate = i >= 7 ? calculateRetentionRate(newUserIds, cohortDate.plusDays(7)) : 0.0;
            double d30Rate = i >= 30 ? calculateRetentionRate(newUserIds, cohortDate.plusDays(30)) : 0.0;

            item.put("d1Rate", Math.round(d1Rate * 10) / 10.0);
            item.put("d7Rate", Math.round(d7Rate * 10) / 10.0);
            item.put("d30Rate", Math.round(d30Rate * 10) / 10.0);

            result.add(item);
        }

        return result;
    }

    /**
     * 计算一批用户在指定日期的留存率（活跃 = 当天发帖 or 报名）
     */
    private double calculateRetentionRate(List<Long> userIds, LocalDate activeDate) {
        if (userIds == null || userIds.isEmpty()) return 0.0;

        LocalDateTime dayStart = LocalDateTime.of(activeDate, LocalTime.MIN);
        LocalDateTime dayEnd = LocalDateTime.of(activeDate, LocalTime.MAX);

        List<Post> activePosts = postMapper.selectList(
                new QueryWrapper<Post>()
                        .select("DISTINCT publisher_id")
                        .in("publisher_id", userIds)
                        .ge("created_time", dayStart)
                        .le("created_time", dayEnd)
        );
        List<PostApply> activeApplies = postApplyMapper.selectList(
                new QueryWrapper<PostApply>()
                        .select("DISTINCT applicant_id")
                        .in("applicant_id", userIds)
                        .ge("apply_time", dayStart)
                        .le("apply_time", dayEnd)
        );

        Set<Long> activeUsers = new HashSet<>();
        for (Post p : activePosts) activeUsers.add(p.getPublisherId());
        for (PostApply a : activeApplies) activeUsers.add(a.getApplicantId());

        return (double) activeUsers.size() / userIds.size() * 100;
    }

    @Override
    public List<Map<String, Object>> getFunnel(int days) {
        LocalDateTime startTime = days > 0
                ? LocalDateTime.now().minusDays(days)
                : LocalDateTime.of(2000, 1, 1, 0, 0);

        List<Map<String, Object>> result = new ArrayList<>();

        // 阶段 1：浏览量（帖子 view_count 总和）
        Long viewCount = 0L;
        List<Post> posts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .ge(Post::getCreatedTime, startTime)
                        .select(Post::getViewCount)
        );
        for (Post p : posts) {
            viewCount += p.getViewCount() != null ? p.getViewCount() : 0;
        }

        // 阶段 2：报名次数
        Long applyCount = postApplyMapper.selectCount(
                new LambdaQueryWrapper<PostApply>()
                        .ge(PostApply::getApplyTime, startTime)
        );

        // 阶段 3：报名通过数
        Long approvedCount = postApplyMapper.selectCount(
                new LambdaQueryWrapper<PostApply>()
                        .ge(PostApply::getApplyTime, startTime)
                        .eq(PostApply::getStatus, ApplyStatus.APPROVED)
        );

        // 阶段 4：实际出勤数
        Long attendedCount = postApplyMapper.selectCount(
                new LambdaQueryWrapper<PostApply>()
                        .ge(PostApply::getApplyTime, startTime)
                        .eq(PostApply::getStatus, ApplyStatus.ATTENDED)
        );

        long base = viewCount > 0 ? viewCount : 1;

        result.add(buildFunnelItem("浏览", viewCount, 100.0));
        result.add(buildFunnelItem("报名", applyCount,
                Math.round((double) applyCount / base * 1000) / 10.0));
        result.add(buildFunnelItem("通过", approvedCount,
                Math.round((double) approvedCount / base * 1000) / 10.0));
        result.add(buildFunnelItem("出勤", attendedCount,
                Math.round((double) attendedCount / base * 1000) / 10.0));

        return result;
    }

    /**
     * 构建漏斗数据项
     */
    private Map<String, Object> buildFunnelItem(String stage, Long count, Double rate) {
        Map<String, Object> item = new HashMap<>();
        item.put("stage", stage);
        item.put("count", count);
        item.put("rate", rate);
        return item;
    }

    // ===== CSV 导出 =====

    @Override
    public void exportUsers(Writer writer) throws IOException {
        // UTF-8 BOM，确保 Windows Excel 正确识别中文编码
        writer.write("﻿");
        writeCsvLine(writer, "ID", "学号", "昵称", "手机号", "学院", "信誉分", "状态", "注册时间");
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedTime)
        );
        for (User u : users) {
            writeCsvLine(writer, u.getId(), u.getStudentNo(), u.getNickname(),
                    u.getPhone(), u.getCollege(), u.getCreditScore(),
                    u.getStatus() == 1 ? "正常" : "封禁", u.getCreatedTime());
        }
    }

    @Override
    public void exportPosts(Writer writer) throws IOException {
        writer.write("﻿");
        writeCsvLine(writer, "ID", "标题", "分类", "发布者ID", "状态", "浏览量", "人数", "创建时间");
        List<Post> posts = postMapper.selectList(
                new LambdaQueryWrapper<Post>().orderByDesc(Post::getCreatedTime)
        );
        for (Post p : posts) {
            writeCsvLine(writer, p.getId(), p.getTitle(), p.getCategory(),
                    p.getPublisherId(), p.getStatus(), p.getViewCount(),
                    p.getCurrentPeople() + "/" + p.getMaxPeople(), p.getCreatedTime());
        }
    }

    @Override
    public void exportStats(int days, String type, Writer writer) throws IOException {
        writer.write("﻿");
        switch (type) {
            case "retention":
                writeCsvLine(writer, "日期", "新增用户", "次日留存%", "7日留存%", "30日留存%");
                List<Map<String, Object>> retention = getRetention(days);
                for (Map<String, Object> item : retention) {
                    writeCsvLine(writer, item.get("date"), item.get("newUsers"),
                            item.get("d1Rate"), item.get("d7Rate"), item.get("d30Rate"));
                }
                break;
            case "funnel":
                writeCsvLine(writer, "阶段", "数量", "转化率%");
                List<Map<String, Object>> funnel = getFunnel(days);
                for (Map<String, Object> item : funnel) {
                    writeCsvLine(writer, item.get("stage"), item.get("count"), item.get("rate"));
                }
                break;
            default: // "trend"
                writeCsvLine(writer, "日期", "发帖量", "成功组局", "活跃用户", "累计用户");
                List<Map<String, Object>> postTrend = getPostTrend(days);
                List<Map<String, Object>> successTrend = getSuccessMatchTrend(days);
                List<Map<String, Object>> activeTrend = getActiveUserTrend(days);
                List<Map<String, Object>> userGrowth = getUserGrowthTrend(days);
                for (int i = 0; i < postTrend.size(); i++) {
                    writeCsvLine(writer, postTrend.get(i).get("date"),
                            postTrend.get(i).get("count"),
                            successTrend.get(i).get("count"),
                            activeTrend.get(i).get("count"),
                            userGrowth.get(i).get("count"));
                }
                break;
        }
    }

    /**
     * 写一行CSV（处理逗号、引号、换行转义）
     */
    private void writeCsvLine(Writer writer, Object... fields) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            Object val = fields[i];
            if (val != null) {
                String str = val.toString();
                if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
                    sb.append('"').append(str.replace("\"", "\"\"")).append('"');
                } else {
                    sb.append(str);
                }
            }
        }
        sb.append('\n');
        writer.write(sb.toString());
    }
}
