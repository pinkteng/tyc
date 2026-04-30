package com.example.demo.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.Result;
import com.example.demo.common.ResultCode;
import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserDetailDTO;
import com.example.demo.entity.User;
import com.example.demo.entity.UserInfo;
import com.example.demo.mapper.UserInfoMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.UserService;
import com.example.demo.utils.RedisCache;
import com.example.demo.vo.UserDetailVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private RedisCache redisCache;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Autowired
    private JwtUtil jwtUtil;

    // Redis缓存键前缀
    private static final String USER_DETAIL_KEY_PREFIX = "user:detail:";
    private static final String USER_DETAIL_LIST_KEY = "user:detail:list";
    // 缓存过期时间（分钟）
    private static final long CACHE_EXPIRE_TIME = 30;

    // StringRedisTemplate 缓存键前缀
    private static final String CACHE_KEY_PREFIX = "user:detail:";

    @Override
    public Result<String> register(UserDTO userDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, userDTO.getUsername());
        User exist = getOne(wrapper);

        if (exist != null) {
            return Result.error(ResultCode.USER_HAS_EXISTED);
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(userDTO.getPassword());
        save(user);

        return Result.success("注册成功！");
    }

    @Override
    public Result<String> login(UserDTO userDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, userDTO.getUsername());
        User user = getOne(wrapper);

        if (user == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }

        // 验证密码
        if (!user.getPassword().equals(userDTO.getPassword())) {
            return Result.error(ResultCode.PARAM_ERROR);
        }

        // 生成 JWT Token
        String jwt = jwtUtil.generateToken(userDTO.getUsername());
        return Result.success(jwt);
    }

    @Override
    public Result<User> getUserById(Long id) {
        User user = getById(id);
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        return Result.success(user);
    }

    @Override
    public Result<Object> getUserPage(Integer pageNum, Integer pageSize) {
        Page<User> pageParam = new Page<>(pageNum, pageSize);
        Page<User> resultPage = baseMapper.selectPage(pageParam, null);
        return Result.success(resultPage);
    }

    // ==================== Redis缓存 + 多表联查方法实现 ====================

    @Override
    public Result<UserDetailDTO> getUserDetailById(Long id) {
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR);
        }

        String cacheKey = USER_DETAIL_KEY_PREFIX + id;

        // 1. 先查询Redis缓存
        UserDetailDTO userDetail = redisCache.getCacheObject(cacheKey);
        if (userDetail != null) {
            log.info("从Redis缓存中获取用户详情，用户ID: {}", id);
            return Result.success(userDetail);
        }

        // 2. 缓存未命中，查询数据库（多表联查）
        log.info("缓存未命中，从数据库查询用户详情，用户ID: {}", id);
        userDetail = baseMapper.selectUserDetailById(id);

        if (userDetail == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }

        // 3. 将查询结果存入Redis缓存
        redisCache.setCacheObject(cacheKey, userDetail, (int) CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        log.info("用户详情已存入Redis缓存，用户ID: {}", id);

        return Result.success(userDetail);
    }

    @Override
    public Result<List<UserDetailDTO>> getUserDetailList(Map<String, Object> params) {
        // 构建缓存键，根据参数生成
        String cacheKey = buildListCacheKey(params);

        // 1. 先查询Redis缓存
        List<UserDetailDTO> userDetailList = redisCache.getCacheObject(cacheKey);
        if (userDetailList != null) {
            log.info("从Redis缓存中获取用户详情列表");
            return Result.success(userDetailList);
        }

        // 2. 缓存未命中，查询数据库（多表联查）
        log.info("缓存未命中，从数据库查询用户详情列表");
        userDetailList = baseMapper.selectUserDetailList(params);

        // 3. 将查询结果存入Redis缓存
        redisCache.setCacheObject(cacheKey, userDetailList, (int) CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        log.info("用户详情列表已存入Redis缓存，共{}条数据", userDetailList.size());

        return Result.success(userDetailList);
    }

    @Override
    public void clearUserDetailCache(Long id) {
        String cacheKey = USER_DETAIL_KEY_PREFIX + id;
        boolean deleted = redisCache.deleteObject(cacheKey);
        if (deleted) {
            log.info("已清除用户详情缓存，用户ID: {}", id);
        }
    }

    @Override
    public void clearAllUserDetailCache() {
        // 这里简化处理，实际项目中可以使用Redis的keys命令或scan命令批量删除
        log.info("清除所有用户详情缓存");
        // 注意：生产环境建议使用Redis的批量删除策略
    }

    /**
     * 构建列表缓存键
     */
    private String buildListCacheKey(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return USER_DETAIL_LIST_KEY + ":all";
        }
        StringBuilder keyBuilder = new StringBuilder(USER_DETAIL_LIST_KEY);
        params.forEach((k, v) -> {
            if (v != null && StrUtil.isNotBlank(v.toString())) {
                keyBuilder.append(":").append(k).append("=").append(v);
            }
        });
        return keyBuilder.toString();
    }

    // ==================== StringRedisTemplate + JSON 缓存方式实现 ====================

    @Override
    public Result<UserDetailVO> getUserDetail(Long userId) {
        String key = CACHE_KEY_PREFIX + userId;

        // 1. 先查缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json != null && !json.isBlank()) {
            try {
                UserDetailVO cacheVO = JSONUtil.toBean(json, UserDetailVO.class);
                log.info("从Redis缓存中获取用户详情，用户ID: {}", userId);
                return Result.success(cacheVO);
            } catch (Exception e) {
                // 缓存数据异常，删掉脏缓存，继续查数据库
                log.warn("缓存数据异常，删除脏缓存，用户ID: {}", userId);
                stringRedisTemplate.delete(key);
            }
        }

        // 2. 查数据库（多表联查）
        log.info("缓存未命中，从数据库查询用户详情，用户ID: {}", userId);
        UserDetailVO detail = userInfoMapper.getUserDetail(userId);

        if (detail == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }

        // 3. 写缓存
        stringRedisTemplate.opsForValue().set(
                key,
                JSONUtil.toJsonStr(detail),
                10,
                TimeUnit.MINUTES);
        log.info("用户详情已存入Redis缓存，用户ID: {}", userId);

        return Result.success(detail);
    }

    @Override
    @Transactional
    public Result<String> updateUserInfo(UserInfo userInfo) {
        // 参数校验，userInfo不能为空，并且userId不能为空
        if (userInfo == null || userInfo.getUserId() == null) {
            return Result.error(ResultCode.PARAM_ERROR);
        }

        Long userId = userInfo.getUserId();

        // 1. 先查询是否存在
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUserId, userId);
        UserInfo existInfo = userInfoMapper.selectOne(wrapper);

        if (existInfo == null) {
            // 不存在则插入
            userInfoMapper.insert(userInfo);
            log.info("插入用户扩展信息，用户ID: {}", userId);
        } else {
            // 存在则更新
            userInfo.setId(existInfo.getId());
            userInfoMapper.updateById(userInfo);
            log.info("更新用户扩展信息，用户ID: {}", userId);
        }

        // 2. 删除缓存（Cache-Aside策略）
        String key = CACHE_KEY_PREFIX + userId;
        stringRedisTemplate.delete(key);
        log.info("删除用户详情缓存，用户ID: {}", userId);

        return Result.success("更新成功");
    }

    @Override
    @Transactional
    public Result<String> deleteUser(Long userId) {
        if (userId == null) {
            return Result.error(ResultCode.PARAM_ERROR);
        }

        // 1. 先删除用户基础信息
        boolean removed = removeById(userId);
        if (!removed) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }

        // 2. 删除用户扩展信息
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUserId, userId);
        userInfoMapper.delete(wrapper);
        log.info("删除用户扩展信息，用户ID: {}", userId);

        // 3. 删除缓存（Cache-Aside策略）
        String key = CACHE_KEY_PREFIX + userId;
        stringRedisTemplate.delete(key);
        log.info("删除用户详情缓存，用户ID: {}", userId);

        return Result.success("删除成功");
    }
}