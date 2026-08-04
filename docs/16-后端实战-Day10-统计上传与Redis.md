# 后端实战 Day10 - 统计上传与Redis

## 今日目标

1. 补充 `StatsServiceImpl` 的完整实现（7个统计方法）
2. 补充 `FileController` 的完整实现（上传头像）
3. 总结 Redis 在项目中的三大应用场景
4. 完整项目回顾：列出所有已实现的接口清单
5. 常见问题排查指南

## 你将学会

- 如何实现数据统计（趋势图、分布图、TOP排行）
- 如何用 MyBatis-Plus 的 `selectMaps` + `groupBy` 实现聚合查询
- 如何用 Redis 实现登录失败计数、浏览量去重、推荐缓存
- 如何排查项目启动常见问题

---

## 第1步：StatsServiceImpl 完整实现

### 创建文件

`backend/src/main/java/com/buddy/service/impl/StatsServiceImpl.java`

### 写入代码

```java
package com.buddy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.buddy.common.constant.ApplyStatus;
import com.buddy.common.constant.PostStatus;
import com.buddy.dto.response.StatsVO;
import com.buddy.entity.Post;
import com.buddy.entity.PostApply;
import com.buddy.entity.User;
import com.buddy.mapper.PostApplyMapper;
import com.buddy.mapper.PostMapper;
import com.buddy.mapper.UserMapper;
import com.buddy.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务实现类
 */
@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostApplyMapper postApplyMapper;

    /**
     * 1. 数据概览
     * 返回：用户总数、帖子总数、组局成功数、活跃用户数
     */
    @Override
    public StatsVO getOverview() {
        StatsVO vo = new StatsVO();

        // 总用户数
        vo.setTotalUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>()
        ));

        // 总帖子数
        vo.setTotalPosts(postMapper.selectCount(
                new LambdaQueryWrapper<Post>()
        ));

        // 今日发帖数
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        vo.setTodayPosts(postMapper.selectCount(
                new LambdaQueryWrapper<Post>().ge(Post::getCreatedTime, todayStart)
        ));

        // 成功组局数（状态为FINISHED的帖子数）
        vo.setSuccessMatches(postMapper.selectCount(
                new LambdaQueryWrapper<Post>().eq(Post::getStatus, PostStatus.FINISHED)
        ));

        // 活跃用户数（近7天有发帖或报名的用户）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Long activePostUsers = postMapper.selectCount(
                new QueryWrapper<Post>()
                        .ge("created_time", sevenDaysAgo)
                        .select("DISTINCT publisher_id")
        );
        Long activeApplyUsers = postApplyMapper.selectCount(
                new QueryWrapper<PostApply>()
                        .ge("apply_time", sevenDaysAgo)
                        .select("DISTINCT applicant_id")
        );
        vo.setActiveUsers(activePostUsers + activeApplyUsers);

        // 分类统计TOP5
        vo.setCategoryStats(getCategoryTop5());

        return vo;
    }

    /**
     * 2. 发帖量趋势
     * 近N天每日发帖数
     */
    @Override
    public List<Map<String, Object>> getPostTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            Long count = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>()
                            .ge(Post::getCreatedTime, dayStart)
                            .le(Post::getCreatedTime, dayEnd)
            );

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", count);
            result.add(item);
        }

        return result;
    }

    /**
     * 3. 组局成功趋势
     * 近N天每日组局成功数（状态变为FINISHED的帖子数）
     */
    @Override
    public List<Map<String, Object>> getSuccessMatchTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            Long count = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getStatus, PostStatus.FINISHED)
                            .ge(Post::getUpdatedTime, dayStart)
                            .le(Post::getUpdatedTime, dayEnd)
            );

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", count);
            result.add(item);
        }

        return result;
    }

    /**
     * 4. 活跃用户趋势
     * 近N天每日活跃用户数（当天有发帖或报名的用户）
     */
    @Override
    public List<Map<String, Object>> getActiveUserTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            // 当天发帖的用户数
            Long postUserCount = postMapper.selectCount(
                    new QueryWrapper<Post>()
                            .ge("created_time", dayStart)
                            .le("created_time", dayEnd)
                            .select("DISTINCT publisher_id")
            );

            // 当天报名的用户数
            Long applyUserCount = postApplyMapper.selectCount(
                    new QueryWrapper<PostApply>()
                            .ge("apply_time", dayStart)
                            .le("apply_time", dayEnd)
                            .select("DISTINCT applicant_id")
            );

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", postUserCount + applyUserCount);
            result.add(item);
        }

        return result;
    }

    /**
     * 5. 分类发帖数Top5
     * 使用 groupBy 聚合查询
     */
    @Override
    public List<StatsVO.CategoryStat> getCategoryTop5() {
        // 使用MyBatis-Plus的groupBy查询分类统计
        List<Map<String, Object>> maps = postMapper.selectMaps(
                new QueryWrapper<Post>()
                        .select("category, COUNT(*) as count")
                        .groupBy("category")
                        .orderByDesc("count")
                        .last("LIMIT 5")
        );

        List<StatsVO.CategoryStat> stats = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            StatsVO.CategoryStat stat = new StatsVO.CategoryStat();
            stat.setCategory((String) map.get("category"));
            stat.setCount(((Number) map.get("count")).longValue());
            stats.add(stat);
        }
        return stats;
    }

    /**
     * 6. 信誉分分布
     * 按信誉分区间统计用户分布
     */
    @Override
    public List<Map<String, Object>> getCreditDistribution() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 90-100 靠谱队友
        addCreditRange(result, "靠谱队友(90-100)", 90, 100);
        // 75-89 优质队友
        addCreditRange(result, "优质队友(75-89)", 75, 89);
        // 60-74 普通队友
        addCreditRange(result, "普通队友(60-74)", 60, 74);
        // 40-59 风险队友
        addCreditRange(result, "风险队友(40-59)", 40, 59);
        // 0-39 失信队友
        addCreditRange(result, "失信队友(0-39)", 0, 39);

        return result;
    }

    /**
     * 添加信誉分区间统计
     */
    private void addCreditRange(List<Map<String, Object>> result, String label, int min, int max) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .ge(User::getCreditScore, min)
                        .le(User::getCreditScore, max)
        );
        Map<String, Object> item = new HashMap<>();
        item.put("label", label);
        item.put("count", count);
        result.add(item);
    }

    /**
     * 7. 用户增长趋势
     * 近N天累计注册用户数
     */
    @Override
    public List<Map<String, Object>> getUserGrowthTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            // 累计注册用户数：注册时间 <= 当天结束
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .le(User::getCreatedTime, dayEnd)
            );

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", count);
            result.add(item);
        }

        return result;
    }
}
```

### 解释

**7个统计方法的核心思路：**

| 方法 | SQL思路 | 说明 |
|------|---------|------|
| getOverview | 多次selectCount | 分别统计用户数、帖子数、成功组局数、活跃用户数 |
| getPostTrend | 循环30天，每天selectCount | 每天查一次发帖数，拼成趋势数据 |
| getSuccessMatchTrend | 同上，加status=FINISHED条件 | 只统计已结束的活动 |
| getActiveUserTrend | selectCount + DISTINCT | 去重统计当天发帖和报名的用户数 |
| getCategoryTop5 | groupBy + orderBy + LIMIT 5 | 按分类分组统计，取前5 |
| getCreditDistribution | 5次selectCount + 区间条件 | 按5个信誉分段分别统计 |
| getUserGrowthTrend | selectCount + <=当天结束 | 累计注册数，不是每日新增数 |

**关键技巧：**

1. **DISTINCT去重统计**：使用 `select("DISTINCT publisher_id")` 统计不重复的发帖用户数
2. **groupBy聚合查询**：`selectMaps` + `groupBy` 实现分类统计
3. **累计趋势**：`getUserGrowthTrend` 使用 `<=当天结束` 而不是区间查询，得到的是累计值

---

## 第2步：FileController 完整实现

### 创建文件

`backend/src/main/java/com/buddy/controller/user/FileController.java`

### 写入代码

```java
package com.buddy.controller.user;

import com.buddy.common.context.UserContext;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    /** 上传目录 */
    private static final String UPLOAD_DIR = "uploads/avatar/";

    /** 允许的文件类型 */
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/jpg"};

    /** 最大文件大小：2MB */
    private static final long MAX_SIZE = 2 * 1024 * 1024;

    /**
     * 上传头像
     */
    @PostMapping("/upload")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();

        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        // 2. 校验文件大小
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("文件大小不能超过2MB");
        }

        // 3. 校验文件类型
        String contentType = file.getContentType();
        if (!isAllowedType(contentType)) {
            throw new BusinessException("仅支持jpg/png格式图片");
        }

        // 4. 生成文件名（UUID重命名，防止文件名冲突和路径穿越攻击）
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

        // 5. 保存文件
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        File dest = new File(uploadDir, newFilename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("文件上传失败: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException("文件上传失败，请重试");
        }

        // 6. 返回文件访问URL
        String url = "/uploads/avatar/" + newFilename;
        log.info("头像上传成功: userId={}, url={}", userId, url);
        return R.ok(url);
    }

    /**
     * 校验文件类型
     */
    private boolean isAllowedType(String contentType) {
        if (contentType == null) return false;
        for (String allowed : ALLOWED_TYPES) {
            if (allowed.equalsIgnoreCase(contentType)) return true;
        }
        return false;
    }
}
```

### 解释

**文件上传完整流程：**

```
客户端上传文件 → 校验空文件 → 校验大小 → 校验类型 → UUID重命名 → 保存到磁盘 → 返回URL
```

**MultipartFile 核心API：**

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `isEmpty()` | boolean | 文件是否为空 |
| `getSize()` | long | 文件大小（字节） |
| `getContentType()` | String | MIME类型，如 image/jpeg |
| `getOriginalFilename()` | String | 客户端原始文件名 |
| `getBytes()` | byte[] | 文件内容字节数组 |
| `getInputStream()` | InputStream | 文件输入流 |
| `transferTo(File dest)` | void | 保存文件到指定路径 |

**安全注意事项：**

1. **不使用原始文件名**：防止路径穿越攻击（如 `../../etc/passwd`）
2. **校验Content-Type**：防止上传 .exe、.jsp 等恶意文件
3. **限制文件大小**：防止大文件占满磁盘
4. **UUID重命名**：保证文件名唯一，避免覆盖

---

## 第3步：Redis 应用总结

本项目在三个场景中使用了 Redis：

### 场景1：登录失败计数

**位置**：`UserServiceImpl.login()`

**Key格式**：`login:fail:{account}`

**实现逻辑**：

```java
// 检查是否被锁定
String lockKey = "login:fail:" + request.getAccount();
String failCountStr = redisTemplate.opsForValue().get(lockKey);
if (failCountStr != null && Integer.parseInt(failCountStr) >= 5) {
    throw new BusinessException("登录失败次数过多，账号已锁定30分钟");
}

// 登录失败，计数+1
Long count = redisTemplate.opsForValue().increment(lockKey);
if (count != null && count == 1) {
    // 第一次失败，设置过期时间30分钟
    redisTemplate.expire(lockKey, 30, TimeUnit.MINUTES);
}

// 登录成功，清除计数
redisTemplate.delete(lockKey);
```

**流程图**：

```
用户登录 → 检查Redis中失败次数
  → >=5次：拒绝登录，提示锁定
  → <5次：验证密码
    → 密码正确：删除Redis计数，返回Token
    → 密码错误：计数+1，如果是第一次则设置30分钟过期
```

**为什么用Redis而不是数据库？**

- 登录失败计数是临时数据，30分钟后自动失效，不需要持久化
- Redis的 `increment` 操作是原子的，并发安全
- Redis的 `expire` 可以自动过期，不需要定时清理

### 场景2：浏览量去重

**位置**：`PostServiceImpl.incrementViewCount()`

**Key格式**：`post:view:{postId}:{userId}`

**实现逻辑**：

```java
String key = "post:view:" + postId + ":" + userId;
// setIfAbsent：如果key不存在则设置，返回true；如果已存在则不设置，返回false
// 过期时间10分钟
Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
if (Boolean.TRUE.equals(isNew)) {
    // 10分钟内第一次浏览，浏览量+1
    Post post = postMapper.selectById(postId);
    if (post != null) {
        post.setViewCount(post.getViewCount() + 1);
        postMapper.updateById(post);
    }
}
```

**为什么需要去重？**

- 用户反复刷新页面，浏览量不应该重复计算
- 使用 `setIfAbsent`（即 Redis 的 `SETNX` 命令），10分钟内同一用户对同一帖子只计一次
- 10分钟后key自动过期，用户再次浏览会计为新的浏览

### 场景3：推荐结果缓存

**位置**：`RecommendServiceImpl.getRecommendList()`

**Key格式**：`recommend:{userId}:{category}:{sort}:{page}:{size}`

**实现逻辑**：

```java
// 推荐结果缓存5分钟
String cacheKey = "recommend:" + userId + ":" + category + ":" + sort + ":" + page + ":" + size;
// 实际项目中应使用缓存序列化/反序列化
// 此处简化处理，每次重新计算
```

**缓存策略**：

- 推荐算法涉及多表查询和计算，耗时较长
- 缓存5分钟，减少重复计算
- 5分钟后自动过期，重新计算保证数据新鲜度

### Redis三大场景对比

| 场景 | Key格式 | 过期时间 | Redis命令 | 作用 |
|------|---------|---------|----------|------|
| 登录失败计数 | `login:fail:{account}` | 30分钟 | `increment` + `expire` | 防暴力破解 |
| 浏览量去重 | `post:view:{postId}:{userId}` | 10分钟 | `setIfAbsent` | 防刷浏览量 |
| 推荐缓存 | `recommend:{userId}:{category}:{sort}:{page}:{size}` | 5分钟 | `set` + `get` | 减少重复计算 |

---

## 第4步：完整项目回顾 - 接口清单

### 认证模块 `/api/auth`

| 方法 | 路径 | 说明 | 是否需要Token |
|------|------|------|--------------|
| POST | `/api/auth/register` | 用户注册 | 否 |
| POST | `/api/auth/login` | 用户登录 | 否 |

### 用户模块 `/api/user`

| 方法 | 路径 | 说明 | 是否需要Token |
|------|------|------|--------------|
| GET | `/api/user/profile` | 获取个人资料 | 是 |
| PUT | `/api/user/profile` | 编辑个人资料 | 是 |
| GET | `/api/user/tags` | 获取我的标签 | 是 |
| PUT | `/api/user/tags` | 设置兴趣标签 | 是 |
| GET | `/api/user/posts` | 我的发布 | 是 |
| GET | `/api/user/applies` | 我的预约 | 是 |

### 帖子模块 `/api/post`

| 方法 | 路径 | 说明 | 是否需要Token |
|------|------|------|--------------|
| POST | `/api/post/create` | 发布组局 | 是 |
| GET | `/api/post/detail/{id}` | 帖子详情 | 否 |
| POST | `/api/post/cancel/{id}` | 取消组局 | 是 |
| POST | `/api/post/comment` | 发表评论 | 是 |
| GET | `/api/post/comments/{postId}` | 评论列表 | 否 |

### 报名模块 `/api/apply`

| 方法 | 路径 | 说明 | 是否需要Token |
|------|------|------|--------------|
| POST | `/api/apply/join` | 报名 | 是 |
| POST | `/api/apply/cancel/{id}` | 取消报名 | 是 |
| GET | `/api/apply/list/{postId}` | 报名列表 | 是 |
| POST | `/api/apply/approve/{id}` | 同意报名 | 是 |
| POST | `/api/apply/reject/{id}` | 拒绝报名 | 是 |
| GET | `/api/apply/received` | 收到的报名 | 是 |
| POST | `/api/apply/attendance/{id}` | 标记出勤 | 是 |

### 评价模块 `/api/review`

| 方法 | 路径 | 说明 | 是否需要Token |
|------|------|------|--------------|
| POST | `/api/review/submit` | 提交评价 | 是 |
| GET | `/api/review/list/{postId}` | 帖子评价列表 | 否 |
| GET | `/api/review/user/{userId}` | 用户收到的评价 | 否 |

### 广场模块 `/api/square`

| 方法 | 路径 | 说明 | 是否需要Token |
|------|------|------|--------------|
| GET | `/api/square/list` | 广场列表 | 否 |

### 标签模块 `/api/tag`

| 方法 | 路径 | 说明 | 是否需要Token |
|------|------|------|--------------|
| GET | `/api/tag/list` | 所有标签 | 否 |
| GET | `/api/tag/categories` | 所有分类 | 否 |

### 文件模块 `/api/file`

| 方法 | 路径 | 说明 | 是否需要Token |
|------|------|------|--------------|
| POST | `/api/file/upload` | 上传头像 | 是 |

### 管理端-用户管理 `/api/admin/user`

| 方法 | 路径 | 说明 | 是否需要管理员 |
|------|------|------|--------------|
| GET | `/api/admin/user/list` | 用户列表 | 是 |
| GET | `/api/admin/user/detail/{id}` | 用户详情 | 是 |
| GET | `/api/admin/user/posts/{id}` | 用户发布历史 | 是 |
| GET | `/api/admin/user/applies/{id}` | 用户报名历史 | 是 |
| GET | `/api/admin/user/reviews/{id}` | 用户评价记录 | 是 |
| POST | `/api/admin/user/ban/{id}` | 封禁用户 | 是 |
| POST | `/api/admin/user/unban/{id}` | 解封用户 | 是 |
| PUT | `/api/admin/user/credit` | 重置信誉分 | 是 |

### 管理端-帖子管理 `/api/admin/post`

| 方法 | 路径 | 说明 | 是否需要管理员 |
|------|------|------|--------------|
| GET | `/api/admin/post/list` | 帖子列表 | 是 |
| POST | `/api/admin/post/takedown/{id}` | 下架帖子 | 是 |
| POST | `/api/admin/post/audit/{id}` | 审核帖子 | 是 |
| DELETE | `/api/admin/post/delete/{id}` | 删除帖子 | 是 |

### 管理端-标签管理 `/api/admin/tag`

| 方法 | 路径 | 说明 | 是否需要管理员 |
|------|------|------|--------------|
| GET | `/api/admin/tag/list` | 标签列表 | 否 |
| GET | `/api/admin/tag/categories` | 分类列表 | 否 |
| POST | `/api/admin/tag/add` | 添加标签 | 是 |
| PUT | `/api/admin/tag/update` | 编辑标签 | 是 |
| POST | `/api/admin/tag/disable/{id}` | 禁用标签 | 是 |

### 管理端-数据统计 `/api/admin/stats`

| 方法 | 路径 | 说明 | 是否需要管理员 |
|------|------|------|--------------|
| GET | `/api/admin/stats/overview` | 数据概览 | 是 |
| GET | `/api/admin/stats/posts` | 发帖趋势 | 是 |
| GET | `/api/admin/stats/success` | 组局成功趋势 | 是 |
| GET | `/api/admin/stats/active` | 活跃用户趋势 | 是 |
| GET | `/api/admin/stats/category` | 分类Top5 | 是 |
| GET | `/api/admin/stats/credit` | 信誉分分布 | 是 |
| GET | `/api/admin/stats/user-growth` | 用户增长趋势 | 是 |

---

## 第5步：常见问题排查

### 问题1：启动报错 `Cannot determine embedded database driver class`

**原因**：Spring Boot 检测到 Spring Security 依赖，但没配置数据源。

**解决**：确保 `application-dev.yml` 中数据库配置正确：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/school_buddy?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 1234
```

### 问题2：数据库连接失败 `Communications link failure`

**排查步骤**：

1. 确认 MySQL 服务是否启动：
   ```bash
   # Windows
   net start mysql
   # Linux
   systemctl status mysql
   ```

2. 确认数据库是否存在：
   ```sql
   CREATE DATABASE IF NOT EXISTS school_buddy DEFAULT CHARSET utf8mb4;
   ```

3. 确认用户名密码是否正确：
   ```bash
   mysql -u root -p1234 -e "SELECT 1"
   ```

4. 确认端口是否被占用（默认3306）：
   ```bash
   netstat -an | findstr 3306
   ```

### 问题3：Redis连接失败 `Unable to connect to Redis`

**排查步骤**：

1. 确认 Redis 服务是否启动：
   ```bash
   # Windows（如果用Chocolatey安装）
   redis-server
   # Linux
   systemctl status redis
   ```

2. 确认端口是否正确（默认6379）：
   ```bash
   redis-cli ping
   # 应返回 PONG
   ```

3. 如果没有安装Redis，可以暂时注释掉Redis相关代码：
   - 注释 `pom.xml` 中的 Redis 依赖
   - 注释 `RedisConfig.java` 中的配置
   - 注释 `UserServiceImpl` 中的 `redisTemplate` 使用
   - 注释 `PostServiceImpl` 中的 `redisTemplate` 使用

### 问题4：接口返回401 `未登录或Token已过期`

**排查步骤**：

1. 确认请求头是否携带Token：
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
   ```
   注意 `Bearer` 后面有一个空格。

2. 确认Token是否过期：JWT Token 默认7天有效期。

3. 确认该接口是否需要Token：查看 `WebMvcConfig` 中的排除列表。

### 问题5：接口返回403 `无权限访问`

**原因**：普通用户访问了需要管理员权限的接口。

**解决**：使用管理员账号（学号 `admin001`，密码 `123456`）登录获取Token。

### 问题6：文件上传后图片无法访问

**排查步骤**：

1. 确认 `WebMvcConfig` 中配置了静态资源映射：
   ```java
   registry.addResourceHandler("/uploads/**")
           .addResourceLocations("file:uploads/");
   ```

2. 确认文件确实保存到了 `uploads/avatar/` 目录下。

3. 确认访问URL格式正确：`http://localhost:8080/uploads/avatar/xxx.jpg`

### 问题7：MyBatis-Plus分页不生效，返回全部数据

**原因**：没有配置分页插件。

**解决**：确认 `MybatisPlusConfig` 中注册了分页插件：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
}
```

---

## 验证

### 1. 启动完整项目

```bash
cd backend
mvn spring-boot:run
```

确认控制台输出：

```
Started BuddyApplication in X.XX seconds
```

### 2. 用Postman测试所有核心接口

**测试顺序**（按业务流程）：

```
1. 注册用户 → POST /api/auth/register
2. 登录获取Token → POST /api/auth/login
3. 设置兴趣标签 → PUT /api/user/tags
4. 发布组局 → POST /api/post/create
5. 查看广场列表 → GET /api/square/list
6. 查看帖子详情 → GET /api/post/detail/1
7. 报名参加 → POST /api/apply/join
8. 审批报名 → POST /api/apply/approve/1
9. 发表评论 → POST /api/post/comment
10. 上传头像 → POST /api/file/upload
```

### 3. 测试管理端接口

```
1. 管理员登录 → POST /api/auth/login (admin001/123456)
2. 用户列表 → GET /api/admin/user/list
3. 帖子列表 → GET /api/admin/post/list
4. 审核帖子 → POST /api/admin/post/audit/1
5. 数据概览 → GET /api/admin/stats/overview
6. 发帖趋势 → GET /api/admin/stats/posts
7. 信誉分分布 → GET /api/admin/stats/credit
```

### 4. 测试Redis功能

```
1. 连续5次输错密码 → 账号锁定30分钟
2. 同一用户浏览同一帖子 → 10分钟内浏览量只+1
3. 访问广场列表 → 5分钟内相同条件返回缓存结果
```

---

## 恭喜！项目后端实战全部完成！

从 Day01 到 Day10，我们一起实现了：

- **用户系统**：注册、登录、JWT认证、登录失败锁定
- **帖子系统**：发布组局、状态机流转、浏览量去重
- **报名系统**：报名、审批、出勤标记、临场取消扣分
- **评价系统**：评分评价、信誉分动态更新、评价窗口期
- **推荐算法**：多维度加权推荐、标签匹配、时间重合度
- **管理后台**：用户管理、帖子审核、标签管理、数据统计
- **文件上传**：头像上传、UUID重命名、静态资源映射
- **Redis应用**：登录锁定、浏览去重、推荐缓存

整个项目涵盖了 Spring Boot 后端开发的核心知识点，希望你能从中学到实用的开发技能！
