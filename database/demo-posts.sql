-- =============================================================
-- 演示用「未来时间」组局帖（用户端广场演示前执行）
--
-- 背景：init-all.sql / more-posts.sql 里的示例帖子时间都是固定的过去日期，
--       导入后会被「帖子状态推进」定时任务很快推到 FINISHED，
--       导致广场（只展示招募中/进行中）看起来是空的。
--       发布时间也**刻意错开**（而不是都用 NOW()）：created_time 只精确到秒，
--       一批帖子若同秒发布，广场「最新」排序里它们的先后是未定义的（实测看起来像乱序）。
--       本脚本用相对时间（NOW() + INTERVAL）生成一批未来的组局帖，
--       因此**任何时候执行都有效**，答辩演示前跑一次即可。
--
-- 前置：已导入 init-all.sql 与 more-users.sql（发布者取自 24421701xx 这批演示账号）
-- 用法：mysql -u root -p school_buddy < database/demo-posts.sql
-- 幂等：否。重复执行会产生重复帖子；如需重来，先执行文件末尾的清理 SQL。
-- =============================================================

USE `school_buddy`;

-- -------------------------------------------------------------
-- 1. 今晚的篮球局（2 小时后开始，已有 2 人通过报名）—— 埃隆·马斯克 发起
-- -------------------------------------------------------------
INSERT INTO `post` (`publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`,
                    `location`, `max_people`, `current_people`, `require_level`, `require_gender`,
                    `description`, `status`, `view_count`, `like_count`, `is_audit_pass`,
                    `created_time`, `updated_time`, `deleted`)
VALUES ((SELECT id FROM `sys_user` WHERE `student_no` = '2442170101'),
        '马斯克发起：东区篮球场 3v3 缺一人，今晚开打', '球类', '1',
        DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR),
        '东区篮球场 2 号场', 6, 3, 3, 0,
        '第一性原理三对三，强度中等，欢迎熟手加入。\n自备球鞋，场地已预约，打完一起聊星舰发射。',
        'RECRUITING', 128, 16, 1, DATE_SUB(NOW(), INTERVAL 8 MINUTE), NOW(), 0);
SET @p1 = LAST_INSERT_ID();

-- -------------------------------------------------------------
-- 2. 明天的羽毛球双打（不限段位）—— 李飞飞 发起
-- -------------------------------------------------------------
INSERT INTO `post` (`publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`,
                    `location`, `max_people`, `current_people`, `require_level`, `require_gender`,
                    `description`, `status`, `view_count`, `like_count`, `is_audit_pass`,
                    `created_time`, `updated_time`, `deleted`)
VALUES ((SELECT id FROM `sys_user` WHERE `student_no` = '2442170102'),
        '李飞飞约球：羽毛球双打，水平不限图个出汗', '球类', '2',
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 19 HOUR),
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 21 HOUR),
        '体育馆羽毛球场 3 号场', 4, 2, 0, 0,
        '实验室科研久坐太累，打两小时羽毛球活动活动，球我带了，欢迎新手！',
        'RECRUITING', 186, 23, 1, DATE_SUB(NOW(), INTERVAL 16 MINUTE), NOW(), 0);
SET @p2 = LAST_INSERT_ID();

-- -------------------------------------------------------------
-- 3. 王者荣耀五排（要求段位「高手」以上）—— 山姆·奥特曼 发起
-- -------------------------------------------------------------
INSERT INTO `post` (`publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`,
                    `location`, `max_people`, `current_people`, `require_level`, `require_gender`,
                    `description`, `status`, `view_count`, `like_count`, `is_audit_pass`,
                    `created_time`, `updated_time`, `deleted`)
VALUES ((SELECT id FROM `sys_user` WHERE `student_no` = '2442170103'),
        '奥特曼组队：王者荣耀五排上分，缺个野王', '电竞', '5',
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 21 HOUR),
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 23 HOUR),
        '线上开黑（语音房）', 5, 2, 4, 0,
        '目标星耀王者以上，要求高手段位，意识堪比GPT-5，会报点、不喷人。',
        'RECRUITING', 214, 32, 1, DATE_SUB(NOW(), INTERVAL 20 MINUTE), NOW(), 0);
SET @p3 = LAST_INSERT_ID();

-- -------------------------------------------------------------
-- 4. 剧本杀 4 人本（3/4，再报 1 人即满员）—— 苏姿丰 发起
-- -------------------------------------------------------------
INSERT INTO `post` (`publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`,
                    `location`, `max_people`, `current_people`, `require_level`, `require_gender`,
                    `description`, `status`, `view_count`, `like_count`, `is_audit_pass`,
                    `created_time`, `updated_time`, `deleted`)
VALUES ((SELECT id FROM `sys_user` WHERE `student_no` = '2442170104'),
        '苏妈拼车：剧本杀《雾山》4人硬核推理本（3=1）', '桌游', '8',
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 2 DAY), INTERVAL 14 HOUR),
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 2 DAY), INTERVAL 18 HOUR),
        '大学城「迷雾」剧本杀店', 4, 3, 0, 0,
        'AMD算力全开盘凶手！硬核推理本时长约4小时，还差1人，凑齐立即锁车。',
        'RECRUITING', 167, 19, 1, DATE_SUB(NOW(), INTERVAL 28 MINUTE), NOW(), 0);
SET @p4 = LAST_INSERT_ID();

-- -------------------------------------------------------------
-- 5. 晨跑打卡（门槛最低，任何账号都能报名）—— 艾伦·图灵 发起
-- -------------------------------------------------------------
INSERT INTO `post` (`publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`,
                    `location`, `max_people`, `current_people`, `require_level`, `require_gender`,
                    `description`, `status`, `view_count`, `like_count`, `is_audit_pass`,
                    `created_time`, `updated_time`, `deleted`)
VALUES ((SELECT id FROM `sys_user` WHERE `student_no` = '2442170105'),
        '图灵晨跑：5公里有氧打卡，跑完一起吃早饭', '跑步', '11',
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 2 DAY), INTERVAL 6 HOUR),
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 2 DAY), INTERVAL 7 HOUR),
        '南区田径场入口', 8, 1, 0, 0,
        '配速6分半左右，跟不上也没关系，跑完在南区食堂集合喝豆浆。',
        'RECRUITING', 192, 24, 1, DATE_SUB(NOW(), INTERVAL 24 MINUTE), NOW(), 0);
SET @p5 = LAST_INSERT_ID();

-- -------------------------------------------------------------
-- 6. 图书馆组队自习 —— 阿达·洛夫莱斯 发起
-- -------------------------------------------------------------
INSERT INTO `post` (`publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`,
                    `location`, `max_people`, `current_people`, `require_level`, `require_gender`,
                    `description`, `status`, `view_count`, `like_count`, `is_audit_pass`,
                    `created_time`, `updated_time`, `deleted`)
VALUES ((SELECT id FROM `sys_user` WHERE `student_no` = '2442170106'),
        '阿达领学：图书馆3楼组队自习，互相监督别摸鱼', '学习', '13',
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 9 HOUR),
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 17 HOUR),
        '图书馆 3 楼自习区', 6, 2, 0, 0,
        '刷算法/考研/期末冲刺都可以，手机统一静音，中午一起去吃食堂。',
        'RECRUITING', 143, 21, 1, DATE_SUB(NOW(), INTERVAL 12 MINUTE), NOW(), 0);
SET @p6 = LAST_INSERT_ID();

-- -------------------------------------------------------------
-- 7. 【马上开始】25 分钟后开始的夜跑局 —— 格蕾丝·霍珀 发起
-- -------------------------------------------------------------
INSERT INTO `post` (`publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`,
                    `location`, `max_people`, `current_people`, `require_level`, `require_gender`,
                    `description`, `status`, `view_count`, `like_count`, `is_audit_pass`,
                    `created_time`, `updated_time`, `deleted`)
VALUES ((SELECT id FROM `sys_user` WHERE `student_no` = '2442170108'),
        '霍珀约跑：南区操场夜跑热身局（马上开跑）', '跑步', '12',
        DATE_ADD(NOW(), INTERVAL 25 MINUTE), DATE_ADD(NOW(), INTERVAL 85 MINUTE),
        '南区操场主席台前', 6, 1, 0, 0,
        '写完代码去De-bug大脑！轻松跑4公里，边跑边聊。',
        'RECRUITING', 137, 15, 1, DATE_SUB(NOW(), INTERVAL 4 MINUTE), NOW(), 0);
SET @p7 = LAST_INSERT_ID();

-- -------------------------------------------------------------
-- 8. 足球 7 人制（已 6 人，人数较多）—— 杨立昆 发起
-- -------------------------------------------------------------
INSERT INTO `post` (`publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`,
                    `location`, `max_people`, `current_people`, `require_level`, `require_gender`,
                    `description`, `status`, `view_count`, `like_count`, `is_audit_pass`,
                    `created_time`, `updated_time`, `deleted`)
VALUES ((SELECT id FROM `sys_user` WHERE `student_no` = '2442170109'),
        '杨立昆约球：西区足球7人制友谊赛，缺两个后卫', '球类', '4',
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 3 DAY), INTERVAL 16 HOUR),
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 3 DAY), INTERVAL 18 HOUR),
        '西区足球场', 14, 6, 0, 0,
        'AI顶会阵容每周固定的绿茵友谊赛，缺后卫，穿好球鞋和护腿板。',
        'RECRUITING', 198, 28, 1, DATE_SUB(NOW(), INTERVAL 32 MINUTE), NOW(), 0);
SET @p8 = LAST_INSERT_ID();

-- -------------------------------------------------------------
-- 报名记录：与上面各帖的 current_people 保持一致（发布者自己算 1 人，其余为已通过报名）
-- -------------------------------------------------------------
INSERT INTO `post_apply` (`post_id`, `applicant_id`, `status`, `apply_time`, `handle_time`, `deleted`) VALUES
-- 帖子 1：3 人 = 发布者 + 2 人
(@p1, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170201'), 'APPROVED', NOW(), NOW(), 0),
(@p1, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170202'), 'APPROVED', NOW(), NOW(), 0),
-- 帖子 2：2 人 = 发布者 + 1 人
(@p2, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170203'), 'APPROVED', NOW(), NOW(), 0),
-- 帖子 3：2 人 = 发布者 + 1 人
(@p3, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170204'), 'APPROVED', NOW(), NOW(), 0),
-- 帖子 4：3 人 = 发布者 + 2 人（再通过 1 人即满员，可演示满员→候补）
(@p4, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170205'), 'APPROVED', NOW(), NOW(), 0),
(@p4, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170206'), 'APPROVED', NOW(), NOW(), 0),
-- 帖子 6：2 人 = 发布者 + 1 人
(@p6, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170207'), 'APPROVED', NOW(), NOW(), 0),
-- 帖子 8：6 人 = 发布者 + 5 人
(@p8, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170201'), 'APPROVED', NOW(), NOW(), 0),
(@p8, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170203'), 'APPROVED', NOW(), NOW(), 0),
(@p8, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170205'), 'APPROVED', NOW(), NOW(), 0),
(@p8, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170208'), 'APPROVED', NOW(), NOW(), 0),
(@p8, (SELECT id FROM `sys_user` WHERE `student_no` = '2442170209'), 'APPROVED', NOW(), NOW(), 0);

-- 校验：应看到未来时间的招募中帖子
-- SELECT id, title, category, start_time, current_people, max_people, status
-- FROM `post` WHERE `start_time` > NOW() AND `status` = 'RECRUITING' ORDER BY `start_time`;

-- =============================================================
-- 清理 SQL（需要撤掉这批演示帖时执行；注意会连同其报名/评论一起删除）
-- =============================================================
-- DELETE pa FROM `post_apply` pa JOIN `post` p ON pa.post_id = p.id
--   WHERE p.publisher_id IN (SELECT id FROM `sys_user` WHERE `student_no` LIKE '244217010%');
-- DELETE pc FROM `post_comment` pc JOIN `post` p ON pc.post_id = p.id
--   WHERE p.publisher_id IN (SELECT id FROM `sys_user` WHERE `student_no` LIKE '244217010%');
-- DELETE FROM `post`
--   WHERE `start_time` > NOW()
--     AND `publisher_id` IN (SELECT id FROM `sys_user` WHERE `student_no` LIKE '244217010%');
