package com.buddy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buddy.entity.UserTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户标签关联 Mapper 接口
 */
@Mapper
public interface UserTagMapper extends BaseMapper<UserTag> {
}
