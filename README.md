# 校园搭子预约系统

一个面向高校学生的**兴趣搭子组局平台**：发布组局（球类 / 电竞 / 桌游 / 跑步 / 健身 / 学习等）、报名审批、满员候补、活动前自动提醒、报名通过后互换联系方式、活动后互评，并用信誉分机制约束守约行为。

> **当前交付物：`backend-v2/`（Java 17 + Spring Boot 3.3.5）**，含 135 例单元测试与 CI 流水线。老版后端已归档至 `backend-legacy/`（仅有追溯价值，勿启动）。
> 文档在 `docs/`：产品需求（PRD） / 功能与技术详解 / 答辩问答准备 / 项目使用文档；另有两份历史设计文档供论文写作参考。

本文件是**面向第一次接触本项目的人**的启动指南。

---

## 一、项目由哪几部分组成？

| 部分 | 技术 | 端口 | 作用 |
|------|------|------|------|
| `backend-v2/` 后端 | Java 17 + Spring Boot 3.3.5 | **8080** | 提供所有接口（登录、发帖、报名、评价…） |
| `frontend-h5/` H5 用户端 | Vue3 + Vant | **3000** | 学生用的手机网页 |
| `frontend-admin/` 管理端 | Vue3 + Element Plus | **3001** | 管理员用的电脑后台 |
| MySQL | 数据库 | 3306 | 存所有业务数据 |
| Redis | （可选） | 6379 | **默认不需要**；仅当把"辅助数据存储"切换为 Redis 时才使用（见第五节） |

> 启动顺序建议：**MySQL → 后端 → 前端**（默认无需 Redis）。

---

## 二、需要的环境与版本

| 软件 | 推荐版本 | 检查命令 |
|------|---------|---------|
| **JDK** | **17（LTS，必须）** | `java -version` |
| **Maven** | 3.8+ | `mvn -version` |
| **MySQL** | 8.0 | 安装时记住 root 密码 |
| **Node.js** | 18+（CI 使用 18，22 亦可） | `node -v` |
| **npm** | Node 自带 | `npm -v` |
| Redis | 可选（5.0+） | 默认不需要，见第五节 |

> JDK 不是 17 后端会启动失败；Node 版本过低前端会装不上依赖。

---

## 三、数据库密码与 JWT 密钥（第 0 步，必做）

项目**没有把密码和密钥写进仓库**，需要你在一个会被 git 忽略的文件里自己填一次：

1. 进入 `backend-v2/src/main/resources/`
2. 把文件 `application-local.example.yml` **复制一份**，改名为 `application-local.yml`
3. 打开它，填两处：
   ```yaml
   spring:
     datasource:
       password: 你的MySQL密码   # ← 改成你的 root 密码

   jwt:
     secret: 至少32位的随机字符串   # ← 例如一串 32+ 位的字母数字组合
   ```
4. 说明：JWT 密钥缺失或不足 32 位时，后端会**拒绝启动**（这是安全设计，防止弱密钥上线）。

> 数据库默认使用 root 账号、库名 `school_buddy`（见 `application-dev.yml`）。`application-local.yml` 已在 `.gitignore` 中，不会提交、不会泄露。

---

## 四、创建数据库并导入数据（推荐 Navicat）

`database/init-all.sql` 是一站式脚本：**建库 + 建 13 张表 + 初始数据 + 测试数据**，一次搞定。

**Navicat 导入步骤**：

1. 打开 Navicat，新建连接：主机 `localhost`、端口 `3306`、用户名 `root`、密码填你的 MySQL 密码 → 测试连接成功 → 双击进入；
2. 右键连接名（或任意数据库）→ 选择 **「运行 SQL 文件…」**（英文版 *Run SQL File*）；
3. 选中 `database/init-all.sql` → 运行；
4. 运行结束后刷新，能看到自动创建了数据库 **`school_buddy`**，即导入成功。

> ⚠️ 该脚本会**清空重建**所有表，重复运行会覆盖已有数据；仅限本地/测试库使用。
>
> 需要更多演示用户时：再执行一次 `database/more-users.sql`（追加 50 个学生账号，含兴趣标签；账号/密码明细见 `docs/测试用户清单.md`，重复执行安全）。
>
> ⚠️ **用命令行导入时必须带字符集参数**，否则中文会变成乱码（`陈嘉豪` → `闄堝槈璞`）：
> ```bash
> mysql -u root -p --default-character-set=utf8mb4 < database/init-all.sql
> mysql -u root -p --default-character-set=utf8mb4 school_buddy < database/more-users.sql
> ```
> 用 Navicat 导入则无需关心（它默认按 UTF-8 处理）。
>
> 📅 **演示前建议**：仓库自带的示例帖子时间都是固定的过去日期，导入后会被定时任务推进成"已结束"，广场看起来是空的。
> 答辩/演示前跑一次 `database/demo-posts.sql`，会生成 8 条**相对当前时间**的未来组局帖（含一条 25 分钟后开始的，用来观察"活动提醒"通知）：
> ```bash
> mysql -u root -p --default-character-set=utf8mb4 school_buddy < database/demo-posts.sql
> ```

---

## 五、辅助数据存储（默认 MySQL，无需 Redis）

登录失败锁定、浏览量去重、上传频率限制、举报去重这四类"辅助数据"由统一组件承载：**默认使用 MySQL 单表 `aux_kv`，零外部依赖**，因此**不需要安装 Redis**。

- **已有旧数据库升级（一次性）**：在你的 `school_buddy` 库执行下面这段建表语句（用 `init-all.sql` 新导入的库已自带，可跳过）：
  ```sql
  CREATE TABLE IF NOT EXISTS `aux_kv` (
      `k`           VARCHAR(128) NOT NULL COMMENT '键',
      `v`           BIGINT       NOT NULL DEFAULT 1 COMMENT '值（计数）',
      `expire_time` DATETIME     NOT NULL COMMENT '过期时间',
      PRIMARY KEY (`k`),
      KEY `idx_expire` (`expire_time`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='辅助键值表';
  ```
- **可选：切换为 Redis**（数据量大、并发高，或觉得 MySQL 版响应偏慢时，可无痛切换）：
  1. 启动一个 Redis：Windows 可用 Memurai，或下载 [github.com/tporadowski/redis/releases](https://github.com/tporadowski/redis/releases) 的 `Redis-x64-5.0.14.1.zip`，解压后双击 `redis-server.exe`；也可使用 WSL / 局域网现有 Redis（改 `application-dev.yml` 中 host/port）；
  2. 在 `application-local.yml` 中加一行：`app.aux.store: redis`；
  3. 重启后端即可。业务代码零改动，两套实现行为一致，随时可改回 MySQL。

---

## 六、启动后端

方式一（命令行，推荐）：
```bash
cd backend-v2
mvn spring-boot:run
```
方式二（任意 IDE，如 IntelliJ IDEA）：直接运行 `backend-v2` 下的主启动类 `BuddyApplication`。

看到类似 `Tomcat started on port(s): 8080` 即成功。可选自检：`mvn test`，预期 **111 例全部通过**。

---

## 七、启动两个前端

**H5 用户端**（`http://localhost:3000`）：
```bash
cd frontend-h5
npm install     # 第一次较慢，可先配国内镜像: npm config set registry https://registry.npmmirror.com
npm run dev
```

**管理端**（`http://localhost:3001`）：
```bash
cd frontend-admin
npm install
npm run dev
```

> **想在手机上看 H5**：手机与电脑连同一 Wi-Fi，用手机浏览器打开 `http://<电脑局域网IP>:3000`（Windows 用 `ipconfig` 查 IPv4）。
> H5 的开发服务器已监听所有网卡，无需额外参数。页面已做移动端适配（全面屏安全区、`100dvh`、大屏限宽 480px 居中、可「添加到主屏幕」全屏打开）；管理端是电脑端后台，不面向手机。

---

## 八、访问与测试账号

| 入口 | 地址 |
|------|------|
| 后端 API | http://localhost:8080 |
| H5 用户端 | http://localhost:3000 |
| 管理端 | http://localhost:3001 |

测试账号（由 `init-all.sql` 初始化）：

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | `admin001` | `123456` |
| 学生 | `20240001` / `20240002` | `123456` |
| 批量测试学生 | 如 `2342170101` | 学号 + "123"（如 `2342170101123`） |

---

## 九、常见问题（速查）

| 现象 | 原因与解决 |
|------|-----------|
| 后端启动失败，提示"JWT 密钥未配置 / 长度不足" | 没配 `application-local.yml`，或密钥短于 32 位（第三步） |
| 后端报数据库连接被拒 | 第三步密码填错，或第四步没导入数据库 |
| 提示 `Table 'school_buddy.aux_kv' doesn't exist` | 旧库未升级：执行第五节的建表 SQL（或重新导入 `init-all.sql`） |
| 登录报「系统繁忙」 | 默认模式多为数据库异常（检查 MySQL）；若已切换 Redis 模式，检查 Redis 是否启动 |
| 后端启动失败（其他） | JDK 不是 17，或 Maven 未配置环境变量 |
| `node` / `npm` 不是内部命令 | Node 没装好或没配 Path，装完重开终端 |
| `npm install` 卡住不动 | 配国内镜像：`npm config set registry https://registry.npmmirror.com` 再重试 |
| Navicat 运行 SQL 报 `Unknown database` | 确认选择的是 `init-all.sql`（该脚本内含建库语句） |
| 提示"操作过于频繁，请稍后再试" | 触发了接口限流（发布 5 次/时、评论 20 次/时、报名与候补 10 次/时、举报 10 次/时），稍后再试 |
| 收不到"活动即将开始"提醒 | 提醒只针对"未来 30 分钟内开始"的已过审组局，且每 5 分钟扫描一次；确认帖子开始时间与报名是否已通过 |

---

## 项目结构

```
school-buddy-system/
├─ backend-v2/       # Spring Boot 3 后端（当前交付物，端口 8080，Java 17）
├─ backend-legacy/   # 老版后端归档（Java 8 + Boot 2.7，仅供追溯，勿启动）
├─ frontend-h5/      # H5 用户端（端口 3000）
├─ frontend-admin/   # 管理后台（端口 3001）
├─ database/         # 数据库脚本（init-all.sql 一站式导入，13 张表；more-users.sql 追加 50 个测试用户）
├─ docs/             # 产品需求（PRD）/ 功能与技术详解 / 答辩问答准备 / 项目使用文档
│                    # （另有两份历史设计文档，写论文可作参考）
└─ 教学文档/          # 分步骤新手教程（历史资料；其中 Redis 相关内容以本文第五节为准）
```

## 文档索引

| 文档 | 内容 |
|------|------|
| `docs/产品需求文档（PRD）.md` | 为什么这样设计、解决什么问题、研究背景与意义、规则速览 |
| `docs/功能与技术详解.md` | 技术栈、项目亮点、各模块实现细节与深水区讲解 |
| `docs/答辩问答准备.md` | 高频问题与建议回答、口径统一表、演示脚本 |
| `docs/项目使用文档.md` | 详细版使用指南（含 FAQ 与答辩演示建议） |
| `docs/测试用户清单.md` | 全部测试账号清单（**管理员 admin001/123456** + 内置 45 个学生 + 追加 50 个学生，含密码与兴趣） |
