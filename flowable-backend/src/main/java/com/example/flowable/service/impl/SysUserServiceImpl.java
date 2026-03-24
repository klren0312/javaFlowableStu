package com.example.flowable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.flowable.entity.SysDepartment;
import com.example.flowable.entity.SysRole;
import com.example.flowable.entity.SysUser;
import com.example.flowable.entity.SysUserRole;
import com.example.flowable.mapper.SysDepartmentMapper;
import com.example.flowable.mapper.SysRoleMapper;
import com.example.flowable.mapper.SysUserMapper;
import com.example.flowable.mapper.SysUserRoleMapper;
import com.example.flowable.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysDepartmentMapper departmentMapper;

    @Override
    public SysUser getByUsername(String username) {
        return this.lambdaQuery()
                .eq(SysUser::getUsername, username)
                .one();
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );
        return userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 删除原有角色
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        
        // 添加新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
    }

    @Override
    public String getDeptManagerUsername(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        
        // 查询部门经理角色
        SysRole deptManagerRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "DEPT_MANAGER")
        );
        if (deptManagerRole == null) {
            return null;
        }
        
        // 查询所有拥有部门经理角色的用户
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, deptManagerRole.getId())
        );
        if (userRoles.isEmpty()) {
            return null;
        }
        
        List<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .collect(Collectors.toList());
        
        // 从当前部门开始，逐级向上查找部门经理
        Long currentDeptId = departmentId;
        while (currentDeptId != null) {
            // 查询当前部门的部门经理
            SysUser deptManager = this.lambdaQuery()
                    .in(SysUser::getId, userIds)
                    .eq(SysUser::getDepartmentId, currentDeptId)
                    .one();
            
            if (deptManager != null) {
                return deptManager.getUsername();
            }
            
            // 查询上级部门
            SysDepartment dept = departmentMapper.selectById(currentDeptId);
            if (dept == null || dept.getParentId() == null || dept.getParentId() == 0) {
                break;
            }
            currentDeptId = dept.getParentId();
        }
        
        // 如果找不到，返回任意一个部门经理作为默认值
        SysUser anyManager = this.lambdaQuery()
                .in(SysUser::getId, userIds)
                .last("LIMIT 1")
                .one();
        
        return anyManager != null ? anyManager.getUsername() : null;
    }

    @Override
    public List<SysUser> getUsersByRoleCode(String roleCode) {
        // 查询角色
        SysRole role = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, roleCode)
        );
        if (role == null) {
            return Collections.emptyList();
        }
        
        // 查询拥有该角色的用户ID列表
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, role.getId())
        );
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .collect(Collectors.toList());
        
        return this.lambdaQuery()
                .in(SysUser::getId, userIds)
                .list();
    }

    @Override
    public String getHrManagerUsername() {
        // 查询人事审批角色
        SysRole hrRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "HR_APPROVER")
        );
        if (hrRole == null) {
            return null;
        }
        
        // 查询拥有人事审批角色的用户
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, hrRole.getId())
        );
        if (userRoles.isEmpty()) {
            return null;
        }
        
        // 返回第一个人事审批人员
        Long userId = userRoles.get(0).getUserId();
        SysUser hrManager = this.getById(userId);
        return hrManager != null ? hrManager.getUsername() : null;
    }
}
