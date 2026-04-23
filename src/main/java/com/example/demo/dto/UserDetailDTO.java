package com.example.demo.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserDetailDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String avatar;
    private String nickname;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 部门信息
    private Long deptId;
    private String deptName;
    private String deptCode;

    // 角色信息
    private Long roleId;
    private String roleName;
    private String roleCode;
}
