-- =====================================================
-- 校园兴趣搭子预约系统 - 数据库脚本
-- 数据库: MySQL 8.0+  (CREATE DATABASE school_buddy DEFAULT CHARSET utf8mb4)
-- =====================================================
USE school_buddy;

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
-- 8. 信誉分变动台账表（V1.2 新增）
--    记录非评价类的信誉分变动（爽约/临场取消/管理员重置），
--    每日重算公式 = 100 + Σ评价影响 + Σ台账变动，惩罚不再被重算冲掉
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
