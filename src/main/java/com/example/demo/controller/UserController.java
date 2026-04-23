package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserDetailDTO;
import com.example.demo.entity.User;
import com.example.demo.entity.UserInfo;
import com.example.demo.service.UserService;
import com.example.demo.vo.UserDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public Result<String> register(@RequestBody UserDTO userDTO) {
        return userService.register(userDTO);
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody UserDTO userDTO) {
        return userService.login(userDTO);
    }

    @GetMapping("/page")
    public Result<Object> getUserPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer pageSize) {
        return userService.getUserPage(pageNum, pageSize);
    }

    // ==================== Redis缓存 + 多表联查接口 ====================

    /**
     * 获取用户详情（带Redis缓存）
     * 使用多表联查获取用户详细信息，包括部门、角色等
     */
    @GetMapping("/detail/{id}")
    public Result<UserDetailDTO> getUserDetailById(@PathVariable Long id) {
        return userService.getUserDetailById(id);
    }

    /**
     * 获取用户详情列表（带Redis缓存）
     * 支持按用户名、状态、部门ID筛选
     */
    @GetMapping("/detail/list")
    public Result<List<UserDetailDTO>> getUserDetailList(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId) {
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("status", status);
        params.put("deptId", deptId);
        return userService.getUserDetailList(params);
    }

    /**
     * 清除指定用户的缓存
     */
    @DeleteMapping("/cache/{id}")
    public Result<String> clearUserCache(@PathVariable Long id) {
        userService.clearUserDetailCache(id);
        return Result.success("缓存清除成功");
    }

    // ==================== StringRedisTemplate + JSON 缓存接口 ====================

    /**
     * 查询用户详情（多表联查 + Redis缓存）
     * 使用StringRedisTemplate + JSON字符串缓存
     */
    @GetMapping("/{id}/detail")
    public Result<UserDetailVO> getUserDetail(@PathVariable("id") Long userId) {
        return userService.getUserDetail(userId);
    }

    /**
     * 更新用户扩展信息（Cache-Aside策略：先更新DB，再删除缓存）
     */
    @PutMapping("/{id}/detail")
    public Result<String> updateUserInfo(@PathVariable("id") Long userId,
            @RequestBody UserInfo userInfo) {
        userInfo.setUserId(userId);
        return userService.updateUserInfo(userInfo);
    }

    /**
     * 删除用户（Cache-Aside策略：先删除DB，再删除缓存）
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteUser(@PathVariable("id") Long userId) {
        return userService.deleteUser(userId);
    }

    /**
     * 根据ID获取用户基础信息
     * 注意：此方法放在最后，避免路径冲突
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}