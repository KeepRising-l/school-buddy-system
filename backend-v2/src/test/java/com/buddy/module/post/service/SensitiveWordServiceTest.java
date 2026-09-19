package com.buddy.module.post.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 敏感词检测单元测试：命中/大小写/多段文本/空值
 */
class SensitiveWordServiceTest {

    private final SensitiveWordService service = new SensitiveWordService();

    @Test
    void cleanTextPasses() {
        assertThat(service.check("周六一起打羽毛球")).isEmpty();
        assertThat(service.check("")).isEmpty();
        assertThat(service.check((String) null)).isEmpty();
    }

    @Test
    void hitWordIsBlocked() {
        List<String> hits = service.check("周末一起赌博吧");
        assertThat(hits).containsExactly("赌博");
    }

    @Test
    void multipleHitsAreAllReported() {
        List<String> hits = service.check("赌博和诈骗都在词库里");
        assertThat(hits).contains("赌博", "诈骗");
    }

    @Test
    void varargsChecksConcatenatedTexts() {
        List<String> hits = service.check("正常标题", "地点正常", "描述里藏了色情");
        assertThat(hits).containsExactly("色情");
    }

    @Test
    void nullVarargsPasses() {
        assertThat(service.check((String[]) null)).isEmpty();
    }

    @Test
    void wordListCoversBothCategories() {
        // 词库包含违法违规类与人身攻击类
        assertThat(service.check("刷单")).containsExactly("刷单");
        assertThat(service.check("你是废物")).containsExactly("废物");
    }
}
