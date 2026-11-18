package com.buddy.module.ai.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.buddy.common.exception.BusinessException;
import com.buddy.module.ai.dto.AiIcebreakerVO;
import com.buddy.module.ai.dto.AiPostDraftVO;
import com.buddy.module.ai.dto.AiReportAdviceVO;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AI 智能搭子服务单元测试：自然语言组局解析、招募文案润色、成局破冰生成、举报风控研判
 */
@ExtendWith(MockitoExtension.class)
class AiBuddyServiceImplTest {

    @Mock
    private TagService tagService;
    @Mock
    private InterestTagMapper interestTagMapper;
    @Mock
    private UserTagMapper userTagMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private PostApplyMapper postApplyMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ReportMapper reportMapper;

    private AiBuddyServiceImpl aiBuddyService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Post.class);
        TableInfoHelper.initTableInfo(assistant, PostApply.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, UserTag.class);
        TableInfoHelper.initTableInfo(assistant, Report.class);
    }

    @BeforeEach
    void setUp() {
        aiBuddyService = new AiBuddyServiceImpl();
        ReflectionTestUtils.setField(aiBuddyService, "tagService", tagService);
        ReflectionTestUtils.setField(aiBuddyService, "interestTagMapper", interestTagMapper);
        ReflectionTestUtils.setField(aiBuddyService, "userTagMapper", userTagMapper);
        ReflectionTestUtils.setField(aiBuddyService, "postMapper", postMapper);
        ReflectionTestUtils.setField(aiBuddyService, "postApplyMapper", postApplyMapper);
        ReflectionTestUtils.setField(aiBuddyService, "userMapper", userMapper);
        ReflectionTestUtils.setField(aiBuddyService, "reportMapper", reportMapper);
        ReflectionTestUtils.setField(aiBuddyService, "sensitiveWordService", new SensitiveWordService());
    }

    @Test
    void parsePostRejectsBlankInput() {
        assertThatThrownBy(() -> aiBuddyService.parsePostFromText("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("一句话");
    }

    @Test
    void parsePostExtractsTagTimeLocationPeopleAndLevel() {
        InterestTag badminton = new InterestTag();
        badminton.setId(2L);
        badminton.setCategory("球类");
        badminton.setName("羽毛球");
        when(tagService.getAllTags()).thenReturn(List.of(badminton));

        AiPostDraftVO draft = aiBuddyService.parsePostFromText(
                "明晚7点到9点在南区体育馆打羽毛球3缺1，要求熟手以上，仅女生");

        assertThat(draft.getCategory()).isEqualTo("球类");
        assertThat(draft.getTagId()).isEqualTo(2L);
        assertThat(draft.getTagName()).isEqualTo("羽毛球");
        assertThat(draft.getLocation()).contains("南区体育馆");
        assertThat(draft.getMaxPeople()).isEqualTo(4);
        assertThat(draft.getRequireLevel()).isEqualTo(3);
        assertThat(draft.getRequireGender()).isEqualTo(2);
        assertThat(draft.getStartTime()).endsWith("19:00:00");
        assertThat(draft.getEndTime()).endsWith("21:00:00");
        assertThat(draft.getTitle()).isNotBlank();
        assertThat(draft.getDescription()).contains("羽毛球");
    }

    @Test
    void polishDescriptionProducesStructuredCopy() {
        String copy = aiBuddyService.polishDescription(Map.of(
                "category", "电竞",
                "tagName", "王者荣耀",
                "location", "线上语音开黑",
                "startTime", "2026-11-19 20:00",
                "maxPeople", 5,
                "requireLevel", 4
        ));
        assertThat(copy).contains("王者荣耀").contains("线上语音开黑").contains("高手");
    }

    @Test
    void generateIcebreakerFindsCommonCollegeAndBuildsGreeting() {
        Post post = new Post();
        post.setId(10L);
        post.setPublisherId(1L);
        post.setTitle("南区羽毛球双打");
        post.setCategory("球类");
        post.setLocation("南区体育馆");
        post.setStartTime(LocalDateTime.now().plusHours(3));
        when(postMapper.selectById(10L)).thenReturn(post);

        PostApply app = new PostApply();
        app.setPostId(10L);
        app.setApplicantId(2L);
        app.setStatus("APPROVED");
        when(postApplyMapper.selectList(any())).thenReturn(List.of(app));

        User u1 = new User();
        u1.setId(1L);
        u1.setCollege("计算机学院");
        u1.setCreditScore(100);
        User u2 = new User();
        u2.setId(2L);
        u2.setCollege("计算机学院");
        u2.setCreditScore(98);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(u1, u2));
        when(userTagMapper.selectList(any())).thenReturn(List.of());

        AiIcebreakerVO vo = aiBuddyService.generateIcebreaker(2L, 10L);

        assertThat(vo.getCommonTopics()).anyMatch(t -> t.contains("计算机学院"));
        assertThat(vo.getGreetingTemplate()).contains("南区羽毛球双打").contains("南区体育馆");
    }

    @Test
    void analyzeReportFlagsHighRiskOnPriorViolationsAndSensitiveContent() {
        Report report = new Report();
        report.setId(50L);
        report.setTargetType(Report.TARGET_POST);
        report.setTargetId(10L);
        report.setReasonType("违规内容");
        when(reportMapper.selectById(50L)).thenReturn(report);
        when(reportMapper.selectCount(any())).thenReturn(2L);

        Post post = new Post();
        post.setId(10L);
        post.setPublisherId(8L);
        post.setTitle("兼职日结刷单");
        post.setDescription("加微日结");
        when(postMapper.selectById(10L)).thenReturn(post);

        AiReportAdviceVO advice = aiBuddyService.analyzeReport(50L);

        assertThat(advice.getRiskLevel()).isEqualTo("HIGH");
        assertThat(advice.getSuggestedValid()).isTrue();
        assertThat(advice.getRiskScore()).isGreaterThanOrEqualTo(70);
    }
}
