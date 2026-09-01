/*
 Navicat Premium Dump SQL

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 260700 (26.7.0)
 Source Host           : localhost:3306
 Source Schema         : school_buddy

 Target Server Type    : MySQL
 Target Server Version : 260700 (26.7.0)
 File Encoding         : 65001

 Date: 31/08/2026 16:03:06
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for credit_log
-- ----------------------------
DROP TABLE IF EXISTS `credit_log`;
CREATE TABLE `credit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `change_value` int NOT NULL COMMENT '变动值（正加负减）',
  `reason` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原因：NOSHOW/CANCEL_LAST_MINUTE/ADMIN_RESET',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务类型：APPLY/ADMIN',
  `biz_id` bigint NULL DEFAULT NULL COMMENT '业务对象ID',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '信誉分变动台账' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of credit_log
-- ----------------------------

-- ----------------------------
-- Table structure for favorite
-- ----------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '收藏用户',
  `post_id` bigint NOT NULL COMMENT '帖子',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_post`(`user_id` ASC, `post_id` ASC) USING BTREE,
  INDEX `idx_post`(`post_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of favorite
-- ----------------------------

-- ----------------------------
-- Table structure for interest_tag
-- ----------------------------
DROP TABLE IF EXISTS `interest_tag`;
CREATE TABLE `interest_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类，如 篮球/羽毛球/王者荣耀',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目名',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sort_order` int NULL DEFAULT 0,
  `status` tinyint NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '兴趣标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of interest_tag
-- ----------------------------
INSERT INTO `interest_tag` VALUES (1, '球类', '篮球', NULL, 1, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (2, '球类', '羽毛球', NULL, 2, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (3, '球类', '乒乓球', NULL, 3, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (4, '球类', '足球', NULL, 4, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (5, '电竞', '王者荣耀', NULL, 10, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (6, '电竞', '英雄联盟', NULL, 11, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (7, '电竞', '和平精英', NULL, 12, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (8, '桌游', '剧本杀', NULL, 20, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (9, '桌游', '狼人杀', NULL, 21, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (10, '桌游', '三国杀', NULL, 22, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (11, '跑步', '晨跑', NULL, 30, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (12, '跑步', '夜跑', NULL, 31, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (13, '学习', '图书馆学习', NULL, 40, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (14, '影视', '观影追剧', NULL, 41, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (15, '美食', '美食探店', NULL, 42, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (16, '休闲', '散步聊天', NULL, 43, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (17, '音乐', '音乐交流', NULL, 44, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (18, '摄影', '摄影扫街', NULL, 45, 1, '2026-08-15 15:57:25');
INSERT INTO `interest_tag` VALUES (19, '健身', '健身撸铁', NULL, 46, 1, '2026-08-15 15:57:25');

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '接收人',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知类型',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内容',
  `link` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '跳转路径',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `biz_id` bigint NULL DEFAULT NULL COMMENT '关联对象ID（帖子/报名）',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_read`(`user_id` ASC, `is_read` ASC, `created_time` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification
-- ----------------------------
INSERT INTO `notification` VALUES (1, 2, 'POST_AUDIT', '帖子审核通过', '你的组局「端到端测试帖」已审核通过，现在可在广场看到', '/post/32', 1, 32, NULL);
INSERT INTO `notification` VALUES (2, 6, 'APPLY_NEW', '新的报名申请', '小明 报名了你的组局「晨跑团」', '/post/114', 0, 114, NULL);

-- ----------------------------
-- Table structure for post
-- ----------------------------
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `publisher_id` bigint NOT NULL COMMENT '发布人',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属分类',
  `tag_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '相关标签id列表，逗号分隔',
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动地点',
  `max_people` int NOT NULL DEFAULT 2 COMMENT '最大人数',
  `current_people` int NOT NULL DEFAULT 1 COMMENT '当前人数(含发布者)',
  `require_level` int NULL DEFAULT NULL COMMENT '要求最低段位，空则不限',
  `require_gender` tinyint NULL DEFAULT 0 COMMENT '0不限 1男 2女',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'RECRUITING',
  `view_count` int NULL DEFAULT 0,
  `like_count` int NULL DEFAULT 0,
  `is_audit_pass` tinyint NULL DEFAULT 0 COMMENT '审核状态 0待审核 1已通过 2已拒绝（先审后发）',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_publisher`(`publisher_id` ASC) USING BTREE,
  INDEX `idx_status_time`(`status` ASC, `start_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 128 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '组局帖子表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of post
-- ----------------------------
INSERT INTO `post` VALUES (1, 4, '周末篮球3v3', '球类', '1', '2026-07-02 14:00:00', '2026-07-02 17:00:00', '东区篮球场', 6, 3, 3, 0, '缺两个队友，求篮球搭子！水平黄金段位以上', 'FINISHED', 56, 5, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (2, 10, '王者荣耀五排上分', '电竞', '5,6', '2026-07-01 20:00:00', '2026-07-01 23:00:00', '线上-王者荣耀', 5, 2, 4, 0, '钻石段位以上，来会玩的', 'FINISHED', 120, 8, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (3, 14, '羽毛球双打约球', '球类', '2', '2026-07-03 09:00:00', '2026-07-03 11:00:00', '体育馆羽毛球场', 4, 2, 0, 2, '仅限女生，新手友好', 'FINISHED', 34, 3, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (4, 7, '周五剧本杀-月落洼', '桌游', '8', '2026-07-04 18:00:00', '2026-07-04 22:00:00', '南门剧本杀店', 5, 1, 0, 0, '经典本，缺4人，费用AA', 'FINISHED', 89, 6, 0, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (5, 6, '英雄联盟内战', '电竞', '6', '2026-07-05 19:00:00', '2026-07-05 22:00:00', '线上-英雄联盟', 5, 5, 0, 0, '5V5内战，目前满员', 'FINISHED', 45, 4, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (6, 17, '晨跑打卡', '跑步', '11', '2026-06-29 06:00:00', '2026-06-29 07:00:00', '操场', 10, 4, 0, 0, '每天早上6点操场集合', 'FINISHED', 67, 7, 1, '2026-08-15 15:57:25', '2026-08-15 16:00:00');
INSERT INTO `post` VALUES (7, 9, '三国杀面基', '桌游', '10', '2026-06-25 15:00:00', '2026-06-25 18:00:00', '图书馆研讨室', 4, 4, 0, 0, '来三国杀高手对决', 'FINISHED', 92, 10, 1, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `post` VALUES (8, 8, '篮球全场5v5', '球类', '1', '2026-06-27 16:00:00', '2026-06-27 18:00:00', '西区篮球场', 10, 10, 2, 0, '全场5v5，目前满员', 'FINISHED', 78, 6, 1, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `post` VALUES (9, 5, '夜跑小分队', '跑步', '12', '2026-06-26 20:00:00', '2026-06-26 21:00:00', '环校路', 8, 5, 0, 2, '女生夜跑团，配速7分', 'FINISHED', 110, 12, 1, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `post` VALUES (10, 15, '足球友谊赛', '球类', '4', '2026-06-28 15:00:00', '2026-06-28 17:00:00', '足球场', 22, 2, 0, 0, '下雨取消', 'CANCELLED', 23, 1, 1, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `post` VALUES (11, 32, '周末一起看电影', '影视', '13', '2026-07-04 15:00:00', '2026-07-04 18:00:00', '万达影城', 4, 5, 0, 2, '最近想看的电影很多，找姐妹一起去看！看完可以顺便逛逛街', 'FINISHED', 45, 6, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (12, 42, '图书馆学习搭子', '学习', '14', '2026-07-01 08:00:00', '2026-07-01 17:00:00', '图书馆三楼自习区', 6, 4, 0, 0, '期末复习局，互相监督不玩手机，学完一起去吃饭', 'FINISHED', 78, 9, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (13, 40, '食堂约饭！北区新开的麻辣烫', '美食', '15', '2026-07-02 11:30:00', '2026-07-02 12:30:00', '北区食堂二楼', 4, 1, 0, 0, '听说新开的麻辣烫很好吃，找人一起去尝尝，AA', 'FINISHED', 34, 4, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (14, 36, '晚饭后散步消食', '休闲', '16', '2026-07-01 19:00:00', '2026-07-01 20:30:00', '学校操场', 8, 4, 0, 0, '吃完晚饭太撑了，找几个人一起遛弯聊天，很轻松的那种', 'FINISHED', 25, 3, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (15, 30, '新手狼人杀教学局', '桌游', '9', '2026-07-03 14:00:00', '2026-07-03 17:00:00', '图书馆研讨室', 8, 5, 0, 0, '纯新手局！我来当法官教大家玩，不会的完全没关系，开心最重要', 'FINISHED', 120, 15, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (16, 37, '羽毛球新手局', '球类', '2', '2026-07-04 09:00:00', '2026-07-04 11:00:00', '体育馆羽毛球场', 4, 2, 0, 0, '只会打和平球的新手，求水平差不多的搭子一起练', 'FINISHED', 56, 5, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (17, 34, '周末探店！学校周边的宝藏小店', '美食', '15', '2026-07-05 10:00:00', '2026-07-05 16:00:00', '学校南门集合', 6, 1, 0, 2, '发掘学校周边好吃的店，边吃边逛边拍照，轻松周末', 'FINISHED', 67, 8, 0, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (18, 46, '摄影扫街：拍校园夕阳', '摄影', '18', '2026-07-02 17:30:00', '2026-07-02 19:00:00', '图书馆门口集合', 8, 2, 0, 0, '喜欢拍照的来！手机也行相机也行，一起拍校园夕阳', 'FINISHED', 43, 7, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (19, 35, '晨跑搭子，新手也能跑', '跑步', '11', '2026-07-03 06:30:00', '2026-07-03 07:30:00', '操场', 10, 5, 0, 0, '跑不动可以走！主要是养成早起习惯，配速不限', 'FINISHED', 38, 4, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (20, 22, '音乐交流：互相分享歌单', '音乐', '17', '2026-07-04 19:00:00', '2026-07-04 21:00:00', '操场草坪', 10, 4, 0, 0, '带上耳机或小音箱，围坐一圈分享自己喜欢的歌，聊聊音乐', 'FINISHED', 52, 11, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (21, 38, '王者荣耀娱乐局', '电竞', '5', '2026-07-01 20:00:00', '2026-07-01 22:00:00', '线上-王者荣耀', 5, 4, 0, 0, '娱乐局不压力！输赢无所谓，开心就好，青铜到星耀都欢迎', 'FINISHED', 98, 7, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (22, 29, '周末篮球半场养生局', '球类', '1', '2026-07-05 16:00:00', '2026-07-05 18:00:00', '东区篮球场', 6, 3, 0, 0, '养生篮球，出出汗就好，不冲不撞，欢迎新手来玩', 'FINISHED', 45, 3, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (23, 48, '剧本杀-窗边的女人（新手本）', '桌游', '8', '2026-07-06 14:00:00', '2026-07-06 18:00:00', '南门外剧本杀店', 6, 6, 0, 0, '新手友好本，店家会带，一起去体验一下', 'FINISHED', 88, 12, 1, '2026-08-15 15:57:25', '2026-08-15 16:05:00');
INSERT INTO `post` VALUES (24, 41, '健身房一起撸铁', '健身', '19', '2026-06-29 18:00:00', '2026-06-29 20:00:00', '学校健身房', 4, 3, 0, 1, '一起练胸日，互相保护，新手可以教你', 'FINISHED', 33, 5, 1, '2026-08-15 15:57:25', '2026-08-15 16:00:00');
INSERT INTO `post` VALUES (25, 44, '图书馆期末冲刺', '学习', '14', '2026-06-29 08:00:00', '2026-06-29 22:00:00', '图书馆四楼', 6, 4, 0, 0, '期末冲刺一天！中午一起吃饭，互相占座', 'FINISHED', 67, 5, 1, '2026-08-15 15:57:25', '2026-08-15 16:00:00');
INSERT INTO `post` VALUES (26, 43, '一起健身打卡', '健身', '19', '2026-06-25 17:00:00', '2026-06-25 19:00:00', '学校健身房', 3, 3, 0, 0, '练背日，效果很好', 'FINISHED', 41, 4, 1, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `post` VALUES (27, 28, '周末追剧：悬疑片专场', '影视', '13', '2026-06-26 14:00:00', '2026-06-26 17:00:00', '宿舍活动室', 6, 4, 0, 2, '一起看悬疑电影，边看边讨论，气氛超好', 'FINISHED', 56, 8, 1, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `post` VALUES (28, 20, '北门新开的日料探店', '美食', '15', '2026-06-27 12:00:00', '2026-06-27 13:30:00', '北门日料店', 4, 4, 0, 0, '新开的日料店，团购很划算，一起组队去', 'FINISHED', 72, 10, 1, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `post` VALUES (29, 45, '看晚霞！操场拍照', '摄影', '18', '2026-06-27 18:00:00', '2026-06-27 19:30:00', '操场', 10, 6, 0, 0, '今天的晚霞绝了，大家都去操场拍照吧', 'FINISHED', 89, 14, 1, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `post` VALUES (30, 39, '狼人杀新手车', '桌游', '9', '2026-06-28 15:00:00', '2026-06-28 18:00:00', '图书馆研讨室', 8, 8, 0, 0, '全是新手的一车，超欢乐，下次还来', 'FINISHED', 110, 16, 1, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `post` VALUES (31, 2, '测试发帖接口', '球类', '1', '2026-08-15 19:10:10', '2026-08-15 21:10:10', '东区篮球场', 4, 1, NULL, 0, '接口测试', 'RECRUITING', 0, 0, 1, NULL, NULL);
INSERT INTO `post` VALUES (32, 2, '端到端测试帖', '球类', '1', '2026-08-15 19:11:09', '2026-08-15 21:11:09', '东区篮球场', 4, 1, 1, 0, '完整流程测试', 'FINISHED', 2, 0, 1, NULL, '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (33, 2, '快来打篮球', '球类', '1', '2026-08-15 18:19:00', '2026-08-15 20:19:00', '祥云体育场', 4, 1, NULL, 0, '快来打篮球', 'RECRUITING', 1, 0, 1, NULL, NULL);
INSERT INTO `post` VALUES (100, 4, '周末羽毛球双打', '球类', '2', '2026-08-05 16:24:30', '2026-08-05 18:24:30', '体育馆羽毛球场', 4, 4, 0, 0, '上周的羽毛球局，大家打得都很尽兴', 'FINISHED', 120, 8, 1, '2026-08-15 16:24:30', '2026-08-15 16:24:30');
INSERT INTO `post` VALUES (101, 5, '王者荣耀五排上分', '电竞', '5', '2026-08-07 16:24:30', '2026-08-07 19:24:30', '线上-王者荣耀', 5, 5, 4, 0, '上周五排冲分，队友配合很默契', 'FINISHED', 210, 15, 1, '2026-08-15 16:24:30', '2026-08-15 16:24:30');
INSERT INTO `post` VALUES (102, 6, '图书馆自习搭子', '学习', '13', '2026-08-08 16:24:30', '2026-08-08 20:24:30', '图书馆三楼自习区', 3, 3, 0, 0, '期末冲刺自习局', 'FINISHED', 45, 2, 1, '2026-08-15 16:24:30', '2026-08-15 16:24:30');
INSERT INTO `post` VALUES (103, 7, '夜跑五公里', '跑步', '12', '2026-08-09 16:24:30', '2026-08-09 17:24:30', '操场', 8, 6, 0, 0, '夜跑打卡，配速6分半', 'FINISHED', 88, 6, 1, '2026-08-15 16:24:30', '2026-08-15 16:24:30');
INSERT INTO `post` VALUES (104, 8, '剧本杀-窗边的女人', '桌游', '8', '2026-08-10 16:24:30', '2026-08-10 20:24:30', '南门剧本杀店', 6, 6, 0, 0, '经典推理本，全员到齐', 'FINISHED', 156, 12, 1, '2026-08-15 16:24:30', '2026-08-15 16:24:30');
INSERT INTO `post` VALUES (105, 10, '乒乓球友谊赛', '球类', '3', '2026-08-11 16:24:30', '2026-08-11 18:24:30', '体育馆乒乓球室', 4, 4, 0, 0, '周末乒乓球交流', 'FINISHED', 67, 5, 1, '2026-08-15 16:24:30', '2026-08-15 16:24:30');
INSERT INTO `post` VALUES (106, 12, '三国杀娱乐局', '桌游', '10', '2026-08-12 16:24:30', '2026-08-12 19:24:30', '宿舍活动室', 5, 5, 0, 0, '休闲三国杀，不争输赢', 'FINISHED', 92, 7, 1, '2026-08-15 16:24:30', '2026-08-15 16:24:30');
INSERT INTO `post` VALUES (107, 14, '羽毛球新手教学', '球类', '2', '2026-08-13 16:24:30', '2026-08-13 18:24:30', '体育馆羽毛球场', 6, 6, 0, 0, '新手友好，教你入门', 'FINISHED', 78, 9, 1, '2026-08-15 16:24:30', '2026-08-15 16:24:30');
INSERT INTO `post` VALUES (108, 15, '篮球半场3v3', '球类', '1', '2026-08-15 15:24:30', '2026-08-15 18:24:30', '东区篮球场', 6, 4, 2, 0, '现在正在打，缺2人随时加入', 'FINISHED', 56, 4, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (109, 16, '和平精英四排', '电竞', '7', '2026-08-15 15:54:30', '2026-08-15 18:24:30', '线上-和平精英', 4, 3, 0, 0, '正在四排，差一个人', 'FINISHED', 34, 3, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (110, 18, '操场夜跑', '跑步', '12', '2026-08-15 16:04:30', '2026-08-15 17:04:30', '操场', 10, 5, 0, 0, '今晚夜跑，随时加入', 'FINISHED', 45, 4, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (111, 20, '摄影扫街-老城区', '摄影', '18', '2026-08-15 15:24:30', '2026-08-15 19:24:30', '老城区步行街', 5, 3, 0, 0, '带着相机扫街，交流摄影', 'FINISHED', 67, 5, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (112, 4, '周末篮球3v3局', '球类', '1', '2026-08-16 16:24:30', '2026-08-16 18:24:30', '东区篮球场', 6, 2, 3, 0, '周六下午篮球局，黄金以上优先', 'FINISHED', 35, 2, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (113, 5, '英雄联盟开黑', '电竞', '6', '2026-08-16 16:24:30', '2026-08-16 19:24:30', '线上-英雄联盟', 5, 2, 4, 0, '晚上开黑上分，钻石段位来', 'FINISHED', 46, 3, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (114, 6, '晨跑团', '跑步', '11', '2026-08-17 16:24:30', '2026-08-17 17:24:30', '操场', 10, 3, 0, 0, '周二早上晨跑，欢迎新手', 'FINISHED', 24, 1, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (115, 7, '周末剧本杀-年轮', '桌游', '8', '2026-08-17 16:24:30', '2026-08-17 20:24:30', '南门剧本杀店', 6, 2, 0, 0, '口碑本，缺4人，费用AA', 'FINISHED', 89, 6, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (116, 8, '羽毛球混双', '球类', '2', '2026-08-18 16:24:30', '2026-08-18 18:24:30', '体育馆羽毛球场', 4, 1, 0, 1, '混双局，欢迎女生报名', 'FINISHED', 28, 2, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (117, 10, '狼人杀12人局', '桌游', '9', '2026-08-18 16:24:30', '2026-08-18 20:24:30', '咖啡厅包间', 12, 4, 0, 0, '标准12人局，缺8人', 'FINISHED', 156, 10, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (118, 12, '健身撸铁', '健身', '19', '2026-08-19 16:24:30', '2026-08-19 18:24:30', '健身房', 4, 1, 0, 0, '一起去撸铁，互相保护', 'FINISHED', 18, 1, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (119, 14, '图书馆学习搭子', '学习', '13', '2026-08-19 16:24:30', '2026-08-19 20:24:30', '图书馆二楼', 4, 2, 0, 0, '找个学习搭子互相监督', 'FINISHED', 30, 2, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (120, 16, '美食探店-川菜', '美食', '15', '2026-08-20 16:24:30', '2026-08-20 18:24:30', '南门川菜馆', 6, 3, 0, 0, '周末探店，爱吃辣的一起来', 'FINISHED', 42, 3, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (121, 18, '音乐交流', '音乐', '17', '2026-08-20 16:24:30', '2026-08-20 19:24:30', '艺术楼琴房', 6, 2, 0, 0, '带乐器来一起玩', 'FINISHED', 25, 2, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (122, 20, '王者荣耀内战5v5', '电竞', '5', '2026-08-16 16:24:30', '2026-08-16 19:24:30', '线上-王者荣耀', 10, 10, 3, 0, '5V5内战已满员，欢迎观战', 'FINISHED', 78, 6, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (123, 22, '剧本杀-持斧奥夫', '桌游', '8', '2026-08-17 16:24:30', '2026-08-17 20:24:30', '北门剧本杀店', 6, 6, 0, 0, '恐怖本已满员', 'FINISHED', 67, 5, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (124, 24, '篮球全场5v5', '球类', '1', '2026-08-18 16:24:30', '2026-08-18 18:24:30', '西区篮球场', 10, 10, 2, 0, '全场已满员', 'FINISHED', 57, 4, 1, '2026-08-15 16:24:30', '2026-08-31 15:30:00');
INSERT INTO `post` VALUES (125, 26, '雨天户外跑步', '跑步', '12', '2026-08-16 16:24:30', '2026-08-16 17:24:30', '操场', 8, 1, 0, 0, '天气预报有雨，取消活动', 'CANCELLED', 12, 0, 1, '2026-08-15 16:24:30', '2026-08-15 16:24:30');
INSERT INTO `post` VALUES (126, 28, '室外篮球局', '球类', '1', '2026-08-17 16:24:30', '2026-08-17 18:24:30', '东区篮球场', 6, 2, 0, 0, '场地维修，活动取消', 'CANCELLED', 20, 1, 1, '2026-08-15 16:24:30', '2026-08-15 16:24:30');
INSERT INTO `post` VALUES (127, 2, '篮球约球活动', '球类', '1', '2026-08-31 17:47:00', '2026-08-31 19:47:00', '祥云体育场', 4, 1, NULL, 0, '1111111', 'RECRUITING', 0, 0, 1, NULL, NULL);

-- ----------------------------
-- Table structure for post_apply
-- ----------------------------
DROP TABLE IF EXISTS `post_apply`;
CREATE TABLE `post_apply`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `applicant_id` bigint NOT NULL,
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `apply_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `handle_time` datetime NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_applicant`(`post_id` ASC, `applicant_id` ASC) USING BTREE,
  INDEX `idx_applicant`(`applicant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 153 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '报名表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of post_apply
-- ----------------------------
INSERT INTO `post_apply` VALUES (1, 1, 8, 'APPROVED', '2026-06-28 14:00:00', '2026-06-28 15:00:00', NULL);
INSERT INTO `post_apply` VALUES (2, 1, 13, 'PENDING', '2026-06-29 10:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (3, 2, 11, 'APPROVED', '2026-06-28 20:00:00', '2026-06-28 21:00:00', NULL);
INSERT INTO `post_apply` VALUES (4, 2, 16, 'PENDING', '2026-06-29 12:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (5, 2, 6, 'REJECTED', '2026-06-28 22:00:00', '2026-06-28 23:00:00', NULL);
INSERT INTO `post_apply` VALUES (6, 3, 12, 'PENDING', '2026-06-29 08:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (7, 5, 11, 'APPROVED', '2026-06-27 18:00:00', '2026-06-27 19:00:00', NULL);
INSERT INTO `post_apply` VALUES (8, 5, 16, 'APPROVED', '2026-06-27 18:30:00', '2026-06-27 19:00:00', NULL);
INSERT INTO `post_apply` VALUES (9, 5, 18, 'APPROVED', '2026-06-27 19:00:00', '2026-06-27 19:30:00', NULL);
INSERT INTO `post_apply` VALUES (10, 6, 4, 'ATTENDED', '2026-06-28 05:30:00', '2026-06-28 06:00:00', NULL);
INSERT INTO `post_apply` VALUES (11, 6, 13, 'ATTENDED', '2026-06-28 05:45:00', '2026-06-28 06:00:00', NULL);
INSERT INTO `post_apply` VALUES (12, 6, 17, 'ATTENDED', '2026-06-28 05:50:00', '2026-06-28 06:00:00', NULL);
INSERT INTO `post_apply` VALUES (13, 7, 4, 'ATTENDED', '2026-06-24 15:00:00', '2026-06-24 16:00:00', NULL);
INSERT INTO `post_apply` VALUES (14, 7, 7, 'ATTENDED', '2026-06-24 15:30:00', '2026-06-24 16:00:00', NULL);
INSERT INTO `post_apply` VALUES (15, 7, 10, 'ATTENDED', '2026-06-24 16:00:00', '2026-06-24 16:30:00', NULL);
INSERT INTO `post_apply` VALUES (16, 8, 4, 'ATTENDED', '2026-06-26 15:00:00', '2026-06-26 15:30:00', NULL);
INSERT INTO `post_apply` VALUES (17, 8, 13, 'ATTENDED', '2026-06-26 15:30:00', '2026-06-26 16:00:00', NULL);
INSERT INTO `post_apply` VALUES (18, 8, 17, 'ATTENDED', '2026-06-26 15:30:00', '2026-06-26 16:00:00', NULL);
INSERT INTO `post_apply` VALUES (19, 8, 6, 'ATTENDED', '2026-06-26 14:00:00', '2026-06-26 15:00:00', NULL);
INSERT INTO `post_apply` VALUES (20, 8, 9, 'ATTENDED', '2026-06-26 14:00:00', '2026-06-26 15:00:00', NULL);
INSERT INTO `post_apply` VALUES (21, 8, 11, 'NOSHOW', '2026-06-26 15:00:00', '2026-06-27 10:00:00', NULL);
INSERT INTO `post_apply` VALUES (22, 8, 15, 'ATTENDED', '2026-06-26 14:30:00', '2026-06-26 15:00:00', NULL);
INSERT INTO `post_apply` VALUES (23, 8, 5, 'CANCELLED', '2026-06-26 10:00:00', '2026-06-26 11:00:00', NULL);
INSERT INTO `post_apply` VALUES (24, 9, 7, 'ATTENDED', '2026-06-25 19:00:00', '2026-06-25 19:30:00', NULL);
INSERT INTO `post_apply` VALUES (25, 9, 10, 'ATTENDED', '2026-06-25 19:30:00', '2026-06-25 20:00:00', NULL);
INSERT INTO `post_apply` VALUES (26, 9, 12, 'ATTENDED', '2026-06-25 18:00:00', '2026-06-25 19:00:00', NULL);
INSERT INTO `post_apply` VALUES (27, 9, 14, 'ATTENDED', '2026-06-25 19:00:00', '2026-06-25 19:30:00', NULL);
INSERT INTO `post_apply` VALUES (28, 11, 34, 'APPROVED', '2026-06-28 12:00:00', '2026-06-28 14:00:00', NULL);
INSERT INTO `post_apply` VALUES (29, 11, 20, 'PENDING', '2026-06-29 09:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (30, 12, 26, 'APPROVED', '2026-06-28 10:00:00', '2026-06-28 11:00:00', NULL);
INSERT INTO `post_apply` VALUES (31, 12, 40, 'PENDING', '2026-06-29 08:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (32, 12, 37, 'PENDING', '2026-06-29 10:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (33, 13, 44, 'PENDING', '2026-06-29 08:30:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (34, 13, 30, 'PENDING', '2026-06-29 10:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (35, 14, 32, 'APPROVED', '2026-06-28 18:00:00', '2026-06-28 19:00:00', NULL);
INSERT INTO `post_apply` VALUES (36, 14, 42, 'APPROVED', '2026-06-28 19:00:00', '2026-06-28 20:00:00', NULL);
INSERT INTO `post_apply` VALUES (37, 14, 36, 'APPROVED', '2026-06-28 19:30:00', '2026-06-28 20:00:00', NULL);
INSERT INTO `post_apply` VALUES (38, 14, 22, 'PENDING', '2026-06-29 11:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (39, 15, 33, 'APPROVED', '2026-06-28 14:00:00', '2026-06-28 15:00:00', NULL);
INSERT INTO `post_apply` VALUES (40, 15, 48, 'APPROVED', '2026-06-28 15:00:00', '2026-06-28 16:00:00', NULL);
INSERT INTO `post_apply` VALUES (41, 15, 38, 'PENDING', '2026-06-29 09:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (42, 15, 31, 'PENDING', '2026-06-29 10:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (43, 16, 24, 'APPROVED', '2026-06-28 10:00:00', '2026-06-28 11:00:00', NULL);
INSERT INTO `post_apply` VALUES (44, 16, 30, 'PENDING', '2026-06-29 08:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (45, 18, 33, 'APPROVED', '2026-06-28 16:00:00', '2026-06-28 17:00:00', NULL);
INSERT INTO `post_apply` VALUES (46, 18, 47, 'PENDING', '2026-06-29 10:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (47, 19, 35, 'APPROVED', '2026-06-28 06:00:00', '2026-06-28 06:30:00', NULL);
INSERT INTO `post_apply` VALUES (48, 19, 29, 'APPROVED', '2026-06-28 06:30:00', '2026-06-28 06:30:00', NULL);
INSERT INTO `post_apply` VALUES (49, 20, 21, 'APPROVED', '2026-06-28 18:00:00', '2026-06-28 19:00:00', NULL);
INSERT INTO `post_apply` VALUES (50, 20, 30, 'PENDING', '2026-06-29 12:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (51, 21, 31, 'APPROVED', '2026-06-28 20:00:00', '2026-06-28 21:00:00', NULL);
INSERT INTO `post_apply` VALUES (52, 21, 16, 'PENDING', '2026-06-29 10:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (53, 21, 38, 'PENDING', '2026-06-29 11:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (54, 22, 4, 'APPROVED', '2026-06-28 15:00:00', '2026-06-28 16:00:00', NULL);
INSERT INTO `post_apply` VALUES (55, 22, 43, 'PENDING', '2026-06-29 09:00:00', NULL, NULL);
INSERT INTO `post_apply` VALUES (56, 23, 39, 'APPROVED', '2026-06-27 14:00:00', '2026-06-27 15:00:00', NULL);
INSERT INTO `post_apply` VALUES (57, 23, 7, 'APPROVED', '2026-06-27 14:30:00', '2026-06-27 15:00:00', NULL);
INSERT INTO `post_apply` VALUES (58, 23, 14, 'APPROVED', '2026-06-27 15:00:00', '2026-06-27 15:30:00', NULL);
INSERT INTO `post_apply` VALUES (59, 23, 8, 'APPROVED', '2026-06-27 15:30:00', '2026-06-27 16:00:00', NULL);
INSERT INTO `post_apply` VALUES (60, 23, 33, 'APPROVED', '2026-06-27 16:00:00', '2026-06-27 16:30:00', NULL);
INSERT INTO `post_apply` VALUES (61, 24, 33, 'ATTENDED', '2026-06-28 17:00:00', '2026-06-28 17:30:00', NULL);
INSERT INTO `post_apply` VALUES (62, 24, 35, 'ATTENDED', '2026-06-28 17:30:00', '2026-06-28 18:00:00', NULL);
INSERT INTO `post_apply` VALUES (63, 25, 22, 'ATTENDED', '2026-06-28 07:30:00', '2026-06-28 08:00:00', NULL);
INSERT INTO `post_apply` VALUES (64, 25, 40, 'ATTENDED', '2026-06-28 08:00:00', '2026-06-28 08:00:00', NULL);
INSERT INTO `post_apply` VALUES (65, 25, 34, 'ATTENDED', '2026-06-28 08:30:00', '2026-06-28 08:30:00', NULL);
INSERT INTO `post_apply` VALUES (66, 26, 35, 'ATTENDED', '2026-06-24 16:00:00', '2026-06-24 16:30:00', NULL);
INSERT INTO `post_apply` VALUES (67, 26, 41, 'ATTENDED', '2026-06-24 16:30:00', '2026-06-24 17:00:00', NULL);
INSERT INTO `post_apply` VALUES (68, 27, 26, 'ATTENDED', '2026-06-25 13:00:00', '2026-06-25 13:30:00', NULL);
INSERT INTO `post_apply` VALUES (69, 27, 32, 'ATTENDED', '2026-06-25 13:30:00', '2026-06-25 14:00:00', NULL);
INSERT INTO `post_apply` VALUES (70, 27, 44, 'ATTENDED', '2026-06-25 14:00:00', '2026-06-25 14:00:00', NULL);
INSERT INTO `post_apply` VALUES (71, 28, 34, 'ATTENDED', '2026-06-26 11:00:00', '2026-06-26 11:30:00', NULL);
INSERT INTO `post_apply` VALUES (72, 28, 42, 'ATTENDED', '2026-06-26 11:30:00', '2026-06-26 12:00:00', NULL);
INSERT INTO `post_apply` VALUES (73, 28, 44, 'ATTENDED', '2026-06-26 11:00:00', '2026-06-26 12:00:00', NULL);
INSERT INTO `post_apply` VALUES (74, 29, 46, 'ATTENDED', '2026-06-26 17:00:00', '2026-06-26 17:30:00', NULL);
INSERT INTO `post_apply` VALUES (75, 29, 21, 'ATTENDED', '2026-06-26 17:30:00', '2026-06-26 17:30:00', NULL);
INSERT INTO `post_apply` VALUES (76, 29, 36, 'ATTENDED', '2026-06-26 17:30:00', '2026-06-26 17:30:00', NULL);
INSERT INTO `post_apply` VALUES (77, 29, 18, 'ATTENDED', '2026-06-26 17:00:00', '2026-06-26 17:30:00', NULL);
INSERT INTO `post_apply` VALUES (78, 29, 24, 'ATTENDED', '2026-06-26 17:30:00', '2026-06-26 18:00:00', NULL);
INSERT INTO `post_apply` VALUES (79, 29, 12, 'NOSHOW', '2026-06-26 17:00:00', '2026-06-27 10:00:00', NULL);
INSERT INTO `post_apply` VALUES (80, 30, 48, 'ATTENDED', '2026-06-27 14:00:00', '2026-06-27 14:30:00', NULL);
INSERT INTO `post_apply` VALUES (81, 30, 32, 'ATTENDED', '2026-06-27 14:00:00', '2026-06-27 14:30:00', NULL);
INSERT INTO `post_apply` VALUES (82, 30, 42, 'ATTENDED', '2026-06-27 14:30:00', '2026-06-27 14:30:00', NULL);
INSERT INTO `post_apply` VALUES (83, 30, 12, 'ATTENDED', '2026-06-27 14:00:00', '2026-06-27 14:30:00', NULL);
INSERT INTO `post_apply` VALUES (84, 30, 10, 'ATTENDED', '2026-06-27 14:30:00', '2026-06-27 14:30:00', NULL);
INSERT INTO `post_apply` VALUES (85, 30, 4, 'ATTENDED', '2026-06-27 14:00:00', '2026-06-27 14:30:00', NULL);
INSERT INTO `post_apply` VALUES (86, 30, 39, 'ATTENDED', '2026-06-27 14:30:00', '2026-06-27 14:30:00', NULL);
INSERT INTO `post_apply` VALUES (87, 100, 5, 'ATTENDED', '2026-08-04 16:24:30', '2026-08-05 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (88, 100, 6, 'ATTENDED', '2026-08-04 16:24:30', '2026-08-05 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (89, 100, 7, 'ATTENDED', '2026-08-05 16:24:30', '2026-08-05 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (90, 101, 6, 'ATTENDED', '2026-08-06 16:24:30', '2026-08-07 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (91, 101, 7, 'ATTENDED', '2026-08-06 16:24:30', '2026-08-07 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (92, 101, 8, 'ATTENDED', '2026-08-06 16:24:30', '2026-08-07 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (93, 101, 9, 'ATTENDED', '2026-08-06 16:24:30', '2026-08-07 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (94, 102, 7, 'ATTENDED', '2026-08-07 16:24:30', '2026-08-08 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (95, 102, 8, 'ATTENDED', '2026-08-07 16:24:30', '2026-08-08 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (96, 103, 8, 'ATTENDED', '2026-08-08 16:24:30', '2026-08-09 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (97, 103, 9, 'ATTENDED', '2026-08-08 16:24:30', '2026-08-09 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (98, 103, 10, 'ATTENDED', '2026-08-08 16:24:30', '2026-08-09 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (99, 103, 11, 'ATTENDED', '2026-08-08 16:24:30', '2026-08-09 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (100, 103, 12, 'ATTENDED', '2026-08-09 16:24:30', '2026-08-09 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (101, 104, 9, 'ATTENDED', '2026-08-09 16:24:30', '2026-08-10 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (102, 104, 10, 'ATTENDED', '2026-08-09 16:24:30', '2026-08-10 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (103, 104, 11, 'ATTENDED', '2026-08-09 16:24:30', '2026-08-10 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (104, 104, 12, 'ATTENDED', '2026-08-09 16:24:30', '2026-08-10 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (105, 104, 13, 'ATTENDED', '2026-08-09 16:24:30', '2026-08-10 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (106, 105, 11, 'ATTENDED', '2026-08-10 16:24:30', '2026-08-11 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (107, 105, 12, 'ATTENDED', '2026-08-10 16:24:30', '2026-08-11 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (108, 105, 13, 'ATTENDED', '2026-08-10 16:24:30', '2026-08-11 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (109, 106, 13, 'ATTENDED', '2026-08-11 16:24:30', '2026-08-12 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (110, 106, 14, 'ATTENDED', '2026-08-11 16:24:30', '2026-08-12 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (111, 106, 15, 'ATTENDED', '2026-08-11 16:24:30', '2026-08-12 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (112, 106, 16, 'ATTENDED', '2026-08-11 16:24:30', '2026-08-12 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (113, 107, 15, 'ATTENDED', '2026-08-12 16:24:30', '2026-08-13 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (114, 107, 16, 'ATTENDED', '2026-08-12 16:24:30', '2026-08-13 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (115, 107, 17, 'ATTENDED', '2026-08-12 16:24:30', '2026-08-13 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (116, 107, 18, 'ATTENDED', '2026-08-12 16:24:30', '2026-08-13 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (117, 107, 19, 'ATTENDED', '2026-08-12 16:24:30', '2026-08-13 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (118, 108, 16, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (119, 108, 17, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (120, 108, 18, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (121, 109, 17, 'APPROVED', '2026-08-14 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (122, 109, 18, 'APPROVED', '2026-08-14 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (123, 110, 19, 'APPROVED', '2026-08-14 16:24:30', '2026-08-15 04:24:30', NULL);
INSERT INTO `post_apply` VALUES (124, 110, 20, 'APPROVED', '2026-08-14 16:24:30', '2026-08-15 04:24:30', NULL);
INSERT INTO `post_apply` VALUES (125, 110, 21, 'APPROVED', '2026-08-15 04:24:30', '2026-08-15 10:24:30', NULL);
INSERT INTO `post_apply` VALUES (126, 110, 22, 'APPROVED', '2026-08-15 04:24:30', '2026-08-15 10:24:30', NULL);
INSERT INTO `post_apply` VALUES (127, 111, 21, 'APPROVED', '2026-08-15 04:24:30', '2026-08-15 10:24:30', NULL);
INSERT INTO `post_apply` VALUES (128, 111, 22, 'APPROVED', '2026-08-15 04:24:30', '2026-08-15 10:24:30', NULL);
INSERT INTO `post_apply` VALUES (129, 122, 21, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (130, 122, 22, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (131, 122, 23, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (132, 122, 24, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (133, 122, 25, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (134, 122, 26, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (135, 122, 27, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (136, 122, 28, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (137, 122, 29, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (138, 123, 23, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (139, 123, 24, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (140, 123, 25, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (141, 123, 26, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (142, 123, 27, 'APPROVED', '2026-08-13 16:24:30', '2026-08-14 16:24:30', NULL);
INSERT INTO `post_apply` VALUES (143, 124, 25, 'APPROVED', '2026-08-14 16:24:30', '2026-08-15 04:24:30', NULL);
INSERT INTO `post_apply` VALUES (144, 124, 26, 'APPROVED', '2026-08-14 16:24:30', '2026-08-15 04:24:30', NULL);
INSERT INTO `post_apply` VALUES (145, 124, 27, 'APPROVED', '2026-08-14 16:24:30', '2026-08-15 04:24:30', NULL);
INSERT INTO `post_apply` VALUES (146, 124, 28, 'APPROVED', '2026-08-14 16:24:30', '2026-08-15 04:24:30', NULL);
INSERT INTO `post_apply` VALUES (147, 124, 29, 'APPROVED', '2026-08-14 16:24:30', '2026-08-15 04:24:30', NULL);
INSERT INTO `post_apply` VALUES (148, 124, 30, 'APPROVED', '2026-08-14 16:24:30', '2026-08-15 04:24:30', NULL);
INSERT INTO `post_apply` VALUES (149, 124, 31, 'APPROVED', '2026-08-14 16:24:30', '2026-08-15 04:24:30', NULL);
INSERT INTO `post_apply` VALUES (150, 124, 32, 'APPROVED', '2026-08-14 16:24:30', '2026-08-15 04:24:30', NULL);
INSERT INTO `post_apply` VALUES (151, 124, 33, 'APPROVED', '2026-08-14 16:24:30', '2026-08-15 04:24:30', NULL);
INSERT INTO `post_apply` VALUES (152, 114, 2, 'PENDING', '2026-08-15 17:07:27', NULL, NULL);

-- ----------------------------
-- Table structure for post_comment
-- ----------------------------
DROP TABLE IF EXISTS `post_comment`;
CREATE TABLE `post_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post`(`post_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of post_comment
-- ----------------------------
INSERT INTO `post_comment` VALUES (1, 1, 8, '求拉！我打控卫的', '2026-06-28 14:30:00');
INSERT INTO `post_comment` VALUES (2, 2, 11, '我玩打野，带飞', '2026-06-28 20:30:00');
INSERT INTO `post_comment` VALUES (3, 2, 16, '还有位置吗？我法师', '2026-06-29 08:00:00');
INSERT INTO `post_comment` VALUES (4, 4, 14, '这个本好玩吗？没玩过', '2026-06-28 19:00:00');
INSERT INTO `post_comment` VALUES (5, 7, 4, '三国杀真不错，下次再约', '2026-06-25 18:30:00');
INSERT INTO `post_comment` VALUES (6, 7, 7, '赢了输了？', '2026-06-25 19:00:00');
INSERT INTO `post_comment` VALUES (7, 8, 17, '今天打得真爽，下次继续', '2026-06-27 18:30:00');
INSERT INTO `post_comment` VALUES (8, 9, 12, '夜跑太舒服了，求固定车队', '2026-06-26 21:00:00');
INSERT INTO `post_comment` VALUES (9, 6, 4, '明天继续！', '2026-06-29 07:00:00');
INSERT INTO `post_comment` VALUES (10, 3, 12, '新手可以吗，想学羽毛球', '2026-06-29 09:00:00');
INSERT INTO `post_comment` VALUES (11, 11, 34, '什么电影呀？', '2026-06-28 12:30:00');
INSERT INTO `post_comment` VALUES (12, 12, 26, '一起冲期末！', '2026-06-28 10:30:00');
INSERT INTO `post_comment` VALUES (13, 15, 33, '新手能教教我吗完全不会', '2026-06-28 14:30:00');
INSERT INTO `post_comment` VALUES (14, 15, 48, '我也是新手哈哈哈一起学', '2026-06-28 15:30:00');
INSERT INTO `post_comment` VALUES (15, 15, 30, '求带！', '2026-06-29 10:30:00');
INSERT INTO `post_comment` VALUES (16, 18, 33, '我只有手机可以吗', '2026-06-28 16:30:00');
INSERT INTO `post_comment` VALUES (17, 20, 21, '我最近在听周杰伦！', '2026-06-28 18:30:00');
INSERT INTO `post_comment` VALUES (18, 21, 31, '我玩射手，来辅助！', '2026-06-28 20:30:00');
INSERT INTO `post_comment` VALUES (19, 22, 4, '养生球我最爱', '2026-06-28 15:30:00');
INSERT INTO `post_comment` VALUES (20, 24, 33, '练胸还是练背？', '2026-06-28 17:30:00');
INSERT INTO `post_comment` VALUES (21, 25, 22, '卷起来！', '2026-06-28 08:00:00');
INSERT INTO `post_comment` VALUES (22, 26, 43, '一起练背效果翻倍', '2026-06-25 17:30:00');
INSERT INTO `post_comment` VALUES (23, 28, 34, '味道怎么样？', '2026-06-27 12:30:00');
INSERT INTO `post_comment` VALUES (24, 29, 46, '今天的夕阳绝了', '2026-06-27 18:30:00');
INSERT INTO `post_comment` VALUES (25, 30, 48, '超好玩！下次还要来', '2026-06-28 18:30:00');
INSERT INTO `post_comment` VALUES (26, 100, 5, '上周打得真爽，下次继续！', '2026-08-06 16:24:30');
INSERT INTO `post_comment` VALUES (27, 100, 6, '求固定球友', '2026-08-06 16:24:30');
INSERT INTO `post_comment` VALUES (28, 101, 7, '五排赢了3把，开心', '2026-08-08 16:24:30');
INSERT INTO `post_comment` VALUES (29, 101, 8, '下次还来', '2026-08-08 16:24:30');
INSERT INTO `post_comment` VALUES (30, 104, 10, '这个本真的推荐！', '2026-08-11 16:24:30');
INSERT INTO `post_comment` VALUES (31, 104, 11, '剧情反转绝了', '2026-08-11 16:24:30');
INSERT INTO `post_comment` VALUES (32, 108, 17, '马上到，等我！', '2026-08-15 15:54:30');
INSERT INTO `post_comment` VALUES (33, 112, 5, '周六有空，报名了', '2026-08-15 10:24:30');
INSERT INTO `post_comment` VALUES (34, 117, 11, '狼人杀专业户，求带', '2026-08-15 13:24:30');
INSERT INTO `post_comment` VALUES (35, 122, 22, '内战快点开始吧！', '2026-08-15 14:24:30');

-- ----------------------------
-- Table structure for report
-- ----------------------------
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reporter_id` bigint NOT NULL COMMENT '举报人',
  `target_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'POST/USER',
  `target_id` bigint NOT NULL COMMENT '被举报对象ID',
  `reason_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '举报类型',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '举报说明',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待处理 1有效 2无效',
  `handle_by` bigint NULL DEFAULT NULL COMMENT '处理人',
  `handle_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理备注',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_reporter`(`reporter_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '举报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of report
-- ----------------------------

-- ----------------------------
-- Table structure for review
-- ----------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `reviewer_id` bigint NOT NULL COMMENT '评价人',
  `target_user_id` bigint NOT NULL COMMENT '被评价人',
  `rating` tinyint NOT NULL COMMENT '1-5 星',
  `tag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '靠谱/爽约/迟到',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_reviewer_target`(`post_id` ASC, `reviewer_id` ASC, `target_user_id` ASC) USING BTREE,
  INDEX `idx_target`(`target_user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 58 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评价表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review
-- ----------------------------
INSERT INTO `review` VALUES (1, 8, 4, 13, 4, '靠谱', '配合不错', '2026-06-27 19:00:00');
INSERT INTO `review` VALUES (2, 8, 4, 17, 5, '靠谱', '大腿！', '2026-06-27 19:00:00');
INSERT INTO `review` VALUES (3, 8, 13, 4, 5, '靠谱', '好队友', '2026-06-27 19:30:00');
INSERT INTO `review` VALUES (4, 8, 13, 17, 4, '靠谱', '下次还一起', '2026-06-27 19:30:00');
INSERT INTO `review` VALUES (5, 8, 17, 4, 5, '靠谱', '组织得很好', '2026-06-27 20:00:00');
INSERT INTO `review` VALUES (6, 8, 17, 13, 4, '靠谱', '很积极', '2026-06-27 20:00:00');
INSERT INTO `review` VALUES (7, 8, 4, 11, 1, '爽约', '说好了来结果没来', '2026-06-27 19:00:00');
INSERT INTO `review` VALUES (8, 8, 17, 11, 2, '爽约', '放鸽子了', '2026-06-27 20:00:00');
INSERT INTO `review` VALUES (9, 7, 4, 7, 5, '靠谱', '玩得很好', '2026-06-25 19:00:00');
INSERT INTO `review` VALUES (10, 7, 4, 10, 4, '靠谱', '新手但很有潜力', '2026-06-25 19:00:00');
INSERT INTO `review` VALUES (11, 7, 7, 4, 5, '靠谱', '老司机带飞', '2026-06-25 19:30:00');
INSERT INTO `review` VALUES (12, 7, 10, 4, 5, '靠谱', '很有耐心教新人', '2026-06-25 20:00:00');
INSERT INTO `review` VALUES (13, 9, 7, 10, 4, '靠谱', '配速很稳', '2026-06-26 22:00:00');
INSERT INTO `review` VALUES (14, 9, 7, 12, 3, '靠谱', '还行', '2026-06-26 22:00:00');
INSERT INTO `review` VALUES (15, 9, 10, 7, 5, '靠谱', '领跑很棒', '2026-06-26 22:30:00');
INSERT INTO `review` VALUES (16, 9, 12, 7, 4, '靠谱', '下次继续约', '2026-06-26 22:30:00');
INSERT INTO `review` VALUES (17, 26, 43, 35, 5, '靠谱', '配合很好，下次继续', '2026-06-25 20:00:00');
INSERT INTO `review` VALUES (18, 26, 43, 41, 4, '靠谱', '动作很标准', '2026-06-25 20:00:00');
INSERT INTO `review` VALUES (19, 26, 35, 43, 5, '靠谱', '很不错', '2026-06-25 20:30:00');
INSERT INTO `review` VALUES (20, 27, 28, 26, 4, '靠谱', '一起看剧很开心', '2026-06-26 18:00:00');
INSERT INTO `review` VALUES (21, 27, 28, 32, 5, '靠谱', '推荐的电影很好看', '2026-06-26 18:00:00');
INSERT INTO `review` VALUES (22, 27, 28, 44, 4, '靠谱', '专注看剧不玩手机', '2026-06-26 18:00:00');
INSERT INTO `review` VALUES (23, 27, 26, 28, 5, '靠谱', '组织得很好', '2026-06-26 18:30:00');
INSERT INTO `review` VALUES (24, 27, 32, 28, 5, '靠谱', '下次还要一起', '2026-06-26 18:30:00');
INSERT INTO `review` VALUES (25, 28, 20, 34, 5, '靠谱', '同样是个吃货！', '2026-06-27 14:00:00');
INSERT INTO `review` VALUES (26, 28, 20, 42, 4, '靠谱', '聊得很开心', '2026-06-27 14:00:00');
INSERT INTO `review` VALUES (27, 28, 20, 44, 4, '靠谱', '很随和', '2026-06-27 14:00:00');
INSERT INTO `review` VALUES (28, 28, 34, 20, 5, '靠谱', '推荐的地方很好吃', '2026-06-27 14:30:00');
INSERT INTO `review` VALUES (29, 29, 46, 21, 4, '靠谱', '拍照技术不错', '2026-06-27 20:00:00');
INSERT INTO `review` VALUES (30, 29, 46, 36, 5, '靠谱', '拍得很好看', '2026-06-27 20:00:00');
INSERT INTO `review` VALUES (31, 29, 46, 18, 4, '靠谱', '构图很棒', '2026-06-27 20:00:00');
INSERT INTO `review` VALUES (32, 29, 46, 24, 5, '靠谱', '准时到达', '2026-06-27 20:00:00');
INSERT INTO `review` VALUES (33, 29, 46, 12, 1, '爽约', '说好来结果放鸽子', '2026-06-27 20:00:00');
INSERT INTO `review` VALUES (34, 29, 21, 46, 5, '靠谱', '拍晚霞太美了', '2026-06-27 20:30:00');
INSERT INTO `review` VALUES (35, 30, 39, 48, 4, '靠谱', '玩得很投入', '2026-06-28 19:00:00');
INSERT INTO `review` VALUES (36, 30, 39, 32, 4, '靠谱', '很会推理', '2026-06-28 19:00:00');
INSERT INTO `review` VALUES (37, 30, 39, 42, 5, '靠谱', '最佳女巫！', '2026-06-28 19:00:00');
INSERT INTO `review` VALUES (38, 30, 39, 12, 3, '靠谱', '还行', '2026-06-28 19:00:00');
INSERT INTO `review` VALUES (39, 30, 39, 10, 4, '靠谱', '很欢乐', '2026-06-28 19:00:00');
INSERT INTO `review` VALUES (40, 30, 39, 4, 5, '靠谱', '发言很搞笑哈哈哈', '2026-06-28 19:00:00');
INSERT INTO `review` VALUES (41, 30, 48, 39, 5, '靠谱', '组织得很好！新手体验满分', '2026-06-28 19:30:00');
INSERT INTO `review` VALUES (42, 30, 32, 39, 5, '靠谱', '下次再约', '2026-06-28 19:30:00');
INSERT INTO `review` VALUES (43, 100, 4, 5, 5, '靠谱', '技术好，配合默契', '2026-08-06 16:24:30');
INSERT INTO `review` VALUES (44, 100, 4, 6, 4, '靠谱', '打得不错', '2026-08-06 16:24:30');
INSERT INTO `review` VALUES (45, 100, 4, 7, 5, '友好', '人很好', '2026-08-06 16:24:30');
INSERT INTO `review` VALUES (46, 100, 5, 4, 5, '靠谱', '组织得很好', '2026-08-06 16:24:30');
INSERT INTO `review` VALUES (47, 100, 6, 4, 4, '准时', '很准时', '2026-08-06 16:24:30');
INSERT INTO `review` VALUES (48, 101, 5, 6, 5, '靠谱', '上分大腿', '2026-08-08 16:24:30');
INSERT INTO `review` VALUES (49, 101, 5, 7, 4, '友好', '配合不错', '2026-08-08 16:24:30');
INSERT INTO `review` VALUES (50, 101, 6, 5, 5, '靠谱', '带飞', '2026-08-08 16:24:30');
INSERT INTO `review` VALUES (51, 104, 8, 9, 5, '靠谱', '推理很强', '2026-08-11 16:24:30');
INSERT INTO `review` VALUES (52, 104, 8, 10, 4, '友好', '体验很好', '2026-08-11 16:24:30');
INSERT INTO `review` VALUES (53, 104, 9, 8, 5, '靠谱', '组织完美', '2026-08-11 16:24:30');
INSERT INTO `review` VALUES (54, 106, 12, 13, 5, '友好', '很欢乐', '2026-08-13 16:24:30');
INSERT INTO `review` VALUES (55, 106, 13, 12, 4, '准时', '守时', '2026-08-13 16:24:30');
INSERT INTO `review` VALUES (56, 107, 14, 15, 5, '靠谱', '教得很好', '2026-08-14 16:24:30');
INSERT INTO `review` VALUES (57, 107, 15, 14, 5, '友好', '耐心', '2026-08-14 16:24:30');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学号',
  `phone` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码(BCrypt)',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `signature` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个性签名',
  `college` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属学院',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  `credit_score` int NULL DEFAULT 100 COMMENT '信誉分 0-100',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 1正常 0封禁',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户名（唯一，登录用）',
  `is_admin` tinyint NULL DEFAULT 0 COMMENT '是否管理员 0否 1是',
  `profile_privacy` tinyint NULL DEFAULT 0 COMMENT '主页隐私 0公开 1隐藏历史组局（V1.1）',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_no`(`student_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin001', '13800000000', '$2a$10$e7ZZEXLXq1W265LW25VN8ecZx47trR8xZB8hBPr7h6n0Kv2Xojfka', '系统管理员', NULL, NULL, '信息学院', 0, 100, 1, 'admin001', 1, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (2, '20240001', '13800000001', '$2a$10$e7ZZEXLXq1W265LW25VN8ecZx47trR8xZB8hBPr7h6n0Kv2Xojfka', '小明', NULL, NULL, '计算机学院', 0, 100, 1, '20240001', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (3, '20240002', '13800000002', '$2a$10$e7ZZEXLXq1W265LW25VN8ecZx47trR8xZB8hBPr7h6n0Kv2Xojfka', '小红', NULL, NULL, '体育学院', 0, 100, 1, '20240002', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (4, '2342170101', '1382340101', '$2a$10$zjooIII90GrE6Btl0mJYUOzeZScN0TCAk6sRv6qvhr1L1hG0t7SQ6', '张晨', NULL, NULL, '大数据人工智能学院', 1, 95, 1, '2342170101', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (5, '2342170102', '1382340102', '$2a$10$MixPESeHXi5lwiXFRjgU/uqrOXvkVxOgubIacH2vCpAZV2YF6zvHe', '李雨桐', NULL, NULL, '大数据人工智能学院', 2, 88, 1, '2342170102', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (6, '2342170103', '1382340103', '$2a$10$47zjN/kngpieE742JmrTqOMWnA.TA7j.dbZIO2QjZCizvoXmTT03m', '王浩然', NULL, NULL, '大数据人工智能学院', 1, 92, 1, '2342170103', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (7, '2342170104', '1382340104', '$2a$10$BZ2oEMv8pvuHCAP3u2xPl.QOuUoHG7VzBCf7WlKd9EG6sziN1qK5m', '赵思琪', NULL, NULL, '大数据人工智能学院', 2, 100, 1, '2342170104', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (8, '2342170105', '1382340105', '$2a$10$3rcqX8yYpnLjx0haiOcmXuxzKv0RszhgCZraYMBOJBYJdt8qD0J8S', '孙宇航', NULL, NULL, '大数据人工智能学院', 1, 78, 1, '2342170105', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (9, '2342170201', '1382340201', '$2a$10$0aGnZGBMrtomC7yT3M2GTeL3xE7b3N/qw4U6ixVaITKY/KXP7Xuti', '周明远', NULL, NULL, '大数据人工智能学院', 1, 85, 1, '2342170201', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (10, '2342170202', '1382340202', '$2a$10$7b0etg6LS92nY/hjvTWw1.MEyO6GOBmAW5GueBGwtJSP1my1dhVom', '吴宛瑜', NULL, NULL, '大数据人工智能学院', 2, 96, 1, '2342170202', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (11, '2342170203', '1382340203', '$2a$10$o76eG9r28nh2oSp4R1UZsu9mw99KIr8o0ghE7SMdAjh8rZQbU69eC', '郑子轩', NULL, NULL, '大数据人工智能学院', 1, 82, 1, '2342170203', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (12, '2342170204', '1382340204', '$2a$10$doSQmUOhU9yemdMtiWfrFur9M8lCfEeTpJGrLcIqiZMCHHptDARBy', '钱诗雅', NULL, NULL, '大数据人工智能学院', 2, 90, 1, '2342170204', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (13, '2342170205', '1382340205', '$2a$10$Cqe3b8upEoivaSXU11cEEumkSmltcqDwvCVkG5hUkfcsl2/vHwMx6', '陈泽宇', NULL, NULL, '大数据人工智能学院', 1, 75, 1, '2342170205', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (14, '2342170301', '1382340301', '$2a$10$Pvm2cBivfcel6GNTwZynoO9kZ9c.ode0A1BgLZsDoo2RVFZcwfQs.', '杨思涵', NULL, NULL, '大数据人工智能学院', 2, 91, 1, '2342170301', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (15, '2342170302', '1382340302', '$2a$10$bVcZozYk0anexpnOxghkbehBoAodPeQ6YaSjkrlYTkaANCnHm/.dG', '马俊豪', NULL, NULL, '大数据人工智能学院', 1, 83, 1, '2342170302', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (16, '2342170303', '1382340303', '$2a$10$Tr4oS/HTwMtiUOfucSO4XuTpedKLS1pZ4TRCKMa7sVQSda/BSyzXW', '林语嫣', NULL, NULL, '大数据人工智能学院', 2, 97, 1, '2342170303', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (17, '2342170304', '1382340304', '$2a$10$fhEfrMyUG50.axYeDtr/juygfFaq9x48MHW.yYvcOMpSYa4sIAsda', '何致远', NULL, NULL, '大数据人工智能学院', 1, 86, 1, '2342170304', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (18, '2342170305', '1382340305', '$2a$10$.mh7tLmePItFz4wkNbQMduQiVfcV0tnlsBacIYOinrNfv6ZDKiGgy', '黄梓萱', NULL, NULL, '大数据人工智能学院', 2, 93, 1, '2342170305', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (19, '2342170106', '1382340106', '$2a$10$sc6s3w.Vt3vJcwgUla29dufnOB/P5qqMwsGt2Id7uDCYQ.AUngVNK', '刘子涵', NULL, NULL, '大数据人工智能学院', 1, 87, 1, '2342170106', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (20, '2342170107', '1382340107', '$2a$10$CoeLgrEaH0LOA8inbAQr5OgNVaB06eROnvYmfjvTs5eruefWYEC8.', '陈雨欣', NULL, NULL, '大数据人工智能学院', 2, 94, 1, '2342170107', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (21, '2342170108', '1382340108', '$2a$10$4HEsWkOFLQfjRp771xT63OoB9Cckf.lJeWyD6t6eoXAswJm2GirDu', '唐一凡', NULL, NULL, '大数据人工智能学院', 1, 80, 1, '2342170108', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (22, '2342170109', '1382340109', '$2a$10$CocqgPGBcxfUJfs7ape2g.6XvGkOHPO6KxYv/kqPYGjzCkh/A.XJi', '苏小暖', NULL, NULL, '大数据人工智能学院', 2, 99, 1, '2342170109', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (23, '2342170110', '1382340110', '$2a$10$CpBDmVyDN/ElHqaj6DvRreI0OAhwjReDBIZ8HkHSMxwjYjCnGuyLu', '许光耀', NULL, NULL, '大数据人工智能学院', 1, 76, 1, '2342170110', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (24, '2342170206', '1382340206', '$2a$10$7RDQ.M/cLV.q.ti.ck7M5eS1TGShdAfXFhLdyV60nDA6y3SXz3/O.', '沈佳宜', NULL, NULL, '大数据人工智能学院', 2, 91, 1, '2342170206', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (25, '2342170207', '1382340207', '$2a$10$d0aYusYqaI.YjQw1mk6D5OniSJHEQsHEthImKSnl0j2oVKkSuREr.', '陆子航', NULL, NULL, '大数据人工智能学院', 1, 84, 1, '2342170207', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (26, '2342170208', '1382340208', '$2a$10$SOUcMWqVFHnM.P1/IfetpOO8I5/0aBKWE.i9iLmvhFWpb6UQoPFf.', '顾一一', NULL, NULL, '大数据人工智能学院', 2, 88, 1, '2342170208', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (27, '2342170209', '1382340209', '$2a$10$jGCLLMuItwcBLu2S99/OfuVNFA1V5/9MsYa.Xeb2FBwQoQBUWIniG', '高旭阳', NULL, NULL, '大数据人工智能学院', 1, 79, 1, '2342170209', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (28, '2342170210', '1382340210', '$2a$10$g.4ReZ36gxq/aQVkBSoY0O0ORo7jr92GvybS0U/EsfyRVaWv/dlw6', '唐诗韵', NULL, NULL, '大数据人工智能学院', 2, 95, 1, '2342170210', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (29, '2332170101', '1382330101', '$2a$10$oq5AQ3VD775cSTcMV8CMwOT8EEfDEyFsP1Uon/3MQPBi/x6iyuRha', '程一鸣', NULL, NULL, '计算机学院', 1, 82, 1, '2332170101', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (30, '2332170102', '1382330102', '$2a$10$lIhSIfA.KXqGCelpC0TqL.pKfyCpTiCtjO2l/rRsZwYI5iofKW.km', '林小鹿', NULL, NULL, '计算机学院', 2, 96, 1, '2332170102', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (31, '2332170103', '1382330103', '$2a$10$AiTvKd8Vxsp2roAbj02vaeiBDTqrrga/A7jJo.29B0TUWRCf/Yqzq', '赵大宝', NULL, NULL, '计算机学院', 1, 73, 1, '2332170103', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (32, '2332170104', '1382330104', '$2a$10$S2QwvxFZnNUjEgMG8UVTa.iE7NlHaxarMWrIkubVRBEN.yubmX1Ai', '周若溪', NULL, NULL, '计算机学院', 2, 90, 1, '2332170104', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (33, '2332170105', '1382330105', '$2a$10$21HejA9qBXqTEfhM9QVpmul4upxI/KarSAImAJbCOAk0Lr74pTff6', '韩子枫', NULL, NULL, '计算机学院', 1, 85, 1, '2332170105', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (34, '2332170106', '1382330106', '$2a$10$dOcYOzyx5YAYu4A54SpWs.4bvexjKUQQK1oDjJnjgXis3ikW/Hkei', '白若溪', NULL, NULL, '计算机学院', 2, 92, 1, '2332170106', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (35, '2332170107', '1382330107', '$2a$10$tBIX7RWM1nTRF9z7do1Z6uAfad6FiiAsmpsCR9gIRjv2zR0BdZQcC', '秦风', NULL, NULL, '计算机学院', 1, 78, 1, '2332170107', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (36, '2332170108', '1382330108', '$2a$10$B/yEb2cOEsywQJitz1AaQ.B8/fwEe3qa2Lwge/ddVqBwXINySfPzG', '叶知秋', NULL, NULL, '计算机学院', 1, 87, 1, '2332170108', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (37, '2332170109', '1382330109', '$2a$10$7slGYpoh.omXFAJAW5JN9uri.Jup5uGNiPd6y6L9GnqIXf0ZNnHU2', '夏浅浅', NULL, NULL, '计算机学院', 2, 83, 1, '2332170109', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (38, '2332170110', '1382330110', '$2a$10$XJNygJqDjldwLiln6PW3rOg4ROjzLeBVo63rFfQZkIozsQQW7BCNy', '江一帆', NULL, NULL, '计算机学院', 1, 88, 1, '2332170110', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (39, '2332170201', '1382330201', '$2a$10$x/T20Otq6m9ctii4D.jQhervGsZXskykde1860YZp8y7GfUKdHNO.', '方浩然', NULL, NULL, '计算机学院', 1, 81, 1, '2332170201', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (40, '2332170202', '1382330202', '$2a$10$wpQwrO5pU2htsYtD2YHD/uvVxTk6zt/rpklxEzKIOjVyKHka6LVQ6', '董小晚', NULL, NULL, '计算机学院', 2, 94, 1, '2332170202', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (41, '2332170203', '1382330203', '$2a$10$DDVvUiwIO6bIUu.aHW7x6uwzGhcL8IDPFMbFS46sNNHQ9lXN.fufa', '魏子骞', NULL, NULL, '计算机学院', 1, 77, 1, '2332170203', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (42, '2332170204', '1382330204', '$2a$10$KnZtiCtQkudz5QYsyY7oROKMfFOk4JLh7ke2Y52PLneRn8lPHjZ6e', '宋暖阳', NULL, NULL, '计算机学院', 2, 91, 1, '2332170204', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (43, '2332170205', '1382330205', '$2a$10$T2/rMdOmz1ZVh2z.eW2R8Op1jgPE5AM1hXhBMB.0yOhhmFfmFQckG', '秦少峰', NULL, NULL, '计算机学院', 1, 86, 1, '2332170205', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (44, '2332170206', '1382330206', '$2a$10$V07Eythl71ffiwiN0dW2jOzAEOeEAkL6cNtgdfMH0IONd5ZKN848K', '温馨', NULL, NULL, '计算机学院', 2, 89, 1, '2332170206', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (45, '2332170207', '1382330207', '$2a$10$RMznYE88gFRV/BKmcgJrc.7Y.F/kuyNFI950xBEGNphIlFFE9uilK', '龙天宇', NULL, NULL, '计算机学院', 1, 75, 1, '2332170207', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (46, '2332170208', '1382330208', '$2a$10$h1fu6RUX7crOtfBMR8/2NOysQCwheAUxMomJtrjHSmavsG3gmNe82', '廖一诺', NULL, NULL, '计算机学院', 2, 97, 1, '2332170208', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (47, '2332170209', '1382330209', '$2a$10$SEOd/SBcoUBLfGoaRdkuhO4O/JUlp/PzZDVMstN6huO1.aLGOAIr.', '田景行', NULL, NULL, '计算机学院', 1, 84, 1, '2332170209', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');
INSERT INTO `sys_user` VALUES (48, '2332170210', '1382330210', '$2a$10$LOsbHpCCgz2BiOsisSYFsuMQ1Ygw4iUQ2y5EWi68RwRGINwEfVmWG', '花浅月', NULL, NULL, '计算机学院', 2, 93, 1, '2332170210', 0, 0, '2026-08-15 15:57:25', '2026-08-15 15:57:25');

-- ----------------------------
-- Table structure for user_tag
-- ----------------------------
DROP TABLE IF EXISTS `user_tag`;
CREATE TABLE `user_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  `self_level` int NULL DEFAULT 1 COMMENT '自评段位 1-5',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_tag`(`user_id` ASC, `tag_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 94 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_tag
-- ----------------------------
INSERT INTO `user_tag` VALUES (1, 4, 1, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (2, 4, 11, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (3, 5, 2, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (4, 5, 12, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (5, 6, 5, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (6, 6, 4, 2, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (7, 7, 2, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (8, 7, 8, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (9, 7, 12, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (10, 8, 1, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (11, 8, 6, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (12, 9, 3, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (13, 9, 8, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (14, 10, 2, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (15, 10, 10, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (16, 11, 5, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (17, 11, 7, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (18, 12, 9, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (19, 12, 12, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (20, 13, 1, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (21, 13, 11, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (22, 14, 2, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (23, 14, 8, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (24, 14, 9, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (25, 15, 4, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (26, 15, 3, 2, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (27, 16, 5, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (28, 16, 10, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (29, 17, 1, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (30, 17, 3, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (31, 17, 11, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (32, 18, 2, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (33, 18, 6, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (34, 19, 14, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (35, 19, 15, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (36, 20, 13, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (37, 20, 12, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (38, 21, 17, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (39, 21, 3, 2, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (40, 22, 15, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (41, 22, 16, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (42, 23, 16, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (43, 23, 18, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (44, 24, 2, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (45, 24, 15, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (46, 25, 11, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (47, 25, 17, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (48, 26, 14, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (49, 26, 16, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (50, 27, 4, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (51, 27, 19, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (52, 28, 13, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (53, 28, 9, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (54, 29, 1, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (55, 29, 5, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (56, 30, 14, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (57, 30, 17, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (58, 31, 5, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (59, 31, 7, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (60, 32, 13, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (61, 32, 16, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (62, 33, 18, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (63, 33, 19, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (64, 34, 15, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (65, 34, 12, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (66, 35, 11, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (67, 35, 19, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (68, 36, 17, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (69, 36, 16, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (70, 37, 2, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (71, 37, 14, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (72, 38, 5, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (73, 38, 7, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (74, 39, 8, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (75, 39, 9, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (76, 40, 14, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (77, 40, 15, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (78, 41, 19, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (79, 41, 4, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (80, 42, 16, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (81, 42, 13, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (82, 43, 1, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (83, 43, 11, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (84, 44, 15, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (85, 44, 14, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (86, 45, 6, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (87, 45, 19, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (88, 46, 18, 5, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (89, 46, 17, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (90, 47, 8, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (91, 47, 16, 4, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (92, 48, 2, 3, '2026-08-15 15:57:25');
INSERT INTO `user_tag` VALUES (93, 48, 9, 4, '2026-08-15 15:57:25');

SET FOREIGN_KEY_CHECKS = 1;
