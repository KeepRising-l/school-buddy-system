package com.buddy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buddy.common.constant.CreditConstant;
import com.buddy.entity.Review;
import com.buddy.entity.User;
import com.buddy.mapper.ReviewMapper;
import com.buddy.mapper.UserMapper;
import com.buddy.service.CreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 信誉分服务实现类
 */
@Service
public class CreditServiceImpl implements CreditService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateUserCredit(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        // 查询用户所有收到的评价
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getTargetUserId, userId)
        );

        // 计算信誉分 = 100 + Σ评价影响
        int creditScore = CreditConstant.INITIAL_SCORE;
        for (Review review : reviews) {
            creditScore += calculateReviewImpact(review);
        }

        // 限制0-100
        creditScore = Math.max(CreditConstant.MIN_SCORE, Math.min(CreditConstant.MAX_SCORE, creditScore));

        // 更新数据库
        user.setCreditScore(creditScore);
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateAllCredit() {
        // 查询所有正常状态的用户
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1)
        );

        // 逐个重新计算
        for (User user : users) {
            recalculateUserCredit(user.getId());
        }
    }

    @Override
    public int getCreditLevel(Integer creditScore) {
        if (creditScore == null) {
            return 1;
        }
        if (creditScore >= 90) {
            return 5;
        } else if (creditScore >= 75) {
            return 4;
        } else if (creditScore >= 60) {
            return 3;
        } else if (creditScore >= 40) {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
    public String getCreditLabel(Integer creditScore) {
        int level = getCreditLevel(creditScore);
        switch (level) {
            case 5:
                return "靠谱队友";
            case 4:
                return "优质队友";
            case 3:
                return "普通队友";
            case 2:
                return "需改善";
            case 1:
                return "失信用户";
            default:
                return "未知";
        }
    }

    @Override
    public boolean isLowCredit(Integer creditScore) {
        return creditScore != null && creditScore < CreditConstant.PUNISH_THRESHOLD;
    }

    /**
     * 计算单条评价对信誉分的影响
     * 以标签为主计算（PRD 中"标签 → 星级 → 信誉分影响"是整体映射关系），
     * 星级仅在没有标签时作兜底，避免 5星+靠谱 被重复累加（原 bug：+2+2=+4）。
     */
    private int calculateReviewImpact(Review review) {
        int impact = 0;

        String tag = review.getTag();
        if (tag != null && !tag.isEmpty()) {
            // 支持逗号分隔的多标签，取第一个有效标签计算
            String primaryTag = tag.split(",")[0].trim();
            switch (primaryTag) {
                case "靠谱":
                case "友好":
                case "技术好":
                    impact += CreditConstant.RELIABLE_BONUS;
                    break;
                case "准时":
                    impact += CreditConstant.FOUR_STAR_BONUS;
                    break;
                case "迟到":
                case "态度差":
                    impact += CreditConstant.LATE_PENALTY;
                    break;
                case "爽约":
                    impact += CreditConstant.NOSHOW_PENALTY;
                    break;
                default:
                    break;
            }
        } else if (review.getRating() != null) {
            // 无标签时用星级兜底
            if (review.getRating() >= 5) {
                impact += CreditConstant.RELIABLE_BONUS;
            } else if (review.getRating() >= 4) {
                impact += CreditConstant.FOUR_STAR_BONUS;
            } else if (review.getRating() <= 2) {
                impact += CreditConstant.LATE_PENALTY;
            }
        }

        return impact;
    }
}
