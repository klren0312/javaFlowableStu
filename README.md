# Flowable 工作流管理系统

基于 Spring Boot + Flowable + Vue3 的流程管理系统，提供完整的流程设计、部署、执行和监控功能。

## 技术栈

### 后端
- Java 17
- Spring Boot 2.7.18
- Flowable 6.8.0
- MyBatis-Plus 3.5.5
- MySQL 8.0

### 前端
- Vue 3.4
- Element Plus 2.6
- bpmn-js 17.2
- Vite 5.1

## 功能特性

### 系统管理
- 用户管理：用户的增删改查、角色分配
- 部门管理：树形结构的部门管理
- 角色管理：角色的增删改查

### 流程管理
- 流程定义：部署、删除、查看流程定义
- 流程实例：查看运行中的流程实例、查看流程图
- 待办任务：查看待办任务、完成任务
- 流程回退：支持回退到任意历史节点

### 流程设计器
- 可视化 BPMN 流程设计
- 支持流程保存、部署、下载
- 缩放、撤销等编辑功能

## 项目结构

```
java-flowable-test/
├── sql/                          # 数据库脚本
│   ├── schema.sql               # 表结构
│   └── data.sql                 # 初始数据
├── flowable-backend/            # 后端项目
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/flowable/
│       │   ├── config/          # 配置类
│       │   ├── controller/      # 控制器
│       │   ├── service/         # 服务层
│       │   ├── mapper/          # Mapper接口
│       │   ├── entity/          # 实体类
│       │   └── dto/             # DTO类
│       └── resources/
│           ├── application.yml  # 配置文件
│           └── processes/       # BPMN流程文件
└── flowable-frontend/           # 前端项目
    ├── package.json
    └── src/
        ├── api/                 # API接口
        ├── layout/              # 布局组件
        ├── router/              # 路由配置
        ├── utils/               # 工具函数
        └── views/               # 页面组件
```

## 快速开始

### 1. 数据库配置

创建数据库并执行SQL脚本：

```sql
CREATE DATABASE flowable_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

执行 `sql/schema.sql` 和 `sql/data.sql`

### 2. 启动后端

```bash
cd flowable-backend
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

### 3. 启动前端

```bash
cd flowable-frontend
npm install
npm run dev
```

前端服务将在 http://localhost:3000 启动

## 默认账号

- 用户名：admin
- 密码：123456

## API 接口

### 用户管理
- GET /api/user/list - 用户列表
- POST /api/user - 新增用户
- PUT /api/user - 更新用户
- DELETE /api/user/{id} - 删除用户

### 流程管理
- GET /api/process/definition/list - 流程定义列表
- POST /api/process/deploy - 部署流程
- POST /api/process/instance/start - 启动流程
- GET /api/process/task/todo - 待办任务
- POST /api/process/task/complete - 完成任务
- POST /api/process/task/rollback - 回退任务

## 示例流程

项目包含一个请假审批流程示例（leaveProcess），流程说明：
1. 员工提交请假申请
2. 部门经理审批
3. 如果请假超过3天，需要人事审批
4. 审批通过或拒绝后结束

## 开发说明

### 配置修改

后端配置文件：`flowable-backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/flowable_test
    username: root
    password: root
```

前端API代理配置：`flowable-frontend/vite.config.js`

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080'
    }
  }
}
```

## 后端开发指南：流程绘制后的代码实现

本章节详细介绍在前端绘制完流程图后，后端如何根据流程定义进行代码开发。

### 一、流程定义文件结构（BPMN XML）

流程绘制完成后，会生成一个 `.bpmn20.xml` 文件，该文件定义了流程的所有元素。

#### 1.1 基本结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://flowable.org/demo">

    <process id="leaveProcess" name="请假流程" isExecutable="true">
        <!-- 流程元素定义 -->
    </process>
</definitions>
```

#### 1.2 关键元素说明

| 元素 | 说明 | 关键属性 |
|------|------|----------|
| `startEvent` | 开始事件 | `id` - 节点ID |
| `endEvent` | 结束事件 | `id` - 节点ID |
| `userTask` | 用户任务 | `id`, `name`, `flowable:assignee` |
| `sequenceFlow` | 顺序流（连线） | `sourceRef`, `targetRef` |
| `exclusiveGateway` | 排他网关 | `id`, 条件表达式 |

#### 1.3 用户任务的三种分配方式

**方式一：直接指定处理人**
```xml
<userTask id="submitTask" name="提交申请" flowable:assignee="zhangsan"/>
```

**方式二：使用流程变量动态指定**
```xml
<userTask id="deptApprovalTask" name="部门审批" flowable:assignee="${deptManager}"/>
```

**方式三：使用候选用户/候选组**
```xml
<userTask id="hrApprovalTask" name="人事审批">
    <potentialOwner>
        <resourceAssignmentExpression>
            <formalExpression>${hrGroup}</formalExpression>
        </resourceAssignmentExpression>
    </potentialOwner>
</userTask>
```

#### 1.4 网关条件表达式

```xml
<!-- 排他网关：根据条件选择一条路径 -->
<exclusiveGateway id="exclusiveGateway" name="审批网关"/>

<!-- 条件分支：请假超过3天需人事审批 -->
<sequenceFlow id="flow4" sourceRef="exclusiveGateway" targetRef="hrApprovalTask">
    <conditionExpression xsi:type="tFormalExpression">
        ${approved == true && days > 3}
    </conditionExpression>
</sequenceFlow>

<!-- 条件分支：审批不通过直接结束 -->
<sequenceFlow id="flow5" sourceRef="exclusiveGateway" targetRef="endEvent">
    <conditionExpression xsi:type="tFormalExpression">
        ${approved == false}
    </conditionExpression>
</sequenceFlow>
```

**常用条件运算符：**
- `==` 等于
- `!=` 不等于
- `>` 大于
- `<` 小于
- `>=` 大于等于
- `<=` 小于等于
- `&&` 逻辑与
- `||` 逻辑或

### 二、后端核心服务注入

Flowable 提供了多个核心服务，通过依赖注入使用：

```java
@Service
@RequiredArgsConstructor
public class ProcessServiceImpl implements ProcessService {

    // 流程仓库服务 - 管理流程定义的部署、查询、删除
    private final RepositoryService repositoryService;
    
    // 运行时服务 - 启动流程实例、管理流程变量
    private final RuntimeService runtimeService;
    
    // 任务服务 - 查询任务、完成任务、添加评论
    private final TaskService taskService;
    
    // 历史服务 - 查询历史数据
    private final HistoryService historyService;
    
    // 流程引擎
    private final ProcessEngine processEngine;
}
```

### 三、流程部署

#### 3.1 方式一：通过文件上传部署

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void deploy(String name, MultipartFile file) {
    try {
        repositoryService.createDeployment()
                .name(name)                                    // 部署名称
                .addInputStream(file.getOriginalFilename(), file.getInputStream())
                .deploy();
    } catch (IOException e) {
        throw new RuntimeException("部署流程定义失败", e);
    }
}
```

#### 3.2 方式二：通过XML字符串部署

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void deployByXml(String name, String xml) {
    repositoryService.createDeployment()
            .name(name)
            .addString(name + ".bpmn20.xml", xml)            // 注意文件后缀
            .deploy();
}
```

#### 3.3 Controller 接口

```java
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
```

### 四、启动流程实例

#### 4.1 基本启动方式

```java
@Override
@Transactional(rollbackFor = Exception.class)
public ProcessInstanceDto startProcess(String processDefinitionKey, Map<String, Object> variables) {
    // processDefinitionKey 对应 BPMN XML 中 <process id="leaveProcess">
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        processDefinitionKey,  // 流程定义Key
        variables              // 流程变量
    );
    return convertToProcessInstanceDto(processInstance);
}
```

#### 4.2 传递流程变量

流程变量用于控制流程流转和动态分配处理人：

```java
// 前端传递的变量
Map<String, Object> variables = new HashMap<>();
variables.put("applicant", "lisi");           // 申请人
variables.put("deptManager", "zhangsan");     // 部门经理
variables.put("hrManager", "wangwu");         // 人事经理
variables.put("days", 5);                      // 请假天数
variables.put("approved", true);               // 审批结果

// 启动流程
processService.startProcess("leaveProcess", variables);
```

#### 4.3 自动分配处理人（业务逻辑示例）

```java
@Override
@Transactional(rollbackFor = Exception.class)
public ProcessInstanceDto startProcess(String processDefinitionKey, Map<String, Object> variables) {
    if (variables == null) {
        variables = new HashMap<>();
    }
    
    // 根据申请人自动查找部门经理和人事经理
    if (variables.containsKey("applicant")) {
        String applicant = (String) variables.get("applicant");
        SysUser applicantUser = sysUserService.getByUsername(applicant);
        
        if (applicantUser != null) {
            // 自动注入部门经理（根据申请人部门查找）
            if (!variables.containsKey("deptManager")) {
                String deptManager = sysUserService.getDeptManagerUsername(
                    applicantUser.getDepartmentId()
                );
                variables.put("deptManager", deptManager);
            }
            
            // 自动注入人事经理
            if (!variables.containsKey("hrManager")) {
                String hrManager = sysUserService.getHrManagerUsername();
                variables.put("hrManager", hrManager);
            }
        }
    }
    
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        processDefinitionKey, variables
    );
    return convertToProcessInstanceDto(processInstance);
}
```

#### 4.4 Controller 接口

```java
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
```

**请求示例：**
```json
{
    "processDefinitionKey": "leaveProcess",
    "variables": {
        "applicant": "lisi",
        "days": 5,
        "reason": "事假"
    }
}
```

### 五、任务查询与完成

#### 5.1 查询待办任务

```java
@Override
public List<TaskDto> getTodoTaskList(String assignee) {
    List<Task> tasks = taskService.createTaskQuery()
            .taskAssignee(assignee)           // 按处理人查询
            .orderByTaskCreateTime()
            .desc()
            .list();
    
    List<TaskDto> result = new ArrayList<>();
    for (Task task : tasks) {
        result.add(convertToTaskDto(task));
    }
    return result;
}
```

#### 5.2 查询候选任务（组任务）

```java
public List<Task> getGroupTasks(String userId) {
    // 查询用户所属组
    List<String> groups = getUserGroups(userId);
    
    return taskService.createTaskQuery()
            .taskCandidateUser(userId)        // 候选用户
            .taskCandidateGroupIn(groups)     // 候选组
            .list();
}
```

#### 5.3 完成任务

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void completeTask(String taskId, Map<String, Object> variables) {
    // 完成任务并传递变量
    taskService.complete(taskId, variables);
}
```

#### 5.4 完成任务并设置审批结果

```java
// Controller
@PostMapping("/task/complete")
public Result<Void> completeTask(@RequestBody Map<String, Object> params) {
    String taskId = (String) params.get("taskId");
    Map<String, Object> variables = (Map<String, Object>) params.get("variables");
    processService.completeTask(taskId, variables);
    return Result.success();
}
```

**请求示例（审批通过）：**
```json
{
    "taskId": "task-uuid",
    "variables": {
        "approved": true,
        "comment": "同意请假"
    }
}
```

**请求示例（审批不通过）：**
```json
{
    "taskId": "task-uuid",
    "variables": {
        "approved": false,
        "comment": "请假理由不充分"
    }
}
```

#### 5.5 添加任务评论

```java
// 添加审批意见
taskService.addComment(taskId, processInstanceId, "审批意见", "同意请假");
taskService.addComment(taskId, processInstanceId, "驳回原因", "理由不充分");
```

### 六、流程变量管理

#### 6.1 设置流程变量

```java
@Override
public void setProcessVariables(String processInstanceId, Map<String, Object> variables) {
    runtimeService.setVariables(processInstanceId, variables);
}
```

#### 6.2 获取流程变量

```java
// 获取单个变量
Object value = runtimeService.getVariable(executionId, "variableName");

// 获取所有变量
Map<String, Object> variables = runtimeService.getVariables(processInstanceId);

// 通过任务获取变量
Object value = taskService.getVariable(taskId, "variableName");
```

### 七、流程图生成

#### 7.1 生成流程图（高亮当前节点）

```java
@Override
public InputStream getProcessDiagram(String processInstanceId) {
    // 获取流程实例
    ProcessInstance pi = runtimeService.createProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult();
    
    // 获取流程定义
    String processDefinitionId = pi.getProcessDefinitionId();
    BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
    
    // 获取当前活动节点
    List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
    
    // 生成流程图
    ProcessDiagramGenerator diagramGenerator = processEngine.getProcessEngineConfiguration()
            .getProcessDiagramGenerator();
    
    return diagramGenerator.generateDiagram(
            bpmnModel,
            "png",                           // 图片格式
            activeActivityIds,               // 高亮节点
            Collections.emptyList(),         // 高亮连线
            "宋体",                          // 字体
            "宋体",
            "宋体",
            null,
            1.0,
            true
    );
}
```

#### 7.2 Controller 接口

```java
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
```

### 八、流程回退功能

#### 8.1 获取历史节点

```java
@Override
public List<Map<String, Object>> getHistoryActivities(String processInstanceId) {
    List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
            .processInstanceId(processInstanceId)
            .activityType("userTask")  // 只查询用户任务节点
            .orderByHistoricActivityInstanceStartTime()
            .asc()
            .list();

    List<Map<String, Object>> result = new ArrayList<>();
    for (HistoricActivityInstance activity : activityInstances) {
        Map<String, Object> map = new HashMap<>();
        map.put("activityId", activity.getActivityId());
        map.put("activityName", activity.getActivityName());
        map.put("assignee", activity.getAssignee());
        map.put("startTime", activity.getStartTime());
        map.put("endTime", activity.getEndTime());
        result.add(map);
    }
    return result;
}
```

#### 8.2 回退到指定节点

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void rollback(String taskId, String targetActivityId, String reason) {
    // 1. 获取当前任务
    Task currentTask = taskService.createTaskQuery()
            .taskId(taskId)
            .singleResult();

    if (currentTask == null) {
        throw new RuntimeException("任务不存在");
    }

    String processInstanceId = currentTask.getProcessInstanceId();

    // 2. 添加审批意见（记录回退原因）
    taskService.addComment(taskId, processInstanceId, "回退", reason);

    // 3. 获取当前活动节点ID
    String currentActivityId = currentTask.getTaskDefinitionKey();

    // 4. 使用 Flowable API 实现跳转
    runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstanceId)
            .moveActivityIdTo(currentActivityId, targetActivityId)
            .changeState();
}
```

### 九、核心API速查表

#### 9.1 RepositoryService（流程仓库服务）

| 方法 | 说明 |
|------|------|
| `createDeployment()` | 创建部署对象 |
| `createProcessDefinitionQuery()` | 查询流程定义 |
| `deleteDeployment(deploymentId, cascade)` | 删除部署 |
| `getBpmnModel(processDefinitionId)` | 获取BPMN模型 |
| `getResourceAsStream(deploymentId, resourceName)` | 获取资源流 |
| `suspendProcessDefinitionById(id)` | 挂起流程定义 |
| `activateProcessDefinitionById(id)` | 激活流程定义 |

#### 9.2 RuntimeService（运行时服务）

| 方法 | 说明 |
|------|------|
| `startProcessInstanceByKey(key)` | 按Key启动流程 |
| `startProcessInstanceByKey(key, variables)` | 带变量启动流程 |
| `deleteProcessInstance(instanceId, reason)` | 删除流程实例 |
| `getVariable(executionId, variableName)` | 获取流程变量 |
| `setVariables(executionId, variables)` | 设置流程变量 |
| `getActiveActivityIds(executionId)` | 获取活动节点ID |
| `createChangeActivityStateBuilder()` | 创建节点跳转构建器 |

#### 9.3 TaskService（任务服务）

| 方法 | 说明 |
|------|------|
| `createTaskQuery()` | 创建任务查询 |
| `complete(taskId)` | 完成任务 |
| `complete(taskId, variables)` | 带变量完成任务 |
| `addComment(taskId, processInstanceId, type, message)` | 添加评论 |
| `claim(taskId, userId)` | 认领任务（组任务） |
| `setAssignee(taskId, userId)` | 设置处理人 |
| `setVariable(taskId, variableName, value)` | 设置任务变量 |
| `getVariable(taskId, variableName)` | 获取任务变量 |

#### 9.4 HistoryService（历史服务）

| 方法 | 说明 |
|------|------|
| `createHistoricProcessInstanceQuery()` | 查询历史流程实例 |
| `createHistoricTaskInstanceQuery()` | 查询历史任务实例 |
| `createHistoricActivityInstanceQuery()` | 查询历史活动实例 |
| `createHistoricVariableInstanceQuery()` | 查询历史变量 |
| `createHistoricDetailQuery()` | 查询历史详情 |

### 十、完整开发流程示例

#### 10.1 场景：开发一个报销审批流程

**步骤一：设计流程（BPMN XML）**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             typeLanguage="http://www.w3.org/2001/XMLSchema"
             expressionLanguage="http://www.w3.org/1999/XPath"
             targetNamespace="http://flowable.org/demo">

    <process id="expenseProcess" name="报销审批流程" isExecutable="true">
        <startEvent id="startEvent"/>
        <sequenceFlow id="flow1" sourceRef="startEvent" targetRef="submitTask"/>
        <userTask id="submitTask" name="提交报销" flowable:assignee="${applicant}"/>
        <sequenceFlow id="flow2" sourceRef="submitTask" targetRef="deptApprovalTask"/>
        <userTask id="deptApprovalTask" name="部门审批" flowable:assignee="${deptManager}"/>
        <sequenceFlow id="flow3" sourceRef="deptApprovalTask" targetRef="amountGateway"/>
        <exclusiveGateway id="amountGateway" name="金额判断"/>
        <sequenceFlow id="flow4" sourceRef="amountGateway" targetRef="endEvent">
            <conditionExpression xsi:type="tFormalExpression">${approved == false}</conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="flow5" sourceRef="amountGateway" targetRef="financeApprovalTask">
            <conditionExpression xsi:type="tFormalExpression">${approved == true &amp;&amp; amount &gt; 1000}</conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="flow6" sourceRef="amountGateway" targetRef="endEvent">
            <conditionExpression xsi:type="tFormalExpression">${approved == true &amp;&amp; amount &lt;= 1000}</conditionExpression>
        </sequenceFlow>
        <userTask id="financeApprovalTask" name="财务审批" flowable:assignee="${financeManager}"/>
        <sequenceFlow id="flow7" sourceRef="financeApprovalTask" targetRef="endEvent"/>
        <endEvent id="endEvent"/>
    </process>

    <!-- 添加这一部分：Diagram 信息 -->
    <bpmndi:BPMNDiagram id="BPMNDiagram_expenseProcess">
        <bpmndi:BPMNPlane id="BPMNPlane_expenseProcess" bpmnElement="expenseProcess">
            <bpmndi:BPMNShape id="BPMNShape_startEvent" bpmnElement="startEvent">
                <omgdc:Bounds x="100" y="150" width="36" height="36"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="BPMNShape_submitTask" bpmnElement="submitTask">
                <omgdc:Bounds x="200" y="130" width="100" height="80"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="BPMNShape_deptApprovalTask" bpmnElement="deptApprovalTask">
                <omgdc:Bounds x="360" y="130" width="100" height="80"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="BPMNShape_amountGateway" bpmnElement="amountGateway">
                <omgdc:Bounds x="520" y="145" width="50" height="50"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="BPMNShape_financeApprovalTask" bpmnElement="financeApprovalTask">
                <omgdc:Bounds x="630" y="130" width="100" height="80"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="BPMNShape_endEvent" bpmnElement="endEvent">
                <omgdc:Bounds x="630" y="240" width="36" height="36"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNEdge id="BPMNEdge_flow1" bpmnElement="flow1">
                <omgdi:waypoint x="136" y="168"/>
                <omgdi:waypoint x="200" y="170"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="BPMNEdge_flow2" bpmnElement="flow2">
                <omgdi:waypoint x="300" y="170"/>
                <omgdi:waypoint x="360" y="170"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="BPMNEdge_flow3" bpmnElement="flow3">
                <omgdi:waypoint x="460" y="170"/>
                <omgdi:waypoint x="520" y="170"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="BPMNEdge_flow4" bpmnElement="flow4">
                <omgdi:waypoint x="545" y="195"/>
                <omgdi:waypoint x="648" y="240"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="BPMNEdge_flow5" bpmnElement="flow5">
                <omgdi:waypoint x="570" y="170"/>
                <omgdi:waypoint x="630" y="170"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="BPMNEdge_flow6" bpmnElement="flow6">
                <omgdi:waypoint x="545" y="195"/>
                <omgdi:waypoint x="648" y="258"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="BPMNEdge_flow7" bpmnElement="flow7">
                <omgdi:waypoint x="680" y="210"/>
                <omgdi:waypoint x="648" y="258"/>
            </bpmndi:BPMNEdge>
        </bpmndi:BPMNPlane>
    </bpmndi:BPMNDiagram>

</definitions>
```

**步骤二：后端Service实现**

```java
@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final SysUserService sysUserService;

    /**
     * 提交报销申请
     */
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstance submitExpense(ExpenseDto expenseDto, String applicant) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicant", applicant);
        variables.put("amount", expenseDto.getAmount());
        variables.put("reason", expenseDto.getReason());
        
        // 自动获取部门经理
        SysUser user = sysUserService.getByUsername(applicant);
        String deptManager = sysUserService.getDeptManagerUsername(user.getDepartmentId());
        variables.put("deptManager", deptManager);
        
        // 设置财务经理
        variables.put("financeManager", "finance_user");
        
        return runtimeService.startProcessInstanceByKey("expenseProcess", variables);
    }

    /**
     * 部门审批
     */
    @Transactional(rollbackFor = Exception.class)
    public void deptApprove(String taskId, boolean approved, String comment) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        taskService.addComment(taskId, task.getProcessInstanceId(), 
            approved ? "审批通过" : "审批驳回", comment);
        
        taskService.complete(taskId, variables);
    }

    /**
     * 财务审批
     */
    @Transactional(rollbackFor = Exception.class)
    public void financeApprove(String taskId, boolean approved, String comment) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("financeApproved", approved);
        
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        taskService.addComment(taskId, task.getProcessInstanceId(),
            approved ? "财务审批通过" : "财务审批驳回", comment);
        
        taskService.complete(taskId, variables);
    }
}
```

**步骤三：Controller接口**

```java
@RestController
@RequestMapping("/expense")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    /**
     * 提交报销申请
     */
    @PostMapping("/submit")
    public Result<ProcessInstanceDto> submit(@RequestBody ExpenseDto dto, 
                                              @RequestParam String applicant) {
        ProcessInstance pi = expenseService.submitExpense(dto, applicant);
        return Result.success(convertToDto(pi));
    }

    /**
     * 部门审批
     */
    @PostMapping("/dept-approve")
    public Result<Void> deptApprove(@RequestParam String taskId,
                                     @RequestParam boolean approved,
                                     @RequestParam String comment) {
        expenseService.deptApprove(taskId, approved, comment);
        return Result.success();
    }

    /**
     * 财务审批
     */
    @PostMapping("/finance-approve")
    public Result<Void> financeApprove(@RequestParam String taskId,
                                        @RequestParam boolean approved,
                                        @RequestParam String comment) {
        expenseService.financeApprove(taskId, approved, comment);
        return Result.success();
    }
}
```

### 十一、常见问题与解决方案

#### 11.1 任务处理人表达式解析

```java
// 处理 ${deptManager} 这种表达式形式的处理人
String assignee = task.getAssignee();
if (assignee != null && assignee.startsWith("${") && assignee.endsWith("}")) {
    String variableName = assignee.substring(2, assignee.length() - 1);
    Object variableValue = runtimeService.getVariable(task.getExecutionId(), variableName);
    if (variableValue != null) {
        assignee = variableValue.toString();
    }
}
```

#### 11.2 流程定义版本管理

```java
// Flowable自动管理版本，相同Key的流程会自动升级版本
// 部署新版本后，新启动的流程使用新版本
// 已运行的流程继续使用旧版本

// 查询最新版本的流程定义
ProcessDefinition latest = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("leaveProcess")
        .latestVersion()
        .singleResult();
```

#### 11.3 流程监听器

```xml
<!-- 在BPMN XML中配置监听器 -->
<userTask id="submitTask" name="提交申请">
    <extensionElements>
        <flowable:executionListener event="start" 
            class="com.example.flowable.listener.TaskStartListener"/>
        <flowable:executionListener event="end" 
            class="com.example.flowable.listener.TaskEndListener"/>
    </extensionElements>
</userTask>
```

```java
// 监听器实现
public class TaskStartListener implements ExecutionListener {
    @Override
    public void notify(DelegateExecution execution) {
        // 任务开始时的业务逻辑
        String processInstanceId = execution.getProcessInstanceId();
        // 发送通知、记录日志等
    }
}
```

### 十二、最佳实践建议

1. **流程定义命名规范**
   - 流程ID：使用驼峰命名，如 `leaveProcess`、`expenseProcess`
   - 任务ID：使用有意义的前缀，如 `submitTask`、`approvalTask`
   - 变量名：使用有意义的名称，如 `applicant`、`approved`、`days`

2. **变量设计**
   - 避免使用过多变量，保持精简
   - 使用统一的命名规范
   - 复杂对象建议存储ID，需要时再查询

3. **异常处理**
   - 关键操作添加事务注解 `@Transactional`
   - 异常信息要有明确的业务含义
   - 记录操作日志便于问题排查

4. **性能优化**
   - 避免在循环中频繁查询数据库
   - 合理使用缓存
   - 定期清理历史数据

## 许可证

MIT License
