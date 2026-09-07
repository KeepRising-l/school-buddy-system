# 技术设计 TECH-V1

> **状态：✅ 已冻结（TECH-V1，冻结时间 2026-09-07，用户显式确认）**
>
> 冻结依据：本文档（原 TECH-DRAFT）+ 用户确认。Demo 验证作为模块四首个工作单元执行，不阻塞本基线冻结。
>
> 依据：《现状说明 V1》+ 需求草稿 + 用户决策 D-002（业务模块分包/整体重写）、D-003（后端升级 Java 17/21 + Boot 3.x）、D-004（功能不变）、D-005（整体重写）。
>
> 原则：**行为等价迁移**——API 路径、数据库结构、业务规则与旧版保持一致；变化仅在代码组织、框架版本与旧版已确认的问题修复上。

---

# 1. 项目技术概述

- 项目目标：同需求草稿（校园兴趣搭子预约系统）；
- 技术边界：单体应用，无微服务、无消息队列、无第三方云服务；
- 技术栈：
  - 后端：**Java 17 + Spring Boot 3.3.x** + MyBatis-Plus 3.5.5+（Boot3 starter）+ MySQL 8 + Redis（Lettuce）+ jjwt 0.12.x + Hutool 5.8 + Lombok + Validation；Maven；
  - 前端 H5：Vue 3 + Vite 5 + Vue Router 4 + Pinia + Axios + Vant 4（不变）；
  - 前端管理端：Vue 3 + Vite 5 + Element Plus 2.6 + ECharts 5（不变）；
- 总体方案：按业务模块重组后端代码，整体重写（旧代码为行为基准），行为与旧版等价。

# 2. 系统架构

```text
frontend-h5 (Vue3+Vant4)  ──┐
                            ├──> backend (Spring Boot 3, :8080, /api/**)
frontend-admin (Element+) ──┘         │
                                      ├── MySQL 8 (school_buddy, 12 表)
                                      └── Redis (推荐缓存/验证类用途)
```

- 认证：JWT（Authorization: Bearer）+ 自定义拦截器（Spring Security 仅做 CSRF/Session 关闭，行为同旧版）；
- 定时任务：Spring @Scheduled（帖子状态推进、每日信誉重算、活动提醒）；
- 文件：头像存本地 uploads/avatar/（与旧版一致；云存储为后续可选项，不进本期）。

# 3. 核心业务技术映射

| 业务对象/规则 | 技术承载 |
|---|---|
| 用户/信誉分 | sys_user.credit_score + credit_log 台账 + CreditService 重算 |
| 帖子生命周期 | post.status + PostStateMachine 校验器 + PostStatusTask 定时推进 |
| 报名闭环 | post_apply 状态机 + 条件更新（乐观并发）+ 原子人数 SQL |
| 候补递补 | WAITLIST FIFO 查询 + promote 事务 |
| 评价→信誉 | review 标签映射常量 + 每日重算任务 |
| 推荐排序 | 内存加权打分 + 手动分页（与旧版一致）|
| 权限 | JwtInterceptor（可选登录）+ @RequireAdmin 注解 |

# 4. 数据模型

**12 张表结构与旧版完全一致**（schema.sql 原样迁移，仅修正注释编号错乱）：

sys_user、interest_tag、user_tag、post、post_apply、review、post_comment、notification、favorite、report、credit_log、sys_announcement。

- 关键状态字段：post.status（5 态字符串）、post.is_audit_pass（0/1/2）、post_apply.status（7 态字符串）、report.status（0/1/2）、user.status（0/1）；
- 约束：学号/用户名/手机号唯一、(post_id,applicant_id) 唯一、(post_id,reviewer_id,target_user_id) 唯一、(user_id,post_id) 收藏唯一；
- 删除行为：【用户确认】帖子删除改为**逻辑删除**：post、post_comment、post_apply、review、favorite 增加 `deleted` 标记字段（TINYINT 默认 0，MyBatis-Plus 全局 @TableLogic），删除帖子时级联逻辑删除其评论/报名/评价/收藏；其余表不变；
- 无快照需求（本产品无订单快照场景）。

# 5. 后端设计（本次重构核心变化）

## 5.1 包结构：按业务功能模块划分（D-002）

```text
com.buddy
├── BuddyApplication.java
├── common/               # 跨模块通用：R/ResultCode、BusinessException/全局异常、
│                         # JwtUtils、UserContext、RequireAdmin、通用常量、PostStateMachine
├── config/               # 全局配置：SecurityConfig、RedisConfig、MybatisPlusConfig、WebMvcConfig
├── interceptor/          # JwtInterceptor
└── module/
    ├── auth/             # 注册/登录        AuthController, AuthService, dto...
    ├── user/             # 用户资料/标签/隐私/他人主页（web/ 用户端 + admin/ 管理端用户管理）
    ├── post/             # 帖子/评论/状态机流转/PostStatusTask（admin/ 下架删除）
    ├── apply/            # 报名/候补/审批/出勤
    ├── review/           # 互评与回复
    ├── credit/           # 信誉分台账/重算/等级/CreditCalculateTask
    ├── square/           # 广场列表与推荐算法/ActivityRemindTask(或归 notification)
    ├── favorite/         # 收藏
    ├── notification/     # 通知中心
    ├── report/           # 举报（web/ 用户侧 + admin/ 管理端处理）
    ├── tag/              # 兴趣标签（web/ 查询 + admin/ 维护）
    ├── announcement/     # 公告（web/ 弹窗拉取 + admin/ 管理）
    ├── file/             # 头像上传
    └── admin/            # 仅跨模块统计聚合与导出（AdminStats）
```

- 每个模块内部：`controller/`（或 web/）、`admin/`（管理端 Controller，与用户端在模块内显式区分）、`service/`、`entity/`、`mapper/`、`dto/`（按需）、`task/`（按需）；
- 管理端接口归属：【用户确认】按功能真正划分——管理端 Controller 分散进各业务模块，与用户端在同一模块内区分（如 module/user/admin/AdminUserController）；仅跨模块的统计聚合留在 module/admin；
- 模块间调用只走 Service 接口，不跨模块摸 Mapper；
- 不做完整 DDD：无 domain/聚合根/仓储层抽象，保持三层（controller-service-mapper）。

## 5.2 核心业务服务（与旧版等价迁移）

PostService（发布/详情/取消/评论/状态流转）、ApplyService（报名/候补/审批/出勤）、CreditService（台账+重算+等级）、RecommendService（打分排序）、NotificationService（7 类通知发送，去除审核类通知，D-011）、StatsService（统计导出）、SensitiveWordService（敏感词）等。

## 5.3 权限 / 状态 / 异常

- 权限：JwtInterceptor（可选登录、封禁校验、@RequireAdmin）；Spring Security 迁移至 Boot 3 lambda DSL（行为不变：CSRF off、无状态、全放行）；
- 状态：PostStateMachine 校验器原样迁移；
- 异常：BusinessException + GlobalExceptionHandler + ResultCode 原样迁移。

## 5.4 Boot 3 升级要点（风险集中区）

- javax.* → jakarta.*（Servlet、Validation）；
- spring-boot-starter-security 配置写法迁移（SecurityFilterChain lambda DSL）；
- jjwt 0.11 → 0.12 API 变化（parserBuilder → Jwts.parser() 等）；
- MyBatis-Plus 换用 mybatis-plus-spring-boot3-starter；
- Java 8 语法可顺手升级（record/var 按需，不强求）。

# 6. API 设计

**路径、方法、请求/响应结构与旧版保持一致**（前端零改动目标）：

- 用户端：/api/auth/**、/api/post/**、/api/square/list、/api/apply/**、/api/review/**、/api/user/**、/api/favorite/**、/api/notification/**、/api/report/**、/api/tag/**、/api/announcement/**、/api/file/upload；
- 管理端：/api/admin/user/**、/api/admin/post/**（下架/删除；人工审核接口取消）、/api/admin/tag/**、/api/admin/report/**、/api/admin/announcement/**、/api/admin/stats/**；
- 统一返回 R{code,message,data}；401/403 语义与旧版一致；
- 完整接口清单见《现状说明 V1》2.3 节（60+ 接口，逐条等价迁移）。

# 7. 前端设计

- 两个前端**基本不动**（D-004 功能不变）：
  - H5：15 路由、TabbarLayout、PostCard 等组件、pinia user store、request.js 拦截器（h5_token）；
  - 管理端：8 路由、Layout/Sidebar/Header、admin store、download.js 导出（admin_token）；
- 仅在联调发现依赖旧版行为差异时做最小修正；
- 前端 API 层不重排，保持现有组织不动（D-012 用户确认：不重排，前端零改动）。

# 8. 关键业务规则技术实现（等价迁移清单）

1. 资格三重校验（信誉/段位/性别）→ ApplyService.validateQualification；
2. 条件更新防并发（审批/取消/出勤）→ LambdaUpdateWrapper 带状态条件；
3. 原子人数增减 → PostMapper.incrementCurrentPeople/decrementCurrentPeople（SQL 带上限条件防超卖）；
4. 候补递补 → cancelApply 事务内 promoteWaitlist；
5. 临场取消扣分 → [start-1h, start) 窗口判断 + recordPenalty；
6. 出勤 24h 窗口 → end_time+24h 判断；
7. 信誉公式 → 100 + Σ评价影响 + Σcredit_log，clamp [0,100]；
8. 推荐公式 → 0.4/0.3/0.2/0.1 加权 + 低信誉 ×0.3 + 游客简化公式；
9. 审核简化 → 发布/评论时敏感词拦截（SensitiveWordService，现状已有），命中拒绝；发布成功即可见（无待审核态）；管理员下架标记用于广场过滤与定时任务过滤；
10. 标签脏数据容错 → parseTagIds try-catch 跳过。

# 9. 全场景覆盖矩阵

| 产品场景 | 技术支持 | 状态 |
|---|---|---|
| 游客浏览/搜索/筛选 | SquareController + RecommendService | ✅ 设计覆盖 |
| 注册登录 | AuthController + JwtUtils + JwtInterceptor | ✅ |
| 发帖→审批→满员→候补→递补 | Post/Apply 模块 + 状态机 + 条件更新 | ✅ |
| 出勤→互评→信誉联动 | Apply/Review/Credit + 定时任务 | ✅ |
| 管理端管理+统计导出 | Admin 模块 + StatsService + download.js | ✅ |
| 通知触达 | NotificationService（同事务发送） | ✅ |
| 头像上传 | FileController（魔数校验） | ✅ |

# 10. 技术风险

| 风险 | 影响 | 解决 | 验证 |
|---|---|---|---|
| Boot 2→3 迁移编译/运行断裂 | 启动失败 | 按 5.4 清单逐项处理，先跑通空壳再迁业务 | 启动 + 冒烟接口 |
| 行为等价遗漏（旧版细节规则多） | 功能回退 | 以《现状说明》23 条规则为核对清单，逐条迁移自测 | 按规则逐条验证 |
| jjwt 0.12 API 变化 | Token 不兼容 | 旧 Token 不迁移（7 天有效期，重写后重新登录即可） | 登录/鉴权用例 |
| 前端隐性依赖旧版响应细节 | 页面异常 | 保持响应结构不变；联调逐页验证 | 前后端联调 |
| MyBatis-Plus Boot3 starter 兼容 | Mapper 失效 | 升级 starter 版本并验证分页插件 | 分页接口 |

# 11. Demo 验证

- 验证目标：Boot 3 + Java 17 环境下，完成 auth + post 两个模块的最小闭环（注册/登录/发帖/详情），证明技术路径可行；
- 验证范围：编译 → 启动 → 连接 MySQL/Redis → 冒烟接口；
- 状态：**待执行**（已列为模块四首个工作单元，冻结后立即执行：Boot3 空壳编译启动 + 连接 MySQL/Redis + 登录冒烟）

# 12. 技术决策记录

| 决策 | 选择 | 理由 | 来源 |
|---|---|---|---|
| 包组织 | 业务模块分包 | 用户明确诉求（D-002） | 用户确认 |
| 路线 | 整体重写 | 包重组+升级使渐进无优势（D-005） | 用户确认 |
| JDK 版本 | Java 17 | LTS，生态最稳 | 用户确认 |
| 前端 | 不动 | 功能不变（D-004） | 用户确认 |
| 删除行为 | 逻辑删除+级联（post/comment/apply/review/favorite 加 deleted） | 可追溯，毕设好讲 | 用户确认 |
| 管理端接口归属 | 分散到各业务模块，模块内 admin/ 子包与用户端区分 | 按功能找代码 | 用户确认 |
| 审核模式 | 敏感词校验+发布即可见，去人工审核，保留下架 | 毕设从简 | 用户确认 |
| 取消组局 | 已通过报名自动取消+通知，不扣分 | 用户裁决 | 用户确认 |

# 13. 技术设计冻结结论

- 已冻结（TECH-V1，2026-09-07，用户显式确认）；
- Demo 验证（Boot 3 空壳编译启动 + 连接 MySQL/Redis + 登录冒烟）已列为模块四首个工作单元，冻结后立即执行。
