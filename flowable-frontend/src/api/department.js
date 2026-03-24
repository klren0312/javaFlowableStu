import request from '@/utils/request'

// 获取部门列表
export function getDepartmentList(params) {
  return request.get('/department/list', { params })
}

// 获取所有部门
export function getAllDepartments() {
  return request.get('/department/all')
}

// 获取部门树形结构
export function getDepartmentTree() {
  return request.get('/department/tree')
}

// 获取部门详情
export function getDepartmentById(id) {
  return request.get(`/department/${id}`)
}

// 新增部门
export function createDepartment(data) {
  return request.post('/department', data)
}

// 更新部门
export function updateDepartment(data) {
  return request.put('/department', data)
}

// 删除部门
export function deleteDepartment(id) {
  return request.delete(`/department/${id}`)
}

// 获取部门用户
export function getDepartmentUsers(id) {
  return request.get(`/department/${id}/users`)
}