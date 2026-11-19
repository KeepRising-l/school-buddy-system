-- ============================================================
-- 校园兴趣搭子预约系统 - 一站式数据库初始化脚本（V1.1）
-- 生成日期: 2026-08-15
-- 内容: 建库 + 建表(13张) + 初始数据 + 测试数据(45用户/30帖子)
-- 用法:  mysql -u root -p < database/init-all.sql
-- 注意: 会 DROP 重建所有表，已有数据将被清空！
-- 范围: 仅供本地开发/测试环境重置数据库使用，严禁在真实/生产数据库执行；
--       脚本包含公开口令的演示账号（admin001/123456），生产环境不要导入本脚本。
-- ============================================================
CREATE DATABASE IF NOT EXISTS school_buddy DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE `school_buddy`;

-- =====================================================
-- 校园兴趣搭子预约系统 - 数据库脚本
-- 数据库: MySQL 8.0+  (CREATE DATABASE school_buddy DEFAULT CHARSET utf8mb4)
-- =====================================================
USE `school_buddy`;

-- -----------------------------------------------------
-- 1. 用户表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_no`   VARCHAR(32)   NOT NULL                   COMMENT '学号',
    `phone`        VARCHAR(16)   DEFAULT NULL               COMMENT '手机号',
    `password`     VARCHAR(128)  NOT NULL                   COMMENT '密码(BCrypt)',
    `nickname`     VARCHAR(64)   DEFAULT NULL               COMMENT '昵称',
    `avatar`       VARCHAR(255)  DEFAULT NULL               COMMENT '头像URL',
    `signature`    VARCHAR(255)  DEFAULT NULL               COMMENT '个性签名',
    `college`      VARCHAR(128)  DEFAULT NULL               COMMENT '所属学院',
    `gender`       TINYINT       DEFAULT 0                  COMMENT '性别 0未知 1男 2女',
    `credit_score` INT           DEFAULT 100                COMMENT '信誉分 0-100',
    `status`       TINYINT       DEFAULT 1                  COMMENT '状态 1正常 0封禁',
    `username`     VARCHAR(64)   DEFAULT NULL               COMMENT '用户名（唯一，登录用）',
    `is_admin`     TINYINT       DEFAULT 0                  COMMENT '是否管理员 0否 1是',
    `profile_privacy` TINYINT    DEFAULT 0                  COMMENT '主页隐私 0公开 1隐藏历史组局（V1.1）',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- -----------------------------------------------------
-- 2. 兴趣标签表（由管理员维护）
--    category: 分类，如 篮球/羽毛球/王者荣耀/桌游
--    level:    段位 1萌新 2小白 3熟手 4高手 5宗师
-- -----------------------------------------------------
DROP TABLE IF EXISTS `interest_tag`;
CREATE TABLE `interest_tag` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT,
    `category`     VARCHAR(64)   NOT NULL        COMMENT '分类，如 篮球/羽毛球/王者荣耀',
    `name`         VARCHAR(64)   NOT NULL        COMMENT '项目名',
    `description`  VARCHAR(255)  DEFAULT NULL,
    `sort_order`   INT           DEFAULT 0,
    `status`       TINYINT       DEFAULT 1       COMMENT '1启用 0禁用',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兴趣标签表';

-- -----------------------------------------------------
-- 3. 用户-标签关联
-- -----------------------------------------------------
DROP TABLE IF EXISTS `user_tag`;
CREATE TABLE `user_tag` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT   NOT NULL,
    `tag_id`       BIGINT   NOT NULL,
    `self_level`   INT      DEFAULT 1         COMMENT '自评段位 1-5',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tag` (`user_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- 4. 组局帖子表
--    status: RECRUITING(招募中) -> FULL(已满员) -> ONGOING(进行中) -> FINISHED(已结束)
--            还可能被 CANCELLED(已取消)
-- -----------------------------------------------------
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `publisher_id`   BIGINT        NOT NULL             COMMENT '发布人',
    `title`          VARCHAR(128)  NOT NULL,
    `category`       VARCHAR(64)   NOT NULL             COMMENT '所属分类',
    `tag_ids`        VARCHAR(255)  DEFAULT NULL         COMMENT '相关标签id列表，逗号分隔',
    `start_time`     DATETIME      NOT NULL,
    `end_time`       DATETIME      NOT NULL,
    `location`       VARCHAR(128)  NOT NULL             COMMENT '活动地点',
    `max_people`     INT           NOT NULL DEFAULT 2   COMMENT '最大人数',
    `current_people` INT           NOT NULL DEFAULT 1   COMMENT '当前人数(含发布者)',
    `require_level`  INT           DEFAULT NULL         COMMENT '要求最低段位，空则不限',
    `require_gender` TINYINT       DEFAULT 0             COMMENT '0不限 1男 2女',
    `description`    TEXT,
    `status`         VARCHAR(24)   NOT NULL DEFAULT 'RECRUITING',
    `checkin_code`   VARCHAR(16)   DEFAULT NULL         COMMENT '4位数字现场签到码（活动开始时生成）',
    `view_count`     INT           DEFAULT 0,
    `like_count`     INT           DEFAULT 0,
    `is_audit_pass`  TINYINT       DEFAULT 0             COMMENT '1正常 2已下架（先发后审；0废弃不用）',
    `created_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_publisher` (`publisher_id`),
    KEY `idx_status_time` (`status`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组局帖子表';

-- -----------------------------------------------------
-- 5. 报名表
--    status: PENDING(待审核) -> APPROVED(已加入) / REJECTED(已拒绝)
--            APPROVED 还可能变为 CANCELLED(已退出) / ATTENDED(已参加) / NOSHOW(爽约)
-- -----------------------------------------------------
DROP TABLE IF EXISTS `post_apply`;
CREATE TABLE `post_apply` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT,
    `post_id`      BIGINT        NOT NULL,
    `applicant_id` BIGINT        NOT NULL,
    `status`       VARCHAR(24)   NOT NULL DEFAULT 'PENDING',
    `apply_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `handle_time`  DATETIME      DEFAULT NULL,
    `remark`       VARCHAR(255)  DEFAULT NULL,
    `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_applicant` (`post_id`, `applicant_id`),
    KEY `idx_applicant` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名表';

-- -----------------------------------------------------
-- 6. 评价表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `post_id`        BIGINT        NOT NULL,
    `reviewer_id`    BIGINT        NOT NULL           COMMENT '评价人',
    `target_user_id` BIGINT        NOT NULL           COMMENT '被评价人',
    `rating`         TINYINT       NOT NULL           COMMENT '1-5 星',
    `tag`            VARCHAR(32)   DEFAULT NULL       COMMENT '靠谱/爽约/迟到',
    `content`        VARCHAR(500)  DEFAULT NULL,
    `reply_content`  VARCHAR(500)  DEFAULT NULL       COMMENT '被评价人的回复（互评解释）',
    `reply_time`     DATETIME      DEFAULT NULL       COMMENT '回复时间',
    `created_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_reviewer_target` (`post_id`, `reviewer_id`, `target_user_id`),
    KEY `idx_target` (`target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- -----------------------------------------------------
-- 7. 评论表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `post_comment`;
CREATE TABLE `post_comment` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT,
    `post_id`      BIGINT        NOT NULL,
    `user_id`      BIGINT        NOT NULL,
    `content`      VARCHAR(500)  NOT NULL,
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- 8. 通知表（V1.1 通知中心）
--    type: APPLY_NEW/APPLY_RESULT/WAITLIST_PROMOTED/
--          POST_AUDIT/POST_TAKEDOWN/POST_CANCEL/ACTIVITY_REMIND/SYSTEM
-- -----------------------------------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT        NOT NULL             COMMENT '接收人',
    `type`         VARCHAR(32)   NOT NULL             COMMENT '通知类型',
    `title`        VARCHAR(128)  NOT NULL             COMMENT '标题',
    `content`      VARCHAR(500)  DEFAULT NULL         COMMENT '内容',
    `link`         VARCHAR(255)  DEFAULT NULL         COMMENT '跳转路径',
    `is_read`      TINYINT       NOT NULL DEFAULT 0   COMMENT '0未读 1已读',
    `biz_id`       BIGINT        DEFAULT NULL         COMMENT '关联对象ID（帖子/报名）',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`, `created_time`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- -----------------------------------------------------
-- 9. 收藏表（V1.1 组局收藏）
-- -----------------------------------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT        NOT NULL             COMMENT '收藏用户',
    `post_id`      BIGINT        NOT NULL             COMMENT '帖子',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
    KEY `idx_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- -----------------------------------------------------
-- 10. 举报表（V1.1 举报闭环）
--    status: 0待处理 1有效 2无效
-- -----------------------------------------------------
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `reporter_id`   BIGINT        NOT NULL             COMMENT '举报人',
    `target_type`   VARCHAR(16)   NOT NULL             COMMENT 'POST/USER',
    `target_id`     BIGINT        NOT NULL             COMMENT '被举报对象ID',
    `reason_type`   VARCHAR(32)   NOT NULL             COMMENT '举报类型',
    `description`   VARCHAR(500)  DEFAULT NULL         COMMENT '举报说明',
    `status`        TINYINT       NOT NULL DEFAULT 0   COMMENT '0待处理 1有效 2无效',
    `handle_by`     BIGINT        DEFAULT NULL         COMMENT '处理人',
    `handle_time`   DATETIME      DEFAULT NULL         COMMENT '处理时间',
    `handle_remark` VARCHAR(255)  DEFAULT NULL         COMMENT '处理备注',
    `created_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_reporter` (`reporter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';

-- -----------------------------------------------------
-- 10. 信誉分变动台账表（V1.2 新增）
--     记录非评价类的信誉分变动（爽约/临场取消/管理员重置），
--     每日重算公式 = 100 + Σ评价影响 + Σ台账变动，惩罚不再被重算冲掉
-- -----------------------------------------------------
DROP TABLE IF EXISTS `credit_log`;
CREATE TABLE `credit_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL             COMMENT '用户ID',
    `change_value` INT          NOT NULL             COMMENT '变动值（正加负减）',
    `reason`       VARCHAR(64)  NOT NULL             COMMENT '原因：NOSHOW/CANCEL_LAST_MINUTE/ADMIN_RESET',
    `biz_type`     VARCHAR(32)  DEFAULT NULL         COMMENT '业务类型：APPLY/ADMIN',
    `biz_id`       BIGINT       DEFAULT NULL         COMMENT '业务对象ID',
    `created_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信誉分变动台账';

-- -----------------------------------------------------
-- 9. 系统公告表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `sys_announcement`;
CREATE TABLE `sys_announcement` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`         VARCHAR(128)  NOT NULL                   COMMENT '标题',
    `content`       TEXT          NOT NULL                   COMMENT '内容',
    `type`          TINYINT       NOT NULL DEFAULT 1         COMMENT '类型 1=公告 2=弹窗',
    `status`        TINYINT       NOT NULL DEFAULT 0         COMMENT '状态 0=关闭 1=启用',
    `publish_time`  DATETIME      DEFAULT NULL               COMMENT '发布时间',
    `expire_time`   DATETIME      DEFAULT NULL               COMMENT '过期时间（NULL=永不过期）',
    `created_by`    BIGINT        DEFAULT NULL               COMMENT '创建人（管理员ID）',
    `created_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- -----------------------------------------------------
-- 13. 辅助键值表（登录锁定/浏览去重/上传限频/举报去重；默认替代 Redis）
-- -----------------------------------------------------
DROP TABLE IF EXISTS `aux_kv`;
CREATE TABLE `aux_kv` (
    `k`           VARCHAR(128) NOT NULL               COMMENT '键',
    `v`           BIGINT       NOT NULL DEFAULT 1     COMMENT '值（计数）',
    `expire_time` DATETIME     NOT NULL               COMMENT '过期时间',
    PRIMARY KEY (`k`),
    KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='辅助键值表';


-- -----------------------------------------------------
-- 初始数据：演示管理员 + 若干兴趣标签 + 测试学生（仅限本地/测试环境）
-- admin001 的公开口令为 123456（哈希如下），生产环境禁止使用本脚本
-- 密码 123456 的 BCrypt: $2a$10$e7ZZEXLXq1W265LW25VN8ecZx47trR8xZB8hBPr7h6n0Kv2Xojfka
-- -----------------------------------------------------
INSERT INTO `sys_user` (`student_no`, `phone`, `password`, `nickname`, `username`, `college`, `gender`, `credit_score`, `signature`, `is_admin`)
VALUES ('admin001', '13800000000', '$2a$10$e7ZZEXLXq1W265LW25VN8ecZx47trR8xZB8hBPr7h6n0Kv2Xojfka', '包拯(管理员)', 'admin001', '信息学院', 1, 100, '铁面无私管社区，举报审核秒处理', 1),
       ('20240001', '13800000001', '$2a$10$e7ZZEXLXq1W265LW25VN8ecZx47trR8xZB8hBPr7h6n0Kv2Xojfka', '姚明', '20240001', '体育学院', 1, 95, '球场小巨人，东区篮球场随时约半场！', 0),
       ('20240002', '13800000002', '$2a$10$e7ZZEXLXq1W265LW25VN8ecZx47trR8xZB8hBPr7h6n0Kv2Xojfka', '谷爱凌', '20240002', '体育学院', 2, 98, '每天睡够10小时，羽毛球、夜跑、剧本杀全能搭子！', 0);

INSERT INTO `interest_tag` (`category`, `name`, `sort_order`) VALUES
('球类', '篮球', 1),
('球类', '羽毛球', 2),
('球类', '乒乓球', 3),
('球类', '足球', 4),
('电竞', '王者荣耀', 10),
('电竞', '英雄联盟', 11),
('电竞', '和平精英', 12),
('桌游', '剧本杀', 20),
('桌游', '狼人杀', 21),
('桌游', '三国杀', 22),
('跑步', '晨跑', 30),
('跑步', '夜跑', 31);

-- =====================================================
-- 校园搭子系统 - 名人主题测试数据脚本
-- 使用前提：先执行 schema.sql 初始化库表结构
-- 使用方法：source database/test-data.sql;
--
-- 学号规则：234(大数据人工智能学院) + 217(专业) + 班级(2位) + 编号(2位)
-- 密码规则：学号 + "123"，例如 2342170101123
-- =====================================================

USE `school_buddy`;

-- =====================================================
-- 1. 批量插入各行业名人学生（15人，3个班）
--    已有用户: admin001(id=1), 20240001(id=2), 20240002(id=3)
--    新学生从 id=4 开始
-- =====================================================
INSERT INTO `sys_user` (`student_no`, `phone`, `password`, `nickname`, `username`, `college`, `gender`, `credit_score`, `signature`, `status`, `is_admin`) VALUES

-- 班级 01（2342170101 ~ 2342170105）
('2342170101', '1382340101', '$2a$10$zjooIII90GrE6Btl0mJYUOzeZScN0TCAk6sRv6qvhr1L1hG0t7SQ6', '雷军',      '2342170101', '大数据人工智能学院', 1, 95, 'Are you OK？周末东区篮球场3v3+操场晨跑，交个朋友！', 1, 0),
('2342170102', '1382340102', '$2a$10$MixPESeHXi5lwiXFRjgU/uqrOXvkVxOgubIacH2vCpAZV2YF6zvHe', '陈雨菲',    '2342170102', '体育学院',           2, 88, '羽球大满贯，晚上环校路女生夜跑小分队领跑！', 1, 0),
('2342170103', '1382340103', '$2a$10$47zjN/kngpieE742JmrTqOMWnA.TA7j.dbZIO2QjZCizvoXmTT03m', '梅西',      '2342170103', '体育学院',           1, 92, '绿茵场与峡谷双修，5v5内战、周末约球叫我！', 1, 0),
('2342170104', '1382340104', '$2a$10$BZ2oEMv8pvuHCAP3u2xPl.QOuUoHG7VzBCf7WlKd9EG6sziN1qK5m', '刘亦菲',    '2342170104', '艺术与传媒学院',     2, 100,'周五南门剧本杀《月落洼》等车，打完羽毛球去夜跑~', 1, 0),
('2342170105', '1382340105', '$2a$10$3rcqX8yYpnLjx0haiOcmXuxzKv0RszhgCZraYMBOJBYJdt8qD0J8S', '科比',      '2342170105', '体育学院',           1, 78, '你见过凌晨四点的球场吗？西区篮球全场5v5控卫！', 1, 0),

-- 班级 02（2342170201 ~ 2342170205）
('2342170201', '1382340201', '$2a$10$0aGnZGBMrtomC7yT3M2GTeL3xE7b3N/qw4U6ixVaITKY/KXP7Xuti', '马龙',      '2342170201', '体育学院',           1, 85, '六边形战士！不打乒乓球的时候喜欢在研讨室组三国杀面基', 1, 0),
('2342170202', '1382340202', '$2a$10$7b0etg6LS92nY/hjvTWw1.MEyO6GOBmAW5GueBGwtJSP1my1dhVom', '邓紫棋',    '2342170202', '艺术与传媒学院',     2, 96, '全开麦唱作人，今晚王者荣耀五排上分来会玩的！', 1, 0),
('2342170203', '1382340203', '$2a$10$o76eG9r28nh2oSp4R1UZsu9mw99KIr8o0ghE7SMdAjh8rZQbU69eC', 'Uzi简自豪', '2342170203', '大数据人工智能学院', 1, 82, '永远滴神！王者/LOL打野射手带飞，吃鸡也能刚枪', 1, 0),
('2342170204', '1382340204', '$2a$10$doSQmUOhU9yemdMtiWfrFur9M8lCfEeTpJGrLcIqiZMCHHptDARBy', '贾玲',      '2342170204', '艺术与传媒学院',     2, 90, '热辣滚烫进行中！夜跑固定车队+狼人杀欢乐局', 1, 0),
('2342170205', '1382340205', '$2a$10$Cqe3b8upEoivaSXU11cEEumkSmltcqDwvCVkG5hUkfcsl2/vHwMx6', '周杰伦',    '2342170205', '艺术与传媒学院',     1, 75, '哎哟不错哦，快使用双截棍！篮球场3v3和晨跑算我一个', 1, 0),

-- 班级 03（2342170301 ~ 2342170305）
('2342170301', '1382340301', '$2a$10$Pvm2cBivfcel6GNTwZynoO9kZ9c.ode0A1BgLZsDoo2RVFZcwfQs.', '杨幂',      '2342170301', '艺术与传媒学院',     2, 91, '体育馆羽毛球女生双打约球，剧本杀狼人杀高玩！', 1, 0),
('2342170302', '1382340302', '$2a$10$bVcZozYk0anexpnOxghkbehBoAodPeQ6YaSjkrlYTkaANCnHm/.dG', 'C罗',       '2342170302', '体育学院',           1, 83, 'Siuuu！拒绝可乐只喝白水，绿茵场足球友谊赛发起人', 1, 0),
('2342170303', '1382340303', '$2a$10$Tr4oS/HTwMtiUOfucSO4XuTpedKLS1pZ4TRCKMa7sVQSda/BSyzXW', '孙颖莎',    '2342170303', '体育学院',           2, 97, '莎莎来啦！国乒小魔王，业余时间喜欢打王者中单法师', 1, 0),
('2342170304', '1382340304', '$2a$10$fhEfrMyUG50.axYeDtr/juygfFaq9x48MHW.yYvcOMpSYa4sIAsda', '苏炳添',    '2342170304', '体育学院',           1, 86, '9秒83亚洲飞人！每天早上6点操场晨跑打卡发起人', 1, 0),
('2342170305', '1382340305', '$2a$10$.mh7tLmePItFz4wkNbQMduQiVfcV0tnlsBacIYOinrNfv6ZDKiGgy', '张伟丽',    '2342170305', '体育学院',           2, 93, '八角笼女王，羽毛球扣杀和LOL内战随时奉陪！', 1, 0);

-- =====================================================
-- 2. 用户-兴趣标签关联
--    已有的标签: 篮球(1) 羽毛球(2) 乒乓球(3) 足球(4)
--                王者荣耀(5) 英雄联盟(6) 和平精英(7)
--                剧本杀(8) 狼人杀(9) 三国杀(10)
--                晨跑(11) 夜跑(12)
-- =====================================================
INSERT INTO `user_tag` (`user_id`, `tag_id`, `self_level`) VALUES
-- 姚明: 篮球、王者荣耀、晨跑
(2, 1, 5), (2, 5, 3), (2, 11, 3),
-- 谷爱凌: 羽毛球、剧本杀、夜跑
(3, 2, 5), (3, 8, 4), (3, 12, 4),
-- 雷军: 篮球、晨跑
(4, 1, 4), (4, 11, 3),
-- 陈雨菲: 羽毛球、夜跑
(5, 2, 5), (5, 12, 4),
-- 梅西: 王者荣耀、足球、英雄联盟
(6, 5, 3), (6, 4, 5), (6, 6, 4),
-- 刘亦菲: 羽毛球、剧本杀、夜跑
(7, 2, 4), (7, 8, 5), (7, 12, 3),
-- 科比: 篮球、英雄联盟
(8, 1, 5), (8, 6, 4),
-- 马龙: 乒乓球、剧本杀、三国杀
(9, 3, 5), (9, 8, 3), (9, 10, 4),
-- 邓紫棋: 羽毛球、三国杀、王者荣耀
(10, 2, 3), (10, 10, 4), (10, 5, 5),
-- Uzi简自豪: 王者荣耀、和平精英、英雄联盟
(11, 5, 5), (11, 7, 4), (11, 6, 5),
-- 贾玲: 狼人杀、夜跑
(12, 9, 4), (12, 12, 5),
-- 周杰伦: 篮球、晨跑
(13, 1, 3), (13, 11, 4),
-- 杨幂: 羽毛球、剧本杀、狼人杀
(14, 2, 4), (14, 8, 5), (14, 9, 3),
-- C罗: 足球、乒乓球
(15, 4, 5), (15, 3, 2),
-- 孙颖莎: 乒乓球、王者荣耀、三国杀
(16, 3, 5), (16, 5, 4), (16, 10, 3),
-- 苏炳添: 篮球、乒乓球、晨跑
(17, 1, 4), (17, 3, 3), (17, 11, 5),
-- 张伟丽: 羽毛球、英雄联盟
(18, 2, 5), (18, 6, 3);

-- =====================================================
-- 3. 组局帖子（post）— 采用相对时间(NOW() + INTERVAL)，确保任何时候启动均在广场热招
--    status: RECRUITING(招募中) / FULL(已满员) / ONGOING(进行中) / FINISHED(已结束) / CANCELLED(已取消)
-- =====================================================
INSERT INTO `post` (`id`, `publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`, `location`, `max_people`, `current_people`, `require_level`, `require_gender`, `description`, `status`, `checkin_code`, `view_count`, `like_count`, `is_audit_pass`) VALUES

-- RECRUITING - 正在招募的帖子（未来 1~4 天）
(1, 4,  '雷总带你打篮球3v3！交个朋友', '球类',   '1',    DATE_ADD(NOW(), INTERVAL 26 HOUR), DATE_ADD(NOW(), INTERVAL 29 HOUR), '东区篮球场',     6, 3, 3, 0, 'Are you OK？缺两个队友，求篮球搭子！水平熟手段位以上，打完请大家喝小米冰美式！', 'RECRUITING', NULL, 156, 15, 1),
(2, 10, '邓紫棋带队：王者荣耀五排上分', '电竞',  '5,6',  DATE_ADD(NOW(), INTERVAL 8 HOUR),  DATE_ADD(NOW(), INTERVAL 11 HOUR), '线上-王者荣耀',  5, 2, 4, 0, '全开麦语音五排！高手段位以上，来会玩的野王和辅助~', 'RECRUITING', NULL, 220, 28, 1),
(3, 14, '杨幂约球：女生羽毛球双打局',   '球类',  '2',    DATE_ADD(NOW(), INTERVAL 42 HOUR), DATE_ADD(NOW(), INTERVAL 44 HOUR), '体育馆羽毛球场', 4, 2, 0, 2, '仅限女生，新手友好，打完球可以一起讨论剧本杀！', 'RECRUITING', NULL, 134, 19, 1),
(4, 7,  '刘亦菲车头：周五剧本杀《月落洼》', '桌游', '8', DATE_ADD(NOW(), INTERVAL 52 HOUR), DATE_ADD(NOW(), INTERVAL 56 HOUR), '南门剧本杀店',   5, 1, 0, 0, '经典沉浸推理本，缺4位戏精搭子，费用AA，不跳车！', 'RECRUITING', NULL, 189, 26, 1),

-- FULL - 已满员，等待开始
(5, 6,  '梅西组织：英雄联盟5v5传奇内战', '电竞', '6',    DATE_ADD(NOW(), INTERVAL 72 HOUR), DATE_ADD(NOW(), INTERVAL 75 HOUR), '线上-英雄联盟',  5, 5, 0, 0, '5V5自定义内战，目前首发已满员，可围观候补', 'FULL', NULL, 145, 14, 1),

-- ONGOING - 正在进行中（带 4 位现场签到码 8848）
(6, 17, '苏炳添领跑：9秒83科学晨跑打卡', '跑步', '11',   DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_ADD(NOW(), INTERVAL 100 MINUTE), '操场',      10, 4, 0, 0, '亚洲飞人带你热身起跑！现场凭4位签到码【8848】核验出勤', 'ONGOING', '8848', 167, 27, 1),

-- FINISHED - 已结束
(7, 9,  '马龙发起：三国杀国标面基局',   '桌游',  '10',   DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 93 HOUR), '图书馆研讨室',   4, 4, 0, 0, '六边形战士来三国杀高手对决', 'FINISHED', NULL, 92, 10, 1),
(8, 8,  '科比曼巴局：西区篮球全场5v5',  '球类',  '1',    DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 46 HOUR), '西区篮球场',     10, 10, 2, 0, '曼巴精神永不熄，全场5v5满员激战', 'FINISHED', NULL, 278, 36, 1),
(9, 5,  '陈雨菲带队：环校路女生夜跑团', '跑步',  '12',   DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 71 HOUR), '环校路',         8, 5, 0, 2, '女生夜跑团，配速7分，跑完拉伸', 'FINISHED', NULL, 110, 12, 1),

-- CANCELLED - 已取消
(10, 15, 'C罗Siuuu：周末绿茵足球友谊赛', '球类', '4',    DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 22 HOUR), '足球场',         22, 2, 0, 0, '因暴雨场地积水暂改期取消', 'CANCELLED', NULL, 63, 5, 1);

-- =====================================================
-- 4. 报名记录（post_apply）
--    status: PENDING(待审核) / APPROVED(已加入) / REJECTED(已拒绝)
--            CANCELLED(已退出) / ATTENDED(已参加) / NOSHOW(爽约)
-- =====================================================
INSERT INTO `post_apply` (`post_id`, `applicant_id`, `status`, `apply_time`, `handle_time`) VALUES

-- 帖子1 篮球3v3 的报名
(1, 8,  'APPROVED', '2026-06-28 14:00:00', '2026-06-28 15:00:00'),
(1, 13, 'PENDING',  '2026-06-29 10:00:00', NULL),

-- 帖子2 王者荣耀 的报名
(2, 11, 'APPROVED', '2026-06-28 20:00:00', '2026-06-28 21:00:00'),
(2, 16, 'PENDING',  '2026-06-29 12:00:00', NULL),
(2, 6,  'REJECTED', '2026-06-28 22:00:00', '2026-06-28 23:00:00'),

-- 帖子3 羽毛球 的报名
(3, 12, 'PENDING',  '2026-06-29 08:00:00', NULL),

-- 帖子5 英雄联盟 已满
(5, 11, 'APPROVED', '2026-06-27 18:00:00', '2026-06-27 19:00:00'),
(5, 16, 'APPROVED', '2026-06-27 18:30:00', '2026-06-27 19:00:00'),
(5, 18, 'APPROVED', '2026-06-27 19:00:00', '2026-06-27 19:30:00'),

-- 帖子6 晨跑 已参加
(6, 4,  'ATTENDED', '2026-06-28 05:30:00', '2026-06-28 06:00:00'),
(6, 13, 'ATTENDED', '2026-06-28 05:45:00', '2026-06-28 06:00:00'),
(6, 17, 'ATTENDED', '2026-06-28 05:50:00', '2026-06-28 06:00:00'),

-- 帖子7 三国杀 已结束
(7, 4,  'ATTENDED', '2026-06-24 15:00:00', '2026-06-24 16:00:00'),
(7, 7,  'ATTENDED', '2026-06-24 15:30:00', '2026-06-24 16:00:00'),
(7, 10, 'ATTENDED', '2026-06-24 16:00:00', '2026-06-24 16:30:00'),

-- 帖子8 篮球全场 已结束
(8, 4,  'ATTENDED', '2026-06-26 15:00:00', '2026-06-26 15:30:00'),
(8, 13, 'ATTENDED', '2026-06-26 15:30:00', '2026-06-26 16:00:00'),
(8, 17, 'ATTENDED', '2026-06-26 15:30:00', '2026-06-26 16:00:00'),
(8, 6,  'ATTENDED', '2026-06-26 14:00:00', '2026-06-26 15:00:00'),
(8, 9,  'ATTENDED', '2026-06-26 14:00:00', '2026-06-26 15:00:00'),
(8, 11, 'NOSHOW',   '2026-06-26 15:00:00', '2026-06-27 10:00:00'),
(8, 15, 'ATTENDED', '2026-06-26 14:30:00', '2026-06-26 15:00:00'),
(8, 5,  'CANCELLED','2026-06-26 10:00:00', '2026-06-26 11:00:00'),

-- 帖子9 夜跑 已结束
(9, 7,  'ATTENDED', '2026-06-25 19:00:00', '2026-06-25 19:30:00'),
(9, 10, 'ATTENDED', '2026-06-25 19:30:00', '2026-06-25 20:00:00'),
(9, 12, 'ATTENDED', '2026-06-25 18:00:00', '2026-06-25 19:00:00'),
(9, 14, 'ATTENDED', '2026-06-25 19:00:00', '2026-06-25 19:30:00');

-- =====================================================
-- 5. 评论（post_comment）
-- =====================================================
INSERT INTO `post_comment` (`post_id`, `user_id`, `content`, `created_time`) VALUES
(1, 8,  '求拉！我打控卫的',                        '2026-06-28 14:30:00'),
(2, 11, '我玩打野，带飞',                          '2026-06-28 20:30:00'),
(2, 16, '还有位置吗？我法师',                      '2026-06-29 08:00:00'),
(4, 14, '这个本好玩吗？没玩过',                    '2026-06-28 19:00:00'),
(7, 4,  '三国杀真不错，下次再约',                  '2026-06-25 18:30:00'),
(7, 7,  '赢了输了？',                              '2026-06-25 19:00:00'),
(8, 17, '今天打得真爽，下次继续',                  '2026-06-27 18:30:00'),
(9, 12, '夜跑太舒服了，求固定车队',                '2026-06-26 21:00:00'),
(6, 4,  '明天继续！',                              '2026-06-29 07:00:00'),
(3, 12, '新手可以吗，想学羽毛球',                  '2026-06-29 09:00:00');

-- =====================================================
-- 6. 评价（review）
--    rating: 1-5 星
--    tag: 靠谱 / 爽约 / 迟到
-- =====================================================
INSERT INTO `review` (`post_id`, `reviewer_id`, `target_user_id`, `rating`, `tag`, `content`, `created_time`) VALUES

-- 帖子8 篮球全场 的评价
(8, 4,  13, 4, '靠谱', '配合不错',                '2026-06-27 19:00:00'),
(8, 4,  17, 5, '靠谱', '大腿！',                   '2026-06-27 19:00:00'),
(8, 13, 4,  5, '靠谱', '好队友',                   '2026-06-27 19:30:00'),
(8, 13, 17, 4, '靠谱', '下次还一起',               '2026-06-27 19:30:00'),
(8, 17, 4,  5, '靠谱', '组织得很好',               '2026-06-27 20:00:00'),
(8, 17, 13, 4, '靠谱', '很积极',                   '2026-06-27 20:00:00'),
(8, 4,  11, 1, '爽约', '说好了来结果没来',         '2026-06-27 19:00:00'),
(8, 17, 11, 2, '爽约', '放鸽子了',                 '2026-06-27 20:00:00'),

-- 帖子7 三国杀 的评价
(7, 4,  7,  5, '靠谱', '玩得很好',                 '2026-06-25 19:00:00'),
(7, 4,  10, 4, '靠谱', '新手但很有潜力',           '2026-06-25 19:00:00'),
(7, 7,  4,  5, '靠谱', '老司机带飞',               '2026-06-25 19:30:00'),
(7, 10, 4,  5, '靠谱', '很有耐心教新人',           '2026-06-25 20:00:00'),

-- 帖子9 夜跑 的评价
(9, 7,  10, 4, '靠谱', '配速很稳',                 '2026-06-26 22:00:00'),
(9, 7,  12, 3, '靠谱', '还行',                     '2026-06-26 22:00:00'),
(9, 10, 7,  5, '靠谱', '领跑很棒',                 '2026-06-26 22:30:00'),
(9, 12, 7,  4, '靠谱', '下次继续约',               '2026-06-26 22:30:00');

-- =====================================================
-- 7. 更新帖子当前人数（使其与报名记录一致）
-- =====================================================
-- 帖子1: 已通过2人 + 发布者 = 3（当前已是3）
-- 帖子2: 已通过1人 + 发布者 = 2（当前已是2）
-- 帖子5: 已通过3人 + 发布者 = 4，但 max=5，且已满员
UPDATE `post` SET `current_people` = 5 WHERE `id` = 5;
-- 帖子6: 已参加3人 + 发布者 = 4（当前已是4）
-- 帖子7: 已参加3人 + 发布者 = 4（当前已是4）
-- 帖子8: 已参加6人 + 发布者 = 7，但实际满员
UPDATE `post` SET `current_people` = 10 WHERE `id` = 8;
-- 帖子9: 已参加4人 + 发布者 = 5（当前已是5）

-- =====================================================
-- 校园搭子系统 - 更多测试数据（补充脚本）
-- 使用前提：先执行 schema.sql，再执行 test-data.sql
-- 使用方法：source database/test-data-more.sql;
--
-- 新增内容：
--   1. 新增兴趣标签（观影、学习、美食、散步、音乐、摄影、健身）
--   2. 新增30个学生
--      2342170106~0110（大数据人工智能学院 01班）
--      2342170206~0210（大数据人工智能学院 02班）
--      2332170101~0110（计算机学院 01班）
--      2332170201~0210（计算机学院 02班）
--   3. 新人友好的组局帖子
--   4. 更多评论和评价
-- =====================================================

USE `school_buddy`;

-- =====================================================
-- 1. 新增兴趣标签（id 从13开始）
-- =====================================================
INSERT INTO `interest_tag` (`category`, `name`, `sort_order`) VALUES
('学习', '图书馆学习', 40),
('影视', '观影追剧', 41),
('美食', '美食探店', 42),
('休闲', '散步聊天', 43),
('音乐', '音乐交流', 44),
('摄影', '摄影扫街', 45),
('健身', '健身撸铁', 46);

-- =====================================================
-- 2. 新增30个学生（user_id 从19开始）
--    已存在: admin001=1, 20240001=2, 20240002=3,
--            2342170101~0305=4~18
-- =====================================================
INSERT INTO `sys_user` (`student_no`, `phone`, `password`, `nickname`, `username`, `college`, `gender`, `credit_score`, `signature`, `status`, `is_admin`) VALUES

-- 大数据人工智能学院 01班 编号06~10
('2342170106', '1382340106', '$2a$10$sc6s3w.Vt3vJcwgUla29dufnOB/P5qqMwsGt2Id7uDCYQ.AUngVNK', '黄仁勋',        '2342170106', '大数据人工智能学院', 1, 87, '皮衣刀客，图书馆算力拉满，学完去逛夜市美食探店！', 1, 0),
('2342170107', '1382340107', '$2a$10$CoeLgrEaH0LOA8inbAQr5OgNVaB06eROnvYmfjvTs5eruefWYEC8.', '赵丽颖',        '2342170107', '艺术与传媒学院',     2, 94, '北门新开的日料探店发起人，追剧夜跑两不误~', 1, 0),
('2342170108', '1382340108', '$2a$10$4HEsWkOFLQfjRp771xT63OoB9Cckf.lJeWyD6t6eoXAswJm2GirDu', '陈奕迅',        '2342170108', '艺术与传媒学院',     1, 80, '十年之后我们还是音乐搭子，操场草坪听歌分享', 1, 0),
('2342170109', '1382340109', '$2a$10$CocqgPGBcxfUJfs7ape2g.6XvGkOHPO6KxYv/kqPYGjzCkh/A.XJi', '李子柒',        '2342170109', '艺术与传媒学院',     2, 99, '操场草坪音乐歌单分享会主理人，校园美食活地图', 1, 0),
('2342170110', '1382340110', '$2a$10$CpBDmVyDN/ElHqaj6DvRreI0OAhwjReDBIZ8HkHSMxwjYjCnGuyLu', '新海诚',        '2342170110', '艺术与传媒学院',     1, 76, '每一帧校园晚霞都像动漫，摄影扫街约起来', 1, 0),

-- 大数据人工智能学院 02班 编号06~10
('2342170206', '1382340206', '$2a$10$7RDQ.M/cLV.q.ti.ck7M5eS1TGShdAfXFhLdyV60nDA6y3SXz3/O.', '王祉怡',        '2342170206', '体育学院',           2, 91, '羽毛球拉吊突击，打完球一起去南门探店', 1, 0),
('2342170207', '1382340207', '$2a$10$d0aYusYqaI.YjQw1mk6D5OniSJHEQsHEthImKSnl0j2oVKkSuREr.', '林俊杰',        '2342170207', '艺术与传媒学院',     1, 84, '行走的CD，晨跑练肺活量，随时随地聊音乐创作', 1, 0),
('2342170208', '1382340208', '$2a$10$SOUcMWqVFHnM.P1/IfetpOO8I5/0aBKWE.i9iLmvhFWpb6UQoPFf.', '屠呦呦',        '2342170208', '生命科学学院',       2, 88, '泡在图书馆查文献冲期末，傍晚去操场散步吹风', 1, 0),
('2342170209', '1382340209', '$2a$10$jGCLLMuItwcBLu2S99/OfuVNFA1V5/9MsYa.Xeb2FBwQoQBUWIniG', '姆巴佩',        '2342170209', '体育学院',           1, 79, '边路超跑！绿茵场踢球+学校健身房力量训练', 1, 0),
('2342170210', '1382340210', '$2a$10$g.4ReZ36gxq/aQVkBSoY0O0ORo7jr92GvybS0U/EsfyRVaWv/dlw6', '阿加莎',        '2342170210', '文学院',             2, 95, '周末悬疑电影追剧专场发起人，高阶狼人杀推理控', 1, 0),

-- 计算机学院 01班 编号01~10
('2332170101', '1382330101', '$2a$10$oq5AQ3VD775cSTcMV8CMwOT8EEfDEyFsP1Uon/3MQPBi/x6iyuRha', '库里',          '2332170101', '体育学院',           1, 82, '晚安金州！周末东区篮球场半场养生三分局发起人', 1, 0),
('2332170102', '1382330102', '$2a$10$lIhSIfA.KXqGCelpC0TqL.pKfyCpTiCtjO2l/rRsZwYI5iofKW.km', '泰勒·斯威夫特', '2332170102', '艺术与传媒学院',     2, 96, '图书馆写歌自习，研讨室新手狼人杀教学局法官', 1, 0),
('2332170103', '1382330103', '$2a$10$AiTvKd8Vxsp2roAbj02vaeiBDTqrrga/A7jJo.29B0TUWRCf/Yqzq', '梦泪',          '2332170103', '计算机学院',         1, 73, '韩信无兵线偷塔！王者荣耀娱乐局、吃鸡摇人', 1, 0),
('2332170104', '1382330104', '$2a$10$S2QwvxFZnNUjEgMG8UVTa.iE7NlHaxarMWrIkubVRBEN.yubmX1Ai', '奥黛丽·赫本',   '2332170104', '艺术与传媒学院',     2, 90, '周末万达影城看电影发起人，晚饭后操场散步聊天', 1, 0),
('2332170105', '1382330105', '$2a$10$21HejA9qBXqTEfhM9QVpmul4upxI/KarSAImAJbCOAk0Lr74pTff6', '施瓦辛格',      '2332170105', '体育学院',           1, 85, 'I will be back！学校健身房撸铁保护，偶尔也拍校园夕阳', 1, 0),
('2332170106', '1382330106', '$2a$10$dOcYOzyx5YAYu4A54SpWs.4bvexjKUQQK1oDjJnjgXis3ikW/Hkei', '迪丽热巴',      '2332170106', '艺术与传媒学院',     2, 92, '周末探店学校周边宝藏小店发起人，边吃边逛边拍照！', 1, 0),
('2332170107', '1382330107', '$2a$10$tBIX7RWM1nTRF9z7do1Z6uAfad6FiiAsmpsCR9gIRjv2zR0BdZQcC', '基普乔格',      '2332170107', '体育学院',           1, 78, '人类没有极限！早6点半操场晨跑，新手也能跟', 1, 0),
('2332170108', '1382330108', '$2a$10$B/yEb2cOEsywQJitz1AaQ.B8/fwEe3qa2Lwge/ddVqBwXINySfPzG', '坂本龙一',      '2332170108', '艺术与传媒学院',     1, 87, '晚饭后操场散步消食局发起人，吹着晚风聊音乐', 1, 0),
('2332170109', '1382330109', '$2a$10$7slGYpoh.omXFAJAW5JN9uri.Jup5uGNiPd6y6L9GnqIXf0ZNnHU2', '黄雅琼',        '2332170109', '体育学院',           2, 83, '混双奥运金牌得主，体育馆羽毛球新手友好局发起人！', 1, 0),
('2332170110', '1382330110', '$2a$10$XJNygJqDjldwLiln6PW3rOg4ROjzLeBVo63rFfQZkIozsQQW7BCNy', '一诺',          '2332170110', '计算机学院',         1, 88, 'KPL顶级射手，今晚8点王者荣耀娱乐局发起人，不压力！', 1, 0),

-- 计算机学院 02班 编号01~10
('2332170201', '1382330201', '$2a$10$x/T20Otq6m9ctii4D.jQhervGsZXskykde1860YZp8y7GfUKdHNO.', '撒贝宁',        '2332170201', '法学院',             1, 81, '明灯高照！研讨室狼人杀新手车发起人，搞笑推理担当', 1, 0),
('2332170202', '1382330202', '$2a$10$wpQwrO5pU2htsYtD2YHD/uvVxTk6zt/rpklxEzKIOjVyKHka6LVQ6', '白鹿',          '2332170202', '艺术与传媒学院',     2, 94, '北区食堂二楼新开的麻辣烫约饭发起人，干饭积极分子！', 1, 0),
('2332170203', '1382330203', '$2a$10$DDVvUiwIO6bIUu.aHW7x6uwzGhcL8IDPFMbFS46sNNHQ9lXN.fufa', '刘畊宏',        '2332170203', '体育学院',           1, 77, '本草纲目练起来！学校健身房练胸日发起人，新手手把手教', 1, 0),
('2332170204', '1382330204', '$2a$10$KnZtiCtQkudz5QYsyY7oROKMfFOk4JLh7ke2Y52PLneRn8lPHjZ6e', '董卿',          '2332170204', '文学院',             2, 91, '图书馆三楼自习区学习搭子发起人，互相监督不玩手机', 1, 0),
('2332170205', '1382330205', '$2a$10$T2/rMdOmz1ZVh2z.eW2R8Op1jgPE5AM1hXhBMB.0yOhhmFfmFQckG', '詹姆斯',        '2332170205', '体育学院',           1, 86, 'Taco Tuesday！学校健身房练背打卡发起人，篮球晨跑全能', 1, 0),
('2332170206', '1382330206', '$2a$10$V07Eythl71ffiwiN0dW2jOzAEOeEAkL6cNtgdfMH0IONd5ZKN848K', '杨紫',          '2332170206', '艺术与传媒学院',     2, 89, '图书馆四楼期末冲刺局发起人，中午一起去食堂干饭！', 1, 0),
('2332170207', '1382330207', '$2a$10$RMznYE88gFRV/BKmcgJrc.7Y.F/kuyNFI950xBEGNphIlFFE9uilK', 'Faker李相赫',   '2332170207', '计算机学院',         1, 75, 'LOL五冠大魔王，傍晚操场拍晚霞发起人，偶尔也去撸铁', 1, 0),
('2332170208', '1382330208', '$2a$10$h1fu6RUX7crOtfBMR8/2NOysQCwheAUxMomJtrjHSmavsG3gmNe82', '陈漫',          '2332170208', '艺术与传媒学院',     2, 97, '视觉摄影大师，下午5点半图书馆门口集合拍校园夕阳！', 1, 0),
('2332170209', '1382330209', '$2a$10$SEOd/SBcoUBLfGoaRdkuhO4O/JUlp/PzZDVMstN6huO1.aLGOAIr.', '何炅',          '2332170209', '艺术与传媒学院',     1, 84, '高情商控场王，剧本杀温柔车头、散步聊天最佳倾听者', 1, 0),
('2332170210', '1382330210', '$2a$10$LOsbHpCCgz2BiOsisSYFsuMQ1Ygw4iUQ2y5EWi68RwRGINwEfVmWG', '王鸥',          '2332170210', '艺术与传媒学院',     2, 93, '南门外剧本杀《窗边的女人》发起人，直觉女神带你盘凶手', 1, 0);

-- =====================================================
-- 3. 用户-兴趣标签关联
-- =====================================================
INSERT INTO `user_tag` (`user_id`, `tag_id`, `self_level`) VALUES

-- 大数据人工智能学院 01班 编号06~10
(19, 14, 3), (19, 15, 4),  -- 黄仁勋: 图书馆学习、美食探店
(20, 13, 5), (20, 12, 3),  -- 赵丽颖: 观影追剧、夜跑
(21, 17, 4), (21, 3, 2),   -- 陈奕迅: 音乐交流、乒乓球
(22, 15, 5), (22, 16, 4), (22, 17, 5), -- 李子柒: 美食探店、散步聊天、音乐交流
(23, 16, 3), (23, 18, 4),  -- 新海诚: 散步聊天、摄影扫街

-- 大数据人工智能学院 02班 编号06~10
(24, 2, 5), (24, 15, 3),   -- 王祉怡: 羽毛球、美食探店
(25, 11, 4), (25, 17, 5),  -- 林俊杰: 晨跑、音乐交流
(26, 14, 5), (26, 16, 3),  -- 屠呦呦: 图书馆学习、散步聊天
(27, 4, 4), (27, 19, 4),   -- 姆巴佩: 足球、健身撸铁
(28, 13, 4), (28, 9, 5),   -- 阿加莎: 观影追剧、狼人杀

-- 计算机学院 01班 编号01~10
(29, 1, 5), (29, 5, 4),    -- 库里: 篮球、王者荣耀
(30, 14, 4), (30, 17, 5), (30, 9, 4),  -- 泰勒·斯威夫特: 图书馆学习、音乐交流、狼人杀
(31, 5, 5), (31, 7, 3),    -- 梦泪: 王者荣耀、和平精英
(32, 13, 5), (32, 16, 4),  -- 奥黛丽·赫本: 观影追剧、散步聊天
(33, 18, 3), (33, 19, 5),  -- 施瓦辛格: 摄影扫街、健身撸铁
(34, 15, 5), (34, 12, 3),  -- 迪丽热巴: 美食探店、夜跑
(35, 11, 5), (35, 19, 4),  -- 基普乔格: 晨跑、健身撸铁
(36, 17, 5), (36, 16, 4),  -- 坂本龙一: 音乐交流、散步聊天
(37, 2, 5), (37, 14, 3),   -- 黄雅琼: 羽毛球、图书馆学习
(38, 5, 5), (38, 7, 4),    -- 一诺: 王者荣耀、和平精英

-- 计算机学院 02班 编号01~10
(39, 8, 4), (39, 9, 4),    -- 撒贝宁: 剧本杀、狼人杀
(40, 14, 5), (40, 15, 4),  -- 白鹿: 图书馆学习、美食探店
(41, 19, 5), (41, 4, 3),   -- 刘畊宏: 健身撸铁、足球
(42, 16, 5), (42, 13, 4), (42, 14, 5), -- 董卿: 散步聊天、观影追剧、图书馆学习
(43, 1, 5), (43, 11, 3), (43, 19, 4),  -- 詹姆斯: 篮球、晨跑、健身撸铁
(44, 15, 4), (44, 14, 4),  -- 杨紫: 美食探店、图书馆学习
(45, 6, 5), (45, 19, 3), (45, 18, 4),  -- Faker李相赫: 英雄联盟、健身撸铁、摄影扫街
(46, 18, 5), (46, 17, 4),  -- 陈漫: 摄影扫街、音乐交流
(47, 8, 4), (47, 16, 5),   -- 何炅: 剧本杀、散步聊天
(48, 2, 3), (48, 9, 4), (48, 8, 5);    -- 王鸥: 羽毛球、狼人杀、剧本杀

-- =====================================================
-- 4. 更多名人组局帖子（post，id从11开始，使用相对时间保持热招状态）
-- =====================================================
INSERT INTO `post` (`id`, `publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`, `location`, `max_people`, `current_people`, `require_level`, `require_gender`, `description`, `status`, `checkin_code`, `view_count`, `like_count`, `is_audit_pass`) VALUES

-- 招募中 - 新人友好（未来 6~96 小时）
(11, 32, '赫本邀约：周末万达一起看经典电影', '影视', '13', DATE_ADD(NOW(), INTERVAL 48 HOUR), DATE_ADD(NOW(), INTERVAL 51 HOUR), '万达影城',         4, 2, 0, 2, '最近想看的院线电影很多，找姐妹一起去看！看完可以顺便逛街喝下午茶~', 'RECRUITING', NULL, 145, 16, 1),
(12, 42, '董卿领读：图书馆三楼沉浸式学习搭子', '学习', '14', DATE_ADD(NOW(), INTERVAL 18 HOUR), DATE_ADD(NOW(), INTERVAL 26 HOUR), '图书馆三楼自习区', 6, 2, 0, 0, '期末复习冲刺局，互相监督静音不玩手机，学完一起去食堂聚餐', 'RECRUITING', NULL, 178, 29, 1),
(13, 40, '白鹿干饭局：北区食堂二楼新开麻辣烫', '美食', '15', DATE_ADD(NOW(), INTERVAL 22 HOUR), DATE_ADD(NOW(), INTERVAL 24 HOUR), '北区食堂二楼',     4, 1, 0, 0, '听说新开的黏糊麻辣烫超级香，找干饭搭子一起去尝鲜，费用AA！', 'RECRUITING', NULL, 134, 14, 1),
(14, 36, '坂本龙一：晚饭后操场吹风散步消食',   '休闲', '16', DATE_ADD(NOW(), INTERVAL 10 HOUR), DATE_ADD(NOW(), INTERVAL 12 HOUR), '学校操场',         8, 4, 0, 0, '吃完晚饭一起绕操场遛弯聊天，聊聊电影配乐与生活，非常轻松治愈', 'RECRUITING', NULL, 125, 13, 1),
(15, 30, '霉霉当法官：研讨室新手狼人杀教学局', '桌游', '9',  DATE_ADD(NOW(), INTERVAL 36 HOUR), DATE_ADD(NOW(), INTERVAL 39 HOUR), '图书馆研讨室',     8, 3, 0, 0, '纯新手欢乐局！我来当法官手把手教大家玩，不会悍跳完全没关系，开心第一', 'RECRUITING', NULL, 220, 35, 1),
(16, 37, '黄雅琼带练：体育馆羽毛球新手和平局', '球类', '2',  DATE_ADD(NOW(), INTERVAL 50 HOUR), DATE_ADD(NOW(), INTERVAL 52 HOUR), '体育馆羽毛球场',   4, 2, 0, 0, '奥运混双冠军手把手教握拍与高远球，欢迎萌新和熟手搭子一起来出汗！', 'RECRUITING', NULL, 156, 25, 1),
(17, 34, '迪丽热巴带队：周末探店学校南门宝藏小店', '美食', '15', DATE_ADD(NOW(), INTERVAL 64 HOUR), DATE_ADD(NOW(), INTERVAL 70 HOUR), '学校南门集合', 6, 1, 0, 2, '发掘学校周边好吃的甜品与特色小吃，边吃边逛边拍美照，轻松周末', 'RECRUITING', NULL, 167, 18, 1),
(18, 46, '陈漫大师课：摄影扫街拍校园绝美夕阳', '摄影', '18', DATE_ADD(NOW(), INTERVAL 28 HOUR), DATE_ADD(NOW(), INTERVAL 30 HOUR), '图书馆门口集合',   8, 2, 0, 0, '喜欢拍照的来！手机相机都行，现场分享构图与逆光人像技巧', 'RECRUITING', NULL, 143, 27, 1),
(19, 35, '基普乔格领跑：早6点半操场有氧晨跑',  '跑步', '11', DATE_ADD(NOW(), INTERVAL 34 HOUR), DATE_ADD(NOW(), INTERVAL 35 HOUR), '操场',             10, 3, 0, 0, '跑不动可以快走！主要是养成早起运动好习惯，配速不限，包教呼吸节奏', 'RECRUITING', NULL, 138, 14, 1),
(20, 22, '李子柒主理：操场草坪星空歌单分享会', '音乐', '17', DATE_ADD(NOW(), INTERVAL 54 HOUR), DATE_ADD(NOW(), INTERVAL 56 HOUR), '操场草坪',         10, 2, 0, 0, '带上耳机或小音箱，围坐草坪分享自己私藏的宝藏歌单，自备桂花酿茶饮', 'RECRUITING', NULL, 152, 31, 1),
(21, 38, 'AG一诺带飞：王者荣耀晚8点快乐五排',  '电竞', '5',  DATE_ADD(NOW(), INTERVAL 12 HOUR), DATE_ADD(NOW(), INTERVAL 14 HOUR), '线上-王者荣耀',    5, 2, 0, 0, '娱乐局不压力！输赢无所谓，开心就好，萌新到宗师都欢迎上车', 'RECRUITING', NULL, 198, 27, 1),
(22, 29, '库里三分训练营：东区篮球半场养生局', '球类', '1',  DATE_ADD(NOW(), INTERVAL 70 HOUR), DATE_ADD(NOW(), INTERVAL 72 HOUR), '东区篮球场',       6, 2, 0, 0, '养生投篮局，出出汗练练三分准度，不冲不撞，欢迎各路球友来玩', 'RECRUITING', NULL, 145, 23, 1),

-- 已满员
(23, 48, '王鸥车头：剧本杀《窗边的女人》新手本', '桌游', '8', DATE_ADD(NOW(), INTERVAL 80 HOUR), DATE_ADD(NOW(), INTERVAL 84 HOUR), '南门外剧本杀店',   6, 6, 0, 0, '新手友好推理本，店家金牌DM带本，目前6人已满车', 'FULL', NULL, 188, 22, 1),

-- 进行中（带 4 位现场签到码 6688 / 2026）
(24, 41, '刘畊宏带练：学校健身房练胸撸铁局',   '健身', '19', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 90 MINUTE), '学校健身房',   4, 3, 0, 1, '腰间盘肥肉咔咔掉！一起练胸互相保护，现场签到码【6688】', 'ONGOING', '6688', 133, 15, 1),
(25, 44, '杨紫陪学：图书馆四楼全天期末冲刺',   '学习', '14', DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_ADD(NOW(), INTERVAL 4 HOUR),    '图书馆四楼',   6, 4, 0, 0, '期末冲刺一整天！中午一起吃食堂，现场签到码【2026】', 'ONGOING', '2026', 167, 25, 1),

-- 已结束
(26, 43, '詹姆斯核心局：学校健身房练背打卡',   '健身', '19', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 118 HOUR), '学校健身房',       3, 3, 0, 0, '练背日，核心力量拉满，效果很好', 'FINISHED', NULL, 141, 14, 1),
(27, 28, '阿加莎专场：周末烧脑悬疑电影鉴赏',   '影视', '13', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 93 HOUR),  '宿舍活动室',       6, 4, 0, 2, '一起看阿加莎经典悬疑电影，边看边推理凶手，气氛超好', 'FINISHED', NULL, 156, 18, 1),
(28, 20, '赵丽颖约饭：北门新开的日料店探店',   '美食', '15', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 70 HOUR),  '北门日料店',       4, 4, 0, 0, '新开的寿喜烧日料店，四人拼团很划算，圆满打卡', 'FINISHED', NULL, 172, 20, 1),
(29, 45, 'Faker约拍：看晚霞！操场火烧云摄影',  '摄影', '18', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 46 HOUR),  '操场',             10, 6, 0, 0, '今天的晚霞绝了，大家都去操场拍大片', 'FINISHED', NULL, 189, 24, 1),
(30, 39, '撒贝宁明灯局：研讨室狼人杀新手车',   '桌游', '9',  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 21 HOUR),  '图书馆研讨室',     8, 8, 0, 0, '全是戏精的一车，小撒明灯指路超欢乐，下次还来', 'FINISHED', NULL, 210, 36, 1);

-- =====================================================
-- 5. 更多报名记录
-- =====================================================
INSERT INTO `post_apply` (`post_id`, `applicant_id`, `status`, `apply_time`, `handle_time`) VALUES

-- 帖子11 看电影
(11, 34, 'APPROVED', '2026-06-28 12:00:00', '2026-06-28 14:00:00'),
(11, 20, 'PENDING',  '2026-06-29 09:00:00', NULL),

-- 帖子12 图书馆学习
(12, 26, 'APPROVED', '2026-06-28 10:00:00', '2026-06-28 11:00:00'),
(12, 40, 'PENDING',  '2026-06-29 08:00:00', NULL),
(12, 37, 'PENDING',  '2026-06-29 10:00:00', NULL),

-- 帖子13 吃麻辣烫
(13, 44, 'PENDING',  '2026-06-29 08:30:00', NULL),
(13, 30, 'PENDING',  '2026-06-29 10:00:00', NULL),

-- 帖子14 散步
(14, 32, 'APPROVED', '2026-06-28 18:00:00', '2026-06-28 19:00:00'),
(14, 42, 'APPROVED', '2026-06-28 19:00:00', '2026-06-28 20:00:00'),
(14, 36, 'APPROVED', '2026-06-28 19:30:00', '2026-06-28 20:00:00'),
(14, 22, 'PENDING',  '2026-06-29 11:00:00', NULL),

-- 帖子15 新手狼人杀
(15, 33, 'APPROVED', '2026-06-28 14:00:00', '2026-06-28 15:00:00'),
(15, 48, 'APPROVED', '2026-06-28 15:00:00', '2026-06-28 16:00:00'),
(15, 38, 'PENDING',  '2026-06-29 09:00:00', NULL),
(15, 31, 'PENDING',  '2026-06-29 10:00:00', NULL),

-- 帖子16 羽毛球新手
(16, 24, 'APPROVED', '2026-06-28 10:00:00', '2026-06-28 11:00:00'),
(16, 30, 'PENDING',  '2026-06-29 08:00:00', NULL),

-- 帖子18 摄影扫街
(18, 33, 'APPROVED', '2026-06-28 16:00:00', '2026-06-28 17:00:00'),
(18, 47, 'PENDING',  '2026-06-29 10:00:00', NULL),

-- 帖子19 晨跑
(19, 35, 'APPROVED', '2026-06-28 06:00:00', '2026-06-28 06:30:00'),
(19, 29, 'APPROVED', '2026-06-28 06:30:00', '2026-06-28 06:30:00'),

-- 帖子20 音乐交流
(20, 21, 'APPROVED', '2026-06-28 18:00:00', '2026-06-28 19:00:00'),
(20, 30, 'PENDING',  '2026-06-29 12:00:00', NULL),

-- 帖子21 王者娱乐局
(21, 31, 'APPROVED', '2026-06-28 20:00:00', '2026-06-28 21:00:00'),
(21, 16, 'PENDING',  '2026-06-29 10:00:00', NULL),
(21, 38, 'PENDING',  '2026-06-29 11:00:00', NULL),

-- 帖子22 养生篮球
(22, 4,  'APPROVED', '2026-06-28 15:00:00', '2026-06-28 16:00:00'),
(22, 43, 'PENDING',  '2026-06-29 09:00:00', NULL),

-- 帖子23 剧本杀 已满
(23, 39, 'APPROVED', '2026-06-27 14:00:00', '2026-06-27 15:00:00'),
(23, 7,  'APPROVED', '2026-06-27 14:30:00', '2026-06-27 15:00:00'),
(23, 14, 'APPROVED', '2026-06-27 15:00:00', '2026-06-27 15:30:00'),
(23, 8,  'APPROVED', '2026-06-27 15:30:00', '2026-06-27 16:00:00'),
(23, 33, 'APPROVED', '2026-06-27 16:00:00', '2026-06-27 16:30:00'),

-- 帖子24 健身进行中
(24, 33, 'ATTENDED', '2026-06-28 17:00:00', '2026-06-28 17:30:00'),
(24, 35, 'ATTENDED', '2026-06-28 17:30:00', '2026-06-28 18:00:00'),

-- 帖子25 图书馆冲刺 进行中
(25, 22, 'ATTENDED', '2026-06-28 07:30:00', '2026-06-28 08:00:00'),
(25, 40, 'ATTENDED', '2026-06-28 08:00:00', '2026-06-28 08:00:00'),
(25, 34, 'ATTENDED', '2026-06-28 08:30:00', '2026-06-28 08:30:00'),

-- 帖子26 健身打卡 已结束
(26, 35, 'ATTENDED', '2026-06-24 16:00:00', '2026-06-24 16:30:00'),
(26, 41, 'ATTENDED', '2026-06-24 16:30:00', '2026-06-24 17:00:00'),

-- 帖子27 追剧 已结束
(27, 26, 'ATTENDED', '2026-06-25 13:00:00', '2026-06-25 13:30:00'),
(27, 32, 'ATTENDED', '2026-06-25 13:30:00', '2026-06-25 14:00:00'),
(27, 44, 'ATTENDED', '2026-06-25 14:00:00', '2026-06-25 14:00:00'),

-- 帖子28 日料探店 已结束
(28, 34, 'ATTENDED', '2026-06-26 11:00:00', '2026-06-26 11:30:00'),
(28, 42, 'ATTENDED', '2026-06-26 11:30:00', '2026-06-26 12:00:00'),
(28, 44, 'ATTENDED', '2026-06-26 11:00:00', '2026-06-26 12:00:00'),

-- 帖子29 拍晚霞 已结束
(29, 46, 'ATTENDED', '2026-06-26 17:00:00', '2026-06-26 17:30:00'),
(29, 21, 'ATTENDED', '2026-06-26 17:30:00', '2026-06-26 17:30:00'),
(29, 36, 'ATTENDED', '2026-06-26 17:30:00', '2026-06-26 17:30:00'),
(29, 18, 'ATTENDED', '2026-06-26 17:00:00', '2026-06-26 17:30:00'),
(29, 24, 'ATTENDED', '2026-06-26 17:30:00', '2026-06-26 18:00:00'),
(29, 12, 'NOSHOW',   '2026-06-26 17:00:00', '2026-06-27 10:00:00'),

-- 帖子30 狼人杀新手车 已结束
(30, 48, 'ATTENDED', '2026-06-27 14:00:00', '2026-06-27 14:30:00'),
(30, 32, 'ATTENDED', '2026-06-27 14:00:00', '2026-06-27 14:30:00'),
(30, 42, 'ATTENDED', '2026-06-27 14:30:00', '2026-06-27 14:30:00'),
(30, 12, 'ATTENDED', '2026-06-27 14:00:00', '2026-06-27 14:30:00'),
(30, 10, 'ATTENDED', '2026-06-27 14:30:00', '2026-06-27 14:30:00'),
(30, 4,  'ATTENDED', '2026-06-27 14:00:00', '2026-06-27 14:30:00'),
(30, 39, 'ATTENDED', '2026-06-27 14:30:00', '2026-06-27 14:30:00');

-- =====================================================
-- 6. 更多评论和评价
-- =====================================================
INSERT INTO `post_comment` (`post_id`, `user_id`, `content`, `created_time`) VALUES
(11, 34, '赫本女神看什么电影呀？算我一个！',     '2026-06-28 12:30:00'),
(12, 26, '和董卿老师一起冲期末！',               '2026-06-28 10:30:00'),
(15, 33, '霉霉法官，新手能教教我吗完全不会悍跳', '2026-06-28 14:30:00'),
(15, 48, '直觉告诉我这把一定很好玩哈哈哈',       '2026-06-28 15:30:00'),
(15, 30, '欢迎大家上车，现场发发言笔记！',       '2026-06-29 10:30:00'),
(18, 33, '陈漫老师，我只有手机可以跟拍吗？',     '2026-06-28 16:30:00'),
(20, 21, '我带吉他来现场弹唱《十年》和周杰伦！', '2026-06-28 18:30:00'),
(21, 31, '一诺打射手，那我拿韩信打野偷塔！',     '2026-06-28 20:30:00'),
(22, 4,  '库里带队的养生三分球我最爱，Are you OK！', '2026-06-28 15:30:00'),
(24, 33, '刘教练，今天练胸还是练背？我来当保护！', '2026-06-28 17:30:00'),
(25, 22, '带了自制桂花糕，学累了一起吃！',       '2026-06-28 08:00:00'),
(26, 43, 'Taco Tuesday！一起练背效果翻倍',       '2026-06-25 17:30:00'),
(28, 34, '这家寿喜烧和牛味道绝了！',             '2026-06-27 12:30:00'),
(29, 46, 'Faker拍的夕阳构图很有大师风范！',      '2026-06-27 18:30:00'),
(30, 48, '小撒明灯指路太欢乐了！下次还要来',     '2026-06-28 18:30:00');

INSERT INTO `review` (`post_id`, `reviewer_id`, `target_user_id`, `rating`, `tag`, `content`, `created_time`) VALUES

-- 帖子26 健身打卡 的评价
(26, 43, 35, 5, '靠谱', '配合很好，下次继续',      '2026-06-25 20:00:00'),
(26, 43, 41, 4, '靠谱', '动作很标准',              '2026-06-25 20:00:00'),
(26, 35, 43, 5, '靠谱', '很不错',                  '2026-06-25 20:30:00'),

-- 帖子27 追剧 的评价
(27, 28, 26, 4, '靠谱', '一起看剧很开心',          '2026-06-26 18:00:00'),
(27, 28, 32, 5, '靠谱', '推荐的电影很好看',        '2026-06-26 18:00:00'),
(27, 28, 44, 4, '靠谱', '专注看剧不玩手机',        '2026-06-26 18:00:00'),
(27, 26, 28, 5, '靠谱', '组织得很好',              '2026-06-26 18:30:00'),
(27, 32, 28, 5, '靠谱', '下次还要一起',            '2026-06-26 18:30:00'),

-- 帖子28 日料探店 的评价
(28, 20, 34, 5, '靠谱', '同样是个吃货！',          '2026-06-27 14:00:00'),
(28, 20, 42, 4, '靠谱', '聊得很开心',              '2026-06-27 14:00:00'),
(28, 20, 44, 4, '靠谱', '很随和',                  '2026-06-27 14:00:00'),
(28, 34, 20, 5, '靠谱', '推荐的地方很好吃',        '2026-06-27 14:30:00'),

-- 帖子29 拍晚霞 的评价
(29, 46, 21, 4, '靠谱', '拍照技术不错',            '2026-06-27 20:00:00'),
(29, 46, 36, 5, '靠谱', '拍得很好看',              '2026-06-27 20:00:00'),
(29, 46, 18, 4, '靠谱', '构图很棒',                '2026-06-27 20:00:00'),
(29, 46, 24, 5, '靠谱', '准时到达',                '2026-06-27 20:00:00'),
(29, 46, 12, 1, '爽约', '说好来结果放鸽子',        '2026-06-27 20:00:00'),
(29, 21, 46, 5, '靠谱', '拍晚霞太美了',            '2026-06-27 20:30:00'),

-- 帖子30 狼人杀新手车 的评价
(30, 39, 48, 4, '靠谱', '玩得很投入',              '2026-06-28 19:00:00'),
(30, 39, 32, 4, '靠谱', '很会推理',                '2026-06-28 19:00:00'),
(30, 39, 42, 5, '靠谱', '最佳女巫！',              '2026-06-28 19:00:00'),
(30, 39, 12, 3, '靠谱', '还行',                    '2026-06-28 19:00:00'),
(30, 39, 10, 4, '靠谱', '很欢乐',                  '2026-06-28 19:00:00'),
(30, 39, 4,  5, '靠谱', '发言很搞笑哈哈哈',        '2026-06-28 19:00:00'),
(30, 48, 39, 5, '靠谱', '组织得很好！新手体验满分','2026-06-28 19:30:00'),
(30, 32, 39, 5, '靠谱', '下次再约' ,               '2026-06-28 19:30:00');

-- =====================================================
-- 7. 更新帖子当前人数（与已通过报名记录保持严格一致，且不超过 max_people）
-- =====================================================
UPDATE `post` SET `current_people` = 2 WHERE `id` = 11;
UPDATE `post` SET `current_people` = 2 WHERE `id` = 12;
UPDATE `post` SET `current_people` = 4 WHERE `id` = 14;
UPDATE `post` SET `current_people` = 3 WHERE `id` = 15;
UPDATE `post` SET `current_people` = 3 WHERE `id` = 19;
UPDATE `post` SET `current_people` = 2 WHERE `id` = 20;
UPDATE `post` SET `current_people` = 2 WHERE `id` = 21;
UPDATE `post` SET `current_people` = 2 WHERE `id` = 22;
UPDATE `post` SET `current_people` = 6 WHERE `id` = 23;
UPDATE `post` SET `current_people` = 3 WHERE `id` = 24;
UPDATE `post` SET `current_people` = 4 WHERE `id` = 25;
UPDATE `post` SET `current_people` = 3 WHERE `id` = 26;
UPDATE `post` SET `current_people` = 4 WHERE `id` = 27;
UPDATE `post` SET `current_people` = 4 WHERE `id` = 28;
UPDATE `post` SET `current_people` = 6 WHERE `id` = 29;
UPDATE `post` SET `current_people` = 8 WHERE `id` = 30;

-- =====================================================
-- 8. 系统公告与举报演示数据（供 H5 公告栏/弹窗与管理端 AI 风险研判演示）
-- =====================================================
INSERT INTO `sys_announcement` (`title`, `content`, `type`, `status`, `publish_time`, `expire_time`, `created_by`) VALUES
('🎉 校园搭子 2.0 AI 智能版正式上线！', '全新上线「AI 智能写帖助手」「AI 润色」「AI 破冰开场白」「4位现场签到码」与「你可能感兴趣可解释推荐」，快约上各路名人搭子一起组局吧！', 1, 1, NOW(), NULL, 1),
('🛡️ 社区诚信组局与防爽约公约', '为保障每位同学的组局体验：\n1. 活动开始后请凭发布者提供的 4 位数字签到码现场核验出勤；\n2. 无故爽约或开场前 2 小时内临场退坑将扣除信誉分；\n3. 信誉分低于 60 分将被暂停发帖与报名权限。祝大家快乐组局！', 2, 1, NOW(), NULL, 1);

INSERT INTO `report` (`reporter_id`, `target_type`, `target_id`, `reason_type`, `description`, `status`, `created_time`) VALUES
(4,  'POST', 10, '虚假/广告', '说是踢球结果疑似推销兼职刷单返利，麻烦包大人核查下架！', 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(10, 'USER', 11, '恶意爽约',  '约好五排结果临时去打比赛放鸽子，申请核实扣分', 0, DATE_SUB(NOW(), INTERVAL 1 HOUR));
