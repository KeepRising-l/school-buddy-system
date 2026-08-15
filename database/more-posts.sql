-- ============================================================
-- 校园搭子预约系统 - 更多模拟帖子数据脚本（V1.1）
-- 生成日期: 2026-08-15
--
-- 特点：
-- 1. 使用相对时间（NOW() - INTERVAL），帖子状态与时间真实匹配：
--    - FINISHED（已完成）：开始/结束都在过去
--    - ONGOING（进行中）：已开始、未结束
--    - RECRUITING（招募中）：即将开始
--    - FULL（已满员）：即将开始且人数已满
--    - CANCELLED（已取消）
-- 2. 帖子 ID 从 31 开始，不冲突
-- 3. 用户 ID 范围 4~48（复用已有测试学生）
-- 4. 已审核通过（is_audit_pass=1），广场可见
--
-- 用法：在已导入 init-all.sql 之后执行
--   mysql -u root -p < database/more-posts.sql
--   或在 MySQL 中: source D:/Code/school-buddy-system-gyy/database/more-posts.sql
-- ============================================================
USE `school_buddy`;

-- ------------------------------------------------------------
-- 一、已完成组局（FINISHED）- 8 条（时间在过去，含报名/评价/评论）
-- ------------------------------------------------------------
INSERT INTO `post` (`id`, `publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`, `location`, `max_people`, `current_people`, `require_level`, `require_gender`, `description`, `status`, `view_count`, `like_count`, `is_audit_pass`) VALUES
(31, 4,  '周末羽毛球双打',      '球类', '2',   NOW() - INTERVAL 10 DAY,     NOW() - INTERVAL 10 DAY + INTERVAL 2 HOUR,  '体育馆羽毛球场', 4, 4, 0, 0, '上周的羽毛球局，大家打得都很尽兴', 'FINISHED', 120, 8, 1),
(32, 5,  '王者荣耀五排上分',    '电竞', '5',   NOW() - INTERVAL 8 DAY,      NOW() - INTERVAL 8 DAY + INTERVAL 3 HOUR,   '线上-王者荣耀', 5, 5, 4, 0, '上周五排冲分，队友配合很默契', 'FINISHED', 210, 15, 1),
(33, 6,  '图书馆自习搭子',      '学习', '13',  NOW() - INTERVAL 7 DAY,      NOW() - INTERVAL 7 DAY + INTERVAL 4 HOUR,   '图书馆三楼自习区', 3, 3, 0, 0, '期末冲刺自习局', 'FINISHED', 45, 2, 1),
(34, 7,  '夜跑五公里',          '跑步', '12',  NOW() - INTERVAL 6 DAY,      NOW() - INTERVAL 6 DAY + INTERVAL 1 HOUR,   '操场', 8, 6, 0, 0, '夜跑打卡，配速6分半', 'FINISHED', 88, 6, 1),
(35, 8,  '剧本杀-窗边的女人',   '桌游', '8',   NOW() - INTERVAL 5 DAY,      NOW() - INTERVAL 5 DAY + INTERVAL 4 HOUR,   '南门剧本杀店', 6, 6, 0, 0, '经典推理本，全员到齐', 'FINISHED', 156, 12, 1),
(36, 10, '乒乓球友谊赛',        '球类', '3',   NOW() - INTERVAL 4 DAY,      NOW() - INTERVAL 4 DAY + INTERVAL 2 HOUR,   '体育馆乒乓球室', 4, 4, 0, 0, '周末乒乓球交流', 'FINISHED', 67, 5, 1),
(37, 12, '三国杀娱乐局',        '桌游', '10',  NOW() - INTERVAL 3 DAY,      NOW() - INTERVAL 3 DAY + INTERVAL 3 HOUR,   '宿舍活动室', 5, 5, 0, 0, '休闲三国杀，不争输赢', 'FINISHED', 92, 7, 1),
(38, 14, '羽毛球新手教学',      '球类', '2',   NOW() - INTERVAL 2 DAY,      NOW() - INTERVAL 2 DAY + INTERVAL 2 HOUR,   '体育馆羽毛球场', 6, 6, 0, 0, '新手友好，教你入门', 'FINISHED', 78, 9, 1);

-- ------------------------------------------------------------
-- 二、进行中组局（ONGOING）- 4 条（已开始未结束）
-- ------------------------------------------------------------
INSERT INTO `post` (`id`, `publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`, `location`, `max_people`, `current_people`, `require_level`, `require_gender`, `description`, `status`, `view_count`, `like_count`, `is_audit_pass`) VALUES
(39, 15, '篮球半场3v3',         '球类', '1',   NOW() - INTERVAL 1 HOUR,     NOW() + INTERVAL 2 HOUR,                   '东区篮球场', 6, 4, 2, 0, '现在正在打，缺2人随时加入', 'ONGOING', 56, 4, 1),
(40, 16, '和平精英四排',        '电竞', '7',   NOW() - INTERVAL 30 MINUTE,  NOW() + INTERVAL 2 HOUR,                   '线上-和平精英', 4, 3, 0, 0, '正在四排，差一个人', 'ONGOING', 34, 3, 1),
(41, 18, '操场夜跑',            '跑步', '12',  NOW() - INTERVAL 20 MINUTE,  NOW() + INTERVAL 40 MINUTE,                '操场', 10, 5, 0, 0, '今晚夜跑，随时加入', 'ONGOING', 45, 4, 1),
(42, 20, '摄影扫街-老城区',     '摄影', '18',  NOW() - INTERVAL 1 HOUR,     NOW() + INTERVAL 3 HOUR,                   '老城区步行街', 5, 3, 0, 0, '带着相机扫街，交流摄影', 'ONGOING', 67, 5, 1);

-- ------------------------------------------------------------
-- 三、招募中（RECRUITING）- 10 条（即将开始）
-- ------------------------------------------------------------
INSERT INTO `post` (`id`, `publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`, `location`, `max_people`, `current_people`, `require_level`, `require_gender`, `description`, `status`, `view_count`, `like_count`, `is_audit_pass`) VALUES
(43, 4,  '周末篮球3v3局',       '球类', '1',   NOW() + INTERVAL 1 DAY,      NOW() + INTERVAL 1 DAY + INTERVAL 2 HOUR,   '东区篮球场', 6, 2, 3, 0, '周六下午篮球局，黄金以上优先', 'RECRUITING', 34, 2, 1),
(44, 5,  '英雄联盟开黑',        '电竞', '6',   NOW() + INTERVAL 1 DAY,      NOW() + INTERVAL 1 DAY + INTERVAL 3 HOUR,   '线上-英雄联盟', 5, 2, 4, 0, '晚上开黑上分，钻石段位来', 'RECRUITING', 45, 3, 1),
(45, 6,  '晨跑团',              '跑步', '11',  NOW() + INTERVAL 2 DAY,      NOW() + INTERVAL 2 DAY + INTERVAL 1 HOUR,   '操场', 10, 3, 0, 0, '周二早上晨跑，欢迎新手', 'RECRUITING', 23, 1, 1),
(46, 7,  '周末剧本杀-年轮',     '桌游', '8',   NOW() + INTERVAL 2 DAY,      NOW() + INTERVAL 2 DAY + INTERVAL 4 HOUR,   '南门剧本杀店', 6, 2, 0, 0, '口碑本，缺4人，费用AA', 'RECRUITING', 89, 6, 1),
(47, 8,  '羽毛球混双',          '球类', '2',   NOW() + INTERVAL 3 DAY,      NOW() + INTERVAL 3 DAY + INTERVAL 2 HOUR,   '体育馆羽毛球场', 4, 1, 0, 1, '混双局，欢迎女生报名', 'RECRUITING', 28, 2, 1),
(48, 10, '狼人杀12人局',        '桌游', '9',   NOW() + INTERVAL 3 DAY,      NOW() + INTERVAL 3 DAY + INTERVAL 4 HOUR,   '咖啡厅包间', 12, 4, 0, 0, '标准12人局，缺8人', 'RECRUITING', 156, 10, 1),
(49, 12, '健身撸铁',            '健身', '19',  NOW() + INTERVAL 4 DAY,      NOW() + INTERVAL 4 DAY + INTERVAL 2 HOUR,   '健身房', 4, 1, 0, 0, '一起去撸铁，互相保护', 'RECRUITING', 18, 1, 1),
(50, 14, '图书馆学习搭子',      '学习', '13',  NOW() + INTERVAL 4 DAY,      NOW() + INTERVAL 4 DAY + INTERVAL 4 HOUR,   '图书馆二楼', 4, 2, 0, 0, '找个学习搭子互相监督', 'RECRUITING', 30, 2, 1),
(51, 16, '美食探店-川菜',       '美食', '15',  NOW() + INTERVAL 5 DAY,      NOW() + INTERVAL 5 DAY + INTERVAL 2 HOUR,   '南门川菜馆', 6, 3, 0, 0, '周末探店，爱吃辣的一起来', 'RECRUITING', 42, 3, 1),
(52, 18, '音乐交流',            '音乐', '17',  NOW() + INTERVAL 5 DAY,      NOW() + INTERVAL 5 DAY + INTERVAL 3 HOUR,   '艺术楼琴房', 6, 2, 0, 0, '带乐器来一起玩', 'RECRUITING', 25, 2, 1);

-- ------------------------------------------------------------
-- 四、已满员（FULL）- 3 条（即将开始，人数已满）
-- ------------------------------------------------------------
INSERT INTO `post` (`id`, `publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`, `location`, `max_people`, `current_people`, `require_level`, `require_gender`, `description`, `status`, `view_count`, `like_count`, `is_audit_pass`) VALUES
(53, 20, '王者荣耀内战5v5',     '电竞', '5',   NOW() + INTERVAL 1 DAY,      NOW() + INTERVAL 1 DAY + INTERVAL 3 HOUR,   '线上-王者荣耀', 10, 10, 3, 0, '5V5内战已满员，欢迎观战', 'FULL', 78, 6, 1),
(54, 22, '剧本杀-持斧奥夫',     '桌游', '8',   NOW() + INTERVAL 2 DAY,      NOW() + INTERVAL 2 DAY + INTERVAL 4 HOUR,   '北门剧本杀店', 6, 6, 0, 0, '恐怖本已满员', 'FULL', 67, 5, 1),
(55, 24, '篮球全场5v5',         '球类', '1',   NOW() + INTERVAL 3 DAY,      NOW() + INTERVAL 3 DAY + INTERVAL 2 HOUR,   '西区篮球场', 10, 10, 2, 0, '全场已满员', 'FULL', 56, 4, 1);

-- ------------------------------------------------------------
-- 五、已取消（CANCELLED）- 2 条
-- ------------------------------------------------------------
INSERT INTO `post` (`id`, `publisher_id`, `title`, `category`, `tag_ids`, `start_time`, `end_time`, `location`, `max_people`, `current_people`, `require_level`, `require_gender`, `description`, `status`, `view_count`, `like_count`, `is_audit_pass`) VALUES
(56, 26, '雨天户外跑步',        '跑步', '12',  NOW() + INTERVAL 1 DAY,      NOW() + INTERVAL 1 DAY + INTERVAL 1 HOUR,   '操场', 8, 1, 0, 0, '天气预报有雨，取消活动', 'CANCELLED', 12, 0, 1),
(57, 28, '室外篮球局',          '球类', '1',   NOW() + INTERVAL 2 DAY,      NOW() + INTERVAL 2 DAY + INTERVAL 2 HOUR,   '东区篮球场', 6, 2, 0, 0, '场地维修，活动取消', 'CANCELLED', 20, 1, 1);

-- ------------------------------------------------------------
-- 六、报名数据（为进行中/已完成/满员的帖子补全报名记录）
-- ------------------------------------------------------------
-- 已完成的帖子（31-38）：全部 APPROVED → ATTENDED（已出勤）
INSERT INTO `post_apply` (`post_id`, `applicant_id`, `status`, `apply_time`, `handle_time`) VALUES
(31, 5,  'ATTENDED', NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 10 DAY),
(31, 6,  'ATTENDED', NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 10 DAY),
(31, 7,  'ATTENDED', NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY),
(32, 6,  'ATTENDED', NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 8 DAY),
(32, 7,  'ATTENDED', NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 8 DAY),
(32, 8,  'ATTENDED', NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 8 DAY),
(32, 9,  'ATTENDED', NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 8 DAY),
(33, 7,  'ATTENDED', NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 7 DAY),
(33, 8,  'ATTENDED', NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 7 DAY),
(34, 8,  'ATTENDED', NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 6 DAY),
(34, 9,  'ATTENDED', NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 6 DAY),
(34, 10, 'ATTENDED', NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 6 DAY),
(34, 11, 'ATTENDED', NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 6 DAY),
(34, 12, 'ATTENDED', NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 6 DAY),
(35, 9,  'ATTENDED', NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 5 DAY),
(35, 10, 'ATTENDED', NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 5 DAY),
(35, 11, 'ATTENDED', NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 5 DAY),
(35, 12, 'ATTENDED', NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 5 DAY),
(35, 13, 'ATTENDED', NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 5 DAY),
(36, 11, 'ATTENDED', NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 4 DAY),
(36, 12, 'ATTENDED', NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 4 DAY),
(36, 13, 'ATTENDED', NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 4 DAY),
(37, 13, 'ATTENDED', NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 3 DAY),
(37, 14, 'ATTENDED', NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 3 DAY),
(37, 15, 'ATTENDED', NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 3 DAY),
(37, 16, 'ATTENDED', NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 3 DAY),
(38, 15, 'ATTENDED', NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 2 DAY),
(38, 16, 'ATTENDED', NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 2 DAY),
(38, 17, 'ATTENDED', NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 2 DAY),
(38, 18, 'ATTENDED', NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 2 DAY),
(38, 19, 'ATTENDED', NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 2 DAY);

-- 进行中的帖子（39-42）：已通过 APPROVED
INSERT INTO `post_apply` (`post_id`, `applicant_id`, `status`, `apply_time`, `handle_time`) VALUES
(39, 16, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(39, 17, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(39, 18, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(40, 17, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
(40, 18, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
(41, 19, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR),
(41, 20, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR),
(41, 21, 'APPROVED', NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 6 HOUR),
(41, 22, 'APPROVED', NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 6 HOUR),
(42, 21, 'APPROVED', NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 6 HOUR),
(42, 22, 'APPROVED', NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 6 HOUR);

-- 满员的帖子（53-55）：已通过满员
INSERT INTO `post_apply` (`post_id`, `applicant_id`, `status`, `apply_time`, `handle_time`) VALUES
(53, 21, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(53, 22, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(53, 23, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(53, 24, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(53, 25, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(53, 26, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(53, 27, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(53, 28, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(53, 29, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(54, 23, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(54, 24, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(54, 25, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(54, 26, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(54, 27, 'APPROVED', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(55, 25, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR),
(55, 26, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR),
(55, 27, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR),
(55, 28, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR),
(55, 29, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR),
(55, 30, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR),
(55, 31, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR),
(55, 32, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR),
(55, 33, 'APPROVED', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR);

-- ------------------------------------------------------------
-- 七、评论数据（热门帖加评论）
-- ------------------------------------------------------------
INSERT INTO `post_comment` (`post_id`, `user_id`, `content`, `created_time`) VALUES
(31, 5,  '上周打得真爽，下次继续！',          NOW() - INTERVAL 9 DAY),
(31, 6,  '求固定球友',                        NOW() - INTERVAL 9 DAY),
(32, 7,  '五排赢了3把，开心',                 NOW() - INTERVAL 7 DAY),
(32, 8,  '下次还来',                          NOW() - INTERVAL 7 DAY),
(35, 10, '这个本真的推荐！',                  NOW() - INTERVAL 4 DAY),
(35, 11, '剧情反转绝了',                      NOW() - INTERVAL 4 DAY),
(39, 17, '马上到，等我！',                    NOW() - INTERVAL 30 MINUTE),
(43, 5,  '周六有空，报名了',                  NOW() - INTERVAL 6 HOUR),
(48, 11, '狼人杀专业户，求带',                NOW() - INTERVAL 3 HOUR),
(53, 22, '内战快点开始吧！',                  NOW() - INTERVAL 2 HOUR);

-- ------------------------------------------------------------
-- 八、评价数据（已完成的帖子产生互评，驱动信誉分展示）
-- ------------------------------------------------------------
INSERT INTO `review` (`post_id`, `reviewer_id`, `target_user_id`, `rating`, `tag`, `content`, `created_time`) VALUES
(31, 4, 5,  5, '靠谱', '技术好，配合默契',     NOW() - INTERVAL 9 DAY),
(31, 4, 6,  4, '靠谱', '打得不错',             NOW() - INTERVAL 9 DAY),
(31, 4, 7,  5, '友好', '人很好',               NOW() - INTERVAL 9 DAY),
(31, 5, 4,  5, '靠谱', '组织得很好',           NOW() - INTERVAL 9 DAY),
(31, 6, 4,  4, '准时', '很准时',               NOW() - INTERVAL 9 DAY),
(32, 5, 6,  5, '靠谱', '上分大腿',             NOW() - INTERVAL 7 DAY),
(32, 5, 7,  4, '友好', '配合不错',             NOW() - INTERVAL 7 DAY),
(32, 6, 5,  5, '靠谱', '带飞',                 NOW() - INTERVAL 7 DAY),
(35, 8, 9,  5, '靠谱', '推理很强',             NOW() - INTERVAL 4 DAY),
(35, 8, 10, 4, '友好', '体验很好',             NOW() - INTERVAL 4 DAY),
(35, 9, 8,  5, '靠谱', '组织完美',             NOW() - INTERVAL 4 DAY),
(37, 12, 13, 5, '友好', '很欢乐',              NOW() - INTERVAL 2 DAY),
(37, 13, 12, 4, '准时', '守时',                NOW() - INTERVAL 2 DAY),
(38, 14, 15, 5, '靠谱', '教得很好',            NOW() - INTERVAL 1 DAY),
(38, 15, 14, 5, '友好', '耐心',                NOW() - INTERVAL 1 DAY);

-- ============================================================
-- 完成！执行后你可以：
-- 1. 广场看到：8个已完成 + 4个进行中 + 10个招募中 + 3个已满员 + 2个已取消
-- 2. 用户 4（小明）信誉分有评价加成
-- 3. 管理端统计有真实数据可看
-- ============================================================
