import request from '@/utils/request'

// ==================== 表单定义相关 ====================

// 创建表单定义
export function createForm(data) {
  return request.post('/form', data)
}

// 更新表单定义
export function updateForm(data) {
  return request.put('/form', data)
}

// 删除表单定义
export function deleteForm(id) {
  return request.delete(`/form/${id}`)
}

// 根据ID获取表单定义
export function getFormById(id) {
  return request.get(`/form/${id}`)
}

// 根据formKey获取表单定义
export function getFormByKey(formKey) {
  return request.get(`/form/key/${formKey}`)
}

// 获取所有表单定义列表
export function getFormList() {
  return request.get('/form/list')
}

// ==================== 流程表单关联相关 ====================

// 绑定表单到流程
export function bindFormToProcess(data) {
  return request.post('/form/bind', data)
}

// 解绑流程表单
export function unbindForm(id) {
  return request.delete(`/form/unbind/${id}`)
}

// 获取流程的启动表单
export function getStartForm(processDefinitionKey) {
  return request.get(`/form/start-form/${processDefinitionKey}`)
}

// 获取任务节点的表单
export function getTaskForm(processDefinitionKey, nodeId) {
  return request.get('/form/task-form', { params: { processDefinitionKey, nodeId } })
}

// 获取流程的所有表单绑定关系
export function getProcessFormRelations(processDefinitionKey) {
  return request.get(`/form/relations/${processDefinitionKey}`)
}

// ==================== 表单实例相关 ====================

// 保存表单实例数据
export function saveFormInstance(data) {
  return request.post('/form/instance', data)
}

// 获取流程实例的表单数据
export function getFormInstanceByProcessId(processInstanceId) {
  return request.get(`/form/instance/process/${processInstanceId}`)
}

// 获取任务的表单数据
export function getFormInstanceByTaskId(taskId) {
  return request.get(`/form/instance/task/${taskId}`)
}