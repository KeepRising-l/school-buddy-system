package com.buddy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buddy.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
