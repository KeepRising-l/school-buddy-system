package com.buddy.module.post.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.module.post.dto.CommentVO;
import com.buddy.module.post.entity.Post;
import com.buddy.module.post.entity.PostComment;
import com.buddy.module.post.mapper.PostCommentMapper;
import com.buddy.module.post.mapper.PostMapper;
import com.buddy.module.post.service.CommentService;
import com.buddy.module.post.service.SensitiveWordService;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论服务实现类（等价迁移自旧版）
 */
@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private PostCommentMapper postCommentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Override
    public void addComment(Long userId, Long postId, String content) {
        // 校验评论内容非空且不超长（数据库限制500字符）
        if (StrUtil.isBlank(content)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论内容不能超过500字");
        }
        // 违禁词检测
        List<String> hitWords = sensitiveWordService.check(content);
        if (!hitWords.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "评论包含违禁词：" + String.join("、", hitWords) + "，请修改后重新发布");
        }
        // 校验帖子存在（防止孤儿评论）
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        // 先发后审：已下架/已取消的帖子不可评论
        if (post.getIsAuditPass() != null && post.getIsAuditPass() == 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "帖子已被下架，暂不可评论");
        }
        if (PostStatus.CANCELLED.equals(post.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该组局已取消，暂不可评论");
        }
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        postCommentMapper.insert(comment);
    }

    @Override
    public IPage<CommentVO> getComments(Long postId, int page, int size) {
        IPage<PostComment> commentPage = postCommentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PostComment>()
                        .eq(PostComment::getPostId, postId)
                        .orderByDesc(PostComment::getCreatedTime)
        );

        // 转换为CommentVO并填充用户信息
        IPage<CommentVO> voPage = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        voPage.setRecords(commentPage.getRecords().stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setPostId(c.getPostId());
            vo.setUserId(c.getUserId());
            vo.setContent(c.getContent());
            vo.setCreatedTime(c.getCreatedTime());

            // 查询评论者信息
            User user = userMapper.selectById(c.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }
}
