package com.buddy.module.tag.service;

import com.buddy.module.tag.entity.InterestTag;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService {

    /** 用户端：获取全部启用中的标签 */
    List<InterestTag> getAllTags();

    /** 管理端：获取全部标签（含已禁用标签，供后台管理与重新启用） */
    List<InterestTag> getAdminAllTags();

    List<InterestTag> getTagsByCategory(String category);

    List<String> getAllCategories();

    InterestTag addTag(InterestTag tag);

    InterestTag updateTag(InterestTag tag);

    void disableTag(Long tagId);
}
