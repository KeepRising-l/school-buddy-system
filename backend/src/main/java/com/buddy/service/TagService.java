package com.buddy.service;

import com.buddy.entity.InterestTag;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService {

    /**
     * 获取所有标签
     */
    List<InterestTag> getAllTags();

    /**
     * 按分类获取标签
     */
    List<InterestTag> getTagsByCategory(String category);

    /**
     * 获取所有分类名称列表
     */
    List<String> getAllCategories();

    /**
     * 新增标签
     */
    InterestTag addTag(InterestTag tag);

    /**
     * 更新标签
     */
    InterestTag updateTag(InterestTag tag);

    /**
     * 禁用标签
     */
    void disableTag(Long tagId);
}
