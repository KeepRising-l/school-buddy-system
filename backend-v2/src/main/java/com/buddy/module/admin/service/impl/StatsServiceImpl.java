package com.buddy.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.PostStatus;
import com.buddy.module.admin.dto.ReportStatsVO;
import com.buddy.module.admin.dto.StatsVO;
import com.buddy.module.admin.service.StatsService;
import com.buddy.module.apply.entity.PostApply;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.report.entity.Report;
import com.buddy.module.report.mapper.ReportMapper;
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

    @Autowired
    private ReportMapper reportMapper;

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

        // 活跃用户数（近7天有发帖或报名的用户，并集去重）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        vo.setActiveUsers(countActiveUsers(
                postMapper.selectObjs(new QueryWrapper<Post>()
                        .select("DISTINCT publisher_id")
                        .ge("created_time", sevenDaysAgo)),
                postApplyMapper.selectObjs(new QueryWrapper<PostApply>()
                        .select("DISTINCT applicant_id")
                        .ge("apply_time", sevenDaysAgo))
        ));

        // 分类统计TOP5
        vo.setCategoryStats(getCategoryTop5());

        return vo;
    }

    /** 趋势类查询天数上限（防止 days 过大导致循环查询拖慢接口） */
    private static final int MAX_TREND_DAYS = 90;

    /** 趋势类查询天数下限 */
    private static final int MIN_TREND_DAYS = 1;

    /**
     * 限制查询天数到 [1, 90]，避免非法或超大 days 造成大量循环查询
     */
    private static int clampDays(int days) {
        return Math.max(MIN_TREND_DAYS, Math.min(days, MAX_TREND_DAYS));
    }

    /**
     * 统计活跃用户并集大小（发帖用户 + 报名用户，按用户ID去重，避免直接相加重复计数）
     */
    private static long countActiveUsers(List<Object> postUserIds, List<Object> applyUserIds) {
        Set<Long> userIds = new HashSet<>();
        addUserIds(userIds, postUserIds);
        addUserIds(userIds, applyUserIds);
        return userIds.size();
    }

    private static void addUserIds(Set<Long> target, List<Object> ids) {
        if (ids == null) {
            return;
        }
        for (Object id : ids) {
            if (id instanceof Number) {
                target.add(((Number) id).longValue());
            }
        }
    }

    @Override
    public List<Map<String, Object>> getPostTrend(int days) {
        days = clampDays(days);
        LocalDate today = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(today.minusDays(days - 1L), LocalTime.MIN);
        Map<String, Long> countByDate = toDateCountMap(postMapper.countDailyPostsSince(start));

        List<Map<String, Object>> result = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("count", countByDate.getOrDefault(date, 0L));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getSuccessMatchTrend(int days) {
        days = clampDays(days);
        LocalDate today = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(today.minusDays(days - 1L), LocalTime.MIN);
        Map<String, Long> countByDate = toDateCountMap(postMapper.countDailyFinishedSince(start));

        List<Map<String, Object>> result = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("count", countByDate.getOrDefault(date, 0L));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getActiveUserTrend(int days) {
        days = clampDays(days);
        LocalDate today = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(today.minusDays(days - 1L), LocalTime.MIN);

        // 仅用 2 条聚合查询取代 2*days 条循环查询，内存按日期求发帖+报名用户并集
        Map<String, Set<Long>> dailyActiveMap = new HashMap<>();
        accumulateDailyUsers(dailyActiveMap, postMapper.selectDailyPublishersSince(start));
        accumulateDailyUsers(dailyActiveMap, postApplyMapper.selectDailyApplicantsSince(start));

        List<Map<String, Object>> result = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            Set<Long> users = dailyActiveMap.get(date);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("count", users == null ? 0L : (long) users.size());
            result.add(item);
        }
        return result;
    }

    private static Map<String, Long> toDateCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                if (row == null) continue;
                Object d = row.get("d");
                Object c = row.get("c");
                if (d != null) {
                    map.put(String.valueOf(d), c instanceof Number ? ((Number) c).longValue() : 0L);
                }
            }
        }
        return map;
    }

    private static void accumulateDailyUsers(Map<String, Set<Long>> target, List<Map<String, Object>> rows) {
        if (rows == null) return;
        for (Map<String, Object> row : rows) {
            if (row == null) continue;
            Object d = row.get("d");
            Object uid = row.get("uid");
            if (d != null && uid instanceof Number) {
                target.computeIfAbsent(String.valueOf(d), k -> new HashSet<>())
                        .add(((Number) uid).longValue());
            }
        }
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
        days = clampDays(days);
        LocalDate today = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(today.minusDays(days - 1L), LocalTime.MIN);

        // 窗口起点之前的历史累计用户数（1次查询）+ 窗口内每日新增聚合（1次查询），消除 N 次循环 COUNT
        Long beforeCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().lt(User::getCreatedTime, start)
        );
        long runningTotal = beforeCount != null ? beforeCount : 0L;
        Map<String, Long> dailyNew = toDateCountMap(userMapper.countDailyNewUsersSince(start));

        List<Map<String, Object>> result = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            runningTotal += dailyNew.getOrDefault(date, 0L);

            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("count", runningTotal);
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
        for (int i = clampDays(days) - 1; i >= 0; i--) {
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

    // ===== 举报处理统计（缺口#4）=====

    /**
     * 举报处理统计：总量/已处理/有效/待处理 + 处理率 + 平均处理时长 + 近 N 天趋势
     *
     * <p>口径说明：原"审核时效统计"随人工审核取消而失效，改为"举报处理时效"，
     * 用于回答"举报有没有人管、处理快不快"。</p>
     */
    @Override
    public ReportStatsVO getReportStats(int days) {
        days = clampDays(days);

        ReportStatsVO vo = new ReportStatsVO();
        long total = reportMapper.selectCount(new LambdaQueryWrapper<Report>());
        long handled = reportMapper.selectCount(
                new LambdaQueryWrapper<Report>().ne(Report::getStatus, Report.STATUS_PENDING));
        long valid = reportMapper.selectCount(
                new LambdaQueryWrapper<Report>().eq(Report::getStatus, Report.STATUS_VALID));
        long pending = reportMapper.selectCount(
                new LambdaQueryWrapper<Report>().eq(Report::getStatus, Report.STATUS_PENDING));

        vo.setTotalReports(total);
        vo.setHandledReports(handled);
        vo.setValidReports(valid);
        vo.setPendingReports(pending);
        // 处理率：总数为 0 时记为 0（避免除零）
        vo.setHandledRate(total == 0 ? 0.0 : Math.round(handled * 1000.0 / total) / 10.0);

        // 平均处理时长（小时，保留 1 位）：无已处理数据时返回 null，前端显示"—"
        Double avgSeconds = reportMapper.avgHandleSeconds();
        vo.setAvgHandleHours(avgSeconds == null ? null : Math.round(avgSeconds / 360.0) / 10.0);

        // 近 N 天每日举报量：补齐没有举报的日期为 0，保证折线图连续
        LocalDate today = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(today.minusDays(days - 1L), LocalTime.MIN);
        Map<String, Long> countByDate = new HashMap<>();
        for (Map<String, Object> row : reportMapper.countDailySince(start)) {
            Object dateValue = row.get("d");
            Object countValue = row.get("c");
            if (dateValue != null) {
                countByDate.put(String.valueOf(dateValue),
                        countValue instanceof Number ? ((Number) countValue).longValue() : 0L);
            }
        }
        List<Map<String, Object>> trend = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("count", countByDate.getOrDefault(date, 0L));
            trend.add(item);
        }
        vo.setTrend(trend);

        return vo;
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
                // 防 CSV 公式注入：以 = + - @ 或制表符/回车开头的文本前置单引号，
                // 避免被 Excel/WPS 当作公式执行（数字类型不受影响）
                if (!(val instanceof Number) && !str.isEmpty() && "=+-@\t\r".indexOf(str.charAt(0)) >= 0) {
                    str = "'" + str;
                }
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
