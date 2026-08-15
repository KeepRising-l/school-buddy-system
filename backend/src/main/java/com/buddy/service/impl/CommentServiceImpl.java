package com.buddy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.dto.response.CommentVO;
import com.buddy.entity.Post;
import com.buddy.entity.PostComment;
import com.buddy.entity.User;
import com.buddy.mapper.PostCommentMapper;
import com.buddy.mapper.PostMapper;
import com.buddy.mapper.UserMapper;
import com.buddy.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private PostCommentMapper postCommentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Override
    public void addComment(Long userId, Long postId, String content) {
        // 校验评论内容非空且不超长（数据库限制500字符）
        if (StrUtil.isBlank(content)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论内容不能超过500字");
        }
        // 校验帖子存在（防止孤儿评论）
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
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
