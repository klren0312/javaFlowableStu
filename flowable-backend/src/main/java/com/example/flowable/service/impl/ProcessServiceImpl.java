package com.example.flowable.service.impl;

import com.example.flowable.dto.ProcessDefinitionDto;
import com.example.flowable.dto.ProcessInstanceDto;
import com.example.flowable.dto.TaskDto;
import com.example.flowable.entity.SysUser;
import com.example.flowable.service.ProcessService;
import com.example.flowable.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 流程服务实现
 */
@Service
@RequiredArgsConstructor
public class ProcessServiceImpl implements ProcessService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final ProcessEngine processEngine;
    private final SysUserService sysUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deploy(String name, MultipartFile file) {
        try {
            repositoryService.createDeployment()
                    .name(name)
                    .addInputStream(file.getOriginalFilename(), file.getInputStream())
                    .deploy();
        } catch (IOException e) {
            throw new RuntimeException("部署流程定义失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deployByXml(String name, String xml) {
        repositoryService.createDeployment()
                .name(name)
                .addString(name + ".bpmn20.xml", xml)
                .deploy();
    }

    @Override
    public List<ProcessDefinitionDto> getProcessDefinitionList() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion()
                .desc()
                .list();

        List<ProcessDefinitionDto> result = new ArrayList<>();
        for (ProcessDefinition pd : processDefinitions) {
            ProcessDefinitionDto dto = new ProcessDefinitionDto();
            dto.setId(pd.getId());
            dto.setKey(pd.getKey());
            dto.setName(pd.getName());
            dto.setVersion(pd.getVersion());
            dto.setDeploymentId(pd.getDeploymentId());
            dto.setDescription(pd.getDescription());
            dto.setSuspended(pd.isSuspended());

            // 获取部署时间
            Deployment deployment = repositoryService.createDeploymentQuery()
                    .deploymentId(pd.getDeploymentId())
                    .singleResult();
            if (deployment != null) {
                dto.setDeploymentTime(deployment.getDeploymentTime());
            }

            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeployment(String deploymentId, boolean cascade) {
        repositoryService.deleteDeployment(deploymentId, cascade);
    }

    @Override
    public String getProcessDefinitionXml(String processDefinitionId) {
        // 先检查流程定义是否存在
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        
        if (processDefinition == null) {
            throw new RuntimeException("流程定义不存在，可能已被删除。流程定义ID: " + processDefinitionId);
        }
        
        // 从部署资源获取XML
        InputStream inputStream = repositoryService.getResourceAsStream(
                processDefinition.getDeploymentId(),
                processDefinition.getResourceName()
        );
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("获取流程定义XML失败", e);
        }
    }

    @Override
    public BpmnModel getBpmnModel(String processDefinitionId) {
        // 先检查流程定义是否存在
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        
        if (processDefinition == null) {
            throw new RuntimeException("流程定义不存在，可能已被删除。流程定义ID: " + processDefinitionId);
        }
        
        return repositoryService.getBpmnModel(processDefinitionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstanceDto startProcess(String processDefinitionKey, Map<String, Object> variables) {
        // 确保变量不为null
        if (variables == null) {
            variables = new HashMap<>();
        }
        
        // 如果有申请人信息，自动查找部门经理和人事经理
        if (variables.containsKey("applicant")) {
            String applicant = (String) variables.get("applicant");
            SysUser applicantUser = sysUserService.getByUsername(applicant);
            
            if (applicantUser != null) {
                // 自动注入部门经理
                if (!variables.containsKey("deptManager")) {
                    String deptManager = sysUserService.getDeptManagerUsername(applicantUser.getDepartmentId());
                    // 如果找不到部门经理，使用默认值（第一个部门经理）
                    if (deptManager == null) {
                        deptManager = "zhangsan"; // 默认部门经理
                    }
                    variables.put("deptManager", deptManager);
                }
                
                // 自动注入人事经理
                if (!variables.containsKey("hrManager")) {
                    String hrManager = sysUserService.getHrManagerUsername();
                    // 如果找不到人事经理，使用默认值
                    if (hrManager == null) {
                        hrManager = "qianqi"; // 默认人事经理
                    }
                    variables.put("hrManager", hrManager);
                }
            } else {
                // 申请人不存在，使用默认值
                if (!variables.containsKey("deptManager")) {
                    variables.put("deptManager", "zhangsan");
                }
                if (!variables.containsKey("hrManager")) {
                    variables.put("hrManager", "qianqi");
                }
            }
        } else {
            // 没有申请人信息，使用默认值
            if (!variables.containsKey("deptManager")) {
                variables.put("deptManager", "zhangsan");
            }
            if (!variables.containsKey("hrManager")) {
                variables.put("hrManager", "qianqi");
            }
        }
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
        return convertToProcessInstanceDto(processInstance);
    }

    @Override
    public List<ProcessInstanceDto> getProcessInstanceList(String processDefinitionKey) {
        List<ProcessInstance> processInstances;
        if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
            processInstances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .orderByProcessInstanceId()
                    .desc()
                    .list();
        } else {
            processInstances = runtimeService.createProcessInstanceQuery()
                    .orderByProcessInstanceId()
                    .desc()
                    .list();
        }

        List<ProcessInstanceDto> result = new ArrayList<>();
        for (ProcessInstance pi : processInstances) {
            result.add(convertToProcessInstanceDto(pi));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessInstance(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
    }

    @Override
    public List<TaskDto> getTodoTaskList(String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime()
                .desc()
                .list();

        List<TaskDto> result = new ArrayList<>();
        for (Task task : tasks) {
            result.add(convertToTaskDto(task));
        }
        return result;
    }

    @Override
    public List<TaskDto> getDoneTaskList(String assignee) {
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(assignee)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();

        List<TaskDto> result = new ArrayList<>();
        for (HistoricTaskInstance task : historicTasks) {
            TaskDto dto = new TaskDto();
            dto.setId(task.getId());
            dto.setName(task.getName());
            dto.setDescription(task.getDescription());
            dto.setTaskDefinitionKey(task.getTaskDefinitionKey());
            dto.setProcessInstanceId(task.getProcessInstanceId());
            dto.setProcessDefinitionId(task.getProcessDefinitionId());
            dto.setCreateTime(task.getCreateTime());
            dto.setEndTime(task.getEndTime());

            // 处理 assignee，如果是表达式则从历史流程变量中获取实际值
            String taskAssignee = task.getAssignee();
            if (taskAssignee != null && taskAssignee.startsWith("${") && taskAssignee.endsWith("}")) {
                // 提取变量名，如 ${hrManager} -> hrManager
                String variableName = taskAssignee.substring(2, taskAssignee.length() - 1);
                // 从历史变量中获取值
                Object variableValue = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(task.getProcessInstanceId())
                        .variableName(variableName)
                        .singleResult();
                if (variableValue != null) {
                    taskAssignee = variableValue.toString();
                }
            }
            dto.setAssignee(taskAssignee);

            // 获取流程定义名称
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            if (pd != null) {
                dto.setProcessDefinitionName(pd.getName());
            }

            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(String taskId, Map<String, Object> variables) {
        taskService.complete(taskId, variables);
    }

    @Override
    public void setProcessVariables(String processInstanceId, Map<String, Object> variables) {
        runtimeService.setVariables(processInstanceId, variables);
    }

    @Override
    public List<Map<String, Object>> getHistoryActivities(String processInstanceId) {
        List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityType("userTask")
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (HistoricActivityInstance activity : activityInstances) {
            Map<String, Object> map = new HashMap<>();
            map.put("activityId", activity.getActivityId());
            map.put("activityName", activity.getActivityName());
            map.put("activityType", activity.getActivityType());
            map.put("assignee", activity.getAssignee());
            map.put("startTime", activity.getStartTime());
            map.put("endTime", activity.getEndTime());
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollback(String taskId, String targetActivityId, String reason) {
        // 获取当前任务
        Task currentTask = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (currentTask == null) {
            throw new RuntimeException("任务不存在");
        }

        String processInstanceId = currentTask.getProcessInstanceId();

        // 添加审批意见
        taskService.addComment(taskId, processInstanceId, "回退", reason);

        // 获取当前活动节点ID
        String currentActivityId = currentTask.getTaskDefinitionKey();

        // 使用 Flowable API 实现跳转
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(currentActivityId, targetActivityId)
                .changeState();
    }

    @Override
    public InputStream getProcessDiagram(String processInstanceId) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        HistoricProcessInstance hpi = null;
        if (pi == null) {
            // 查询历史流程实例
            hpi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (hpi == null) {
                throw new RuntimeException("流程实例不存在");
            }
        }

        // 获取流程定义
        String processDefinitionId = pi != null ? pi.getProcessDefinitionId() : hpi.getProcessDefinitionId();

        // 检查流程定义是否存在
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        
        if (processDefinition == null) {
            throw new RuntimeException("流程定义不存在，可能已被删除。流程定义ID: " + processDefinitionId);
        }

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

        // 获取当前活动节点
        List<String> activeActivityIds = new ArrayList<>();
        if (pi != null) {
            activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
        }

        // 生成流程图
        ProcessDiagramGenerator diagramGenerator = processEngine.getProcessEngineConfiguration()
                .getProcessDiagramGenerator();
        
        return diagramGenerator.generateDiagram(
                bpmnModel,
                "png",
                activeActivityIds,
                Collections.emptyList(),
                "宋体",
                "宋体",
                "宋体",
                null,
                1.0,
                true
        );
    }

    /**
     * 转换ProcessInstance为DTO
     */
    private ProcessInstanceDto convertToProcessInstanceDto(ProcessInstance pi) {
        ProcessInstanceDto dto = new ProcessInstanceDto();
        dto.setId(pi.getId());
        dto.setProcessDefinitionId(pi.getProcessDefinitionId());
        dto.setProcessDefinitionKey(pi.getProcessDefinitionKey());
        dto.setProcessDefinitionName(pi.getProcessDefinitionName());
        dto.setProcessDefinitionVersion(pi.getProcessDefinitionVersion());
        dto.setStartUserId(pi.getStartUserId());
        dto.setStartTime(pi.getStartTime());
        dto.setBusinessKey(pi.getBusinessKey());
        dto.setSuspended(pi.isSuspended());
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessDefinition(String processDefinitionId, String xml) {
        // 获取原流程定义
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        
        if (processDefinition == null) {
            throw new RuntimeException("流程定义不存在");
        }
        
        // 使用相同的名称部署新版本
        String name = processDefinition.getName();
        if (name == null || name.isEmpty()) {
            name = processDefinition.getKey();
        }
        
        repositoryService.createDeployment()
                .name(name)
                .addString(name + ".bpmn20.xml", xml)
                .deploy();
    }

    /**
     * 转换Task为DTO
     */
    private TaskDto convertToTaskDto(Task task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setName(task.getName());
        dto.setDescription(task.getDescription());
        dto.setTaskDefinitionKey(task.getTaskDefinitionKey());
        dto.setProcessInstanceId(task.getProcessInstanceId());
        dto.setProcessDefinitionId(task.getProcessDefinitionId());
        dto.setCreateTime(task.getCreateTime());
        dto.setPriority(task.getPriority());
        dto.setSuspended(task.isSuspended());

        // 处理 assignee，如果是表达式则从流程变量中获取实际值
        String assignee = task.getAssignee();
        if (assignee != null && assignee.startsWith("${") && assignee.endsWith("}")) {
            // 提取变量名，如 ${hrManager} -> hrManager
            String variableName = assignee.substring(2, assignee.length() - 1);
            Object variableValue = runtimeService.getVariable(task.getExecutionId(), variableName);
            if (variableValue != null) {
                assignee = variableValue.toString();
            }
        }
        dto.setAssignee(assignee);

        // 获取流程定义名称
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();
        if (pd != null) {
            dto.setProcessDefinitionName(pd.getName());
        }

        return dto;
    }
}