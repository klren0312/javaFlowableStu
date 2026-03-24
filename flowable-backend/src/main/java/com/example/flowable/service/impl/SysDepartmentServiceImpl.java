package com.example.flowable.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.flowable.entity.SysDepartment;
import com.example.flowable.mapper.SysDepartmentMapper;
import com.example.flowable.service.SysDepartmentService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门服务实现
 */
@Service
public class SysDepartmentServiceImpl extends ServiceImpl<SysDepartmentMapper, SysDepartment> implements SysDepartmentService {

    @Override
    public List<SysDepartment> getDepartmentTree() {
        // 查询所有部门，按排序字段排序
        return this.lambdaQuery()
                .orderByAsc(SysDepartment::getSort)
                .list();
    }

    @Override
    public List<SysDepartment> getChildren(Long parentId) {
        return this.lambdaQuery()
                .eq(SysDepartment::getParentId, parentId)
                .orderByAsc(SysDepartment::getSort)
                .list();
    }
}