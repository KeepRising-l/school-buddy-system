# 校园搭子预约系统 🎓

一个面向高校学生的**兴趣搭子组局平台**：用户可以发布组局（球类 / 电竞 / 桌游 / 跑步 / 健身 / 学习等），寻找志同道合的伙伴一起参与，并通过评价与信誉分机制维护组局质量。

> **🆕 重构版后端 `backend-v2/`（2026-09）**：Java 17 + Spring Boot 3.3.5 重写完成（S0~S9 全切片），功能与旧版 `backend/` 等价，接口路径不变、前端零改动，另有 3 处增强：逻辑删除+级联（D-007）、取消组局自动通知不扣分（D-008）、通知 7 类（D-011）。
> 启动方式与旧版相同，仅 JDK 要求不同：
> ```bash
> # 前置：JDK 17（旧版 backend 用 JDK 8）；MySQL/Redis 准备步骤与下文一致
> cd backend-v2
> mvn spring-boot:run
> ```
> 详细重构记录见 `AI-Dev-Workflow/`（需求/技术设计/开发计划三份基线 + 切片进度看板）。

本文件是面向**第一次接触本项目的小白**的启动指南。更详细的分步骤教程（含依赖安装、常见报错排查）见 **`教学文档/00-新手小白从零启动教程.md`**。

---

## 一、这个项目由哪几部分组成？

| 部分 | 技术 | 端口 | 作用 |
|------|------|------|------|
| `backend/` 后端 | Java + Spring Boot | **8080** | 提供所有接口（登录、发帖、报名、评价…） |
| `frontend-h5/` H5 用户端 | Vue3 + Vant | **3000** | 学生用的手机网页 |
| `frontend-admin/` 管理端 | Vue3 + Element Plus | **3001** | 管理员用的电脑后台 |
| MySQL | 数据库 | 3306 | 存所有数据 |
| Redis | 缓存 | 6379 | 登录失败锁定、浏览量去重等 |

> 启动顺序建议：**先 Redis → 再 MySQL → 再后端 → 再前端**。

---

## 二、需要的环境与版本（一定要选对版本）

| 软件 | 推荐版本 | 检查命令 |
|------|---------|---------|
| **JDK** | **1.8（Java 8）** | `java -version` |
| **Maven** | **3.6 及以上（3.6.3+）** | `mvn -version` |
| **MySQL** | **8.0** | 安装时记住 root 密码 |
| **Redis** | **5.0（Windows 版）** | 见第 4 步 |
| **Node.js** | **18（推荐 18.17+）** | `node -v` |
| **npm** | Node 18 自带（9.0+） | `npm -v` |

> ⚠️ **JDK、Node 必须按上面的版本装**，版本不对后端/前端会启动失败。

---

## 三、数据库账号密码（第 0 步，必做）

为了安全，**项目没有把数据库密码写进仓库**。密码放在一个会被 git 忽略的文件里，你需要自己填一次：

1. 进入 `backend/src/main/resources/`
2. 把文件 `application-local.example.yml` **复制一份**，改名为 `application-local.yml`
3. 打开它，把 `password` 改成你安装 MySQL 时设置的 **root 密码**：
   ```yaml
   spring:
     datasource:
       password: 你的MySQL密码   # ← 改成你的 root 密码
   ```
4. 如果你的 root 密码正好是 `1234`，文件里已经填好了，直接跳过这步。

> 数据库使用的是 **root 账号**、数据库名 `school_buddy`（见 `application-dev.yml`）。这个文件已在 `.gitignore` 里，不会被提交，密码不会泄露。

---

## 四、创建数据库并导入数据（用 Navicat，推荐）

`database/init-all.sql` 是一站式脚本：**建库 + 建 10 张表 + 初始数据 + 测试数据**，一次搞定，推荐用 Navicat 导入。

**准备工作**：本地 MySQL 已启动（Windows 服务里看到 MySQL80 在运行，或用 Navicat 能连上 3306 即代表启动成功）。

**Navicat 导入步骤**：
1. 打开 **Navicat**，新建连接：主机 `localhost`、端口 `3306`、用户名 `root`、密码填你的 MySQL 密码 → 测试连接成功 → 双击连接名称进入。
2. 右键连接名（或任意数据库）→ 选择 **「运行 SQL 文件…」**（英文版是 *Run SQL File*）。
3. 选中 `database/init-all.sql` → 运行。
4. 运行结束后，刷新，能看到自动创建了一个数据库 **`school_buddy`**，里面有若干张表，说明导入成功。

> 不用手动建库、不用单独导建表脚本和测试数据，一个 `init-all.sql` 全搞定。
> ⚠️ 该脚本会**清空重建**所有表，重复运行会覆盖已有数据。

---

## 五、启动 Redis（必开）

后端依赖 Redis（登录失败锁定、浏览量去重），**Redis 没启动，登录功能会不可用**。

- 本项目面向 Windows，Redis 官方不提供 Windows 客户端，推荐用社区维护版：
  1. 打开 [github.com/tporadowski/redis/releases](https://github.com/tporadowski/redis/releases)
  2. 下载 `Redis-x64-5.0.14.1.zip`，解压到 `D:\redis`
  3. 每次启动时，双击里面的 **`redis-server.exe`**
- 看到类似下面的输出就代表启动成功（**这个黑窗口别关**）：
  ```
  [xxxx] Ready to accept connections tcp 127.0.0.1:6379
  ```

---

## 六、启动后端

方式一（推荐，命令行）：
```bash
cd backend
mvn spring-boot:run
```
方式二（任意 IDE，如 IntelliJ IDEA）：直接运行 `backend` 下的主启动类。

看到类似 `Tomcat started on port(s): 8080` 即成功，**这个窗口也先别关**。

---

## 七、启动两个前端

**H5 用户端**（`http://localhost:3000`）：
```bash
cd frontend-h5
npm install     # 第一次较慢，可先配国内镜像: npm config set registry https://registry.npmmirror.com
npm run dev
```
看到 `Local: http://localhost:3000/` 后，用浏览器打开，按 F12 → 设备图标可模拟手机效果。

**管理端**（`http://localhost:3001`）：
```bash
cd frontend-admin
npm install
npm run dev
```
看到 `Local: http://localhost:3001/` 后，浏览器打开 `http://localhost:3001`。

---

## 八、访问与测试账号

现在你开着：**Redis、后端、两个前端**一共 4 个窗口 + 浏览器。

| 入口 | 地址 |
|------|------|
| 后端 API | http://localhost:8080 |
| H5 用户端 | http://localhost:3000 |
| 管理端 | http://localhost:3001 |

测试账号（由 `init-all.sql` 初始化）：

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | `admin001` | `123456` |
| 学生 | `20240001` | `123456` |
| 学生 | `20240002` | `123456` |

---

## 九、常见问题（速查）

| 现象 | 原因与解决 |
|------|-----------|
| 后端报数据库连接被拒 | 第 3 步密码填错，或第 4 步没导入数据库 |
| 登录报「系统繁忙」/「无法连接 Redis」 | Redis 没启动，双击 `redis-server.exe`（第 5 步） |
| 后端启动失败 | JDK 不是 1.8，或 Maven 没配环境变量，重开后重试 |
| `node` / `npm` 不是内部命令 | Node 没装好或没配 Path，装完重开终端 |
| `npm install` 卡住不动 | 配国内镜像：`npm config set registry https://registry.npmmirror.com` 再重试 |
| Navicat 运行 SQL 报 `Unknown database` | 说明没用 `init-all.sql`（该脚本内含建库）；确认选的是这个文件 |

> 更多问题排查见 **`教学文档/00-新手小白从零启动教程.md`** 的 FAQ。

---

## 📁 项目结构

```
school-buddy-system/
├─ backend/          # Spring Boot 后端（端口 8080）
├─ frontend-h5/      # H5 用户端（端口 3000）
├─ frontend-admin/   # 管理后台（端口 3001）
├─ database/         # 建表脚本 + 测试数据（init-all.sql 一站式导入）
└─ docs/             # 需求 / 设计 / 开发文档
教学文档/             # 分步骤新手教程（含安装、排错）
```

## 📄 文档索引

| 文档 | 内容 |
|------|------|
| `docs/01-PRD需求分析文档.md` | 产品需求与业务规则 |
| `docs/02-库表结构设计文档.md` | 数据库表结构 |
| `docs/03-研发系统设计文档.md` | 系统设计 |
| `docs/04-功能实现清单.md` | 功能实现情况 |
| `docs/05-未实现功能清单.md` | 尚未实现的功能 |
| `docs/17-项目不足与改进方向.md` | 不足与改进方向 |
| `docs/18-毕设完善计划.md` | 毕设完善计划（分层） |
| `教学文档/00-新手小白从零启动教程.md` | 从零搭建运行教程（详细版） |