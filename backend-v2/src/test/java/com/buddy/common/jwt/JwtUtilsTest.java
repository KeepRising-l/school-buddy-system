package com.buddy.common.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 密钥自检测试：缺失/过短必须启动失败，有效密钥正常签发与解析
 */
class JwtUtilsTest {

    private static JwtUtils newJwtUtils(String secret) {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", secret);
        ReflectionTestUtils.setField(jwtUtils, "expiration", 3600_000L);
        return jwtUtils;
    }

    @Test
    void initRejectsMissingSecret() {
        assertThatThrownBy(() -> newJwtUtils(null).init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT");
        assertThatThrownBy(() -> newJwtUtils("   ").init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT");
    }

    @Test
    void initRejectsShortSecret() {
        assertThatThrownBy(() -> newJwtUtils("short-secret").init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("长度不足");
    }

    @Test
    void initAcceptsValidSecretAndRoundTripsToken() {
        JwtUtils jwtUtils = newJwtUtils("unit-test-secret-key-0123456789abcdef");
        jwtUtils.init();

        String token = jwtUtils.generateToken(42L, "20240001");

        assertThat(jwtUtils.validateToken(token)).isTrue();
        assertThat(jwtUtils.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(jwtUtils.getStudentNoFromToken(token)).isEqualTo("20240001");
    }

    @Test
    void validateTokenRejectsTamperedToken() {
        JwtUtils jwtUtils = newJwtUtils("unit-test-secret-key-0123456789abcdef");
        jwtUtils.init();

        String token = jwtUtils.generateToken(1L, "x");

        assertThat(jwtUtils.validateToken(token + "x")).isFalse();
    }
}
