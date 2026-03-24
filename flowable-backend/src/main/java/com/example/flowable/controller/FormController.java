package com.example.flowable.controller;

import com.example.flowable.common.Result;
import com.example.flowable.entity.FormDefinition;
import com.example.flowable.entity.FormInstance;
import com.example.flowable.entity.ProcessFormRelation;
import com.example.flowable.service.FormService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表单控制器
 */
@RestController
@RequestMapping("/form")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;

    // ==================== 表单定义相关 ====================

    /**
     * 创建表单定义
     */
    @PostMapping
    public Result<FormDefinition> createForm(@RequestBody FormDefinition formDefinition) {
        return Result.success(formService.createForm(formDefinition));
    }

    /**
     * 更新表单定义
     */
    @PutMapping
    public Result<FormDefinition> updateForm(@RequestBody FormDefinition formDefinition) {
        return Result.success(formService.updateForm(formDefinition));
    }

    /**
     * 删除表单定义
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteForm(@PathVariable Long id) {
        formService.deleteForm(id);
        return Result.success();
    }

    /**
     * 根据ID获取表单定义
     */
    @GetMapping("/{id}")
    public Result<FormDefinition> getFormById(@PathVariable Long id) {
        return Result.success(formService.getFormById(id));
    }

    /**
     * 根据formKey获取表单定义
     */
    @GetMapping("/key/{formKey}")
    public Result<FormDefinition> getFormByKey(@PathVariable String formKey) {
        return Result.success(formService.getFormByKey(formKey));
    }

    /**
     * 获取所有表单定义列表
     */
    @GetMapping("/list")
    public Result<List<FormDefinition>> getFormList() {
        return Result.success(formService.getFormList());
    }

    // ==================== 流程表单关联相关 ====================

    /**
     * 绑定表单到流程
     */
    @PostMapping("/bind")
    public Result<Void> bindFormToProcess(@RequestBody ProcessFormRelation relation) {
        formService.bindFormToProcess(relation);
        return Result.success();
    }

    /**
     * 解绑流程表单
     */
    @DeleteMapping("/unbind/{id}")
    public Result<Void> unbindForm(@PathVariable Long id) {
        formService.unbindForm(id);
        return Result.success();
    }

    /**
     * 获取流程的启动表单
     */
    @GetMapping("/start-form/{processDefinitionKey}")
    public Result<FormDefinition> getStartForm(@PathVariable String processDefinitionKey) {
        return Result.success(formService.getStartForm(processDefinitionKey));
    }

    /**
     * 获取任务节点的表单
     */
    @GetMapping("/task-form")
    public Result<FormDefinition> getTaskForm(
            @RequestParam String processDefinitionKey,
            @RequestParam String nodeId) {
        return Result.success(formService.getTaskForm(processDefinitionKey, nodeId));
    }

    /**
     * 获取流程的所有表单绑定关系
     */
    @GetMapping("/relations/{processDefinitionKey}")
    public Result<List<ProcessFormRelation>> getProcessFormRelations(@PathVariable String processDefinitionKey) {
        return Result.success(formService.getProcessFormRelations(processDefinitionKey));
    }

    // ==================== 表单实例相关 ====================

    /**
     * 保存表单实例数据
     */
    @PostMapping("/instance")
    public Result<Void> saveFormInstance(@RequestBody FormInstance formInstance) {
        formService.saveFormInstance(formInstance);
        return Result.success();
    }

    /**
     * 获取流程实例的表单数据
     */
    @GetMapping("/instance/process/{processInstanceId}")
    public Result<FormInstance> getFormInstanceByProcessInstanceId(@PathVariable String processInstanceId) {
        return Result.success(formService.getFormInstanceByProcessInstanceId(processInstanceId));
    }

    /**
     * 获取任务的表单数据
     */
    @GetMapping("/instance/task/{taskId}")
    public Result<FormInstance> getFormInstanceByTaskId(@PathVariable String taskId) {
        return Result.success(formService.getFormInstanceByTaskId(taskId));
    }
}