package com.buddy.module.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buddy.common.constant.CreditConstant;
import com.buddy.common.exception.BusinessException;
import com.buddy.common.jwt.JwtUtils;
import com.buddy.common.result.ResultCode;
import com.buddy.module.auth.dto.request.LoginRequest;
import com.buddy.module.auth.dto.request.RegisterRequest;
import com.buddy.module.credit.service.CreditService;
import com.buddy.module.user.dto.UserVO;
import com.buddy.module.user.entity.User;
import com.buddy.module.user.mapper.UserMapper;
import com.buddy.module.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 * Demo 范围：注册 / 登录 / 本人资料；登录安全（Redis 失败计数）等价迁移，可验证 Redis 连通性
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CreditService creditService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 登录失败计数缓存前缀 */
    private static final String LOGIN_FAIL_KEY = "login:fail:";
    /** 最大登录失败次数 */
    private static final int MAX_LOGIN_FAIL = 5;
    /** 锁定时间（分钟） */
    private static final int LOCK_MINUTES = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        boolean isPhoneRegister = "phone".equals(request.getRegisterType());

        if (isPhoneRegister) {
            // 手机号注册：手机号必填
            if (StrUtil.isBlank(request.getPhone())) {
                throw new BusinessException("手机号不能为空");
            }
            // 校验手机号是否已存在
            Long countByPhone = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
            );
            if (countByPhone > 0) {
                throw new BusinessException(ResultCode.USER_EXISTS, "该手机号已注册");
            }
            // 学号选填，如果填了也校验唯一性
            if (StrUtil.isNotBlank(request.getStudentNo())) {
                Long countByStudentNo = userMapper.selectCount(
                        new LambdaQueryWrapper<User>().eq(User::getStudentNo, request.getStudentNo())
                );
                if (countByStudentNo > 0) {
                    throw new BusinessException(ResultCode.USER_EXISTS, "该学号已注册");
                }
            }
        } else {
            // 学号注册：学号必填
            if (StrUtil.isBlank(request.getStudentNo())) {
                throw new BusinessException("学号不能为空");
            }
            // 校验学号是否已存在
            Long countByStudentNo = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getStudentNo, request.getStudentNo())
            );
            if (countByStudentNo > 0) {
                throw new BusinessException(ResultCode.USER_EXISTS, "该学号已注册");
            }
            // 手机号选填，如果填了也校验唯一性
            if (StrUtil.isNotBlank(request.getPhone())) {
                Long countByPhone = userMapper.selectCount(
                        new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
                );
                if (countByPhone > 0) {
                    throw new BusinessException(ResultCode.USER_EXISTS, "该手机号已注册");
                }
            }
        }

        // 构建用户实体
        User user = new User();
        // 手机号注册不填学号时，student_no NOT NULL 约束导致 500：
        // 生成唯一占位学号（前缀 P + 时间戳），保证入库成功且可登录
        String studentNo = request.getStudentNo();
        String username;
        if (StrUtil.isBlank(studentNo)) {
            studentNo = "P" + System.currentTimeMillis();
            username = "P" + System.currentTimeMillis();
        } else {
            username = studentNo;
        }
        user.setStudentNo(studentNo);
        user.setPhone(request.getPhone());
        // 默认用户名 = 学号（注册后可在个人中心修改）
        user.setUsername(username);
        // BCrypt加密密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getRealName());
        user.setCreditScore(CreditConstant.INITIAL_SCORE);
        user.setStatus(1);
        user.setIsAdmin(0);
        user.setGender(0);

        userMapper.insert(user);
    }

    @Override
    public String login(LoginRequest request) {
        // 检查账号是否被锁定（Redis 不可用时 fail-closed）
        String lockKey = LOGIN_FAIL_KEY + request.getAccount();
        try {
            String failCountStr = redisTemplate.opsForValue().get(lockKey);
            if (failCountStr != null && Integer.parseInt(failCountStr) >= MAX_LOGIN_FAIL) {
                throw new BusinessException("登录失败次数过多，账号已锁定" + LOCK_MINUTES + "分钟，请稍后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis 不可用时采取 fail-closed：拒绝登录而不是跳过锁定检查
            log.error("Redis不可用，登录安全检查异常", e);
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        // 根据用户名、学号或手机号查询用户（昵称可重复，不作为登录凭证）
        // selectList 取第一条，避免跨字段碰撞时 selectOne 抛 TooManyResultsException
        List<User> matchedUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getAccount())
                        .or()
                        .eq(User::getStudentNo, request.getAccount())
                        .or()
                        .eq(User::getPhone, request.getAccount())
                        .last("LIMIT 1")
        );
        User user = matchedUsers.isEmpty() ? null : matchedUsers.get(0);
        if (user == null) {
            incrementLoginFail(lockKey);
            // 安全：统一文案，避免通过报错差异枚举账号是否存在
            throw new BusinessException("账号或密码错误");
        }

        // 校验用户是否被封禁
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被封禁");
        }

        // BCrypt校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            incrementLoginFail(lockKey);
            throw new BusinessException("账号或密码错误");
        }

        // 登录成功，清除失败计数
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            // Redis不可用，跳过
        }

        // 生成JWT Token
        return jwtUtils.generateToken(user.getId(), user.getStudentNo());
    }

    /**
     * 增加登录失败计数（Redis不可用时静默跳过）
     * 每次失败都续期锁定时间（从最后一次失败起算）
     */
    private void incrementLoginFail(String lockKey) {
        try {
            Long count = redisTemplate.opsForValue().increment(lockKey);
            if (count != null) {
                redisTemplate.expire(lockKey, LOCK_MINUTES, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("Redis不可用，登录失败计数异常", e);
        }
    }

    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToUserVO(user);
    }

    /**
     * 将User实体转换为UserVO
     * Demo 范围：postCount/applyCount 待 post/apply 模块迁移后接入
     */
    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setStudentNo(user.getStudentNo());
        vo.setPhone(user.getPhone());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setSignature(user.getSignature());
        vo.setCollege(user.getCollege());
        vo.setGender(user.getGender());
        vo.setCreditScore(user.getCreditScore());
        vo.setCreditLevel(creditService.getCreditLevel(user.getCreditScore()));
        vo.setCreditLabel(creditService.getCreditLabel(user.getCreditScore()));
        vo.setStatus(user.getStatus());
        vo.setCreatedTime(user.getCreatedTime());
        return vo;
    }
}
