package com.example.flowable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.flowable.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}