import request from '@/utils/request'

// 部署流程定义（文件上传）
export function deployProcess(name, file) {
  const formData = new FormData()
  formData.append('name', name)
  formData.append('file', file)
  return request.post('/process/deploy', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 部署流程定义（XML字符串）
export function deployProcessByXml(name, xml) {
  return request.post('/process/deploy/xml', xml, {
    params: { name },
    headers: { 'Content-Type': 'application/xml' }
  })
}

// 获取流程定义列表
export function getProcessDefinitionList() {
  return request.get('/process/definition/list')
}

// 删除流程定义
export function deleteDeployment(deploymentId, cascade = false) {
  return request.delete(`/process/definition/${deploymentId}`, { params: { cascade } })
}

// 获取流程定义XML
export function getProcessDefinitionXml(processDefinitionId) {
  return request.get('/process/definition/xml', { params: { processDefinitionId } })
}

// 启动流程实例
export function startProcess(data) {
  return request.post('/process/instance/start', data)
}

// 获取流程实例列表
export function getProcessInstanceList(processDefinitionKey) {
  return request.get('/process/instance/list', { params: { processDefinitionKey } })
}

// 删除流程实例
export function deleteProcessInstance(processInstanceId, reason) {
  return request.delete(`/process/instance/${processInstanceId}`, { params: { reason } })
}

// 获取待办任务列表
export function getTodoTaskList(assignee) {
  return request.get('/process/task/todo', { params: { assignee } })
}

// 获取已完成任务列表
export function getDoneTaskList(assignee) {
  return request.get('/process/task/done', { params: { assignee } })
}

// 完成任务
export function completeTask(data) {
  return request.post('/process/task/complete', data)
}

// 获取流程历史节点
export function getHistoryActivities(processInstanceId) {
  return request.get(`/process/history/activities/${processInstanceId}`)
}

// 回退到指定节点
export function rollbackTask(data) {
  return request.post('/process/task/rollback', data)
}

// 获取流程图URL
export function getProcessDiagramUrl(processInstanceId) {
  return `/api/process/diagram/${processInstanceId}`
}

// 更新流程定义（重新部署新版本）
export function updateProcessDefinition(processDefinitionId, xml) {
  return request.put('/process/definition', xml, {
    params: { processDefinitionId },
    headers: { 'Content-Type': 'application/xml' }
  })
}
