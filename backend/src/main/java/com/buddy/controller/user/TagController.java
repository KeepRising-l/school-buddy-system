package com.buddy.controller.user;

import com.buddy.common.result.R;
import com.buddy.entity.InterestTag;
import com.buddy.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端标签控制器（公开接口，无需管理员权限）
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
