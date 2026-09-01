-- ============================================================
-- 校园兴趣搭子预约系统 - 一站式数据库初始化脚本（V1.1）
-- 生成日期: 2026-08-15
-- 内容: 建库 + 建表(10张) + 初始数据 + 测试数据(45用户/30帖子)
-- 用法:  mysql -u root -p < database/init-all.sql
-- 注意: 会 DROP 重建所有表，已有数据将被清空！
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
--    level:    段位 1青铜 2白银 3黄金 4钻石 5王者
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
    `view_count`     INT           DEFAULT 0,
    `like_count`     INT           DEFAULT 0,
    `is_audit_pass`  TINYINT       DEFAULT 0             COMMENT '审核状态 0待审核 1已通过 2已拒绝（先审后发）',
    `created_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
-- 初始数据：一个管理员 + 若干兴趣标签 + 测试学生
-- 密码 123456 的 BCrypt: $2a$10$e7ZZEXLXq1W265LW25VN8ecZx47trR8xZB8hBPr7h6n0Kv2Xojfka
-- -----------------------------------------------------
INSERT INTO `sys_user` (`student_no`, `phone`, `password`, `nickname`, `username`, `college`, `is_admin`)
VALUES ('admin001', '13800000000', '$2a$10$e7ZZEXLXq1W265LW25VN8ecZx47trR8xZB8hBPr7h6n0Kv2Xojfka', '系统管理员', 'admin001', '信息学院', 1),
       ('20240001', '13800000001', '$2a$10$e7ZZEXLXq1W265LW25VN8ecZx47trR8xZB8hBPr7h6n0Kv2Xojfka', '小明', '20240001', '计算机学院', 0),
       ('20240002', '13800000002', '$2a$10$e7ZZEXLXq1W265LW25VN8ecZx47trR8xZB8hBPr7h6n0Kv2Xojfka', '小红', '20240002', '体育学院', 0);

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
-- 校园搭子系统 - 测试数据脚本
-- 使用前提：先执行 schema.sql 初始化库表结构
-- 使用方法：source database/test-data.sql;
--
-- 学号规则：234(大数据人工智能学院) + 217(专业) + 班级(2位) + 编号(2位)
-- 密码规则：学号 + "123"，例如 2342170101123
-- =====================================================

USE `school_buddy`;

-- =====================================================
-- 1. 批量插入学生（15人，3个班）
--    已有用户: admin001(id=1), 20240001(id=2), 20240002(id=3)
--    新学生从 id=4 开始
-- =====================================================
INSERT INTO `sys_user` (`student_no`, `phone`, `password`, `nickname`, `username`, `college`, `gender`, `credit_score`, `status`, `is_admin`) VALUES

-- 班级 01（2342170101 ~ 2342170105）
('2342170101', '1382340101', '$2a$10$zjooIII90GrE6Btl0mJYUOzeZScN0TCAk6sRv6qvhr1L1hG0t7SQ6', '张晨',    '2342170101', '大数据人工智能学院', 1, 95, 1, 0),
('2342170102', '1382340102', '$2a$10$MixPESeHXi5lwiXFRjgU/uqrOXvkVxOgubIacH2vCpAZV2YF6zvHe', '李雨桐',  '2342170102', '大数据人工智能学院', 2, 88, 1, 0),
('2342170103', '1382340103', '$2a$10$47zjN/kngpieE742JmrTqOMWnA.TA7j.dbZIO2QjZCizvoXmTT03m', '王浩然',  '2342170103', '大数据人工智能学院', 1, 92, 1, 0),
('2342170104', '1382340104', '$2a$10$BZ2oEMv8pvuHCAP3u2xPl.QOuUoHG7VzBCf7WlKd9EG6sziN1qK5m', '赵思琪',  '2342170104', '大数据人工智能学院', 2, 100, 1, 0),
('2342170105', '1382340105', '$2a$10$3rcqX8yYpnLjx0haiOcmXuxzKv0RszhgCZraYMBOJBYJdt8qD0J8S', '孙宇航',  '2342170105', '大数据人工智能学院', 1, 78, 1, 0),

-- 班级 02（2342170201 ~ 2342170205）
('2342170201', '1382340201', '$2a$10$0aGnZGBMrtomC7yT3M2GTeL3xE7b3N/qw4U6ixVaITKY/KXP7Xuti', '周明远',  '2342170201', '大数据人工智能学院', 1, 85, 1, 0),
('2342170202', '1382340202', '$2a$10$7b0etg6LS92nY/hjvTWw1.MEyO6GOBmAW5GueBGwtJSP1my1dhVom', '吴宛瑜',  '2342170202', '大数据人工智能学院', 2, 96, 1, 0),
('2342170203', '1382340203', '$2a$10$o76eG9r28nh2oSp4R1UZsu9mw99KIr8o0ghE7SMdAjh8rZQbU69eC', '郑子轩',  '2342170203', '大数据人工智能学院', 1, 82, 1, 0),
('2342170204', '1382340204', '$2a$10$doSQmUOhU9yemdMtiWfrFur9M8lCfEeTpJGrLcIqiZMCHHptDARBy', '钱诗雅',  '2342170204', '大数据人工智能学院', 2, 90, 1, 0),
('2342170205', '1382340205', '$2a$10$Cqe3b8upEoivaSXU11cEEumkSmltcqDwvCVkG5hUkfcsl2/vHwMx6', '陈泽宇',  '2342170205', '大数据人工智能学院', 1, 75, 1, 0),

-- 班级 03（2342170301 ~ 2342170305）
('2342170301', '1382340301', '$2a$10$Pvm2cBivfcel6GNTwZynoO9kZ9c.ode0A1BgLZsDoo2RVFZcwfQs.', '杨思涵',  '2342170301', '大数据人工智能学院', 2, 91, 1, 0),
('2342170302', '1382340302', '$2a$10$bVcZozYk0anexpnOxghkbehBoAodPeQ6YaSjkrlYTkaANCnHm/.dG', '马俊豪',  '2342170302', '大数据人工智能学院', 1, 83, 1, 0),
('2342170303', '1382340303', '$2a$10$Tr4oS/HTwMtiUOfucSO4XuTpedKLS1pZ4TRCKMa7sVQSda/BSyzXW', '林语嫣',  '2342170303', '大数据人工智能学院', 2, 97, 1, 0),
('2342170304', '1382340304', '$2a$10$fhEfrMyUG50.axYeDtr/juygfFaq9x48MHW.yYvcOMpSYa4sIAsda', '何致远',  '2342170304', '大数据人工智能学院', 1, 86, 1, 0),
('2342170305', '1382340305', '$2a$10$.mh7tLmePItFz4wkNbQMduQiVfcV0tnlsBacIYOinrNfv6ZDKiGgy', '黄梓萱',  '2342170305', '大数据人工智能学院', 2, 93, 1, 0);

-- =====================================================
-- 2. 用户-兴趣标签关联
--    已有的标签: 篮球(1) 羽毛球(2) 乒乓球(3) 足球(4)
--                王者荣耀(5) 英雄联盟(6) 和平精英(7)
--                剧本杀(8) 狼人杀(9) 三国杀(10)
--                晨跑(11) 夜跑(12)
-- =====================================================
INSERT INTO `user_tag` (`user_id`, `tag_id`, `self_level`) VALUES
-- 张晨: 篮球、晨跑
(4, 1, 4), (4, 11, 3),
-- 李雨桐: 羽毛球、夜跑
(5, 2, 5), (5, 12, 4),
-- 王浩然: 王者荣耀、足球
(6, 5, 3), (6, 4, 2),
-- 赵思琪: 羽毛球、剧本杀、夜跑
(7, 2, 4), (7, 8, 5), (7, 12, 3),
-- 孙宇航: 篮球、英雄联盟
(8, 1, 5), (8, 6, 4),
-- 周明远: 乒乓球、剧本杀
(9, 3, 4), (9, 8, 3),
-- 吴宛瑜: 羽毛球、三国杀
(10, 2, 3), (10, 10, 4),
-- 郑子轩: 王者荣耀、和平精英
(11, 5, 5), (11, 7, 4),
-- 钱诗雅: 瑜伽(无此标签,跳过)、狼人杀、夜跑
(12, 9, 4), (12, 12, 5),
-- 陈泽宇: 篮球、晨跑
(13, 1, 3), (13, 11, 4),
-- 杨思涵: 羽毛球、剧本杀、狼人杀
(14, 2, 4), (14, 8, 5), (14, 9, 3),
-- 马俊豪: 足球、乒乓球
(15, 4, 3), (15, 3, 2),
-- 林语嫣: 王者荣耀、三国杀
(16, 5, 4), (16, 10, 3),
-- 何致远: 篮球、乒乓球、晨跑
(17, 1, 4), (17, 3, 3), (17, 11, 5),
-- 黄梓萱: 羽毛球、英雄联盟
(18, 2, 5), (18, 6, 3);

-- =====================================================
-- 3. 组局帖子（post）
--    status: RECRUITING(招募中) / FULL(已满员) / ONGOING(进行中) / FINISHED(已结束) / CANCELLED(已取消)
-- =====================================================
INSERT INTO `post` (`id`, `publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`, `location`, `max_people`, `current_people`, `require_level`, `require_gender`, `description`, `status`, `view_count`, `like_count`, `is_audit_pass`) VALUES

-- RECRUITING - 正在招募的帖子
(1, 4,  '周末篮球3v3',       '球类',   '1',    '2026-07-02 14:00:00', '2026-07-02 17:00:00', '东区篮球场',     6, 3, 3, 0, '缺两个队友，求篮球搭子！水平黄金段位以上', 'RECRUITING', 56, 5, 1),
(2, 10, '王者荣耀五排上分',  '电竞',   '5,6',  '2026-07-01 20:00:00', '2026-07-01 23:00:00', '线上-王者荣耀',  5, 2, 4, 0, '钻石段位以上，来会玩的', 'RECRUITING', 120, 8, 1),
(3, 14, '羽毛球双打约球',    '球类',   '2',    '2026-07-03 09:00:00', '2026-07-03 11:00:00', '体育馆羽毛球场', 4, 2, 0, 2, '仅限女生，新手友好', 'RECRUITING', 34, 3, 1),
(4, 7,  '周五剧本杀-月落洼', '桌游',   '8',    '2026-07-04 18:00:00', '2026-07-04 22:00:00', '南门剧本杀店',   5, 1, 0, 0, '经典本，缺4人，费用AA', 'RECRUITING', 89, 6, 0),

-- FULL - 已满员，等待开始
(5, 6,  '英雄联盟内战',      '电竞',   '6',    '2026-07-05 19:00:00', '2026-07-05 22:00:00', '线上-英雄联盟',  5, 5, 0, 0, '5V5内战，目前满员', 'FULL', 45, 4, 1),

-- ONGOING - 正在进行中
(6, 17, '晨跑打卡',          '跑步',   '11',   '2026-06-29 06:00:00', '2026-06-29 07:00:00', '操场',           10, 4, 0, 0, '每天早上6点操场集合', 'ONGOING', 67, 7, 1),

-- FINISHED - 已结束
(7, 9,  '三国杀面基',        '桌游',   '10',   '2026-06-25 15:00:00', '2026-06-25 18:00:00', '图书馆研讨室',   4, 4, 0, 0, '来三国杀高手对决', 'FINISHED', 92, 10, 1),
(8, 8,  '篮球全场5v5',       '球类',   '1',    '2026-06-27 16:00:00', '2026-06-27 18:00:00', '西区篮球场',     10, 10, 2, 0, '全场5v5，目前满员', 'FINISHED', 78, 6, 1),
(9, 5,  '夜跑小分队',        '跑步',   '12',   '2026-06-26 20:00:00', '2026-06-26 21:00:00', '环校路',         8, 5, 0, 2, '女生夜跑团，配速7分', 'FINISHED', 110, 12, 1),

-- CANCELLED - 已取消
(10, 15, '足球友谊赛',        '球类',   '4',    '2026-06-28 15:00:00', '2026-06-28 17:00:00', '足球场',          22, 2, 0, 0, '下雨取消', 'CANCELLED', 23, 1, 1);

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
INSERT INTO `sys_user` (`student_no`, `phone`, `password`, `nickname`, `username`, `college`, `gender`, `credit_score`, `status`, `is_admin`) VALUES

-- 大数据人工智能学院 01班 编号06~10
('2342170106', '1382340106', '$2a$10$sc6s3w.Vt3vJcwgUla29dufnOB/P5qqMwsGt2Id7uDCYQ.AUngVNK', '刘子涵', '2342170106', '大数据人工智能学院', 1, 87, 1, 0),
('2342170107', '1382340107', '$2a$10$CoeLgrEaH0LOA8inbAQr5OgNVaB06eROnvYmfjvTs5eruefWYEC8.', '陈雨欣', '2342170107', '大数据人工智能学院', 2, 94, 1, 0),
('2342170108', '1382340108', '$2a$10$4HEsWkOFLQfjRp771xT63OoB9Cckf.lJeWyD6t6eoXAswJm2GirDu', '唐一凡', '2342170108', '大数据人工智能学院', 1, 80, 1, 0),
('2342170109', '1382340109', '$2a$10$CocqgPGBcxfUJfs7ape2g.6XvGkOHPO6KxYv/kqPYGjzCkh/A.XJi', '苏小暖', '2342170109', '大数据人工智能学院', 2, 99, 1, 0),
('2342170110', '1382340110', '$2a$10$CpBDmVyDN/ElHqaj6DvRreI0OAhwjReDBIZ8HkHSMxwjYjCnGuyLu', '许光耀', '2342170110', '大数据人工智能学院', 1, 76, 1, 0),

-- 大数据人工智能学院 02班 编号06~10
('2342170206', '1382340206', '$2a$10$7RDQ.M/cLV.q.ti.ck7M5eS1TGShdAfXFhLdyV60nDA6y3SXz3/O.', '沈佳宜', '2342170206', '大数据人工智能学院', 2, 91, 1, 0),
('2342170207', '1382340207', '$2a$10$d0aYusYqaI.YjQw1mk6D5OniSJHEQsHEthImKSnl0j2oVKkSuREr.', '陆子航', '2342170207', '大数据人工智能学院', 1, 84, 1, 0),
('2342170208', '1382340208', '$2a$10$SOUcMWqVFHnM.P1/IfetpOO8I5/0aBKWE.i9iLmvhFWpb6UQoPFf.', '顾一一', '2342170208', '大数据人工智能学院', 2, 88, 1, 0),
('2342170209', '1382340209', '$2a$10$jGCLLMuItwcBLu2S99/OfuVNFA1V5/9MsYa.Xeb2FBwQoQBUWIniG', '高旭阳', '2342170209', '大数据人工智能学院', 1, 79, 1, 0),
('2342170210', '1382340210', '$2a$10$g.4ReZ36gxq/aQVkBSoY0O0ORo7jr92GvybS0U/EsfyRVaWv/dlw6', '唐诗韵', '2342170210', '大数据人工智能学院', 2, 95, 1, 0),

-- 计算机学院 01班 编号01~10
('2332170101', '1382330101', '$2a$10$oq5AQ3VD775cSTcMV8CMwOT8EEfDEyFsP1Uon/3MQPBi/x6iyuRha', '程一鸣', '2332170101', '计算机学院', 1, 82, 1, 0),
('2332170102', '1382330102', '$2a$10$lIhSIfA.KXqGCelpC0TqL.pKfyCpTiCtjO2l/rRsZwYI5iofKW.km', '林小鹿', '2332170102', '计算机学院', 2, 96, 1, 0),
('2332170103', '1382330103', '$2a$10$AiTvKd8Vxsp2roAbj02vaeiBDTqrrga/A7jJo.29B0TUWRCf/Yqzq', '赵大宝', '2332170103', '计算机学院', 1, 73, 1, 0),
('2332170104', '1382330104', '$2a$10$S2QwvxFZnNUjEgMG8UVTa.iE7NlHaxarMWrIkubVRBEN.yubmX1Ai', '周若溪', '2332170104', '计算机学院', 2, 90, 1, 0),
('2332170105', '1382330105', '$2a$10$21HejA9qBXqTEfhM9QVpmul4upxI/KarSAImAJbCOAk0Lr74pTff6', '韩子枫', '2332170105', '计算机学院', 1, 85, 1, 0),
('2332170106', '1382330106', '$2a$10$dOcYOzyx5YAYu4A54SpWs.4bvexjKUQQK1oDjJnjgXis3ikW/Hkei', '白若溪', '2332170106', '计算机学院', 2, 92, 1, 0),
('2332170107', '1382330107', '$2a$10$tBIX7RWM1nTRF9z7do1Z6uAfad6FiiAsmpsCR9gIRjv2zR0BdZQcC', '秦风',   '2332170107', '计算机学院', 1, 78, 1, 0),
('2332170108', '1382330108', '$2a$10$B/yEb2cOEsywQJitz1AaQ.B8/fwEe3qa2Lwge/ddVqBwXINySfPzG', '叶知秋', '2332170108', '计算机学院', 1, 87, 1, 0),
('2332170109', '1382330109', '$2a$10$7slGYpoh.omXFAJAW5JN9uri.Jup5uGNiPd6y6L9GnqIXf0ZNnHU2', '夏浅浅', '2332170109', '计算机学院', 2, 83, 1, 0),
('2332170110', '1382330110', '$2a$10$XJNygJqDjldwLiln6PW3rOg4ROjzLeBVo63rFfQZkIozsQQW7BCNy', '江一帆', '2332170110', '计算机学院', 1, 88, 1, 0),

-- 计算机学院 02班 编号01~10
('2332170201', '1382330201', '$2a$10$x/T20Otq6m9ctii4D.jQhervGsZXskykde1860YZp8y7GfUKdHNO.', '方浩然', '2332170201', '计算机学院', 1, 81, 1, 0),
('2332170202', '1382330202', '$2a$10$wpQwrO5pU2htsYtD2YHD/uvVxTk6zt/rpklxEzKIOjVyKHka6LVQ6', '董小晚', '2332170202', '计算机学院', 2, 94, 1, 0),
('2332170203', '1382330203', '$2a$10$DDVvUiwIO6bIUu.aHW7x6uwzGhcL8IDPFMbFS46sNNHQ9lXN.fufa', '魏子骞', '2332170203', '计算机学院', 1, 77, 1, 0),
('2332170204', '1382330204', '$2a$10$KnZtiCtQkudz5QYsyY7oROKMfFOk4JLh7ke2Y52PLneRn8lPHjZ6e', '宋暖阳', '2332170204', '计算机学院', 2, 91, 1, 0),
('2332170205', '1382330205', '$2a$10$T2/rMdOmz1ZVh2z.eW2R8Op1jgPE5AM1hXhBMB.0yOhhmFfmFQckG', '秦少峰', '2332170205', '计算机学院', 1, 86, 1, 0),
('2332170206', '1382330206', '$2a$10$V07Eythl71ffiwiN0dW2jOzAEOeEAkL6cNtgdfMH0IONd5ZKN848K', '温馨',   '2332170206', '计算机学院', 2, 89, 1, 0),
('2332170207', '1382330207', '$2a$10$RMznYE88gFRV/BKmcgJrc.7Y.F/kuyNFI950xBEGNphIlFFE9uilK', '龙天宇', '2332170207', '计算机学院', 1, 75, 1, 0),
('2332170208', '1382330208', '$2a$10$h1fu6RUX7crOtfBMR8/2NOysQCwheAUxMomJtrjHSmavsG3gmNe82', '廖一诺', '2332170208', '计算机学院', 2, 97, 1, 0),
('2332170209', '1382330209', '$2a$10$SEOd/SBcoUBLfGoaRdkuhO4O/JUlp/PzZDVMstN6huO1.aLGOAIr.', '田景行', '2332170209', '计算机学院', 1, 84, 1, 0),
('2332170210', '1382330210', '$2a$10$LOsbHpCCgz2BiOsisSYFsuMQ1Ygw4iUQ2y5EWi68RwRGINwEfVmWG', '花浅月', '2332170210', '计算机学院', 2, 93, 1, 0);

-- =====================================================
-- 3. 用户-兴趣标签关联
-- =====================================================
INSERT INTO `user_tag` (`user_id`, `tag_id`, `self_level`) VALUES

-- 大数据人工智能学院 01班 编号06~10
(19, 14, 3), (19, 15, 4),  -- 刘子涵: 图书馆学习、美食探店
(20, 13, 5), (20, 12, 3),  -- 陈雨欣: 观影追剧、夜跑
(21, 17, 4), (21, 3, 2),   -- 唐一凡: 音乐交流、乒乓球
(22, 15, 5), (22, 16, 4),  -- 苏小暖: 美食探店、散步聊天
(23, 16, 3), (23, 18, 4),  -- 许光耀: 散步聊天、摄影扫街

-- 大数据人工智能学院 02班 编号06~10
(24, 2, 5), (24, 15, 3),   -- 沈佳宜: 羽毛球、美食探店
(25, 11, 4), (25, 17, 3),  -- 陆子航: 晨跑、音乐交流
(26, 14, 5), (26, 16, 3),  -- 顾一一: 图书馆学习、散步聊天
(27, 4, 3), (27, 19, 4),   -- 高旭阳: 足球、健身撸铁
(28, 13, 4), (28, 9, 5),   -- 唐诗韵: 观影追剧、狼人杀

-- 计算机学院 01班 编号01~10
(29, 1, 3), (29, 5, 4),    -- 程一鸣: 篮球、王者荣耀
(30, 14, 4), (30, 17, 5),  -- 林小鹿: 图书馆学习、音乐交流
(31, 5, 5), (31, 7, 3),    -- 赵大宝: 王者荣耀、和平精英
(32, 13, 5), (32, 16, 4),  -- 周若溪: 观影追剧、散步聊天
(33, 18, 3), (33, 19, 4),  -- 韩子枫: 摄影扫街、健身撸铁
(34, 15, 4), (34, 12, 3),  -- 白若溪: 美食探店、夜跑
(35, 11, 5), (35, 19, 4),  -- 秦风:   晨跑、健身撸铁
(36, 17, 4), (36, 16, 3),  -- 叶知秋: 音乐交流、散步聊天
(37, 2, 4), (37, 14, 3),   -- 夏浅浅: 羽毛球、图书馆学习
(38, 5, 3), (38, 7, 4),    -- 江一帆: 王者荣耀、和平精英

-- 计算机学院 02班 编号01~10
(39, 8, 4), (39, 9, 3),    -- 方浩然: 剧本杀、狼人杀
(40, 14, 5), (40, 15, 4),  -- 董小晚: 图书馆学习、美食探店
(41, 19, 5), (41, 4, 3),   -- 魏子骞: 健身撸铁、足球
(42, 16, 5), (42, 13, 4),  -- 宋暖阳: 散步聊天、观影追剧
(43, 1, 4), (43, 11, 3),   -- 秦少峰: 篮球、晨跑
(44, 15, 4), (44, 14, 3),  -- 温馨:   美食探店、图书馆学习
(45, 6, 4), (45, 19, 3),   -- 龙天宇: 英雄联盟、健身撸铁
(46, 18, 5), (46, 17, 4),  -- 廖一诺: 摄影扫街、音乐交流
(47, 8, 3), (47, 16, 4),   -- 田景行: 剧本杀、散步聊天
(48, 2, 3), (48, 9, 4);    -- 花浅月: 羽毛球、狼人杀

-- =====================================================
-- 4. 更多组局帖子（post，id从11开始）
--    全都是新人友好、门槛低的活动
-- =====================================================
INSERT INTO `post` (`id`, `publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`, `location`, `max_people`, `current_people`, `require_level`, `require_gender`, `description`, `status`, `view_count`, `like_count`, `is_audit_pass`) VALUES

-- 招募中 - 新人友好
(11, 32, '周末一起看电影',         '影视',   '13',   '2026-07-04 15:00:00', '2026-07-04 18:00:00', '万达影城',          4, 2, 0, 2, '最近想看的电影很多，找姐妹一起去看！看完可以顺便逛逛街', 'RECRUITING', 45, 6, 1),
(12, 42, '图书馆学习搭子',         '学习',   '14',   '2026-07-01 08:00:00', '2026-07-01 17:00:00', '图书馆三楼自习区',  6, 2, 0, 0, '期末复习局，互相监督不玩手机，学完一起去吃饭', 'RECRUITING', 78, 9, 1),
(13, 40, '食堂约饭！北区新开的麻辣烫', '美食', '15',   '2026-07-02 11:30:00', '2026-07-02 12:30:00', '北区食堂二楼',      4, 1, 0, 0, '听说新开的麻辣烫很好吃，找人一起去尝尝，AA', 'RECRUITING', 34, 4, 1),
(14, 36, '晚饭后散步消食',          '休闲',   '16',   '2026-07-01 19:00:00', '2026-07-01 20:30:00', '学校操场',          8, 3, 0, 0, '吃完晚饭太撑了，找几个人一起遛弯聊天，很轻松的那种', 'RECRUITING', 25, 3, 1),
(15, 30, '新手狼人杀教学局',        '桌游',   '9',    '2026-07-03 14:00:00', '2026-07-03 17:00:00', '图书馆研讨室',      8, 3, 0, 0, '纯新手局！我来当法官教大家玩，不会的完全没关系，开心最重要', 'RECRUITING', 120, 15, 1),
(16, 37, '羽毛球新手局',            '球类',   '2',    '2026-07-04 09:00:00', '2026-07-04 11:00:00', '体育馆羽毛球场',    4, 2, 0, 0, '只会打和平球的新手，求水平差不多的搭子一起练', 'RECRUITING', 56, 5, 1),
(17, 34, '周末探店！学校周边的宝藏小店', '美食', '15', '2026-07-05 10:00:00', '2026-07-05 16:00:00', '学校南门集合',      6, 1, 0, 2, '发掘学校周边好吃的店，边吃边逛边拍照，轻松周末', 'RECRUITING', 67, 8, 0),
(18, 46, '摄影扫街：拍校园夕阳',     '摄影',   '18',   '2026-07-02 17:30:00', '2026-07-02 19:00:00', '图书馆门口集合',    8, 2, 0, 0, '喜欢拍照的来！手机也行相机也行，一起拍校园夕阳', 'RECRUITING', 43, 7, 1),
(19, 35, '晨跑搭子，新手也能跑',     '跑步',   '11',   '2026-07-03 06:30:00', '2026-07-03 07:30:00', '操场',              10, 3, 0, 0, '跑不动可以走！主要是养成早起习惯，配速不限', 'RECRUITING', 38, 4, 1),
(20, 22, '音乐交流：互相分享歌单',   '音乐',   '17',   '2026-07-04 19:00:00', '2026-07-04 21:00:00', '操场草坪',          10, 2, 0, 0, '带上耳机或小音箱，围坐一圈分享自己喜欢的歌，聊聊音乐', 'RECRUITING', 52, 11, 1),
(21, 38, '王者荣耀娱乐局',           '电竞',   '5',    '2026-07-01 20:00:00', '2026-07-01 22:00:00', '线上-王者荣耀',     5, 3, 0, 0, '娱乐局不压力！输赢无所谓，开心就好，青铜到星耀都欢迎', 'RECRUITING', 98, 7, 1),
(22, 29, '周末篮球半场养生局',       '球类',   '1',    '2026-07-05 16:00:00', '2026-07-05 18:00:00', '东区篮球场',        6, 2, 0, 0, '养生篮球，出出汗就好，不冲不撞，欢迎新手来玩', 'RECRUITING', 45, 3, 1),

-- 已满员
(23, 48, '剧本杀-窗边的女人（新手本）', '桌游', '8',   '2026-07-06 14:00:00', '2026-07-06 18:00:00', '南门外剧本杀店',    6, 6, 0, 0, '新手友好本，店家会带，一起去体验一下', 'FULL', 88, 12, 1),

-- 进行中
(24, 41, '健身房一起撸铁',           '健身',   '19',   '2026-06-29 18:00:00', '2026-06-29 20:00:00', '学校健身房',        4, 3, 0, 1, '一起练胸日，互相保护，新手可以教你', 'ONGOING', 33, 5, 1),
(25, 44, '图书馆期末冲刺',           '学习',   '14',   '2026-06-29 08:00:00', '2026-06-29 22:00:00', '图书馆四楼',        6, 4, 0, 0, '期末冲刺一天！中午一起吃饭，互相占座', 'ONGOING', 67, 5, 1),

-- 已结束
(26, 43, '一起健身打卡',             '健身',   '19',   '2026-06-25 17:00:00', '2026-06-25 19:00:00', '学校健身房',        3, 3, 0, 0, '练背日，效果很好', 'FINISHED', 41, 4, 1),
(27, 28, '周末追剧：悬疑片专场',     '影视',   '13',   '2026-06-26 14:00:00', '2026-06-26 17:00:00', '宿舍活动室',        6, 4, 0, 2, '一起看悬疑电影，边看边讨论，气氛超好', 'FINISHED', 56, 8, 1),
(28, 20, '北门新开的日料探店',       '美食',   '15',   '2026-06-27 12:00:00', '2026-06-27 13:30:00', '北门日料店',        4, 4, 0, 0, '新开的日料店，团购很划算，一起组队去', 'FINISHED', 72, 10, 1),
(29, 45, '看晚霞！操场拍照',         '摄影',   '18',   '2026-06-27 18:00:00', '2026-06-27 19:30:00', '操场',              10, 6, 0, 0, '今天的晚霞绝了，大家都去操场拍照吧', 'FINISHED', 89, 14, 1),
(30, 39, '狼人杀新手车',             '桌游',   '9',    '2026-06-28 15:00:00', '2026-06-28 18:00:00', '图书馆研讨室',      8, 8, 0, 0, '全是新手的一车，超欢乐，下次还来', 'FINISHED', 110, 16, 1);

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
(11, 34, '什么电影呀？',                         '2026-06-28 12:30:00'),
(12, 26, '一起冲期末！',                         '2026-06-28 10:30:00'),
(15, 33, '新手能教教我吗完全不会',               '2026-06-28 14:30:00'),
(15, 48, '我也是新手哈哈哈一起学',               '2026-06-28 15:30:00'),
(15, 30, '求带！',                               '2026-06-29 10:30:00'),
(18, 33, '我只有手机可以吗',                     '2026-06-28 16:30:00'),
(20, 21, '我最近在听周杰伦！',                   '2026-06-28 18:30:00'),
(21, 31, '我玩射手，来辅助！',                   '2026-06-28 20:30:00'),
(22, 4,  '养生球我最爱',                         '2026-06-28 15:30:00'),
(24, 33, '练胸还是练背？',                       '2026-06-28 17:30:00'),
(25, 22, '卷起来！',                             '2026-06-28 08:00:00'),
(26, 43, '一起练背效果翻倍',                     '2026-06-25 17:30:00'),
(28, 34, '味道怎么样？',                         '2026-06-27 12:30:00'),
(29, 46, '今天的夕阳绝了',                       '2026-06-27 18:30:00'),
(30, 48, '超好玩！下次还要来',                   '2026-06-28 18:30:00');

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
-- 7. 更新帖子当前人数
-- =====================================================
UPDATE `post` SET `current_people` = 5 WHERE `id` = 11;
UPDATE `post` SET `current_people` = 4 WHERE `id` = 12;
UPDATE `post` SET `current_people` = 4 WHERE `id` = 14;
UPDATE `post` SET `current_people` = 5 WHERE `id` = 15;
UPDATE `post` SET `current_people` = 5 WHERE `id` = 19;
UPDATE `post` SET `current_people` = 4 WHERE `id` = 20;
UPDATE `post` SET `current_people` = 4 WHERE `id` = 21;
UPDATE `post` SET `current_people` = 3 WHERE `id` = 22;
UPDATE `post` SET `current_people` = 6 WHERE `id` = 23;
UPDATE `post` SET `current_people` = 3 WHERE `id` = 24;
UPDATE `post` SET `current_people` = 4 WHERE `id` = 25;
UPDATE `post` SET `current_people` = 3 WHERE `id` = 26;
UPDATE `post` SET `current_people` = 4 WHERE `id` = 27;
UPDATE `post` SET `current_people` = 4 WHERE `id` = 28;
UPDATE `post` SET `current_people` = 6 WHERE `id` = 29;
UPDATE `post` SET `current_people` = 8 WHERE `id` = 30;
