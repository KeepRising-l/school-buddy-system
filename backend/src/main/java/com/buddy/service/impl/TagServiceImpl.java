package com.buddy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buddy.entity.InterestTag;
import com.buddy.mapper.InterestTagMapper;
import com.buddy.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标签服务实现类
 */
@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private InterestTagMapper interestTagMapper;

    @Override
    public List<InterestTag> getAllTags() {
        return interestTagMapper.selectList(
                new LambdaQueryWrapper<InterestTag>()
                        .eq(InterestTag::getStatus, 1)
                        .orderByAsc(InterestTag::getSortOrder)
        );
    }

    @Override
    public List<InterestTag> getTagsByCategory(String category) {
        return interestTagMapper.selectList(
                new LambdaQueryWrapper<InterestTag>()
                        .eq(InterestTag::getCategory, category)
                        .eq(InterestTag::getStatus, 1)
                        .orderByAsc(InterestTag::getSortOrder)
        );
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
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public InterestTag addTag(InterestTag tag) {
        tag.setStatus(1);
        interestTagMapper.insert(tag);
        return tag;
    }

    @Override
    public InterestTag updateTag(InterestTag tag) {
        interestTagMapper.updateById(tag);
        return tag;
    }

    @Override
    public void disableTag(Long tagId) {
        InterestTag tag = interestTagMapper.selectById(tagId);
        if (tag != null) {
            tag.setStatus(0);
            interestTagMapper.updateById(tag);
        }
    }
}
