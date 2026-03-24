package com.example.flowable.service;

import com.example.flowable.entity.FormDefinition;
import com.example.flowable.entity.FormInstance;
import com.example.flowable.entity.ProcessFormRelation;

import java.util.List;
import java.util.Map;

/**
 * 表单服务接口
 */
public interface FormService {

    // ==================== 表单定义相关 ====================

    /**
     * 创建表单定义
     */
    FormDefinition createForm(FormDefinition formDefinition);

    /**
     * 更新表单定义
     */
    FormDefinition updateForm(FormDefinition formDefinition);

    /**
     * 删除表单定义
     */
    void deleteForm(Long id);

    /**
     * 根据ID获取表单定义
     */
    FormDefinition getFormById(Long id);

    /**
     * 根据formKey获取表单定义
     */
    FormDefinition getFormByKey(String formKey);

    /**
     * 获取所有表单定义列表
     */
    List<FormDefinition> getFormList();

    // ==================== 流程表单关联相关 ====================

    /**
     * 绑定表单到流程
     */
    void bindFormToProcess(ProcessFormRelation relation);

    /**
     * 解绑流程表单
     */
    void unbindForm(Long id);

    /**
     * 获取流程的启动表单
     */
    FormDefinition getStartForm(String processDefinitionKey);

    /**
     * 获取任务节点的表单
     */
    FormDefinition getTaskForm(String processDefinitionKey, String nodeId);

    /**
     * 获取流程的所有表单绑定关系
     */
    List<ProcessFormRelation> getProcessFormRelations(String processDefinitionKey);

    // ==================== 表单实例相关 ====================

    /**
     * 保存表单实例数据
     */
    void saveFormInstance(FormInstance formInstance);

    /**
     * 获取流程实例的表单数据
     */
    FormInstance getFormInstanceByProcessInstanceId(String processInstanceId);

    /**
     * 获取任务的表单数据
     */
    FormInstance getFormInstanceByTaskId(String taskId);
}