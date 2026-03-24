package com.example.flowable.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.flowable.entity.SysDepartment;

import java.util.List;

/**
 * 部门服务接口
 */
public interface SysDepartmentService extends IService<SysDepartment> {

    /**
     * 获取部门树形结构
     */
    List<SysDepartment> getDepartmentTree();

    /**
     * 获取子部门列表
     */
    List<SysDepartment> getChildren(Long parentId);
}