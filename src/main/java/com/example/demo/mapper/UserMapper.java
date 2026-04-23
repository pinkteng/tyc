package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.dto.UserDetailDTO;
import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户ID查询用户详情（多表联查）
     *
     * @param userId 用户ID
     * @return 用户详情DTO
     */
    UserDetailDTO selectUserDetailById(@Param("userId") Long userId);

    /**
     * 查询所有用户详情列表（多表联查）
     *
     * @param params 查询参数
     * @return 用户详情列表
     */
    List<UserDetailDTO> selectUserDetailList(@Param("params") Map<String, Object> params);
}