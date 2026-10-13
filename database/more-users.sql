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
-- 1. 学生账号（密码 = BCrypt(学号+"123")）
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_user`
(`student_no`, `phone`, `password`, `nickname`, `username`, `college`, `gender`, `credit_score`, `status`, `is_admin`)
VALUES
('2442170101', '1392170101', '$2a$10$8t02hKOd/1THGohqsDxJX.hTEM5BHWTbQR4gpDd1dG1RbcfK.doOu', '陈嘉豪', '2442170101', '大数据人工智能学院', 1, 95, 1, 0),
('2442170102', '1392170102', '$2a$10$/D.gmFgmLU0rG7upF5oHXeQmhB6e6QTdhPdSJaBi3EmBqcsebI0Km', '林晓萌', '2442170102', '大数据人工智能学院', 2, 90, 1, 0),
('2442170103', '1392170103', '$2a$10$7bTTELjZjrz7fh6ILaP4t.9BNw/b/ERNSkzC4jdTXOAC11ucfcwzu', '张一鸣', '2442170103', '大数据人工智能学院', 1, 92, 1, 0),
('2442170104', '1392170104', '$2a$10$fsljiUG3HqGhpoS2WMJeF.hU1PvhfiOqIBy6sKFYT0VAkHkH1PjBS', '王雨琪', '2442170104', '大数据人工智能学院', 2, 98, 1, 0),
('2442170105', '1392170105', '$2a$10$GemVzOFaD6KUpNWcLOOn/O1Ztbno1F6IzlD6ac/G3QU4fnpUizyUe', '刘泽宇', '2442170105', '大数据人工智能学院', 1, 88, 1, 0),
('2442170106', '1392170106', '$2a$10$xY9iZZwIqbBfW3diZAfSVOR2wGW5BI2wyQsCJh60oFvO0jJPBsqTa', '赵梦洁', '2442170106', '大数据人工智能学院', 2, 91, 1, 0),
('2442170107', '1392170107', '$2a$10$78TBGt5AK1yCsDFMIXlQ.OJjhAUWbuYoiIOrZXGlhc/OOv25bFbYi', '孙浩宇', '2442170107', '大数据人工智能学院', 1, 96, 1, 0),
('2442170108', '1392170108', '$2a$10$Sk74Ivai5f/vgaH3sn0/dOsfA3TJJ3GqbvtCIA5O6GIefudA48f42', '周诗涵', '2442170108', '大数据人工智能学院', 2, 89, 1, 0),
('2442170109', '1392170109', '$2a$10$h4EK4sbBnfnVc1AgAQOiCeprF5ryrOfStU1bzfDOK0wtNu1ZGWxuO', '吴俊杰', '2442170109', '大数据人工智能学院', 1, 94, 1, 0),
('2442170110', '1392170110', '$2a$10$ThF04wcQDl2StUdc9K2RBelpQDxFDaWrMYyQ7SVypWUn08DcFRS3W', '郑欣怡', '2442170110', '大数据人工智能学院', 2, 97, 1, 0),
('2442170201', '1392170201', '$2a$10$WyIVo/D/OP9NuY5L1LRtAuMvGXIJfDC.r38YFyolCLV.2TxhrmKDO', '冯子豪', '2442170201', '大数据人工智能学院', 1, 95, 1, 0),
('2442170202', '1392170202', '$2a$10$cpZYOs9/9JsP5SezArA8Z.DquxxgrK3jC9OLk5sEAvP962ibuPNkW', '陈思雨', '2442170202', '大数据人工智能学院', 2, 90, 1, 0),
('2442170203', '1392170203', '$2a$10$v9qT26Esdqq3snGz1EmV3Oy1CNHdlanYSPSwyZvTj/645CSEjNaWG', '蒋宇轩', '2442170203', '大数据人工智能学院', 1, 92, 1, 0),
('2442170204', '1392170204', '$2a$10$YzsfYgpbhvNIlm4Fnses6uTbtvF49FPUNTufNvVzNNP3xhxN2/QQ6', '沈佳慧', '2442170204', '大数据人工智能学院', 2, 98, 1, 0),
('2442170205', '1392170205', '$2a$10$5CLz5vNKWawcXkw1pNt09el34NXFmnwoxltVkk4gtVICILvIt66R6', '韩泽楷', '2442170205', '大数据人工智能学院', 1, 88, 1, 0),
('2442170206', '1392170206', '$2a$10$6rQNr20ED71/aNOo89/8SuLm1U./mwCUB/tAlNIgY9EU776QNJpoW', '杨露露', '2442170206', '大数据人工智能学院', 2, 91, 1, 0),
('2442170207', '1392170207', '$2a$10$faBkTQkEe7P4vmalRqq1GO9zIrn2ZHD1EzY87uWokTYcZpZYELezG', '朱天豪', '2442170207', '大数据人工智能学院', 1, 96, 1, 0),
('2442170208', '1392170208', '$2a$10$lm7pT7p4FxHYZpCbf131k.g3j5sdvdl13eYXzrJiSfH1IjvhNC4F6', '秦嘉怡', '2442170208', '大数据人工智能学院', 2, 89, 1, 0),
('2442170209', '1392170209', '$2a$10$.r7kocrC9TrQyf7uHIJQUOCnUZmhZpDFPUS.lIiuEEH7VN27wLqNq', '许文博', '2442170209', '大数据人工智能学院', 1, 94, 1, 0),
('2442170210', '1392170210', '$2a$10$rUNIu/X4KTL5TNEnDTaoI.WjTyBrF9awDxRKdc6nOrITb7HdCDszy', '何梦琪', '2442170210', '大数据人工智能学院', 2, 97, 1, 0),
('2442170301', '1392170301', '$2a$10$kNRl/Y8lHnNuArpoDvEzU.m0jKnoWOdOxaX7UzMZE8gpkVImgpCra', '罗子健', '2442170301', '大数据人工智能学院', 1, 95, 1, 0),
('2442170302', '1392170302', '$2a$10$F9RqybgNpfw3p/qkmF57VeinPTpD/EcKiI3xRtCmcwNZMoQcwE5py', '高语彤', '2442170302', '大数据人工智能学院', 2, 90, 1, 0),
('2442170303', '1392170303', '$2a$10$RNMMHg2WCY/93k9S5PT4su8PEQeiOOMv4J/nWgak0zjyCvpuLDEW.', '梁逸凡', '2442170303', '大数据人工智能学院', 1, 92, 1, 0),
('2442170304', '1392170304', '$2a$10$lO1zuxsfGBCtPajAoPqRAuPk6YNlGUeL18Wspu2.MPlClD9bl/Xem', '谢雅静', '2442170304', '大数据人工智能学院', 2, 98, 1, 0),
('2442170305', '1392170305', '$2a$10$1lii9Y1p53tOGbBhRuT1TeYB5dQnpbgozBTkJTQkVKcF2NG3op5Qy', '唐嘉铭', '2442170305', '大数据人工智能学院', 1, 88, 1, 0),
('2442170306', '1392170306', '$2a$10$BwK0c7xDAt.yNrig3NaViuxKUXaSaxVxe3cmVitNZF6vTsOioL92i', '曹欣妍', '2442170306', '大数据人工智能学院', 2, 91, 1, 0),
('2442170307', '1392170307', '$2a$10$rZRdYdwSykEa7qnGJvKlgeszlGx/pXoVvo8X6G2B62l6yyqEYM.6G', '彭浩楠', '2442170307', '大数据人工智能学院', 1, 96, 1, 0),
('2442170308', '1392170308', '$2a$10$XasJTp5JQ5zeHYdFptlYfe/xZthlW6iJ.s98KuuyKaP8fZUSlSjuK', '潘依琳', '2442170308', '大数据人工智能学院', 2, 89, 1, 0),
('2442170309', '1392170309', '$2a$10$aOJfpCqPZ2Q47A8BAYRdpe1LwsS.qz45BgLAhUXmbrSdqWVMWF8l.', '袁子扬', '2442170309', '大数据人工智能学院', 1, 58, 1, 0),
('2442170310', '1392170310', '$2a$10$2nokEIPfhruQITto1qpWXuoGL7L4scZ9Vh5aUiU2UkGPN12MEmDeG', '卢诗琪', '2442170310', '大数据人工智能学院', 2, 97, 1, 0),
('2442170401', '1392170401', '$2a$10$zPyOZoEojvyDjnUeXKhDsu6zA5B2eCe0X9T69AURdNclO0VEVZ8ni', '杜俊熙', '2442170401', '计算机学院', 1, 95, 1, 0),
('2442170402', '1392170402', '$2a$10$gZaawW2sFZ7kduKJsNpqXejmgpJgH8zHqhWahQ6hxN7Q4MM5w07Pq', '苏若涵', '2442170402', '计算机学院', 2, 90, 1, 0),
('2442170403', '1392170403', '$2a$10$jjreNdllXlrzHUO0pmyU2eiVKoVx5.CxymTnm/qKApCCsqKhLotzO', '叶梓豪', '2442170403', '计算机学院', 1, 92, 1, 0),
('2442170404', '1392170404', '$2a$10$F3Tr/zeX2ClPEdbpPJoOoeStI6r4mp5N33U3W/7LrotF9eP7iZxf6', '吕佳琪', '2442170404', '计算机学院', 2, 98, 1, 0),
('2442170405', '1392170405', '$2a$10$NwGlCuTlDHn0Hq2ML1xYOOoODN2E.glmtpothi5w36IgBGTsZ2/Ai', '魏晨旭', '2442170405', '计算机学院', 1, 88, 1, 0),
('2442170406', '1392170406', '$2a$10$CUNOI/OuWsbqMrpXn2m1iO01qkZfrPnhY/OOImLiaMPGvRiNyKE3a', '任雨桐', '2442170406', '计算机学院', 2, 91, 1, 0),
('2442170407', '1392170407', '$2a$10$IF2LQV8MpR9VrR0iMEMB6.2kxP4rqDKo9bB0sHgTXjkdSbZBQao6q', '方冠宇', '2442170407', '计算机学院', 1, 96, 1, 0),
('2442170408', '1392170408', '$2a$10$DxGsK0GOVH847Ad8WgXNVOlFfWqiIachil1wdq67MM.3RpR8cTssu', '石美琳', '2442170408', '计算机学院', 2, 89, 1, 0),
('2442170409', '1392170409', '$2a$10$/.ntJJRHgdzAa0JvEF.Gw./DN5NA8Lx68Lmndvs/itzZPssTTG4YG', '段卓远', '2442170409', '计算机学院', 1, 45, 1, 0),
('2442170410', '1392170410', '$2a$10$SkO/p5JOr8BkhwmoJ3ytreugxLuUSVPOnGGn5rlSP1oerGodxhngW', '白若曦', '2442170410', '计算机学院', 2, 35, 1, 0),
('2442170501', '1392170501', '$2a$10$i7E7P9z8lM/IWdFoFiLHnOG5wsq/6pewVs0H7n26oBj4AilJx6Y4G', '崔浩然', '2442170501', '体育学院', 1, 95, 1, 0),
('2442170502', '1392170502', '$2a$10$p7wi0dUC0o6LjWpa3KiYkOM.0rydQ.EDKcbkFEVpRrQkW/kyug1YO', '康馨予', '2442170502', '体育学院', 2, 90, 1, 0),
('2442170503', '1392170503', '$2a$10$YoD2DoHilmMCv.hqn5Z92eNwSLWdsHl7fuCMEsprEorx6qw457.uK', '温子豪', '2442170503', '体育学院', 1, 92, 1, 0),
('2442170504', '1392170504', '$2a$10$k0w.ehIKZ7gTueOX/A6jkeiNrA8n19zmw5T0p2SmUGFVtJLh/e48e', '洪雨薇', '2442170504', '体育学院', 2, 98, 1, 0),
('2442170505', '1392170505', '$2a$10$mm5Ig5JomI49BWlgB//ioeTodYzm8Fegmm7UvGay5Q.FRxHGkk3kq', '龚浩天', '2442170505', '体育学院', 1, 88, 1, 0),
('2442170506', '1392170506', '$2a$10$ZHwcp36Xzt.dHp6NOA4pXue4jD0o9J3UTB19awW1OkqUrmOZPSVa2', '曾雅琪', '2442170506', '体育学院', 2, 91, 1, 0),
('2442170507', '1392170507', '$2a$10$vDHVHTM/yBFZQIIFAHT/7.X5XQSEHrNNXsAPKe6N/3vVpXBwZO6Sm', '邵俊豪', '2442170507', '体育学院', 1, 96, 1, 0),
('2442170508', '1392170508', '$2a$10$tfm5mS4b0UB6VBSYP5zBwOvJBFpQN1lGOzbBeJI.fTOQF8oa.dNIu', '万思彤', '2442170508', '体育学院', 2, 89, 1, 0),
('2442170509', '1392170509', '$2a$10$dsMVggPj3guYnIy31TPk9.vBefl4KhKXUXB1cLtAmyxPU6lYTprEi', '钱泽洋', '2442170509', '体育学院', 1, 94, 1, 0),
('2442170510', '1392170510', '$2a$10$dux9Bqd1wFnWVIWKWGhQD.zNMKTlrTyUr3O2lgiLWpn6bZ2hoVKUS', '严雨欣', '2442170510', '体育学院', 2, 97, 1, 0);

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
