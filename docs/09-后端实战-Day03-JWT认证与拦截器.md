# 后端实战 Day03 - JWT 认证与拦截器

## 今日目标

1. 创建 `JwtUtils` 工具类（生成 Token、解析 Token、校验 Token）
2. 创建 `UserContext`（基于 ThreadLocal 存储当前用户 ID）
3. 创建 `JwtInterceptor`（从请求头取 Token、解析用户身份、校验管理员权限）
4. 创建 `@RequireAdmin` 注解（标记需要管理员权限的接口）
5. 创建 `WebMvcConfig`（注册拦截器、配置跨域、静态资源映射）
6. 完善 `SecurityConfig`（放行所有请求，用自定义 JWT 拦截器做认证）
7. 验证：不带 Token 返回 401，带 Token 返回 200

## 你将学会

- JWT（JSON Web Token）的原理和完整实现
- Spring MVC 拦截器的执行流程和编写方法
- ThreadLocal 在 Web 项目中的用法和注意事项
- 自定义注解 + 拦截器实现权限校验
- CORS 跨域配置

---

## 背景知识：JWT 是什么？

JWT 是一种无状态的认证方案，工作流程如下：

```
1. 用户登录 → 服务器验证密码 → 生成 Token 返回给前端
2. 前端每次请求在 Header 中携带 Token：Authorization: Bearer <token>
3. 服务器拦截器取出 Token → 解析出用户 ID → 放行请求
```

**JWT 的结构（三段用 `.` 分隔）：**

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwic3R1ZGVudE5vIjoiMjAyNDAwMDEiLCJpYXQiOjE3MDYxMjM0NTYsImV4cCI6MTcwNjcyODI1Nn0.xxx
│          Header             │              Payload              │   Signature
│  (算法类型)                 │  (用户数据 + 过期时间)              │  (签名，防篡改)
```

- **Header**：声明算法类型（HS256）
- **Payload**：存放用户数据（userId、studentNo）和过期时间
- **Signature**：用密钥对前两段签名，任何篡改都会导致签名校验失败

**为什么用 JWT 而不是 Session？** 前后端分离项目通常需要跨域，Session 在跨域场景下不好处理。JWT 是无状态的，服务器不需要存储会话信息，天然支持分布式部署。

---

## 第一步：创建 JwtUtils 工具类

### 创建文件

`src/main/java/com/buddy/common/jwt/JwtUtils.java`

### 写入代码

```java
package com.buddy.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

/**
 * JWT 工具类
 * 负责令牌的生成、解析与校验
 */
@Slf4j
@Component
public class JwtUtils {

    /** 从 application.yml 读取密钥 */
    @Value("${jwt.secret}")
    private String secret;

    /** 从 application.yml 读取过期时间（毫秒） */
    @Value("${jwt.expiration}")
    private long expiration;

    /** HMAC-SHA 签名密钥对象 */
    private Key key;

    /**
     * @PostConstruct 在 Bean 初始化后自动执行
     * 将字符串密钥转换为 Key 对象，HMAC-SHA256 要求密钥至少 256 位（32 字节）
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成 Token
     *
     * @param userId    用户 ID，作为 Token 的 subject（主体）
     * @param studentNo 学号，作为自定义 claim 存入
     * @return Token 字符串
     */
    public String generateToken(Long userId, String studentNo) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))   // 主体：用户 ID
                .claim("studentNo", studentNo)         // 自定义字段：学号
                .setIssuedAt(now)                      // 签发时间
                .setExpiration(expireDate)              // 过期时间
                .signWith(key)                          // 用密钥签名
                .compact();                             // 生成 Token 字符串
    }

    /**
     * 从 Token 中获取用户 ID
     * subject 字段存的就是 userId
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 Token 中获取学号
     * 自定义 claim 字段
     */
    public String getStudentNoFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("studentNo", String.class);
    }

    /**
     * 校验 Token 是否有效
     * 有效返回 true，无效或过期返回 false
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.warn("Token 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析 Token，返回 Claims（声明对象）
     * Claims 包含了 Token 中的所有字段（sub、iat、exp、自定义 claim 等）
     * 如果 Token 过期或签名不匹配，会抛出异常
     */
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
```

### 解释

- **@Component**：把 JwtUtils 注册为 Spring Bean，其他类可以通过 `@Autowired` 注入使用
- **@Value("${jwt.secret}")**：从 `application.yml` 中读取 `jwt.secret` 配置项的值注入到字段中
- **@PostConstruct**：在依赖注入完成后自动执行，这里用来把字符串密钥转成 `Key` 对象。因为 `Keys.hmacShaKeyFor()` 只需要执行一次，放在 `@PostConstruct` 里避免每次生成 Token 都转换
- **Jwts.builder()**：jjwt 库的链式 API，按顺序设置 subject、claim、签发时间、过期时间、签名，最后 `compact()` 生成 Token 字符串
- **Jwts.parserBuilder()**：解析 Token 的链式 API，设置签名密钥后调用 `parseClaimsJws()` 解析，如果 Token 过期会抛出 `ExpiredJwtException`，签名不匹配会抛出 `SignatureException`
- **为什么 subject 存 userId 而不是学号？** userId 是数字，更短，解析更快。学号是字符串，放在自定义 claim 里

---

## 第二步：创建 UserContext

### 为什么需要 UserContext？

拦截器从 Token 中解析出 userId 后，Controller 和 Service 需要拿到这个 userId。有几种方案：

1. **每个方法加 `@RequestHeader` 参数** → 太麻烦，每个接口都要加
2. **用 HttpServletRequest.getAttribute()** → 需要每次传 request 对象
3. **用 ThreadLocal** → ✅ 最优雅，任何地方都能直接取，不需要传参

**ThreadLocal 原理**：每个线程有自己独立的存储空间，互不干扰。Web 项目中每个请求由一个独立线程处理，所以 ThreadLocal 天然适合存储"当前请求的用户信息"。

### 创建文件

`src/main/java/com/buddy/common/context/UserContext.java`

### 写入代码

```java
package com.buddy.common.context;

/**
 * 用户上下文工具类
 * 基于 ThreadLocal 存储当前请求的用户 ID
 *
 * 使用方式：
 *   拦截器中：UserContext.setUserId(userId);   // 存入
 *   Service中：Long userId = UserContext.getUserId();  // 取出
 *   拦截器中：UserContext.clear();              // 清除（必须！）
 */
public class UserContext {

    /** ThreadLocal 存储，key 是线程，value 是用户 ID */
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /**
     * 设置当前用户 ID
     * 在 JwtInterceptor 中调用
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取当前用户 ID
     * 在 Controller/Service 中调用
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 清除当前线程的用户信息
     * ⚠️ 必须在请求结束后调用，否则会导致内存泄漏！
     * 在 JwtInterceptor.afterCompletion() 中调用
     */
    public static void clear() {
        USER_ID.remove();
    }
}
```

### 解释

- **为什么用 `static final`？** ThreadLocal 本身是线程安全的，用 static 保证全局只有一个实例，final 保证不被重新赋值
- **为什么必须调用 `clear()`？** Tomcat 使用线程池处理请求，线程会被复用。如果不清理，下一个请求复用同一线程时会拿到上一个请求的 userId，导致严重的安全问题！这就是所谓的 ThreadLocal 内存泄漏
- **为什么不用 `InheritableThreadLocal`？** 我们不需要子线程继承父线程的用户信息，普通 ThreadLocal 就够了

---

## 第三步：创建 @RequireAdmin 注解

### 为什么需要自定义注解？

有些接口只允许管理员访问（如审核帖子、管理标签）。我们可以在方法上加 `@RequireAdmin` 注解，拦截器检测到这个注解时自动校验管理员权限，比在每个方法里写 `if (!user.getIsAdmin())` 优雅得多。

### 创建文件

`src/main/java/com/buddy/common/annotation/RequireAdmin.java`

### 写入代码

```java
package com.buddy.common.annotation;

import java.lang.annotation.*;

/**
 * 管理员权限注解
 * 标注在 Controller 方法上，表示该接口需要管理员权限
 *
 * 使用方式：
 *   @RequireAdmin
 *   @PostMapping("/api/admin/post/audit")
 *   public R<Void> auditPost(...) { ... }
 */
@Target(ElementType.METHOD)       // 作用在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留，拦截器通过反射读取
@Documented                        // 生成 JavaDoc 时包含此注解
public @interface RequireAdmin {
}
```

### 解释

- **@Target(ElementType.METHOD)**：这个注解只能加在方法上，不能加在类或字段上
- **@Retention(RetentionPolicy.RUNTIME)**：注解在运行时仍然存在，拦截器通过 `method.getAnnotation(RequireAdmin.class)` 读取。如果选 `SOURCE` 或 `CLASS`，运行时就读不到了
- **@Documented**：生成 JavaDoc 时会包含这个注解的信息，方便其他开发者了解接口需要管理员权限

---

## 第四步：创建 JwtInterceptor

这是整个认证体系的核心！拦截器在请求到达 Controller 之前执行，负责：

1. 从请求头取出 Token
2. 校验 Token 是否有效
3. 解析出用户 ID，存入 UserContext
4. 如果方法有 `@RequireAdmin` 注解，校验管理员权限
5. 请求结束后清除 UserContext

### 创建文件

`src/main/java/com/buddy/interceptor/JwtInterceptor.java`

### 写入代码

```java
package com.buddy.interceptor;

import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.context.UserContext;
import com.buddy.common.jwt.JwtUtils;
import com.buddy.entity.User;
import com.buddy.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 认证拦截器
 * 校验请求头中的 Token，验证用户身份，设置用户上下文
 *
 * 执行流程：
 * 1. 请求到达 → preHandle() → 校验 Token
 * 2. 校验通过 → 放行请求到 Controller
 * 3. 请求完成 → afterCompletion() → 清除 ThreadLocal
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 请求预处理
     * 在 Controller 方法执行之前调用
     * 返回 true 表示放行，返回 false 表示拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 预检请求（OPTIONS）直接放行
        // 浏览器跨域请求会先发 OPTIONS 请求，不带 Token，必须放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 从请求头获取 Token
        // 前端约定格式：Authorization: Bearer <token>
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);  // 去掉 "Bearer " 前缀
        }

        // 3. Token 为空，返回 401
        if (token == null || token.isEmpty()) {
            log.warn("请求缺少Token: {} {}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        // 4. 校验 Token 有效性（签名、过期时间）
        if (!jwtUtils.validateToken(token)) {
            log.warn("Token无效或已过期: {} {}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        // 5. 解析 Token，提取用户 ID，存入上下文
        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            UserContext.setUserId(userId);

            // 6. 检查 @RequireAdmin 注解，校验管理员权限
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
                if (requireAdmin != null) {
                    // 查询用户信息，判断是否管理员
                    User user = userMapper.selectById(userId);
                    if (user == null || user.getIsAdmin() == null || user.getIsAdmin() != 1) {
                        log.warn("非管理员访问管理接口: userId={}, uri={}", userId, request.getRequestURI());
                        writeForbidden(response, "无管理员权限");
                        return false;
                    }
                }
            }

            return true;  // 放行请求
        } catch (Exception e) {
            log.error("Token解析异常: {}", e.getMessage());
            writeUnauthorized(response, "登录状态异常，请重新登录");
            return false;
        }
    }

    /**
     * 请求完成后的回调
     * 无论请求成功还是失败都会执行
     * ⚠️ 必须清除 ThreadLocal，防止内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    /**
     * 写入 401 未授权响应
     * 直接往 response 写 JSON，绕过 Spring 的视图解析
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", message);
        result.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 写入 403 禁止访问响应
     */
    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("message", message);
        result.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
```

### 解释

**整体流程图：**

```
请求到达
  │
  ▼
OPTIONS 请求？ ──是──→ 直接放行
  │否
  ▼
Header 有 Token？ ──否──→ 返回 401 "未登录"
  │是
  ▼
Token 有效？ ──否──→ 返回 401 "登录已过期"
  │是
  ▼
解析 userId → 存入 UserContext
  │
  ▼
方法有 @RequireAdmin？ ──否──→ 放行
  │是
  ▼
用户是管理员？ ──否──→ 返回 403 "无管理员权限"
  │是
  ▼
放行请求 → Controller 处理 → afterCompletion() 清除 UserContext
```

**关键点解释：**

- **`handler instanceof HandlerMethod`**：handler 有两种类型——`HandlerMethod`（Controller 方法）和 `ResourceHttpRequestHandler`（静态资源）。只有 Controller 方法才需要检查注解
- **`ObjectMapper`**：Jackson 的 JSON 序列化工具，Spring Boot 自动配置的 Bean，直接注入使用
- **为什么不用 `R.unauthorized()` 直接返回？** 因为拦截器不在 Spring MVC 的正常返回流程中，直接返回对象不会被序列化。必须手动往 `HttpServletResponse` 写 JSON 字符串
- **`writeUnauthorized` 和 `writeForbidden`**：这两个方法手动构造 JSON 响应，格式和 `R<T>` 保持一致（`{code, message, data}`），前端不需要特殊处理

---

## 第五步：创建 WebMvcConfig

拦截器写好了，还需要注册到 Spring MVC 中才能生效。同时配置跨域和静态资源映射。

### 创建文件

`src/main/java/com/buddy/config/WebMvcConfig.java`

### 写入代码

```java
package com.buddy.config;

import com.buddy.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 处理跨域、JWT 拦截器注册、静态资源映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    /**
     * 注册 JWT 拦截器
     * addPathPatterns：拦截哪些路径
     * excludePathPatterns：排除哪些路径（不需要登录就能访问的接口）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")           // 拦截所有 /api/ 开头的请求
                .excludePathPatterns(
                        "/api/auth/**",               // 登录、注册接口
                        "/api/square/list",            // 广场列表（公开）
                        "/api/post/detail/**",         // 帖子详情（公开）
                        "/api/post/comments/**",        // 帖子评论（公开）
                        "/api/review/list/**",         // 评价列表（公开）
                        "/api/review/user/**",         // 用户评价（公开）
                        "/api/admin/tag/list"          // 标签列表（公开，前端选择标签用）
                );
    }

    /**
     * 配置 CORS 跨域
     * 前端（localhost:5173）和后端（localhost:8080）端口不同，属于跨域
     * 必须配置跨域，否则前端请求会被浏览器拦截
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                    // 对所有路径生效
                .allowedOriginPatterns("*")           // 允许所有来源
                .allowedMethods("*")                  // 允许所有 HTTP 方法
                .allowedHeaders("*")                  // 允许所有请求头
                .allowCredentials(true)               // 允许携带 Cookie
                .maxAge(3600);                        // 预检请求缓存时间（秒）
    }

    /**
     * 配置静态资源映射
     * 用户上传的图片存在 uploads/ 目录下
     * 通过 /uploads/** 路径可以直接访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
```

### 解释

**拦截器注册：**

- **addPathPatterns("/api/**")**：拦截所有以 `/api/` 开头的请求。我们的接口都以 `/api/` 开头，所以这里用通配符
- **excludePathPatterns**：排除不需要登录的公开接口。比如登录注册（`/api/auth/**`）、广场列表（`/api/square/list`）等，这些接口不登录也能访问

**跨域配置：**

- **allowedOriginPatterns("\*")**：允许所有来源的跨域请求。生产环境应该改成具体域名，如 `allowedOriginPatterns("https://www.example.com")`
- **allowCredentials(true)**：允许前端携带 Cookie 和 Authorization 头。如果设为 false，前端即使设了 Token 也不会发送
- **maxAge(3600)**：预检请求（OPTIONS）的结果缓存 1 小时，避免每次都发预检请求

**静态资源映射：**

- **file:uploads/**：映射到服务器上的 `uploads/` 目录，用户上传的头像、图片可以通过 `http://localhost:8080/uploads/xxx.jpg` 直接访问

---

## 第六步：完善 SecurityConfig

Day01 我们创建了一个临时放行所有请求的 SecurityConfig。现在 JWT 拦截器已经写好了，SecurityConfig 的职责更清晰——它只需要关闭 Spring Security 的默认行为，让所有请求都通过，认证交给我们的 `JwtInterceptor`。

### 创建文件

`src/main/java/com/buddy/config/SecurityConfig.java`

### 写入代码

```java
package com.buddy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置
 * 关闭 CSRF 和 Session，放行所有请求
 * 认证和鉴权由自定义 JwtInterceptor 处理
 *
 * 为什么不直接用 Spring Security 做认证？
 * 1. Spring Security 的过滤器链配置复杂，学习成本高
 * 2. 自定义拦截器更灵活，可以精确控制哪些接口需要登录
 * 3. @RequireAdmin 注解比 @PreAuthorize 更直观
 * 4. ThreadLocal 方式在 Service 层取用户 ID 更方便
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF（跨站请求伪造防护）
                // 前后端分离项目使用 JWT 认证，不需要 CSRF 防护
                .csrf().disable()
                // 关闭 Session，使用无状态方式
                // STATELESS 表示 Spring Security 不会创建或使用 HttpSession
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 放行所有请求
                // 认证逻辑由 JwtInterceptor 处理，Spring Security 只做安全头等基础功能
                .authorizeRequests()
                .anyRequest().permitAll()
                .and()
                // 禁用 frameOptions，允许 iframe 嵌入
                // 如果项目内嵌了 H2 控制台或其他 iframe 页面需要此配置
                .headers().frameOptions().disable();

        return http.build();
    }
}
```

### 解释

- **为什么要保留 SecurityConfig？** 虽然我们放行了所有请求，但 Spring Security 还提供了安全响应头（X-Content-Type-Options、X-XSS-Protection 等），保留它比完全移除更安全
- **SessionCreationPolicy.STATELESS**：告诉 Spring Security 不要创建 Session，节省服务器内存。这也是 JWT 无状态认证的核心——服务器不存储任何会话信息
- **SecurityConfig vs JwtInterceptor 的分工**：
  - `SecurityConfig`：关闭默认安全行为，放行所有请求
  - `JwtInterceptor`：自定义认证逻辑，精确控制哪些接口需要登录、哪些需要管理员权限

---

## 验证

### 1. 创建测试接口

在 `TestController` 中添加 JWT 测试接口：

#### 创建文件

`src/main/java/com/buddy/controller/user/TestController.java`

#### 写入代码

```java
package com.buddy.controller.user;

import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.context.UserContext;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.jwt.JwtUtils;
import com.buddy.common.result.R;
import com.buddy.common.result.ResultCode;
import com.buddy.entity.User;
import com.buddy.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器 - 验证 JWT 认证和拦截器
 * 开发完成后可以删除
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserMapper userMapper;

    // ==================== 以下接口不需要登录（在 WebMvcConfig 中排除了） ====================

    /**
     * 模拟登录，生成 Token
     * GET /api/auth/login?studentNo=admin001
     * 这个接口在 /api/auth/** 路径下，不需要 Token
     */
    @GetMapping("/auth/login")
    public R<Map<String, Object>> mockLogin(String studentNo) {
        // 查询用户
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getStudentNo, studentNo)
        );
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 生成 Token
        String token = jwtUtils.generateToken(user.getId(), user.getStudentNo());

        // 返回 Token 和用户信息
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("isAdmin", user.getIsAdmin());
        return R.ok(data);
    }

    // ==================== 以下接口需要登录（需要 Token） ====================

    /**
     * 测试：获取当前用户信息
     * GET /api/test/my-info
     * 需要在 Header 中携带 Token
     */
    @GetMapping("/my-info")
    public R<Map<String, Object>> testMyInfo() {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("nickname", user != null ? user.getNickname() : null);
        return R.ok(data);
    }

    /**
     * 测试：管理员接口
     * GET /api/test/admin-only
     * 需要 Token + 管理员权限
     */
    @RequireAdmin
    @GetMapping("/admin-only")
    public R<Void> testAdminOnly() {
        return R.ok();
    }
}
```

> ⚠️ 注意：`/api/test/auth/login` 路径需要加入 WebMvcConfig 的排除列表。修改 `WebMvcConfig` 的 `excludePathPatterns`，加入 `"/api/test/auth/**"`。

### 2. 修改 WebMvcConfig 排除列表

在 `WebMvcConfig.java` 的 `excludePathPatterns` 中添加测试接口的排除路径：

```java
.excludePathPatterns(
        "/api/auth/**",
        "/api/test/auth/**",          // ← 新增：测试登录接口
        "/api/square/list",
        "/api/post/detail/**",
        "/api/post/comments/**",
        "/api/review/list/**",
        "/api/review/user/**",
        "/api/admin/tag/list"
);
```

### 3. 启动项目

运行 `BuddyApplication.main()`，确认控制台没有报错。

### 4. 用 Postman 测试

**测试1：不带 Token 访问受保护接口 → 返回 401**

```
GET http://localhost:8080/api/test/my-info
```

返回（HTTP 状态码 401）：

```json
{
  "code": 401,
  "message": "未登录，请先登录",
  "data": null
}
```

**测试2：模拟登录获取 Token**

```
GET http://localhost:8080/api/test/auth/login?studentNo=admin001
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwic3R1ZGVudE5vIjoiYWRtaW4wMDEiLCJpYXQiOjE3MDYxMjM0NTYsImV4cCI6MTcwNjcyODI1Nn0.xxxxx",
    "userId": 1,
    "nickname": "系统管理员",
    "isAdmin": 1
  }
}
```

**测试3：带 Token 访问受保护接口 → 返回 200**

复制上一步返回的 Token，在请求头中添加：

```
GET http://localhost:8080/api/test/my-info
Header: Authorization: Bearer <上一步的token>
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "nickname": "系统管理员"
  }
}
```

**测试4：管理员 Token 访问管理员接口 → 返回 200**

```
GET http://localhost:8080/api/test/admin-only
Header: Authorization: Bearer <admin001的token>
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**测试5：普通用户 Token 访问管理员接口 → 返回 403**

先用普通学号登录获取 Token：

```
GET http://localhost:8080/api/test/auth/login?studentNo=20240001
```

然后用普通用户的 Token 访问管理员接口：

```
GET http://localhost:8080/api/test/admin-only
Header: Authorization: Bearer <20240001的token>
```

返回（HTTP 状态码 403）：

```json
{
  "code": 403,
  "message": "无管理员权限",
  "data": null
}
```

**测试6：伪造 Token → 返回 401**

```
GET http://localhost:8080/api/test/my-info
Header: Authorization: Bearer fake_token_12345
```

返回（HTTP 状态码 401）：

```json
{
  "code": 401,
  "message": "登录已过期，请重新登录",
  "data": null
}
```

---

## 总结：JWT 认证完整流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  前端     │     │ 拦截器    │     │ UserContext│    │ Controller│
│          │     │JwtInterceptor│  │(ThreadLocal)│  │ / Service │
└────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘
     │                │                │                │
     │ 1.发送请求      │                │                │
     │ Authorization: │                │                │
     │ Bearer <token> │                │                │
     │───────────────>│                │                │
     │                │                │                │
     │                │ 2.解析Token     │                │
     │                │ 得到userId      │                │
     │                │                │                │
     │                │ 3.存入UserContext│               │
     │                │───────────────>│                │
     │                │                │                │
     │                │ 4.放行请求      │                │
     │                │────────────────────────────────>│
     │                │                │                │
     │                │                │ 5.取userId     │
     │                │                │<───────────────│
     │                │                │                │
     │                │ 6.请求完成，清除UserContext       │
     │                │<───────────────│                │
     │                │                │                │
     │ 7.返回响应      │                │                │
     │<───────────────│                │                │
```

**核心类职责一览：**

| 类 | 职责 | 一句话总结 |
|----|------|-----------|
| `JwtUtils` | 生成/解析/校验 Token | Token 的"工厂"和"检验员" |
| `UserContext` | ThreadLocal 存储当前用户 ID | 请求级别的"全局变量" |
| `JwtInterceptor` | 拦截请求，校验 Token，检查权限 | 认证的"守门员" |
| `@RequireAdmin` | 标记需要管理员权限的方法 | 权限的"标签" |
| `WebMvcConfig` | 注册拦截器、配置跨域 | 拦截器的"调度中心" |
| `SecurityConfig` | 关闭 Spring Security 默认行为 | Security 的"关闭开关" |

---

**恭喜！Day03 完成！** JWT 认证体系已经搭建完毕，接口有了真正的安全保障。后续的 Controller 和 Service 都可以直接用 `UserContext.getUserId()` 获取当前登录用户，非常方便。
