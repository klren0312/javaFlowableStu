package com.example.flowable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.flowable.entity.SysRole;
import com.example.flowable.entity.SysUserRole;
import com.example.flowable.mapper.SysRoleMapper;
import com.example.flowable.mapper.SysUserRoleMapper;
import com.example.flowable.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysUserRoleMapper userRoleMapper;

    @Override
    public SysRole getByCode(String code) {
        return this.lambdaQuery()
                .eq(SysRole::getCode, code)
                .one();
    }

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        return this.lambdaQuery()
                .in(SysRole::getId, roleIds)
                .list();
    }
}