package com.buddy.module.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.LevelTagCategories;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.module.ai.dto.AiIcebreakerVO;
import com.buddy.module.ai.dto.AiPostDraftVO;
import com.buddy.module.ai.dto.AiReportAdviceVO;
import com.buddy.module.ai.service.AiBuddyService;
import com.buddy.module.apply.entity.PostApply;
import com.buddy.module.apply.mapper.PostApplyMapper;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.post.service.SensitiveWordService;
import com.buddy.module.report.entity.Report;
import com.buddy.module.report.mapper.ReportMapper;
import com.buddy.module.tag.entity.InterestTag;
import com.buddy.module.tag.entity.UserTag;
import com.buddy.module.tag.mapper.InterestTagMapper;
import com.buddy.module.tag.mapper.UserTagMapper;
import com.buddy.module.tag.service.TagService;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 智能搭子服务实现类
 *
 * <p>采用「双模混合架构」：
 * 1) 若配置了 {@code app.ai.api-key}（兼容 DeepSeek / 通义千问 Qwen / OpenAI），优先调用大模型 JSON 模式；
 * 2) 未配置 Key 或网络超时时，无缝降级为内置的高精度校园组局语义解析引擎（SMART_ENGINE），
 *    零外部依赖，任何电脑与答辩现场 100% 即开即用。</p>
 */
@Slf4j
@Service
public class AiBuddyServiceImpl implements AiBuddyService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.base-url:https://api.deepseek.com/v1/chat/completions}")
    private String baseUrl;

    @Value("${app.ai.model:deepseek-chat}")
    private String model;

    @Autowired(required = false)
    private TagService tagService;

    @Autowired(required = false)
    private InterestTagMapper interestTagMapper;

    @Autowired(required = false)
    private UserTagMapper userTagMapper;

    @Autowired(required = false)
    private PostMapper postMapper;

    @Autowired(required = false)
    private PostApplyMapper postApplyMapper;

    @Autowired(required = false)
    private UserMapper userMapper;

    @Autowired(required = false)
    private ReportMapper reportMapper;

    @Autowired(required = false)
    private SensitiveWordService sensitiveWordService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiPostDraftVO parsePostFromText(String prompt) {
        if (StrUtil.isBlank(prompt)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请输入一句话组局描述");
        }
        String text = prompt.trim();
        if (text.length() > 300) {
            text = text.substring(0, 300);
        }

        // 先用内置校园语义引擎解析出基准结构化结果（确保 tagId 与系统数据库真实标签严格对应）
        AiPostDraftVO draft = parseBySmartEngine(text);

        // 如配置了外部大模型 API Key，尝试用大模型增强标题与描述
        if (StrUtil.isNotBlank(apiKey)) {
            try {
                enhanceDraftWithLlm(text, draft);
                draft.setEngine("LLM");
            } catch (Exception e) {
                log.warn("外部 LLM 调用失败，已自动降级为内置智能引擎: {}", e.getMessage());
            }
        }
        return draft;
    }

    @Override
    public String polishDescription(Map<String, Object> params) {
        String category = strVal(params, "category", "校园活动");
        String tagName = strVal(params, "tagName", category);
        String location = strVal(params, "location", "校内约定地点");
        String startTime = strVal(params, "startTime", "近期");
        String draft = strVal(params, "description", "");
        int maxPeople = intVal(params, "maxPeople", 4);
        int requireLevel = intVal(params, "requireLevel", 0);

        String levelDesc = levelLabel(requireLevel);
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(tagName).append("组局召集】\n");
        sb.append("📍 活动地点：").append(location).append("\n");
        sb.append("⏰ 活动时间：").append(startTime).append("\n");
        sb.append("👥 招募人数：").append(maxPeople).append("人成局（差你一个！）\n");
        if (StrUtil.isNotBlank(levelDesc)) {
            sb.append("🏆 水平要求：").append(levelDesc).append("及以上\n");
        }
        sb.append("💬 局主留言：");
        if (StrUtil.isNotBlank(draft)) {
            sb.append(draft.trim()).append("。");
        } else {
            if ("球类".equals(category)) {
                sb.append("场地已看好，出汗解压局，节奏友好不压力，欢迎准时守约的同学一起切磋！");
            } else if ("电竞".equals(category)) {
                sb.append("开麦沟通不压力，心态好不摆烂，默契配合一起上分！");
            } else if ("桌游".equals(category)) {
                sb.append("气氛组已就位，新手老手都欢迎，人齐准时开局，拒绝临时放鸽子~");
            } else if ("跑步".equals(category)) {
                sb.append("配速友好，互相督促打卡，跑完步顺便吹吹晚风，一起坚持运动！");
            } else {
                sb.append("找节奏相近的靠谱搭子一起行动，互相监督不摸鱼，报名通过后看联系方式对接~");
            }
        }
        return sb.toString();
    }

    @Override
    public AiIcebreakerVO generateIcebreaker(Long userId, Long postId) {
        if (postMapper == null) {
            throw new BusinessException("服务未就绪");
        }
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        AiIcebreakerVO vo = new AiIcebreakerVO();
        vo.setPostId(postId);

        // 收集本局成员（发布者 + APPROVED/ATTENDED 报名者）
        Set<Long> memberIds = new HashSet<>();
        if (post.getPublisherId() != null) {
            memberIds.add(post.getPublisherId());
        }
        if (postApplyMapper != null) {
            List<PostApply> applies = postApplyMapper.selectList(
                    new LambdaQueryWrapper<PostApply>()
                            .eq(PostApply::getPostId, postId)
                            .in(PostApply::getStatus, ApplyStatus.APPROVED, ApplyStatus.ATTENDED)
            );
            if (applies != null) {
                for (PostApply a : applies) {
                    if (a.getApplicantId() != null) {
                        memberIds.add(a.getApplicantId());
                    }
                }
            }
        }

        List<User> members = (userMapper != null && !memberIds.isEmpty())
                ? userMapper.selectBatchIds(memberIds)
                : Collections.emptyList();

        List<String> topics = new ArrayList<>();
        // 1. 统计同院系情况
        Map<String, Integer> collegeCount = new HashMap<>();
        int highCreditCount = 0;
        for (User m : members) {
            if (m == null) continue;
            if (StrUtil.isNotBlank(m.getCollege())) {
                collegeCount.merge(m.getCollege(), 1, Integer::sum);
            }
            if (m.getCreditScore() != null && m.getCreditScore() >= 95) {
                highCreditCount++;
            }
        }
        for (Map.Entry<String, Integer> entry : collegeCount.entrySet()) {
            if (entry.getValue() >= 2) {
                topics.add("🎓 本局有 " + entry.getValue() + " 位伙伴来自「" + entry.getKey() + "」，校友破冰零距离！");
            }
        }
        if (highCreditCount >= 2) {
            topics.add("🛡️ 本局共有 " + highCreditCount + " 位 95+ 分高信誉靠谱队友，守约率拉满！");
        }

        // 2. 统计共同兴趣标签
        if (userTagMapper != null && interestTagMapper != null && memberIds.size() >= 2) {
            List<UserTag> allUserTags = userTagMapper.selectList(
                    new LambdaQueryWrapper<UserTag>().in(UserTag::getUserId, memberIds)
            );
            Map<Long, Integer> tagFreq = new HashMap<>();
            if (allUserTags != null) {
                for (UserTag ut : allUserTags) {
                    if (ut.getTagId() != null) {
                        tagFreq.merge(ut.getTagId(), 1, Integer::sum);
                    }
                }
            }
            Set<Long> sharedTagIds = new HashSet<>();
            for (Map.Entry<Long, Integer> e : tagFreq.entrySet()) {
                if (e.getValue() >= 2) {
                    sharedTagIds.add(e.getKey());
                }
            }
            if (!sharedTagIds.isEmpty()) {
                List<InterestTag> sharedTags = interestTagMapper.selectBatchIds(sharedTagIds);
                if (sharedTags != null && !sharedTags.isEmpty()) {
                    List<String> names = new ArrayList<>();
                    for (InterestTag t : sharedTags) {
                        if (t != null && StrUtil.isNotBlank(t.getName())) {
                            names.add(t.getName());
                        }
                    }
                    if (!names.isEmpty()) {
                        topics.add("🎯 大家共同喜欢：「" + String.join("、", names.subList(0, Math.min(3, names.size()))) + "」，见面可以从这里聊起~");
                    }
                }
            }
        }

        if (topics.isEmpty()) {
            topics.add("✨ 大家都对「" + post.getCategory() + "」感兴趣，主动打个招呼更容易熟络哦！");
        }
        vo.setCommonTopics(topics);

        // 生成一键复制打招呼话术
        String timeStr = post.getStartTime() != null
                ? (post.getStartTime().getMonthValue() + "月" + post.getStartTime().getDayOfMonth() + "日 "
                        + String.format("%02d:%02d", post.getStartTime().getHour(), post.getStartTime().getMinute()))
                : "活动时间";
        vo.setGreetingTemplate("嗨！我是校园搭子「" + post.getTitle() + "」的同局伙伴，我们约的是 "
                + timeStr + " 在「" + post.getLocation() + "」碰面，请问有什么需要提前准备的吗？");

        List<String> tips = new ArrayList<>();
        tips.add("建议提前 5~10 分钟到达「" + post.getLocation() + "」，如遇突发情况请提前在开始前1小时外沟通");
        tips.add("碰面后请向局主索要 4 位数字【活动签到码】，输入即可自助完成出勤核销并开启互评");
        vo.setActivityTips(tips);

        return vo;
    }

    @Override
    public AiReportAdviceVO analyzeReport(Long reportId) {
        if (reportMapper == null) {
            throw new BusinessException("服务未就绪");
        }
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报记录不存在");
        }

        AiReportAdviceVO vo = new AiReportAdviceVO();
        vo.setReportId(reportId);

        List<String> evidences = new ArrayList<>();
        int score = 35;

        // 1. 历史有效举报次数
        Long priorValidCount = reportMapper.selectCount(
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getTargetType, report.getTargetType())
                        .eq(Report::getTargetId, report.getTargetId())
                        .eq(Report::getStatus, Report.STATUS_VALID)
        );
        if (priorValidCount != null && priorValidCount > 0) {
            score += (int) Math.min(40, priorValidCount * 20);
            evidences.add("被举报对象历史已有 " + priorValidCount + " 次核实有效的违规记录（满3次触发自动风控）");
        } else {
            evidences.add("被举报对象此前无有效违规记录（首次被举报）");
        }

        // 2. 检测目标内容与举报说明中的风险特征
        if (Report.TARGET_POST.equals(report.getTargetType()) && postMapper != null) {
            Post targetPost = postMapper.selectById(report.getTargetId());
            if (targetPost != null) {
                if (sensitiveWordService != null) {
                    List<String> hits = sensitiveWordService.check(
                            targetPost.getTitle(), targetPost.getDescription(), targetPost.getLocation());
                    if (!hits.isEmpty()) {
                        score += 45;
                        evidences.add("帖子正文检出高危敏感词：" + String.join("、", hits));
                    }
                }
                if (hasSuspiciousMarketingPattern(targetPost.getDescription())
                        || hasSuspiciousMarketingPattern(targetPost.getTitle())) {
                    score += 30;
                    evidences.add("帖子内容含疑似兼职引流/站外营销特征词");
                }
                if (userMapper != null && targetPost.getPublisherId() != null) {
                    User pub = userMapper.selectById(targetPost.getPublisherId());
                    if (pub != null && pub.getCreditScore() != null && pub.getCreditScore() < 60) {
                        score += 20;
                        evidences.add("发布者当前信誉分较低（" + pub.getCreditScore() + "分），属高风险失信人员");
                    }
                }
            }
        } else if (Report.TARGET_USER.equals(report.getTargetType()) && userMapper != null) {
            User targetUser = userMapper.selectById(report.getTargetId());
            if (targetUser != null && targetUser.getCreditScore() != null && targetUser.getCreditScore() < 60) {
                score += 25;
                evidences.add("被举报用户当前信誉分仅为 " + targetUser.getCreditScore() + " 分（低于60分阈值）");
            }
        }

        if (StrUtil.isNotBlank(report.getReasonType()) && report.getReasonType().contains("违规")) {
            score += 15;
            evidences.add("举报分类为「" + report.getReasonType() + "」，属优先核查类别");
        }

        score = Math.max(10, Math.min(98, score));
        vo.setRiskScore(score);

        if (score >= 70) {
            vo.setRiskLevel("HIGH");
            vo.setSuggestedValid(true);
            vo.setSummary("高风险违规：发现明确风险特征或累犯记录，建议判定【有效】并联动下架/封禁。");
            vo.setSuggestedRemark("经 AI 辅助风控与人工复核，确认存在" + report.getReasonType() + "违规行为，已执行联动处置。");
        } else if (score >= 50) {
            vo.setRiskLevel("MEDIUM");
            vo.setSuggestedValid(true);
            vo.setSummary("中等风险：存在一定违规嫌疑，请结合举报说明核对原帖内容后从严处置。");
            vo.setSuggestedRemark("经核查举报内容属实，已对相关内容进行规范处置。");
        } else {
            vo.setRiskLevel("LOW");
            vo.setSuggestedValid(false);
            vo.setSummary("低风险：未检出明显违禁词与历史违规记录，若属主观分歧建议判定【无效】。");
            vo.setSuggestedRemark("经核实暂未发现明确违规证据，建议双方友好沟通。");
        }
        vo.setEvidences(evidences);
        return vo;
    }

    // ==================== 内置高精度校园语义解析引擎 ====================

    private AiPostDraftVO parseBySmartEngine(String text) {
        AiPostDraftVO vo = new AiPostDraftVO();
        vo.setEngine("SMART_ENGINE");

        // 1. 匹配兴趣分类与具体项目标签
        List<InterestTag> allTags = tagService != null ? tagService.getAllTags() : Collections.emptyList();
        InterestTag matchedTag = matchTagFromText(text, allTags);
        if (matchedTag != null) {
            vo.setCategory(matchedTag.getCategory());
            vo.setTagId(matchedTag.getId());
            vo.setTagName(matchedTag.getName());
            vo.setHasLevel(LevelTagCategories.contains(matchedTag.getCategory()));
        } else {
            String cat = inferCategoryFallback(text);
            vo.setCategory(cat);
            vo.setHasLevel(LevelTagCategories.contains(cat));
            if (allTags != null) {
                for (InterestTag t : allTags) {
                    if (cat.equals(t.getCategory())) {
                        vo.setTagId(t.getId());
                        vo.setTagName(t.getName());
                        break;
                    }
                }
            }
        }

        // 2. 解析开始时间与结束时间
        LocalDateTime[] timeRange = parseDateTimeRange(text);
        vo.setStartTime(timeRange[0].format(DT_FMT));
        vo.setEndTime(timeRange[1].format(DT_FMT));

        // 3. 解析人数限制
        vo.setMaxPeople(parseMaxPeople(text, vo.getCategory(), vo.getTagName()));

        // 4. 解析段位要求
        if (Boolean.TRUE.equals(vo.getHasLevel())) {
            vo.setRequireLevel(parseRequireLevel(text));
        } else {
            vo.setRequireLevel(null);
        }

        // 5. 解析性别要求
        vo.setRequireGender(parseRequireGender(text));

        // 6. 解析活动地点
        vo.setLocation(parseLocation(text, vo.getCategory(), vo.getTagName()));

        // 7. 生成有吸引力的标题（5~30字）
        String tagLabel = StrUtil.isNotBlank(vo.getTagName()) ? vo.getTagName() : vo.getCategory();
        String title = buildSmartTitle(vo.getLocation(), tagLabel, vo.getMaxPeople(), vo.getRequireLevel());
        vo.setTitle(title);

        // 8. 生成结构化招募描述
        Map<String, Object> polishParams = new HashMap<>();
        polishParams.put("category", vo.getCategory());
        polishParams.put("tagName", tagLabel);
        polishParams.put("location", vo.getLocation());
        polishParams.put("startTime", vo.getStartTime().substring(0, 16) + " ~ " + vo.getEndTime().substring(11, 16));
        polishParams.put("maxPeople", vo.getMaxPeople());
        polishParams.put("requireLevel", vo.getRequireLevel());
        polishParams.put("description", text);
        vo.setDescription(polishDescription(polishParams));

        return vo;
    }

    private InterestTag matchTagFromText(String text, List<InterestTag> allTags) {
        if (allTags == null || allTags.isEmpty()) {
            return null;
        }
        String lower = text.toLowerCase();
        // 先直接按标签名精确包含匹配
        for (InterestTag tag : allTags) {
            if (tag != null && StrUtil.isNotBlank(tag.getName()) && lower.contains(tag.getName().toLowerCase())) {
                return tag;
            }
        }
        // 常见同义词/简称映射
        Map<String, String> synonymMap = new LinkedHashMap<>();
        synonymMap.put("羽球", "羽毛球");
        synonymMap.put("投篮", "篮球");
        synonymMap.put("半场", "篮球");
        synonymMap.put("全场", "篮球");
        synonymMap.put("国球", "乒乓球");
        synonymMap.put("踢球", "足球");
        synonymMap.put("王者", "王者荣耀");
        synonymMap.put("农", "王者荣耀");
        synonymMap.put("五排", "王者荣耀");
        synonymMap.put("lol", "英雄联盟");
        synonymMap.put("峡谷", "英雄联盟");
        synonymMap.put("大乱斗", "英雄联盟");
        synonymMap.put("吃鸡", "和平精英");
        synonymMap.put("海岛", "和平精英");
        synonymMap.put("打本", "剧本杀");
        synonymMap.put("拼本", "剧本杀");
        synonymMap.put("狼人", "狼人杀");
        synonymMap.put("杀一下", "三国杀");
        synonymMap.put("夜跑", "夜跑");
        synonymMap.put("晨跑", "晨跑");
        synonymMap.put("慢跑", "夜跑");
        synonymMap.put("操场跑", "夜跑");

        for (Map.Entry<String, String> entry : synonymMap.entrySet()) {
            if (lower.contains(entry.getKey())) {
                for (InterestTag tag : allTags) {
                    if (entry.getValue().equals(tag.getName())) {
                        return tag;
                    }
                }
            }
        }
        return null;
    }

    private String inferCategoryFallback(String text) {
        if (text.matches(".*(球|投篮|双打|单打|挥拍).*")) return "球类";
        if (text.matches(".*(上分|开黑|排位|游戏|王者|吃鸡|联盟|电竞).*")) return "电竞";
        if (text.matches(".*(桌游|剧本|狼人|三国杀|卡牌).*")) return "桌游";
        if (text.matches(".*(跑|公里|配速|刷圈).*")) return "跑步";
        return "球类";
    }

    private LocalDateTime[] parseDateTimeRange(String text) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate targetDate = now.toLocalDate();

        if (text.contains("大后天")) {
            targetDate = targetDate.plusDays(3);
        } else if (text.contains("后天")) {
            targetDate = targetDate.plusDays(2);
        } else if (text.contains("明天") || text.contains("明早") || text.contains("明晚") || text.contains("明午")) {
            targetDate = targetDate.plusDays(1);
        } else if (text.contains("今天") || text.contains("今晚") || text.contains("今早")) {
            targetDate = now.toLocalDate();
        } else {
            DayOfWeek dow = parseDayOfWeek(text);
            if (dow != null) {
                targetDate = targetDate.with(TemporalAdjusters.nextOrSame(dow));
                if (targetDate.equals(now.toLocalDate()) && now.getHour() >= 20) {
                    targetDate = targetDate.plusWeeks(1);
                }
            }
        }

        // 处理“半小时后 / X小时后”
        Matcher relMin = Pattern.compile("(半|\\d+)\\s*(个)?小时后").matcher(text);
        if (relMin.find()) {
            String val = relMin.group(1);
            long minutes = "半".equals(val) ? 30 : Long.parseLong(val) * 60;
            LocalDateTime start = now.plusMinutes(minutes).withSecond(0).withNano(0);
            return new LocalDateTime[]{start, start.plusHours(2)};
        }

        boolean isPm = text.matches(".*(下午|傍晚|晚上|明晚|今晚|夜里).*");
        boolean isMorning = text.matches(".*(早上|上午|明早|今早|晨).*");

        // 匹配时间区间：如 "19:00-21:00"、"7点到9点"、"7点半到9点半"
        Pattern rangePattern = Pattern.compile("(\\d{1,2})(?:[:点时](\\d{1,2}|半)?)?\\s*(?:到|至|-|~|—)\\s*(\\d{1,2})(?:[:点时](\\d{1,2}|半)?)?");
        Matcher rm = rangePattern.matcher(text);
        int startHour = 19;
        int startMin = 0;
        int endHour = 21;
        int endMin = 0;
        boolean timeFound = false;

        if (rm.find()) {
            // 排除 "3到5人" 这种人数区间
            String afterMatch = text.substring(rm.end());
            if (!afterMatch.startsWith("人") && !afterMatch.startsWith("个")) {
                startHour = Integer.parseInt(rm.group(1));
                startMin = parseMinuteGroup(rm.group(2));
                endHour = Integer.parseInt(rm.group(3));
                endMin = parseMinuteGroup(rm.group(4));
                timeFound = true;
            }
        }

        if (!timeFound) {
            Pattern singleTime = Pattern.compile("(\\d{1,2})\\s*(?:[:点时])\\s*(\\d{1,2}|半)?");
            Matcher sm = singleTime.matcher(text);
            if (sm.find()) {
                startHour = Integer.parseInt(sm.group(1));
                startMin = parseMinuteGroup(sm.group(2));
                endHour = startHour + 2;
                endMin = startMin;
                timeFound = true;
            }
        }

        if (!timeFound) {
            if (isMorning) {
                startHour = 8;
                endHour = 10;
            } else {
                startHour = 19;
                endHour = 21;
            }
        } else {
            if (isPm && startHour < 12) {
                startHour += 12;
            }
            if (isPm && endHour < 12) {
                endHour += 12;
            }
            if (!isMorning && !isPm && startHour >= 1 && startHour <= 6) {
                startHour += 12;
                if (endHour <= 12) endHour += 12;
            }
        }

        startHour = Math.min(23, Math.max(0, startHour));
        LocalDateTime start = LocalDateTime.of(targetDate, LocalTime.of(startHour, startMin, 0));
        // 若推算出的开始时间已经早于当前时间，顺延到明天同一时间
        if (!start.isAfter(now)) {
            start = start.plusDays(1);
        }

        LocalDateTime end;
        if (endHour > startHour || (endHour == startHour && endMin > startMin)) {
            end = LocalDateTime.of(start.toLocalDate(), LocalTime.of(Math.min(23, endHour), endMin, 0));
        } else {
            end = start.plusHours(2);
        }
        return new LocalDateTime[]{start, end};
    }

    private int parseMinuteGroup(String g) {
        if (StrUtil.isBlank(g)) return 0;
        if ("半".equals(g)) return 30;
        try {
            return Math.min(59, Math.max(0, Integer.parseInt(g)));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private DayOfWeek parseDayOfWeek(String text) {
        if (text.matches(".*(周一|星期一|礼拜一).*")) return DayOfWeek.MONDAY;
        if (text.matches(".*(周二|星期二|礼拜二).*")) return DayOfWeek.TUESDAY;
        if (text.matches(".*(周三|星期三|礼拜三).*")) return DayOfWeek.WEDNESDAY;
        if (text.matches(".*(周四|星期四|礼拜四).*")) return DayOfWeek.THURSDAY;
        if (text.matches(".*(周五|星期五|礼拜五).*")) return DayOfWeek.FRIDAY;
        if (text.matches(".*(周六|星期六|礼拜六|周末).*")) return DayOfWeek.SATURDAY;
        if (text.matches(".*(周日|周天|星期日|星期天|礼拜天).*")) return DayOfWeek.SUNDAY;
        return null;
    }

    private int parseMaxPeople(String text, String category, String tagName) {
        // 1. "X缺Y"（如 3缺1 -> 4人，4缺2 -> 6人）
        Matcher mLack = Pattern.compile("([一二三四五六七八九\\d])\\s*缺\\s*([一二三四五六七八九\\d])").matcher(text);
        if (mLack.find()) {
            int have = chineseOrDigitToInt(mLack.group(1), 3);
            int lack = chineseOrDigitToInt(mLack.group(2), 1);
            return clampPeople(have + lack);
        }
        // 2. "五排/5排/双排/四排/双打/单打"
        if (text.contains("五排") || text.contains("5排")) return 5;
        if (text.contains("四排") || text.contains("4排") || text.contains("双打")) return 4;
        if (text.contains("三排") || text.contains("3排")) return 3;
        if (text.contains("双排") || text.contains("单打") || text.contains("单挑") || text.toLowerCase().contains("1v1")) return 2;

        // 3. "约X个人 / 找X个人 / X人局"
        Matcher mNum = Pattern.compile("(?:约|找|招|差|缺|共)?\\s*(\\d{1,2}|[两二三四五六七八九十])\\s*个?\\s*(?:人|位|名|队友|搭子)").matcher(text);
        if (mNum.find()) {
            int n = chineseOrDigitToInt(mNum.group(1), 4);
            // 如果写的是 "约3个人/差1个人"，加上发布者自己
            if (text.contains("差" + mNum.group(1)) || text.contains("缺" + mNum.group(1)) || text.contains("找" + mNum.group(1))) {
                return clampPeople(n + 1);
            }
            return clampPeople(n);
        }
        if ("桌游".equals(category)) return 6;
        if ("电竞".equals(category)) return 5;
        return 4;
    }

    private int chineseOrDigitToInt(String s, int def) {
        if (StrUtil.isBlank(s)) return def;
        switch (s.trim()) {
            case "一": return 1;
            case "二":
            case "两": return 2;
            case "三": return 3;
            case "四": return 4;
            case "五": return 5;
            case "六": return 6;
            case "七": return 7;
            case "八": return 8;
            case "九": return 9;
            case "十": return 10;
            default:
                try {
                    return Integer.parseInt(s.trim());
                } catch (NumberFormatException e) {
                    return def;
                }
        }
    }

    private int clampPeople(int p) {
        return Math.max(2, Math.min(20, p));
    }

    private Integer parseRequireLevel(String text) {
        if (text.contains("宗师") || text.contains("荣耀王者") || text.contains("百星")) return 5;
        if (text.contains("高手") || text.contains("大神") || text.contains("大佬") || text.contains("高水平")) return 4;
        if (text.contains("熟手") || text.contains("熟练") || text.contains("会打") || text.contains("有一点基础")) return 3;
        if (text.contains("小白") || text.contains("入门")) return 2;
        if (text.contains("萌新") || text.contains("新手") || text.contains("零基础")) return 1;
        return null;
    }

    private int parseRequireGender(String text) {
        if (text.matches(".*(仅女|限女|女生局|只招女|姐妹).*")) return 2;
        if (text.matches(".*(仅男|限男|男生局|只招男|兄弟局).*")) return 1;
        return 0;
    }

    private String parseLocation(String text, String category, String tagName) {
        // 提取常见校园地点短语
        Pattern locPattern = Pattern.compile("((?:东区|西区|南区|北区|中心|校内|第一|第二|三号|综合)?[\\u4e00-\\u9fa5A-Za-z0-9]{1,8}(?:体育馆|篮球场|羽球馆|羽毛球馆|乒乓球室|网球场|足球场|田径场|操场|图书馆|自习室|活动中心|健身房|桌游吧|食堂|咖啡厅))");
        Matcher m = locPattern.matcher(text);
        if (m.find()) {
            String loc = m.group(1);
            // 去除可能误粘连的前置介词（如 "在南区体育馆" -> "南区体育馆"）
            loc = loc.replaceAll("^.*?(?:在|去|到)", "");
            if (StrUtil.isNotBlank(loc)) {
                return loc;
            }
        }
        if ("电竞".equals(category)) return "线上语音开黑";
        if ("桌游".equals(category)) return "学生活动中心桌游区";
        if ("跑步".equals(category)) return "南区田径运动场";
        return "校内综合体育馆";
    }

    private String buildSmartTitle(String location, String tagLabel, Integer maxPeople, Integer requireLevel) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(location) && !location.equals("线上语音开黑")) {
            sb.append(location);
        }
        sb.append(tagLabel);
        if (maxPeople != null && maxPeople >= 2) {
            sb.append(maxPeople - 1).append("缺1");
        }
        if (requireLevel != null && requireLevel >= 3) {
            sb.append("·").append(levelLabel(requireLevel)).append("局");
        } else {
            sb.append("·找靠谱搭子");
        }
        String res = sb.toString();
        if (res.length() < 5) {
            res = res + "欢迎加入";
        }
        return res.length() > 30 ? res.substring(0, 30) : res;
    }

    private void enhanceDraftWithLlm(String prompt, AiPostDraftVO draft) throws Exception {
        String sysPrompt = "你是高校校园搭子预约系统的智能助手。请根据用户的组局描述，返回纯JSON对象（不要markdown代码块），"
                + "包含字段 title(5-25字吸引人的标题) 和 description(50-120字热情清晰的招募详情)。";
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", sysPrompt),
                Map.of("role", "user", "content", prompt)
        ));
        body.put("temperature", 0.5);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 && StrUtil.isNotBlank(response.body())) {
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (StrUtil.isNotBlank(content)) {
                String cleanJson = content.replaceAll("(?s)^```(?:json)?\\s*|\\s*```$", "").trim();
                JsonNode parsed = objectMapper.readTree(cleanJson);
                String llmTitle = parsed.path("title").asText("");
                String llmDesc = parsed.path("description").asText("");
                if (llmTitle.length() >= 5 && llmTitle.length() <= 30) {
                    draft.setTitle(llmTitle);
                }
                if (StrUtil.isNotBlank(llmDesc)) {
                    draft.setDescription(llmDesc);
                }
            }
        }
    }

    private boolean hasSuspiciousMarketingPattern(String text) {
        if (StrUtil.isBlank(text)) return false;
        return text.matches("(?s).*(兼职|日结|加微|vx|VX|返利|代课|有偿|私聊领).*");
    }

    private String levelLabel(Integer level) {
        if (level == null || level <= 0) return "";
        switch (level) {
            case 1: return "萌新";
            case 2: return "小白";
            case 3: return "熟手";
            case 4: return "高手";
            case 5: return "宗师";
            default: return "";
        }
    }

    private String strVal(Map<String, Object> map, String key, String def) {
        if (map == null || map.get(key) == null) return def;
        String s = map.get(key).toString().trim();
        return s.isEmpty() ? def : s;
    }

    private int intVal(Map<String, Object> map, String key, int def) {
        if (map == null || map.get(key) == null) return def;
        try {
            return Integer.parseInt(map.get(key).toString().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
