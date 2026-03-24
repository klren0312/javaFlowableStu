package com.example.flowable.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.flowable.common.Result;
import com.example.flowable.entity.SysRole;
import com.example.flowable.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色控制器
 */
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    /**
     * 分页查询角色列表
     */
    @GetMapping("/list")
    public Result<IPage<SysRole>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String name, String code, Integer status) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), SysRole::getName, name)
                .like(StringUtils.hasText(code), SysRole::getCode, code)
                .eq(status != null, SysRole::getStatus, status)
                .orderByDesc(SysRole::getCreateTime);
        return Result.success(roleService.page(page, wrapper));
    }

    /**
     * 查询所有角色
     */
    @GetMapping("/all")
    public Result<List<SysRole>> all() {
        return Result.success(roleService.lambdaQuery()
                .eq(SysRole::getStatus, 1)
                .list());
    }

    /**
     * 根据ID查询角色
     */
    @GetMapping("/{id}")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    /**
     * 新增角色
     */
    @PostMapping
    public Result<Void> save(@RequestBody SysRole role) {
        // 检查角色编码是否已存在
        if (roleService.getByCode(role.getCode()) != null) {
            return Result.error("角色编码已存在");
        }
        roleService.save(role);
        return Result.success();
    }

    /**
     * 更新角色
     */
    @PutMapping
    public Result<Void> update(@RequestBody SysRole role) {
        roleService.updateById(role);
        return Result.success();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return Result.success();
    }
}