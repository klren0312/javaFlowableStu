package com.example.flowable.controller;

import com.example.flowable.common.Result;
import com.example.flowable.entity.SysDepartment;
import com.example.flowable.service.SysDepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门控制器
 */
@RestController
@RequestMapping("/department")
@RequiredArgsConstructor
public class SysDepartmentController {

    private final SysDepartmentService departmentService;

    /**
     * 获取所有部门列表
     */
    @GetMapping("/all")
    public Result<List<SysDepartment>> all() {
        return Result.success(departmentService.list());
    }

    /**
     * 获取部门列表
     */
    @GetMapping("/list")
    public Result<List<SysDepartment>> list() {
        return Result.success(departmentService.list());
    }

    /**
     * 获取部门树形结构
     */
    @GetMapping("/tree")
    public Result<List<SysDepartment>> tree() {
        return Result.success(departmentService.getDepartmentTree());
    }

    /**
     * 获取子部门列表
     */
    @GetMapping("/children/{parentId}")
    public Result<List<SysDepartment>> children(@PathVariable Long parentId) {
        return Result.success(departmentService.getChildren(parentId));
    }

    /**
     * 根据ID查询部门
     */
    @GetMapping("/{id}")
    public Result<SysDepartment> getById(@PathVariable Long id) {
        return Result.success(departmentService.getById(id));
    }

    /**
     * 新增部门
     */
    @PostMapping
    public Result<Void> save(@RequestBody SysDepartment department) {
        departmentService.save(department);
        return Result.success();
    }

    /**
     * 更新部门
     */
    @PutMapping
    public Result<Void> update(@RequestBody SysDepartment department) {
        departmentService.updateById(department);
        return Result.success();
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查是否有子部门
        List<SysDepartment> children = departmentService.getChildren(id);
        if (!children.isEmpty()) {
            return Result.error("存在子部门，无法删除");
        }
        departmentService.removeById(id);
        return Result.success();
    }
}