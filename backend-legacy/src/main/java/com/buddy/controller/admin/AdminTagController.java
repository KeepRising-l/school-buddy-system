package com.buddy.controller.admin;

import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.result.R;
import com.buddy.entity.InterestTag;
import com.buddy.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端-标签管理控制器
 */
@RestController
@RequestMapping("/api/admin/tag")
public class AdminTagController {

    @Autowired
    private TagService tagService;

    /**
     * 标签列表
     */
    @RequireAdmin
    @GetMapping("/list")
    public R<List<InterestTag>> getAllTags() {
        return R.ok(tagService.getAllTags());
    }

    /**
     * 获取所有分类名称列表
     */
    @RequireAdmin
    @GetMapping("/categories")
    public R<List<String>> getAllCategories() {
        return R.ok(tagService.getAllCategories());
    }

    /**
     * 添加标签
     */
    @RequireAdmin
    @PostMapping("/add")
    public R<InterestTag> addTag(@RequestBody InterestTag tag) {
        return R.ok(tagService.addTag(tag));
    }

    /**
     * 编辑标签
     */
    @RequireAdmin
    @PutMapping("/update")
    public R<InterestTag> updateTag(@RequestBody InterestTag tag) {
        return R.ok(tagService.updateTag(tag));
    }

    /**
     * 禁用标签
     */
    @RequireAdmin
    @PostMapping("/disable/{id}")
    public R<Void> disableTag(@PathVariable Long id) {
        tagService.disableTag(id);
        return R.ok();
    }
}
