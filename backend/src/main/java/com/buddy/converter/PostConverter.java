package com.buddy.converter;

import cn.hutool.core.util.StrUtil;
import com.buddy.dto.response.PostVO;
import com.buddy.entity.InterestTag;
import com.buddy.entity.Post;
import com.buddy.entity.User;
import com.buddy.mapper.InterestTagMapper;
import com.buddy.mapper.UserMapper;
import com.buddy.service.CreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 帖子 -> PostVO 转换器。
 *
 * <p>集中了原先散落在 {@code PostServiceImpl / RecommendServiceImpl / AdminServiceImpl}
 * 三处的 {@code convertToPostVO}（消除重复，对应《17-项目不足与改进方向》4.4）。
 * </p>
 *
 * <p>同时解决列表接口的 N+1 查询（对应 2.2）：{@link #toVOList(List)} 只发起
 * 两次批量 {@code IN} 查询（一次查发布者、一次查标签），用内存 Map 组装所有 VO，
 * 把"广场 10 条帖子 = 1 + 10 + N 次查询"降为常数次。</p>
 */
@Component
public class PostConverter {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private InterestTagMapper interestTagMapper;

    @Autowired
    private CreditService creditService;

    /**
     * 单条转换（详情页等单对象场景）
     */
    public PostVO toVO(Post post) {
        if (post == null) {
            return null;
        }
        PostVO vo = baseVO(post);

        if (post.getPublisherId() != null) {
            User publisher = userMapper.selectById(post.getPublisherId());
            if (publisher != null) {
                fillPublisher(vo, publisher);
            }
        }
        if (StrUtil.isNotBlank(post.getTagIds())) {
            vo.setTagName(resolveTagNames(post.getTagIds()));
        }
        return vo;
    }

    /**
     * 批量转换（列表场景）。两次 IN 查询解决 N+1：
     * 一次查出所有发布者，一次查出所有标签，再用内存 Map 组装。
     */
    public List<PostVO> toVOList(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集本页所有发布者ID与标签ID
        Set<Long> publisherIds = new HashSet<>();
        Set<Long> tagIds = new HashSet<>();
        for (Post p : posts) {
            if (p == null) {
                continue;
            }
            if (p.getPublisherId() != null) {
                publisherIds.add(p.getPublisherId());
            }
            tagIds.addAll(parseTagIds(p.getTagIds()));
        }

        // 两次批量查询（空集合会生成非法 `IN ()`，需跳过）
        Map<Long, User> userMap = publisherIds.isEmpty()
                ? Collections.emptyMap()
                : toMapById(userMapper.selectBatchIds(publisherIds), User::getId);
        Map<Long, InterestTag> tagMap = tagIds.isEmpty()
                ? Collections.emptyMap()
                : toMapById(interestTagMapper.selectBatchIds(tagIds), InterestTag::getId);

        List<PostVO> result = new ArrayList<>(posts.size());
        for (Post p : posts) {
            if (p == null) {
                result.add(null);
                continue;
            }
            PostVO vo = baseVO(p);
            if (p.getPublisherId() != null) {
                User publisher = userMap.get(p.getPublisherId());
                if (publisher != null) {
                    fillPublisher(vo, publisher);
                }
            }
            // 沿用原逻辑：tagIds 为空时 tagName 保持 null，只在有标签时拼名字
            if (StrUtil.isNotBlank(p.getTagIds())) {
                vo.setTagName(resolveTagNames(p.getTagIds(), tagMap));
            }
            result.add(vo);
        }
        return result;
    }

    /** 基础字段拷贝（不含发布者、标签名这两处 N+1 关联） */
    private PostVO baseVO(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setCategory(post.getCategory());
        vo.setTagIds(post.getTagIds());
        vo.setStartTime(post.getStartTime());
        vo.setEndTime(post.getEndTime());
        vo.setLocation(post.getLocation());
        vo.setMaxPeople(post.getMaxPeople());
        vo.setCurrentPeople(post.getCurrentPeople());
        vo.setStatus(post.getStatus());
        vo.setViewCount(post.getViewCount());
        vo.setPublisherId(post.getPublisherId());
        vo.setCreatedTime(post.getCreatedTime());
        vo.setDescription(post.getDescription());
        vo.setIsAuditPass(post.getIsAuditPass());
        vo.setRequireLevel(post.getRequireLevel());
        vo.setRequireGender(post.getRequireGender());
        return vo;
    }

    private void fillPublisher(PostVO vo, User publisher) {
        vo.setPublisherNickname(publisher.getNickname());
        vo.setPublisherAvatar(publisher.getAvatar());
        vo.setPublisherCreditLevel(creditService.getCreditLevel(publisher.getCreditScore()));
    }

    /** 单条场景：标签名逐一查询 */
    private String resolveTagNames(String tagIds) {
        List<String> names = new ArrayList<>();
        for (Long tagId : parseTagIds(tagIds)) {
            InterestTag tag = interestTagMapper.selectById(tagId);
            if (tag != null) {
                names.add(tag.getName());
            }
        }
        return String.join(",", names);
    }

    /** 批量场景：从内存 Map 取标签，不再查库 */
    private String resolveTagNames(String tagIds, Map<Long, InterestTag> tagMap) {
        List<String> names = new ArrayList<>();
        for (Long tagId : parseTagIds(tagIds)) {
            InterestTag tag = tagMap.get(tagId);
            if (tag != null) {
                names.add(tag.getName());
            }
        }
        return String.join(",", names);
    }

    private static List<Long> parseTagIds(String tagIds) {
        List<Long> ids = new ArrayList<>();
        if (StrUtil.isNotBlank(tagIds)) {
            for (String idStr : tagIds.split(",")) {
                try {
                    ids.add(Long.valueOf(idStr.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ids;
    }

    private static <T> Map<Long, T> toMapById(List<T> items, Function<T, Long> idFn) {
        Map<Long, T> map = new HashMap<>();
        if (items != null) {
            for (T item : items) {
                Long key = idFn.apply(item);
                if (key != null) {
                    map.put(key, item);
                }
            }
        }
        return map;
    }
}