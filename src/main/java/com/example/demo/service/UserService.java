package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserDetailDTO;
import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.entity.UserInfo;
import com.example.demo.vo.UserDetailVO;

import java.util.List;
import java.util.Map;

public interface UserService extends IService<User> {
    Result<String> register(UserDTO userDTO);
    Result<String> login(UserDTO userDTO);
    Result<User> getUserById(Long id);
    Result<Object> getUserPage(Integer pageNum, Integer pageSize);

    // ==================== Redis缓存 + 多表联查方法 ====================

    /**
     * 根据ID获取用户详情（带Redis缓存）
     *
     * @param id 用户ID
     * @return 用户详情
     */
    Result<UserDetailDTO> getUserDetailById(Long id);

    /**
     * 获取用户详情列表（带Redis缓存）
     *
     * @param params 查询参数
     * @return 用户详情列表
     */
    Result<List<UserDetailDTO>> getUserDetailList(Map<String, Object> params);

    /**
     * 清除用户详情缓存
     *
     * @param id 用户ID
     */
    void clearUserDetailCache(Long id);

    /**
     * 清除所有用户详情缓存
     */
    void clearAllUserDetailCache();

    // ==================== StringRedisTemplate + JSON 缓存方式 ====================

    /**
     * 根据ID获取用户详情（使用StringRedisTemplate + JSON缓存）
     *
     * @param userId 用户ID
     * @return 用户详情VO
     */
    Result<UserDetailVO> getUserDetail(Long userId);

    /**
     * 更新用户扩展信息（Cache-Aside策略：先更新DB，再删除缓存）
     *
     * @param userInfo 用户信息
     * @return 操作结果
     */
    Result<String> updateUserInfo(UserInfo userInfo);

    /**
     * 删除用户（Cache-Aside策略：先删除DB，再删除缓存）
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    Result<String> deleteUser(Long userId);
}