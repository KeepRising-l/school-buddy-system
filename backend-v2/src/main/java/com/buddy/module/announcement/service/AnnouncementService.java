package com.buddy.module.announcement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.module.announcement.entity.SysAnnouncement;

import java.util.List;

/**
 * 公告服务接口
 */
public interface AnnouncementService {

    /**
     * 获取最新一条启用中的弹窗公告（未过期）
     */
    SysAnnouncement getLatestPopup();

    /**
     * 获取当前所有启用且未过期的公告列表（用于首页滚动公告栏展示）
     */
    List<SysAnnouncement> getActiveList();

    IPage<SysAnnouncement> getList(int page, int size, Integer status);

    Long add(SysAnnouncement announcement, Long adminId);

    void update(SysAnnouncement announcement, Long adminId);

    void publish(Long id, Long adminId);

    void close(Long id, Long adminId);

    void delete(Long id);
}
