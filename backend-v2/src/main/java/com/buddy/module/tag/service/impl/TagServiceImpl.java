package com.buddy.module.tag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buddy.common.constant.LevelTagCategories;
import com.buddy.module.tag.entity.InterestTag;
import com.buddy.module.tag.mapper.InterestTagMapper;
import com.buddy.module.tag.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务实现类（等价迁移自旧版）
 */
@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private InterestTagMapper interestTagMapper;

    @Override
    public List<InterestTag> getAllTags() {
        List<InterestTag> tags = interestTagMapper.selectList(
                new LambdaQueryWrapper<InterestTag>()
                        .eq(InterestTag::getStatus, 1)
                        .orderByAsc(InterestTag::getSortOrder)
        );
        fillHasLevel(tags);
        return tags;
    }

    @Override
    public List<InterestTag> getTagsByCategory(String category) {
        List<InterestTag> tags = interestTagMapper.selectList(
                new LambdaQueryWrapper<InterestTag>()
                        .eq(InterestTag::getCategory, category)
                        .eq(InterestTag::getStatus, 1)
                        .orderByAsc(InterestTag::getSortOrder)
        );
        fillHasLevel(tags);
        return tags;
    }

    /**
     * 根据分类填充 hasLevel（是否有段位体系）
     */
    private void fillHasLevel(List<InterestTag> tags) {
        if (tags == null) {
            return;
        }
        for (InterestTag tag : tags) {
            tag.setHasLevel(LevelTagCategories.contains(tag.getCategory()));
        }
    }

    @Override
    public List<String> getAllCategories() {
        List<InterestTag> allTags = interestTagMapper.selectList(
                new LambdaQueryWrapper<InterestTag>()
                        .eq(InterestTag::getStatus, 1)
                        .select(InterestTag::getCategory)
                        .groupBy(InterestTag::getCategory)
        );
        return allTags.stream()
                .map(InterestTag::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
