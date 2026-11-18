# 校园搭子预约系统 · 全量优化与 AI 升级交付总结 (Walkthrough)

## 一、远端提交记录总览

本次重构严格按 **4 个阶段** 实施，每完成一个阶段均执行后端全量单测与双前端生产构建验证，并已全部推送至远程仓库 `origin/main`（`https://gitcode.com/keeprising/school-buddy-system.git`）：

| 阶段 | 提交哈希 | 提交信息 | 核心产出 |
|------|---------|---------|---------|
| **Phase 1** | `e7994cf` | `fix(phase1): 修复收藏冲突与候补递补等11项缺陷并优化N+1与统计聚合SQL` | 修复 11 项 P0/P1 缺陷、消除 6 处 N+1 查询、统计模块 150 次循环 SQL 改为 `GROUP BY` 聚合、产出 `docs/项目优化与重新规划方案.md` |
| **Phase 2** | `aebd79f` | `feat(phase2): 打通首页公告与精选推荐位并新增4位签到码核销与修改密码` | H5 首页滚动公告栏+弹窗公告、「你可能感兴趣」横向推荐位（带契合度%与推荐理由胶囊）、4位签到码自助出勤核销、修改密码、底部导航栏未读消息红点 |
| **Phase 3** | `96760b4` | `feat(phase3): 接入AI一键组局与成局破冰小秘书及DFA去噪敏感词与举报AI风控` | 新增 `com.buddy.module.ai` 模块（双模架构：支持外部大模型 + 内置校园语义引擎）、发帖页 AI 一键填表与润色、详情页 AI 破冰小秘书、DFA 去噪敏感词引擎、管理端举报 AI 风控研判 |
| **Phase 4** | `9b1f83b` | `test(phase4): 扩充AI与DFA及签到码单测至146例并清理仓库与同步文档` | 单元测试从 137 例扩充至 **146 例全绿**，清理 `dump.rdb` / `app*.log`，同步更新 `README.md`、PRD 与技术详解文档 |

---

## 二、各阶段核心改动详解

### 1. Phase 1：Bug 清零与 SQL 性能加固
- **收藏唯一键冲突与分页截断修复**：
  - `FavoriteMapper.java` 与 `FavoriteServiceImpl.java`：新增 `restoreDeleted` 恢复已软删行（解决取消收藏后再次收藏触发 `DuplicateKeyException` 500 的问题），新增 `selectFavoritePostsPage` SQL 层关联分页查询（解决内存过滤导致第 2 页以后收藏永远加载不出的问题）。
- **候补递补防插队与拒审自动递补**：
  - `ApplyServiceImpl.java`：当 `APPROVED` 报名取消且成功递补队首 `WAITLIST` 时，保持 `FULL` 状态防止路人插队；当发布者 `reject` 递补申请时，自动继续递补下一位 `WAITLIST` 候补者。
- **管理端分类筛选与标签双向启停修复**：
  - `PostList.vue`：修复下拉框误绑具体项目名的问题，改调 `getTagCategories()` 绑定大类名称；
  - `TagServiceImpl.java` 与 `TagManage.vue`：新增 `getAdminAllTags()` 返回全部状态标签，`disableTag` 支持 `1 <-> 0` 双向状态切换，表格绑定 `sortedData` 修复合并行错位。
- **N+1 查询清零与统计聚合优化**：
  - `CommentServiceImpl.java`、`ApplyServiceImpl.java`、`UserServiceImpl.java`、`PostServiceImpl.java` 全部改为批量 `selectBatchIds` 组装；
  - `StatsServiceImpl.java` 将发帖、成局、活跃用户、累计用户趋势从 $30 \sim 60$ 次循环查询重构为 `GROUP BY DATE(...)` 聚合 SQL。
- **H5 细节修复**：
  - `PostDetail.vue` 修复评论时间 `c.createdTime` 字段名与无段位帖子跳过信誉/性别预检问题；
  - `ReviewServiceImpl.java` 与 `CreditServiceImpl.java` 支持提交评价后立即重算信誉分且多选标签与顺序无关。

### 2. Phase 2：产品体验闭环与履约核销升级
- **首页公告与「你可能感兴趣」精选推荐位**：
  - `Square.vue` 与 `PostCard.vue`：接入顶部滚动公告栏、未读弹窗公告，以及带契合度百分比（如 `92% 契合`）和可解释推荐理由胶囊（如 `✨ 契合你的羽毛球`、`✨ 即将开局`、`✨ 差1人成局`）的横向精选卡片。
- **4 位数字活动签到码核销 + 24h 超时自动守约兜底**：
  - `PostStateMachine.java`、`ApplyServiceImpl.java` 与 `PostDetail.vue`：局主端详情页展示 4 位签到码，已通过参与者可输入签到码自助完成 `ATTENDED` 核销；`PostStatusTask.java` 对结束超 24 小时仍未标记的 `APPROVED` 报名自动兜底为 `ATTENDED`。
- **修改密码与全局未读角标**：
  - `Profile.vue` 新增修改登录密码弹窗；`TabbarLayout.vue` 在底部导航栏“我的”图标展示未读消息红点数字。

### 3. Phase 3：AI 原生（AI-Native）能力赋能
- **AI 智能搭子服务**：
  - `AiController.java` 与 `AiBuddyServiceImpl.java`：支持外部大模型 API（`app.ai.api-key`）与内置高精度校园语义引擎双模运行；
  - `PostCreate.vue`：新增 **「✨ AI 智能成局」** 一句话自动填表卡片与 **「✨ AI 帮写/润色」** 按钮；
  - `PostDetail.vue`：成局解锁联系方式后展示 **「💡 AI 成局破冰小秘书」**（共同院系/共同兴趣话题 + 一键复制打招呼语）；
  - `SensitiveWordService.java`：升级为 **DFA 前缀树 + 干扰符穿透引擎**（自动拦截 `代*考`、`刷 - 单` 等变体）；
  - `ReportList.vue`：举报详情与处置弹窗新增 **「🤖 AI 风控研判建议」** 及一键采纳功能。

---

## 三、验证结果

1. **后端单元测试（JDK 17）**：
   - 执行命令：`$env:JAVA_HOME="D:\develop\JDK17"; mvn -f backend-v2/pom.xml test -B`
   - 结果：**19 个测试类 / 146 例单元测试全部通过（0 Failures, 0 Errors）**。
2. **双前端生产构建验证**：
   - `frontend-h5`：`npm run build -- --emptyOutDir false` → `✓ built in 2.28s`
   - `frontend-admin`：`npm run build -- --emptyOutDir false` → `✓ built in 9.52s`
3. **远端仓库状态**：
   - `main` 分支与 `origin/main` 完全同步，远端 Git Hooks 检查全部 `[PASSED]`。
