package com.buddy.module.square.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.module.post.dto.PostVO;

/**
 * 推荐服务接口
 */
public interface RecommendService {

    /**
     * 获取推荐帖子列表（基于兴趣标签的加权推荐算法）
     * @param timeRange 时间筛选：today/week/month，为空则不限
     */
    IPage<PostVO> getRecommendList(Long userId, int page, int size, String category, String sort, String timeRange, String keyword);
}
