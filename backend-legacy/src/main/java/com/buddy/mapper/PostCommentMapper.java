package com.buddy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buddy.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper 接口
 */
@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {
}
