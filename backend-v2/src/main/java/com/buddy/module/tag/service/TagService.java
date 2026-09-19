package com.buddy.module.tag.service;

import com.buddy.module.tag.entity.InterestTag;

import java.util.List;

/**
 * 标签服务接口
 * S7 迁移用户端查询；管理端增改禁在 S8 补齐
 */
public interface TagService {

    List<InterestTag> getAllTags();

    List<InterestTag> getTagsByCategory(String category);

    List<String> getAllCategories();
}
