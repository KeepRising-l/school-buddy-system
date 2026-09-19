package com.buddy.module.tag.controller.admin;

import com.buddy.common.annotation.RequireAdmin;
import com.buddy.common.result.R;
import com.buddy.module.tag.entity.InterestTag;
import com.buddy.module.tag.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-标签管理控制器（等价迁移自旧版，D-009 归属 tag 模块）
 */
@RestController
@RequestMapping("/api/admin/tag")
public class AdminTagController {

    @Autowired
    private TagService tagService;

    /**
     * 全部标签（与用户端同源：仅启用项，等价旧版）
     */
    @RequireAdmin
    @GetMapping("/list")
    public R<List<InterestTag>> getAllTags() {
        return R.ok(tagService.getAllTags());
    }

    /**
     * 全部分类名称
     */
    @RequireAdmin
    @GetMapping("/categories")
    public R<List<String>> getAllCategories() {
        return R.ok(tagService.getAllCategories());
    }

    /**
     * 新增标签
     */
    @RequireAdmin
    @PostMapping("/add")
    public R<InterestTag> addTag(@RequestBody InterestTag tag) {
        return R.ok(tagService.addTag(tag));
    }

    /**
     * 更新标签
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
