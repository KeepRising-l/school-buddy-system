package com.buddy.module.post.service;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 违禁词检测服务（DFA 前缀树状态机 + 干扰符穿透引擎）
 * 先发后审模式下的第一道防线：发帖/评论时毫秒级检测违禁词，自动穿透空格、*、- 等绕过符号
 */
@Service
public class SensitiveWordService {

    /**
     * 违禁词列表（覆盖违法违规、校园兼职诈骗、代考作弊、人身攻击）
     */
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            // 违法违规与校园黑灰产
            "赌博", "博彩", "色情", "约炮", "一夜情", "毒品", "吸毒",
            "枪支", "炸药", "传销", "诈骗", "刷单", "贷款", "高利贷",
            "代考", "代写", "答案", "作弊", "枪手", "裸贷", "跑分", "洗钱",
            // 人身攻击
            "傻逼", "操你", "草泥马", "去死", "废物", "智障", "脑残",
            "贱人", "滚蛋", "垃圾人"
    );

    /** 常见穿插绕过干扰符（在已匹配违禁词首字后自动跳过） */
    private static final String NOISE_CHARS = " \t*-_·.|/~#@+^&=";

    /** DFA 前缀树根节点 */
    private static final TrieNode ROOT = new TrieNode();

    static {
        for (String word : SENSITIVE_WORDS) {
            insertWord(word);
        }
    }

    private static void insertWord(String word) {
        if (StrUtil.isBlank(word)) {
            return;
        }
        TrieNode node = ROOT;
        String lower = word.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isEnd = true;
        node.word = word;
    }

    private static boolean isNoiseChar(char c) {
        return NOISE_CHARS.indexOf(c) >= 0;
    }

    /**
     * 检测文本中是否包含违禁词（支持 DFA 匹配与穿插干扰符穿透，如 "代*考"、"刷 单"）
     *
     * @param text 待检测文本
     * @return 命中的违禁词列表（按词库原词去重返回），空列表表示通过
     */
    public List<String> check(String text) {
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }
        String lower = text.toLowerCase();
        int len = lower.length();
        Set<String> hits = new LinkedHashSet<>();

        for (int i = 0; i < len; i++) {
            char startChar = lower.charAt(i);
            TrieNode current = ROOT.children.get(startChar);
            if (current == null) {
                continue;
            }
            if (current.isEnd) {
                hits.add(current.word);
            }
            int noiseCount = 0;
            for (int j = i + 1; j < len; j++) {
                char c = lower.charAt(j);
                TrieNode next = current.children.get(c);
                if (next != null) {
                    current = next;
                    if (current.isEnd) {
                        hits.add(current.word);
                    }
                } else if (isNoiseChar(c) && noiseCount < 4) {
                    // 仅在词中间允许跳过最多4个连续/分散干扰符，防绕过
                    noiseCount++;
                } else {
                    break;
                }
            }
        }
        return new ArrayList<>(hits);
    }

    /**
     * 检测多段文本中的违禁词（分段独立检测并去重，避免跨字段首尾误拼接）
     *
     * @param texts 多段文本（标题、描述、地点等）
     * @return 命中的违禁词列表
     */
    public List<String> check(String... texts) {
        if (texts == null || texts.length == 0) {
            return Collections.emptyList();
        }
        Set<String> allHits = new LinkedHashSet<>();
        for (String t : texts) {
            if (StrUtil.isNotBlank(t)) {
                allHits.addAll(check(t));
            }
        }
        return new ArrayList<>(allHits);
    }

    private static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd = false;
        String word;
    }
}
