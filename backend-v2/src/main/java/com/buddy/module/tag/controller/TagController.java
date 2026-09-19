package com.buddy.module.tag.controller;

import com.buddy.common.result.R;
import com.buddy.module.tag.entity.InterestTag;
import com.buddy.module.tag.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端标签控制器（公开接口，等价迁移自旧版）
 */
@RestController
@RequestMapping("/api/tag")
public class TagController {

    @Autowired
    private TagService tagService;

    /**
     * 获取所有标签（用户端使用）
     */
    @GetMapping("/list")
    public R<List<InterestTag>> getAllTags() {
        return R.ok(tagService.getAllTags());
    }

    /**
     * 获取所有分类名称（用户端使用）
     */
    @GetMapping("/categories")
    public R<List<String>> getAllCategories() {
        return R.ok(tagService.getAllCategories());
    }
}
