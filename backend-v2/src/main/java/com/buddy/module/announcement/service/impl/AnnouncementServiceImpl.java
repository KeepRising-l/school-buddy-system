package com.buddy.module.announcement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.module.announcement.entity.SysAnnouncement;
import com.buddy.module.announcement.mapper.AnnouncementMapper;
import com.buddy.module.announcement.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 公告服务实现类（等价迁移自旧版）
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

    @Override
    public IPage<SysAnnouncement> getList(int page, int size, Integer status) {
        LambdaQueryWrapper<SysAnnouncement> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SysAnnouncement::getStatus, status);
        }
        wrapper.orderByDesc(SysAnnouncement::getCreatedTime);
        return announcementMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Long add(SysAnnouncement announcement, Long adminId) {
        if (announcement.getTitle() == null || announcement.getTitle().trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标题不能为空");
        }
        if (announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "内容不能为空");
        }
        if (announcement.getType() == null) {
            announcement.setType(SysAnnouncement.TYPE_NOTICE);
        }
        announcement.setStatus(SysAnnouncement.STATUS_OFF);
        announcement.setCreatedBy(adminId);
        announcementMapper.insert(announcement);
        return announcement.getId();
    }

    @Override
    public void update(SysAnnouncement announcement, Long adminId) {
        SysAnnouncement exist = announcementMapper.selectById(announcement.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        if (exist.getStatus() == SysAnnouncement.STATUS_ON) {
            throw new BusinessException("已启用的公告请先关闭再修改");
        }
        exist.setTitle(announcement.getTitle());
        exist.setContent(announcement.getContent());
        exist.setType(announcement.getType());
        exist.setExpireTime(announcement.getExpireTime());
        announcementMapper.updateById(exist);
    }

    @Override
    public void publish(Long id, Long adminId) {
        SysAnnouncement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        exist.setStatus(SysAnnouncement.STATUS_ON);
        exist.setPublishTime(LocalDateTime.now());
        announcementMapper.updateById(exist);
    }

    @Override
    public void close(Long id, Long adminId) {
        SysAnnouncement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        exist.setStatus(SysAnnouncement.STATUS_OFF);
        announcementMapper.updateById(exist);
    }

    @Override
    public void delete(Long id) {
        SysAnnouncement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        announcementMapper.deleteById(id);
    }
}
