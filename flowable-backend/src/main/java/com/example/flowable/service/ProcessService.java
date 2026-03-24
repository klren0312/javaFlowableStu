package com.example.flowable.service;

import com.example.flowable.dto.ProcessDefinitionDto;
import com.example.flowable.dto.ProcessInstanceDto;
import com.example.flowable.dto.TaskDto;

import org.flowable.bpmn.model.BpmnModel;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 流程服务接口
 */
public interface ProcessService {

    /**
     * 部署流程定义
     */
    void deploy(String name, MultipartFile file);

    /**
     * 部署流程定义（通过XML字符串）
     */
    void deployByXml(String name, String xml);

    /**
     * 获取流程定义列表
     */
    List<ProcessDefinitionDto> getProcessDefinitionList();

    /**
     * 删除流程定义
     */
    void deleteDeployment(String deploymentId, boolean cascade);

    /**
     * 获取流程定义XML
     */
    String getProcessDefinitionXml(String processDefinitionId);

    /**
     * 获取BpmnModel
     */
    BpmnModel getBpmnModel(String processDefinitionId);

    /**
     * 启动流程实例
     */
    ProcessInstanceDto startProcess(String processDefinitionKey, Map<String, Object> variables);

    /**
     * 获取流程实例列表
     */
    List<ProcessInstanceDto> getProcessInstanceList(String processDefinitionKey);

    /**
     * 删除流程实例
     */
    void deleteProcessInstance(String processInstanceId, String reason);

    /**
     * 获取待办任务列表
     */
    List<TaskDto> getTodoTaskList(String assignee);

    /**
     * 获取已完成任务列表
     */
    List<TaskDto> getDoneTaskList(String assignee);

    /**
     * 完成任务
     */
    void completeTask(String taskId, Map<String, Object> variables);

    /**
     * 获取流程历史节点
     */
    List<Map<String, Object>> getHistoryActivities(String processInstanceId);

    /**
     * 回退到指定节点
     */
    void rollback(String taskId, String targetActivityId, String reason);

    /**
     * 获取流程图（高亮当前节点）
     */
    InputStream getProcessDiagram(String processInstanceId);

    /**
     * 设置流程变量
     * @param processInstanceId 流程实例ID
     * @param variables 变量Map
     */
    void setProcessVariables(String processInstanceId, Map<String, Object> variables);

    /**
     * 更新流程定义（重新部署新版本）
     * @param processDefinitionId 原流程定义ID
     * @param xml 新的XML内容
     */
    void updateProcessDefinition(String processDefinitionId, String xml);
}
