package com.buddy.module.ai.controller;

import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.annotation.RequireLogin;
import com.buddy.common.context.UserContext;
import com.buddy.common.result.R;
import com.buddy.module.ai.dto.AiIcebreakerVO;
import com.buddy.module.ai.dto.AiPostDraftVO;
import com.buddy.module.ai.dto.AiReportAdviceVO;
import com.buddy.module.ai.service.AiBuddyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * AI 原生能力控制器（用户端智能发帖/文案润色/破冰小秘书 + 管理端举报风控研判）
 */
@RestController
public class AiController {

    @Autowired
    private AiBuddyService aiBuddyService;

    /**
     * AI 一键成局：自然语言一句话解析为结构化组局草稿
     * body: {prompt: "明晚7点到9点南区体育馆约3个人打羽毛球，要求熟手以上"}
     */
    @RequireLogin
    @PostMapping("/api/ai/parse-post")
    public R<AiPostDraftVO> parsePost(@RequestBody Map<String, Object> body) {
        String prompt = body == null || body.get("prompt") == null ? "" : body.get("prompt").toString();
        return R.ok(aiBuddyService.parsePostFromText(prompt));
    }

    /**
     * AI 润色/扩写活动描述
     */
    @RequireLogin
    @PostMapping("/api/ai/polish-desc")
    public R<Map<String, String>> polishDescription(@RequestBody Map<String, Object> body) {
        String polished = aiBuddyService.polishDescription(body);
        return R.ok(Collections.singletonMap("description", polished));
    }

    /**
     * AI 成局破冰小秘书：生成共同话题与打招呼模板
     */
    @RequireLogin
    @GetMapping("/api/ai/icebreaker/{postId}")
    public R<AiIcebreakerVO> getIcebreaker(@PathVariable Long postId) {
        Long userId = UserContext.getUserId();
        return R.ok(aiBuddyService.generateIcebreaker(userId, postId));
    }

    /**
     * 管理端：举报单 AI 风控研判与处置建议
     */
    @RequireAdmin
    @GetMapping("/api/admin/report/ai-advice/{id}")
    public R<AiReportAdviceVO> getReportAiAdvice(@PathVariable Long id) {
        return R.ok(aiBuddyService.analyzeReport(id));
    }
}
