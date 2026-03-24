package com.example.flowable.controller;

import com.example.flowable.common.Result;
import com.example.flowable.dto.ProcessDefinitionDto;
import com.example.flowable.dto.ProcessInstanceDto;
import com.example.flowable.dto.TaskDto;
import com.example.flowable.service.ProcessService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 流程控制器
 */
@RestController
@RequestMapping("/process")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;

    // ==================== 流程定义相关 ====================

    /**
     * 部署流程定义（上传文件）
     */
    @PostMapping("/deploy")
    public Result<Void> deploy(@RequestParam String name, @RequestParam MultipartFile file) {
        processService.deploy(name, file);
        return Result.success();
    }

    /**
     * 部署流程定义（XML字符串）
     */
    @PostMapping("/deploy/xml")
    public Result<Void> deployByXml(@RequestParam String name, @RequestBody String xml) {
        processService.deployByXml(name, xml);
        return Result.success();
    }

    /**
     * 获取流程定义列表
     */
    @GetMapping("/definition/list")
    public Result<List<ProcessDefinitionDto>> getProcessDefinitionList() {
        return Result.success(processService.getProcessDefinitionList());
    }

    /**
     * 删除流程定义
     */
    @DeleteMapping("/definition/{deploymentId}")
    public Result<Void> deleteDeployment(@PathVariable String deploymentId,
                                          @RequestParam(defaultValue = "false") boolean cascade) {
        processService.deleteDeployment(deploymentId, cascade);
        return Result.success();
    }

    /**
     * 获取流程定义XML
     */
    @GetMapping("/definition/xml")
    public Result<String> getProcessDefinitionXml(@RequestParam String processDefinitionId) {
        return Result.success(processService.getProcessDefinitionXml(processDefinitionId));
    }

    /**
     * 获取流程定义模型
     */
    @GetMapping("/definition/model")
    public Result<BpmnModel> getBpmnModel(@RequestParam String processDefinitionId) {
        return Result.success(processService.getBpmnModel(processDefinitionId));
    }

    // ==================== 流程实例相关 ====================

    /**
     * 启动流程实例
     */
    @PostMapping("/instance/start")
    public Result<ProcessInstanceDto> startProcess(@RequestBody Map<String, Object> params) {
        String processDefinitionKey = (String) params.get("processDefinitionKey");
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) params.get("variables");
        return Result.success(processService.startProcess(processDefinitionKey, variables));
    }

    /**
     * 获取流程实例列表
     */
    @GetMapping("/instance/list")
    public Result<List<ProcessInstanceDto>> getProcessInstanceList(
            @RequestParam(required = false) String processDefinitionKey) {
        return Result.success(processService.getProcessInstanceList(processDefinitionKey));
    }

    /**
     * 删除流程实例
     */
    @DeleteMapping("/instance/{processInstanceId}")
    public Result<Void> deleteProcessInstance(@PathVariable String processInstanceId,
                                               @RequestParam(defaultValue = "管理员删除") String reason) {
        processService.deleteProcessInstance(processInstanceId, reason);
        return Result.success();
    }

    /**
     * 获取流程图
     */
    @GetMapping(value = "/diagram/{processInstanceId}", produces = MediaType.IMAGE_PNG_VALUE)
    public void getProcessDiagram(@PathVariable String processInstanceId, HttpServletResponse response) {
        try {
            InputStream inputStream = processService.getProcessDiagram(processInstanceId);
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
            IOUtils.copy(inputStream, response.getOutputStream());
            inputStream.close();
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw new RuntimeException("获取流程图失败", e);
        }
    }

    // ==================== 任务相关 ====================

    /**
     * 获取待办任务列表
     */
    @GetMapping("/task/todo")
    public Result<List<TaskDto>> getTodoTaskList(@RequestParam String assignee) {
        return Result.success(processService.getTodoTaskList(assignee));
    }

    /**
     * 获取已完成任务列表
     */
    @GetMapping("/task/done")
    public Result<List<TaskDto>> getDoneTaskList(@RequestParam String assignee) {
        return Result.success(processService.getDoneTaskList(assignee));
    }

    /**
     * 完成任务
     */
    @PostMapping("/task/complete")
    public Result<Void> completeTask(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) params.get("variables");
        processService.completeTask(taskId, variables);
        return Result.success();
    }

    /**
     * 获取流程历史节点
     */
    @GetMapping("/history/activities/{processInstanceId}")
    public Result<List<Map<String, Object>>> getHistoryActivities(@PathVariable String processInstanceId) {
        return Result.success(processService.getHistoryActivities(processInstanceId));
    }

    /**
     * 回退到指定节点
     */
    @PostMapping("/task/rollback")
    public Result<Void> rollback(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        String targetActivityId = (String) params.get("targetActivityId");
        String reason = (String) params.get("reason");
        processService.rollback(taskId, targetActivityId, reason);
        return Result.success();
    }

    /**
     * 设置流程变量
     */
    @PostMapping("/instance/variables/{processInstanceId}")
    public Result<Void> setProcessVariables(@PathVariable String processInstanceId,
                                             @RequestBody Map<String, Object> variables) {
        processService.setProcessVariables(processInstanceId, variables);
        return Result.success();
    }

    /**
     * 更新流程定义（重新部署新版本）
     */
    @PutMapping("/definition")
    public Result<Void> updateProcessDefinition(@RequestParam String processDefinitionId,
                                                 @RequestBody String xml) {
        processService.updateProcessDefinition(processDefinitionId, xml);
        return Result.success();
    }
}
