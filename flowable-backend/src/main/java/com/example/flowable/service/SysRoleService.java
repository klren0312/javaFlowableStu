package com.example.flowable.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.flowable.entity.SysRole;

import java.util.List;

/**
 * 角色服务接口
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 根据角色编码查询角色
     */
    SysRole getByCode(String code);

    /**
     * 根据用户ID查询角色列表
     */
    List<SysRole> getRolesByUserId(Long userId);
}