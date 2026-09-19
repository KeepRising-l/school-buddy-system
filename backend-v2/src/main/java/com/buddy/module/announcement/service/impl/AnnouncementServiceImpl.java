package com.buddy.module.announcement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buddy.module.announcement.entity.SysAnnouncement;
import com.buddy.module.announcement.mapper.AnnouncementMapper;
import com.buddy.module.announcement.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 公告服务实现类（等价迁移自旧版，S7 范围：用户端弹窗）
 */
@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Override
    public SysAnnouncement getLatestPopup() {
        // 取最新一条启用中的弹窗公告，且未过期
        LambdaQueryWrapper<SysAnnouncement> wrapper = new LambdaQueryWrapper<SysAnnouncement>()
                .eq(SysAnnouncement::getType, SysAnnouncement.TYPE_POPUP)
                .eq(SysAnnouncement::getStatus, SysAnnouncement.STATUS_ON)
                .and(w -> w.isNull(SysAnnouncement::getExpireTime)
                        .or().gt(SysAnnouncement::getExpireTime, LocalDateTime.now()))
                .orderByDesc(SysAnnouncement::getPublishTime)
                .last("LIMIT 1");
        return announcementMapper.selectOne(wrapper);
    }
}
