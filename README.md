# 校园搭子预约系统 🎓

一个面向高校学生的**兴趣搭子组局平台**：用户可以发布组局（球类 / 电竞 / 桌游 / 跑步 / 健身 / 学习等），寻找志同道合的伙伴一起参与，并通过评价与信誉分机制维护组局质量。

## ✨ 功能亮点

| 模块 | 说明 |
|------|------|
| 🔐 用户系统 | 学号 / 手机号注册、JWT 登录、登录失败锁定、封禁管理 |
| 📢 组局广场 | 发布 / 搜索 / 分类筛选 / 时间与热度排序、兴趣推荐 |
| 🙋 报名管理 | 报名 / 审核 / 取消、出勤标记、名额原子控制（防超卖） |
| 💬 互动 | 帖子评论、完成组局后互评 |
| ⭐ 信誉分 | 基于评价标签与评分的信誉分体系，信誉等级 / 星级展示 |
| 🖥 管理后台 | 数据看板、用户管理（封禁 / 信誉重置）、帖子审核、标签管理、数据统计 |

## 🛠 技术栈

- **后端**: Java 8+ · Spring Boot 2.7 · MyBatis-Plus · JJWT · Redis · MySQL 8.0
- **H5 用户端**: Vue 3 · Vite · Vant 4 · Pinia
- **管理端**: Vue 3 · Vite · Element Plus · ECharts

## 📁 项目结构

```
school-buddy-system/
├─ backend/          # Spring Boot 后端（端口 8080）
├─ frontend-h5/      # H5 用户端（端口 3000）
├─ frontend-admin/   # 管理后台（端口 3001）
├─ database/         # 建表脚本 + 测试数据
└─ docs/             # 需求 / 设计 / 开发文档
```

## 🚀 快速启动

完整新手教程见 **`docs/00-新手小白从零启动教程.md`**（含依赖安装、配置、排错）。

```bash
# 1. 准备：MySQL 8.0 + Redis + JDK + Maven + Node 18+

# 2. 复制本地配置（含数据库密码 / JWT 密钥，不入库）
cd backend/src/main/resources
cp application-local.example.yml application-local.yml   # 填上你的 MySQL 密码

# 3. 初始化数据库（MySQL）
mysql -u root -p < database/schema.sql
mysql -u root -p < database/test-data.sql
mysql -u root -p < database/test-data-more.sql

# 4. 启动 Redis（双击 redis-server.exe）

# 5. 启动后端
cd backend && mvn spring-boot:run          # http://localhost:8080

# 6. 启动 H5 用户端
cd frontend-h5 && npm install && npm run dev   # http://localhost:3000

# 7. 启动管理端
cd frontend-admin && npm install && npm run dev  # http://localhost:3001
```

## 🔑 测试账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | `admin001` | `123456` |
| 学生 | `20240001` | `123456` |
| 学生 | `20240002` | `123456` |

## 🎨 设计风格

H5 用户端与管理端统一采用**高端柔和风（Soft Structuralism）**：翡翠主色 `#10A37F`、冷银白背景、squircle 大圆角、扩散阴影与柔和的微动效。

## 📄 文档索引

| 文档 | 内容 |
|------|------|
| `docs/01-PRD需求分析文档.md` | 产品需求与业务规则 |
| `docs/02-库表结构设计文档.md` | 数据库表结构 |
| `docs/03-研发系统设计文档.md` | 系统设计 |
| `docs/04-功能实现清单.md` | 功能实现情况 |
| `docs/00-新手小白从零启动教程.md` | 从零搭建运行教程 |
