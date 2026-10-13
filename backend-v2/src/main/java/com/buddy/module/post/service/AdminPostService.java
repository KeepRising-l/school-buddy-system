package com.buddy.module.post.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.module.post.dto.PostVO;

/**
 * 管理端-帖子管理服务接口（D-009：管理端接口归属各业务模块）
 */
public interface AdminPostService {

    IPage<PostVO> getPostList(int page, int size, String status, String category, Integer isAuditPass);

    /**
     * 下架帖子。reason 为下架原因（可选），会随通知发送给发布者；
     * 报名者会收到"报名已被取消"通知（报名联动取消）。
     */
    void takedownPost(Long postId, String reason);

    void auditPost(Long postId, boolean pass, String reason);

    void deletePost(Long postId);
}
