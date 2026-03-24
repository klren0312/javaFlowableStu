package com.example.flowable.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.flowable.entity.SysUser;

import java.util.List;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser getByUsername(String username);

    /**
     * 查询用户的角色列表
     */
    List<Long> getUserRoleIds(Long userId);

    /**
     * 分配用户角色
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 根据部门ID查询部门经理
     * @param departmentId 部门ID
     * @return 部门经理用户名，如果不存在返回null
     */
    String getDeptManagerUsername(Long departmentId);

    /**
     * 根据角色编码查询用户
     * @param roleCode 角色编码
     * @return 用户列表
     */
    List<SysUser> getUsersByRoleCode(String roleCode);

    /**
     * 获取人事审批人员用户名
     * @return 人事审批人员用户名，如果不存在返回null
     */
    String getHrManagerUsername();
}
