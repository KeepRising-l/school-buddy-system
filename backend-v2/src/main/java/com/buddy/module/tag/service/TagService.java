package com.buddy.module.tag.service;

import com.buddy.module.tag.entity.InterestTag;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService {

    List<InterestTag> getAllTags();

    List<InterestTag> getTagsByCategory(String category);

    List<String> getAllCategories();

    InterestTag addTag(InterestTag tag);

    InterestTag updateTag(InterestTag tag);

    void disableTag(Long tagId);
}
