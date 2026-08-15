package com.buddy.common.constant;

import java.util.Set;

/**
 * 有段位体系的标签分类常量
 *
 * <p>只有竞技类项目（球类/电竞）存在"青铜~王者"水平段位的概念；
 * 跑步、桌游、学习等休闲分类不适用段位，用户设置标签时无需自评段位，
 * 发布组局时也不应出现"段位要求"选项。</p>
 *
 * <p>管理端新增分类时，如需支持段位请在此补充。</p>
 */
public final class LevelTagCategories {

    /** 有段位体系的分类集合 */
    public static final Set<String> CATEGORIES = Set.of("球类", "电竞");

    /**
     * 判断分类是否适用段位体系
     */
    public static boolean contains(String category) {
        return category != null && CATEGORIES.contains(category);
    }

    private LevelTagCategories() {
    }
}
