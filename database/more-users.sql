-- ============================================================
-- 校园搭子预约系统 - 追加 50 个测试用户脚本
-- 生成日期: 2026-10-13
-- 用途: 在已导入 database/init-all.sql 的测试库中，补充 50 个学生账号（含兴趣标签+段位），
--       用于演示多人报名、候补递推、推荐匹配与信誉分层等场景。
-- 用法:  mysql -u root -p school_buddy < database/more-users.sql
--        （Navicat：先选中 school_buddy 库，再「运行 SQL 文件」选择本文件）
-- 说明:
--   1. 仅限本地/测试库使用，严禁对生产/真实数据库执行；
--   2. 脚本可重复执行（INSERT IGNORE）：已存在的用户/标签关系会自动跳过，不报错；
--   3. 密码规则与既有测试学生一致：密码 = 学号 + "123"；
--   4. 全部 50 个账号的账号/密码/兴趣清单见 docs/测试用户清单.md。
-- ============================================================
USE `school_buddy`;

-- ------------------------------------------------------------
-- 1. 各行业名人学生账号（密码 = BCrypt(学号+"123")）
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_user`
(`student_no`, `phone`, `password`, `nickname`, `username`, `college`, `gender`, `credit_score`, `signature`, `status`, `is_admin`)
VALUES
('2442170101', '1392170101', '$2a$10$8t02hKOd/1THGohqsDxJX.hTEM5BHWTbQR4gpDd1dG1RbcfK.doOu', '埃隆·马斯克',   '2442170101', '大数据人工智能学院', 1, 95, '第一性原理打篮球！今晚东区球场3v3，打完研究火箭回收', 1, 0),
('2442170102', '1392170102', '$2a$10$/D.gmFgmLU0rG7upF5oHXeQmhB6e6QTdhPdSJaBi3EmBqcsebI0Km', '李飞飞',        '2442170102', '大数据人工智能学院', 2, 90, '机器视觉之外最爱羽毛球双打与硬核剧本杀！', 1, 0),
('2442170103', '1392170103', '$2a$10$7bTTELjZjrz7fh6ILaP4t.9BNw/b/ERNSkzC4jdTXOAC11ucfcwzu', '山姆·奥特曼',   '2442170103', '大数据人工智能学院', 1, 92, 'AGI到来之前先上荣耀王者，五排缺个野王！', 1, 0),
('2442170104', '1392170104', '$2a$10$fsljiUG3HqGhpoS2WMJeF.hU1PvhfiOqIBy6sKFYT0VAkHkH1PjBS', '苏姿丰',        '2442170104', '大数据人工智能学院', 2, 98, 'AMD Yes！乒乓球旋转扣杀，周末硬核剧本杀《雾山》车头', 1, 0),
('2442170105', '1392170105', '$2a$10$GemVzOFaD6KUpNWcLOOn/O1Ztbno1F6IzlD6ac/G3QU4fnpUizyUe', '艾伦·图灵',     '2442170105', '大数据人工智能学院', 1, 88, '图灵测试通过！早起南区田径场5公里晨跑打卡，跑完吃早饭', 1, 0),
('2442170106', '1392170106', '$2a$10$xY9iZZwIqbBfW3diZAfSVOR2wGW5BI2wyQsCJh60oFvO0jJPBsqTa', '阿达·洛夫莱斯', '2442170106', '大数据人工智能学院', 2, 91, '世界第一位程序员，图书馆3楼组队自习互相监督不摸鱼', 1, 0),
('2442170107', '1392170107', '$2a$10$78TBGt5AK1yCsDFMIXlQ.OJjhAUWbuYoiIOrZXGlhc/OOv25bFbYi', '梁文锋',        '2442170107', '大数据人工智能学院', 1, 96, '深度求索DeepSeek！王者荣耀宗师段位，三国杀运筹帷幄', 1, 0),
('2442170108', '1392170108', '$2a$10$Sk74Ivai5f/vgaH3sn0/dOsfA3TJJ3GqbvtCIA5O6GIefudA48f42', '格蕾丝·霍珀',   '2442170108', '大数据人工智能学院', 2, 89, '抓出第一只Bug的人，今晚南区操场夜跑热身局马上开跑！', 1, 0),
('2442170109', '1392170109', '$2a$10$h4EK4sbBnfnVc1AgAQOiCeprF5ryrOfStU1bzfDOK0wtNu1ZGWxuO', '杨立昆',        '2442170109', '大数据人工智能学院', 1, 94, '卷积神经网络之父，西区足球场7人制友谊赛缺两个后卫！', 1, 0),
('2442170110', '1392170110', '$2a$10$ThF04wcQDl2StUdc9K2RBelpQDxFDaWrMYyQ7SVypWUn08DcFRS3W', '庄晓莹',        '2442170110', '大数据人工智能学院', 2, 97, '大模型架构师，狼人杀宗师段位，听一轮发言就能抿出全狼', 1, 0),
('2442170201', '1392170201', '$2a$10$WyIVo/D/OP9NuY5L1LRtAuMvGXIJfDC.r38YFyolCLV.2TxhrmKDO', '任正非',        '2442170201', '大数据人工智能学院', 1, 95, '把寒气传递给对手！篮球场强硬防守，晨跑雷打不动', 1, 0),
('2442170202', '1392170202', '$2a$10$cpZYOs9/9JsP5SezArA8Z.DquxxgrK3jC9OLk5sEAvP962ibuPNkW', '董明珠',        '2442170202', '大数据人工智能学院', 2, 90, '好球拍格力造！羽毛球宗师级扣杀，夜跑从不掉队', 1, 0),
('2442170203', '1392170203', '$2a$10$v9qT26Esdqq3snGz1EmV3Oy1CNHdlanYSPSwyZvTj/645CSEjNaWG', '王兴兴',        '2442170203', '大数据人工智能学院', 1, 92, '宇树机器人陪练！和平精英宗师枪法，王者荣耀全能补位', 1, 0),
('2442170204', '1392170204', '$2a$10$YzsfYgpbhvNIlm4Fnses6uTbtvF49FPUNTufNvVzNNP3xhxN2/QQ6', '何超琼',        '2442170204', '大数据人工智能学院', 2, 98, '守时守信98分！乒乓球高手切磋，桌游推理局常客', 1, 0),
('2442170205', '1392170205', '$2a$10$5CLz5vNKWawcXkw1pNt09el34NXFmnwoxltVkk4gtVICILvIt66R6', '张一鸣',        '2442170205', '大数据人工智能学院', 1, 88, '延迟满足感！绿茵场足球宗师，篮球晨跑长期搭子', 1, 0),
('2442170206', '1392170206', '$2a$10$6rQNr20ED71/aNOo89/8SuLm1U./mwCUB/tAlNIgY9EU776QNJpoW', '彭蕾',          '2442170206', '大数据人工智能学院', 2, 91, '剧本杀宗师级逻辑推土机，羽毛球双打默契搭档', 1, 0),
('2442170207', '1392170207', '$2a$10$faBkTQkEe7P4vmalRqq1GO9zIrn2ZHD1EzY87uWokTYcZpZYELezG', '马化腾',        '2442170207', '大数据人工智能学院', 1, 96, '峡谷与王者双区贵族！英雄联盟宗师局随时摇人', 1, 0),
('2442170208', '1392170208', '$2a$10$lm7pT7p4FxHYZpCbf131k.g3j5sdvdl13eYXzrJiSfH1IjvhNC4F6', '陶华碧',        '2442170208', '大数据人工智能学院', 2, 89, '干饭要配老干妈！狼人杀火辣发言，夜跑羽毛球随时奉陪', 1, 0),
('2442170209', '1392170209', '$2a$10$.r7kocrC9TrQyf7uHIJQUOCnUZmhZpDFPUS.lIiuEEH7VN27wLqNq', '王传福',        '2442170209', '大数据人工智能学院', 1, 94, '技术为王！篮球场发动机，足球乒乓球全能混搭', 1, 0),
('2442170210', '1392170210', '$2a$10$rUNIu/X4KTL5TNEnDTaoI.WjTyBrF9awDxRKdc6nOrITb7HdCDszy', 'J.K.罗琳',      '2442170210', '大数据人工智能学院', 2, 97, '霍格沃茨驻校女巫，狼人杀盲毒必中，奇幻剧本杀狂热粉', 1, 0),
('2442170301', '1392170301', '$2a$10$kNRl/Y8lHnNuArpoDvEzU.m0jKnoWOdOxaX7UzMZE8gpkVImgpCra', '刘慈欣',        '2442170301', '大数据人工智能学院', 1, 95, '给岁月以文明，给球场以降维打击！篮球宗师+晨跑高玩', 1, 0),
('2442170302', '1392170302', '$2a$10$F9RqybgNpfw3p/qkmF57VeinPTpD/EcKiI3xRtCmcwNZMoQcwE5py', '张爱玲',        '2442170302', '大数据人工智能学院', 2, 90, '出名要趁早，夜跑也要趁晚风正好！羽毛球高手约球', 1, 0),
('2442170303', '1392170303', '$2a$10$RNMMHg2WCY/93k9S5PT4su8PEQeiOOMv4J/nWgak0zjyCvpuLDEW.', '罗翔',          '2442170303', '大数据人工智能学院', 1, 92, '如果组局放鸽子，在道德和信誉分上是要受谴责的！足球LOL搭子', 1, 0),
('2442170304', '1392170304', '$2a$10$lO1zuxsfGBCtPajAoPqRAuPk6YNlGUeL18Wspu2.MPlClD9bl/Xem', '林徽因',        '2442170304', '大数据人工智能学院', 2, 98, '你是人间的四月天！古风情感剧本杀宗师，乒乓球夜跑搭子', 1, 0),
('2442170305', '1392170305', '$2a$10$1lii9Y1p53tOGbBhRuT1TeYB5dQnpbgozBTkJTQkVKcF2NG3op5Qy', '余华',          '2442170305', '大数据人工智能学院', 1, 88, '把悲伤留给读者，把快乐留给吃鸡和篮球局哈哈哈', 1, 0),
('2442170306', '1392170306', '$2a$10$BwK0c7xDAt.yNrig3NaViuxKUXaSaxVxe3cmVitNZF6vTsOioL92i', '三毛',          '2442170306', '大数据人工智能学院', 2, 91, '每想你一次天上飘落一粒沙，剧本杀羽毛球浪漫组局', 1, 0),
('2442170307', '1392170307', '$2a$10$rZRdYdwSykEa7qnGJvKlgeszlGx/pXoVvo8X6G2B62l6yyqEYM.6G', '莫言',          '2442170307', '大数据人工智能学院', 1, 96, '诺奖得主在峡谷也是王者宗师！打游戏从不多话只靠操作', 1, 0),
('2442170308', '1392170308', '$2a$10$XasJTp5JQ5zeHYdFptlYfe/xZthlW6iJ.s98KuuyKaP8fZUSlSjuK', '冰心',          '2442170308', '大数据人工智能学院', 2, 89, '成功的花需要汗水浇灌，每天清晨操场5公里晨跑宗师', 1, 0),
('2442170309', '1392170309', '$2a$10$aOJfpCqPZ2Q47A8BAYRdpe1LwsS.qz45BgLAhUXmbrSdqWVMWF8l.', '法外狂徒张三',  '2442170309', '大数据人工智能学院', 1, 58, '【低信誉58分演示】罗翔老师钦点案例男主，因多次爽约被限制组局', 1, 0),
('2442170310', '1392170310', '$2a$10$2nokEIPfhruQITto1qpWXuoGL7L4scZ9Vh5aUiU2UkGPN12MEmDeG', '李清照',        '2442170310', '大数据人工智能学院', 2, 97, '争渡争渡，惊起一滩鸥鹭！桌游博弈高手，羽毛球熟手', 1, 0),
('2442170401', '1392170401', '$2a$10$zPyOZoEojvyDjnUeXKhDsu6zA5B2eCe0X9T69AURdNclO0VEVZ8ni', '林纳斯(Linus)', '2442170401', '计算机学院',         1, 95, 'Talk is cheap, show me the basketball! 东区球场见', 1, 0),
('2442170402', '1392170402', '$2a$10$gZaawW2sFZ7kduKJsNpqXejmgpJgH8zHqhWahQ6hxN7Q4MM5w07Pq', '王菲',          '2442170402', '计算机学院',         2, 90, '相约九八不如相约羽毛球馆，羽球宗师高冷切磋', 1, 0),
('2442170403', '1392170403', '$2a$10$jjreNdllXlrzHUO0pmyU2eiVKoVx5.CxymTnm/qKApCCsqKhLotzO', 'TheShy姜承録',  '2442170403', '计算机学院',         1, 92, '我喜欢发育！英雄联盟上单剑魔天神下凡，吃鸡也爱刚枪', 1, 0),
('2442170404', '1392170404', '$2a$10$F3Tr/zeX2ClPEdbpPJoOoeStI6r4mp5N33U3W/7LrotF9eP7iZxf6', '孙燕姿',        '2442170404', '计算机学院',         2, 98, '遇见你是最美丽的意外！乒乓球高手，治愈系剧本杀搭子', 1, 0),
('2442170405', '1392170405', '$2a$10$NwGlCuTlDHn0Hq2ML1xYOOoODN2E.glmtpothi5w36IgBGTsZ2/Ai', '周星驰',        '2442170405', '计算机学院',         1, 88, '做人如果没有梦想跟咸鱼有什么分别？少林篮球宗师参上！', 1, 0),
('2442170406', '1392170406', '$2a$10$CUNOI/OuWsbqMrpXn2m1iO01qkZfrPnhY/OOImLiaMPGvRiNyKE3a', '蔡依林',        '2442170406', '计算机学院',         2, 91, '旋转跳跃我不停歇！环校路夜跑宗师，体能拉满', 1, 0),
('2442170407', '1392170407', '$2a$10$IF2LQV8MpR9VrR0iMEMB6.2kxP4rqDKo9bB0sHgTXjkdSbZBQao6q', '沈腾',          '2442170407', '计算机学院',         1, 96, '你过来呀！西区足球场守门员兼三国杀搞笑内奸', 1, 0),
('2442170408', '1392170408', '$2a$10$DxGsK0GOVH847Ad8WgXNVOlFfWqiIachil1wdq67MM.3RpR8cTssu', '马丽',          '2442170408', '计算机学院',         2, 89, '剧本杀欢乐本宗师车头，有我在的局绝对不冷场！', 1, 0),
('2442170409', '1392170409', '$2a$10$/.ntJJRHgdzAa0JvEF.Gw./DN5NA8Lx68Lmndvs/itzZPssTTG4YG', '鸽王周幽王',    '2442170409', '计算机学院',         1, 45, '【低信誉45分演示】烽火戏诸侯放鸽子鼻祖，登录后发帖报名将被拦截', 1, 0),
('2442170410', '1392170410', '$2a$10$SkO/p5JOr8BkhwmoJ3ytreugxLuUSVPOnGGn5rlSP1oerGodxhngW', '高冷褒姒',      '2442170410', '计算机学院',         2, 35, '【低信誉35分演示】千金难买一笑，因频繁爽约处于信誉冻结区', 1, 0),
('2442170501', '1392170501', '$2a$10$i7E7P9z8lM/IWdFoFiLHnOG5wsq/6pewVs0H7n26oBj4AilJx6Y4G', '樊振东',        '2442170501', '体育学院',           1, 95, '小胖大满贯！除了乒乓球，晨跑和皇马足球局也是最爱', 1, 0),
('2442170502', '1392170502', '$2a$10$p7wi0dUC0o6LjWpa3KiYkOM.0rydQ.EDKcbkFEVpRrQkW/kyug1YO', '郑钦文',        '2442170502', '体育学院',           2, 90, 'Queen Wen！挥拍永不言弃，羽毛球、夜跑、剧本杀随时约', 1, 0),
('2442170503', '1392170503', '$2a$10$YoD2DoHilmMCv.hqn5Z92eNwSLWdsHl7fuCMEsprEorx6qw457.uK', '潘展乐',        '2442170503', '体育学院',           1, 92, '百米自由泳破纪录只是日常，王者荣耀我也是宗师段位！', 1, 0),
('2442170504', '1392170504', '$2a$10$k0w.ehIKZ7gTueOX/A6jkeiNrA8n19zmw5T0p2SmUGFVtJLh/e48e', '全红婵',        '2442170504', '体育学院',           2, 98, '水花消失术传人！喜欢乌龟玩偶，乒乓球和狼人杀都很拿手', 1, 0),
('2442170505', '1392170505', '$2a$10$mm5Ig5JomI49BWlgB//ioeTodYzm8Fegmm7UvGay5Q.FRxHGkk3kq', '汪顺',          '2442170505', '体育学院',           1, 88, '泳池混合泳冠军，上岸就是LOL高手和绿茵场主力', 1, 0),
('2442170506', '1392170506', '$2a$10$ZHwcp36Xzt.dHp6NOA4pXue4jD0o9J3UTB19awW1OkqUrmOZPSVa2', '陈芋汐',        '2442170506', '体育学院',           2, 91, '跳水双子星，晨跑自律达人，周末最爱和朋友玩剧本杀', 1, 0),
('2442170507', '1392170507', '$2a$10$vDHVHTM/yBFZQIIFAHT/7.X5XQSEHrNNXsAPKe6N/3vVpXBwZO6Sm', '奥尼尔',        '2442170507', '体育学院',           1, 96, '大鲨鱼统治东区禁区！扣碎篮板不赔，打完球玩两把三国杀', 1, 0),
('2442170508', '1392170508', '$2a$10$tfm5mS4b0UB6VBSYP5zBwOvJBFpQN1lGOzbBeJI.fTOQF8oa.dNIu', '王曼昱',        '2442170508', '体育学院',           2, 89, '反手拧拉天花板，狼人杀宗师逻辑流，夜跑配速稳健', 1, 0),
('2442170509', '1392170509', '$2a$10$dsMVggPj3guYnIy31TPk9.vBefl4KhKXUXB1cLtAmyxPU6lYTprEi', '贝克汉姆',      '2442170509', '体育学院',           1, 94, '黄金右脚圆月弯刀，西区足球场任意球教学局约起！', 1, 0),
('2442170510', '1392170510', '$2a$10$dux9Bqd1wFnWVIWKWGhQD.zNMKTlrTyUr3O2lgiLWpn6bZ2hoVKUS', '李娜',          '2442170510', '体育学院',           2, 97, '大满贯传奇！羽毛球高手、夜跑宗师，运动场不见不散', 1, 0);

-- ------------------------------------------------------------
-- 2. 兴趣标签与自评段位
--    标签ID对照：1篮球 2羽毛球 3乒乓球 4足球 5王者荣耀 6英雄联盟
--               7和平精英 8剧本杀 9狼人杀 10三国杀 11晨跑 12夜跑
-- ------------------------------------------------------------
INSERT IGNORE INTO `user_tag` (`user_id`, `tag_id`, `self_level`)
SELECT u.id, m.tag_id, m.self_level
FROM `sys_user` u
JOIN (
    SELECT '2442170101' AS student_no, 1 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170101' AS student_no, 11 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170101' AS student_no, 5 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170102' AS student_no, 2 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170102' AS student_no, 8 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170102' AS student_no, 12 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170103' AS student_no, 4 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170103' AS student_no, 5 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170103' AS student_no, 7 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170104' AS student_no, 3 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170104' AS student_no, 9 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170104' AS student_no, 12 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170105' AS student_no, 1 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170105' AS student_no, 6 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170105' AS student_no, 11 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170106' AS student_no, 2 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170106' AS student_no, 8 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170106' AS student_no, 3 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170107' AS student_no, 5 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170107' AS student_no, 6 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170107' AS student_no, 10 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170108' AS student_no, 12 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170108' AS student_no, 11 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170108' AS student_no, 2 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170109' AS student_no, 4 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170109' AS student_no, 1 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170109' AS student_no, 7 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170110' AS student_no, 8 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170110' AS student_no, 9 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170110' AS student_no, 2 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170201' AS student_no, 1 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170201' AS student_no, 11 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170201' AS student_no, 6 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170202' AS student_no, 2 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170202' AS student_no, 12 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170202' AS student_no, 8 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170203' AS student_no, 5 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170203' AS student_no, 7 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170203' AS student_no, 10 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170204' AS student_no, 3 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170204' AS student_no, 9 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170204' AS student_no, 11 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170205' AS student_no, 1 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170205' AS student_no, 4 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170205' AS student_no, 11 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170206' AS student_no, 8 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170206' AS student_no, 2 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170206' AS student_no, 12 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170207' AS student_no, 6 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170207' AS student_no, 5 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170207' AS student_no, 1 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170208' AS student_no, 12 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170208' AS student_no, 2 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170208' AS student_no, 9 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170209' AS student_no, 4 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170209' AS student_no, 1 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170209' AS student_no, 3 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170210' AS student_no, 9 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170210' AS student_no, 8 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170210' AS student_no, 11 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170301' AS student_no, 1 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170301' AS student_no, 5 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170301' AS student_no, 11 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170302' AS student_no, 2 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170302' AS student_no, 12 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170302' AS student_no, 8 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170303' AS student_no, 4 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170303' AS student_no, 6 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170303' AS student_no, 10 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170304' AS student_no, 3 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170304' AS student_no, 8 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170304' AS student_no, 12 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170305' AS student_no, 1 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170305' AS student_no, 7 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170305' AS student_no, 11 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170306' AS student_no, 8 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170306' AS student_no, 9 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170306' AS student_no, 2 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170307' AS student_no, 5 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170307' AS student_no, 6 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170307' AS student_no, 1 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170308' AS student_no, 12 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170308' AS student_no, 11 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170308' AS student_no, 3 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170309' AS student_no, 4 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170309' AS student_no, 1 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170309' AS student_no, 7 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170310' AS student_no, 2 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170310' AS student_no, 9 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170310' AS student_no, 8 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170401' AS student_no, 1 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170401' AS student_no, 5 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170401' AS student_no, 11 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170402' AS student_no, 2 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170402' AS student_no, 12 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170402' AS student_no, 9 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170403' AS student_no, 6 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170403' AS student_no, 7 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170403' AS student_no, 4 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170404' AS student_no, 8 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170404' AS student_no, 3 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170404' AS student_no, 11 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170405' AS student_no, 1 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170405' AS student_no, 11 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170405' AS student_no, 5 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170406' AS student_no, 12 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170406' AS student_no, 2 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170406' AS student_no, 8 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170407' AS student_no, 4 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170407' AS student_no, 10 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170407' AS student_no, 1 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170408' AS student_no, 9 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170408' AS student_no, 8 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170408' AS student_no, 2 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170409' AS student_no, 5 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170409' AS student_no, 6 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170409' AS student_no, 3 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170410' AS student_no, 2 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170410' AS student_no, 12 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170410' AS student_no, 9 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170501' AS student_no, 1 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170501' AS student_no, 4 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170501' AS student_no, 11 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170502' AS student_no, 2 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170502' AS student_no, 8 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170502' AS student_no, 12 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170503' AS student_no, 5 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170503' AS student_no, 1 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170503' AS student_no, 7 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170504' AS student_no, 3 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170504' AS student_no, 12 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170504' AS student_no, 9 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170505' AS student_no, 6 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170505' AS student_no, 4 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170505' AS student_no, 1 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170506' AS student_no, 8 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170506' AS student_no, 2 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170506' AS student_no, 11 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170507' AS student_no, 1 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170507' AS student_no, 5 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170507' AS student_no, 10 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170508' AS student_no, 12 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170508' AS student_no, 9 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170508' AS student_no, 3 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170509' AS student_no, 4 AS tag_id, 5 AS self_level
    UNION ALL SELECT '2442170509' AS student_no, 7 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170509' AS student_no, 11 AS tag_id, 2 AS self_level
    UNION ALL SELECT '2442170510' AS student_no, 2 AS tag_id, 4 AS self_level
    UNION ALL SELECT '2442170510' AS student_no, 8 AS tag_id, 3 AS self_level
    UNION ALL SELECT '2442170510' AS student_no, 12 AS tag_id, 5 AS self_level
) m ON m.student_no = u.student_no;

-- ------------------------------------------------------------
-- 3. 校验（可选，手动执行）
--    SELECT COUNT(*) AS added_users FROM sys_user WHERE student_no LIKE '244217%';        -- 应为 50
--    SELECT COUNT(*) AS added_tags  FROM user_tag ut JOIN sys_user u ON u.id = ut.user_id
--      WHERE u.student_no LIKE '244217%';                                                -- 应为 150
--
-- 4. 如需撤掉这批用户（谨慎，会同时删除其标签关系）
--    DELETE ut FROM user_tag ut JOIN sys_user u ON u.id = ut.user_id WHERE u.student_no LIKE '244217%';
--    DELETE FROM sys_user WHERE student_no LIKE '244217%';
