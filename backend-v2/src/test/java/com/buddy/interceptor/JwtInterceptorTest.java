package com.buddy.interceptor;

import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.annotation.RequireLogin;
import com.buddy.common.context.UserContext;
import com.buddy.common.jwt.JwtUtils;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 登录边界回归测试：匿名游客 / 普通用户 / 管理员 / 封禁用户 / 无效 Token
 */
class JwtInterceptorTest {

    /** 测试用控制器，覆盖三类接口形态 */
    static class DemoController {
        @RequireLogin
        public void needLogin() {
        }

        @RequireAdmin
        public void needAdmin() {
        }

        public void open() {
        }
    }

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private JwtInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(interceptor, "userMapper", userMapper);
        ReflectionTestUtils.setField(interceptor, "objectMapper", new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new DemoController(), DemoController.class.getMethod(methodName));
    }

    private static User user(int status, int isAdmin) {
        User u = new User();
        u.setStatus(status);
        u.setIsAdmin(isAdmin);
        return u;
    }

    @Test
    void anonymousBlockedOnRequireLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/apply/join");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(request, response, handler("needLogin"));

        assertThat(pass).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void anonymousBlockedOnRequireAdmin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/user/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(request, response, handler("needAdmin"));

        assertThat(pass).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void anonymousAllowedOnPublicEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/post/detail/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(request, response, handler("open"));

        assertThat(pass).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void optionsRequestAlwaysPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/apply/join");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(request, response, handler("needLogin"));

        assertThat(pass).isTrue();
    }

    @Test
    void invalidTokenRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtils.validateToken("bad-token")).thenReturn(false);

        boolean pass = interceptor.preHandle(request, response, handler("needLogin"));

        assertThat(pass).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void loggedInUserPassesRequireLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer ok-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtils.validateToken("ok-token")).thenReturn(true);
        when(jwtUtils.getUserIdFromToken("ok-token")).thenReturn(7L);
        when(userMapper.selectById(7L)).thenReturn(user(1, 0));

        boolean pass = interceptor.preHandle(request, response, handler("needLogin"));

        assertThat(pass).isTrue();
        assertThat(UserContext.getUserId()).isEqualTo(7L);
    }

    @Test
    void normalUserBlockedOnAdminEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/user/list");
        request.addHeader("Authorization", "Bearer ok-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtils.validateToken("ok-token")).thenReturn(true);
        when(jwtUtils.getUserIdFromToken("ok-token")).thenReturn(7L);
        when(userMapper.selectById(7L)).thenReturn(user(1, 0));

        boolean pass = interceptor.preHandle(request, response, handler("needAdmin"));

        assertThat(pass).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(UserContext.getUserId()).isNull();
    }

    @Test
    void adminPassesAdminEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/user/list");
        request.addHeader("Authorization", "Bearer ok-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtils.validateToken("ok-token")).thenReturn(true);
        when(jwtUtils.getUserIdFromToken("ok-token")).thenReturn(7L);
        when(userMapper.selectById(7L)).thenReturn(user(1, 1));

        boolean pass = interceptor.preHandle(request, response, handler("needAdmin"));

        assertThat(pass).isTrue();
    }

    @Test
    void bannedUserBlocked() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer ok-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtils.validateToken("ok-token")).thenReturn(true);
        when(jwtUtils.getUserIdFromToken("ok-token")).thenReturn(7L);
        when(userMapper.selectById(7L)).thenReturn(user(0, 0));

        boolean pass = interceptor.preHandle(request, response, handler("needLogin"));

        assertThat(pass).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void afterCompletionClearsUserContext() {
        UserContext.setUserId(7L);

        interceptor.afterCompletion(new MockHttpServletRequest(), new MockHttpServletResponse(), null, null);

        assertThat(UserContext.getUserId()).isNull();
    }
}
