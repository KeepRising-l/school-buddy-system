package com.buddy.module.post.service;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 违禁词检测服务（等价迁移自旧版）
 * 先发后审模式下的第一道防线：发帖/评论时自动检测违禁词，命中则拦截
 * 后续可迁移到数据库或配置中心，由管理端维护词库
 */
@Service
public class SensitiveWordService {

    /**
     * 违禁词列表（硬编码，后续可升级为数据库维护）
     */
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            // 违法违规
            "赌博", "博彩", "色情", "约炮", "一夜情", "毒品", "吸毒",
            "枪支", "炸药", "传销", "诈骗", "刷单", "贷款", "高利贷",
            "代考", "代写", "答案", "作弊", "枪手",
            // 人身攻击
            "傻逼", "操你", "草泥马", "去死", "废物", "智障", "脑残",
            "贱人", "滚蛋", "垃圾人"
    );

    /**
     * 检测文本中是否包含违禁词
     *
     * @param text 待检测文本
     * @return 命中的违禁词列表，空列表表示通过
     */
    public List<String> check(String text) {
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }
        String lower = text.toLowerCase();
        return SENSITIVE_WORDS.stream()
                .filter(word -> lower.contains(word.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * 检测多段文本拼接后的违禁词
     *
     * @param texts 多段文本（标题、描述、地点等）
     * @return 命中的违禁词列表
     */
    public List<String> check(String... texts) {
        if (texts == null || texts.length == 0) {
            return Collections.emptyList();
        }
        StringBuilder sb = new StringBuilder();
        for (String t : texts) {
            if (t != null) {
                sb.append(t).append(" ");
            }
        }
        return check(sb.toString());
    }
}
