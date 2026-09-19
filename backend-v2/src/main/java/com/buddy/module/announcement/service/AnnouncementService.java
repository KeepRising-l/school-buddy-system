package com.buddy.module.announcement.service;

import com.buddy.module.announcement.entity.SysAnnouncement;

/**
 * 公告服务接口
 * S7 迁移用户端弹窗拉取；管理端增删改/发布/关闭在 S8 补齐
 */
public interface AnnouncementService {

    /**
     * 获取最新一条启用中的弹窗公告（未过期）
     */
    SysAnnouncement getLatestPopup();
}
