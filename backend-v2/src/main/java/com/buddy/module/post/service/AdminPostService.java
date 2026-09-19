package com.buddy.module.post.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.buddy.module.post.dto.PostVO;

/**
 * 管理端-帖子管理服务接口（D-009：管理端接口归属各业务模块）
 */
public interface AdminPostService {

    IPage<PostVO> getPostList(int page, int size, String status, String category, Integer isAuditPass);

    void takedownPost(Long postId);

    void auditPost(Long postId, boolean pass, String reason);

    void deletePost(Long postId);
}
