package com.buddy.interceptor;

import com.buddy.common.annotation.RateLimit;
import com.buddy.common.annotation.RequireLogin;
import com.buddy.common.context.UserContext;
import com.buddy.common.store.AuxStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 接口限流拦截器测试（缺口#4）：未超限放行 / 超限 429 / 无注解放行 / 未登录放行 / 存储异常降级放行
 */
class RateLimitInterceptorTest {

    static class DemoController {
        @RequireLogin
        @RateLimit(value = "post:create", max = 5, windowHours = 1)
        public void limited() {
        }

        public void open() {
        }
    }

    private final AuxStore auxStore = mock(AuxStore.class);
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor();
        ReflectionTestUtils.setField(interceptor, "auxStore", auxStore);
        ReflectionTestUtils.setField(interceptor, "objectMapper", new ObjectMapper());
        UserContext.setUserId(7L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private HandlerMethod handler(String name) throws NoSuchMethodException {
        return new HandlerMethod(new DemoController(), DemoController.class.getMethod(name));
    }

    @Test
    void allowsWhenUnderLimit() throws Exception {
        when(auxStore.increment(anyString(), any(Duration.class))).thenReturn(1L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(new MockHttpServletRequest("POST", "/api/post/create"), response, handler("limited"));

        assertThat(pass).isTrue();
    }

    @Test
    void blocksWhenLimitExceeded() throws Exception {
        when(auxStore.increment(anyString(), any(Duration.class))).thenReturn(6L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(new MockHttpServletRequest("POST", "/api/post/create"), response, handler("limited"));

        assertThat(pass).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("操作过于频繁");
    }

    @Test
    void passesThroughWhenNoAnnotation() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(new MockHttpServletRequest("GET", "/api/post/detail/1"), response, handler("open"));

        assertThat(pass).isTrue();
    }

    @Test
    void passesThroughWhenNotLoggedIn() throws Exception {
        UserContext.clear();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(new MockHttpServletRequest("POST", "/api/post/create"), response, handler("limited"));

        assertThat(pass).isTrue();
    }

    @Test
    void failsOpenWhenStoreUnavailable() throws Exception {
        when(auxStore.increment(anyString(), any(Duration.class))).thenThrow(new RuntimeException("store down"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(new MockHttpServletRequest("POST", "/api/post/create"), response, handler("limited"));

        assertThat(pass).isTrue();
    }
}
