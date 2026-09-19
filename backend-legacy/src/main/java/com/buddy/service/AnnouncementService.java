package com.buddy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.entity.Announcement;

/**
 * 系统公告服务
 */
public interface AnnouncementService {

    /**
     * 分页查询公告列表
     */
    IPage<Announcement> getList(int page, int size, Integer status);

    /**
     * 新增公告
     */
    Long add(Announcement announcement, Long adminId);

    /**
     * 编辑公告
     */
    void update(Announcement announcement, Long adminId);

    /**
     * 发布公告
     */
    void publish(Long id, Long adminId);

    /**
     * 关闭公告
     */
    void close(Long id, Long adminId);

    /**
     * 删除公告
     */
    void delete(Long id);

    /**
     * 获取最新的启用中的弹窗公告（用户端用）
     */
    Announcement getLatestPopup();
}
