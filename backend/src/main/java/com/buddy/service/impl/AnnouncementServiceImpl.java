package com.buddy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.result.ResultCode;
import com.buddy.entity.Announcement;
import com.buddy.mapper.AnnouncementMapper;
import com.buddy.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    /** 公告类型：普通公告 */
    public static final int TYPE_NOTICE = 1;
    /** 公告类型：弹窗公告 */
    public static final int TYPE_POPUP = 2;

    /** 状态：关闭 */
    public static final int STATUS_OFF = 0;
    /** 状态：启用 */
    public static final int STATUS_ON = 1;

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Override
    public IPage<Announcement> getList(int page, int size, Integer status) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Announcement::getStatus, status);
        }
        wrapper.orderByDesc(Announcement::getCreatedTime);
        return announcementMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Long add(Announcement announcement, Long adminId) {
        if (announcement.getTitle() == null || announcement.getTitle().trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标题不能为空");
        }
        if (announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "内容不能为空");
        }
        if (announcement.getType() == null) {
            announcement.setType(TYPE_NOTICE);
        }
        announcement.setStatus(STATUS_OFF);
        announcement.setCreatedBy(adminId);
        announcementMapper.insert(announcement);
        return announcement.getId();
    }

    @Override
    public void update(Announcement announcement, Long adminId) {
        Announcement exist = announcementMapper.selectById(announcement.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        if (exist.getStatus() == STATUS_ON) {
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
        Announcement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        exist.setStatus(STATUS_ON);
        exist.setPublishTime(LocalDateTime.now());
        announcementMapper.updateById(exist);
    }

    @Override
    public void close(Long id, Long adminId) {
        Announcement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        exist.setStatus(STATUS_OFF);
        announcementMapper.updateById(exist);
    }

    @Override
    public void delete(Long id) {
        Announcement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        announcementMapper.deleteById(id);
    }

    @Override
    public Announcement getLatestPopup() {
        // 取最新一条启用中的弹窗公告，且未过期
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getType, TYPE_POPUP)
                .eq(Announcement::getStatus, STATUS_ON)
                .and(w -> w.isNull(Announcement::getExpireTime)
                        .or().gt(Announcement::getExpireTime, LocalDateTime.now()))
                .orderByDesc(Announcement::getPublishTime)
                .last("LIMIT 1");
        return announcementMapper.selectOne(wrapper);
    }
}
