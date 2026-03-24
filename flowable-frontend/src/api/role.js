import request from '@/utils/request'

// 获取所有角色
export function getAllRoles() {
  return request.get('/role/all')
}

// 获取角色列表（分页）
export function getRoleList(params) {
  return request.get('/role/list', { params })
}

// 获取角色详情
export function getRoleById(id) {
  return request.get(`/role/${id}`)
}

// 新增角色
export function createRole(data) {
  return request.post('/role', data)
}

// 更新角色
export function updateRole(data) {
  return request.put('/role', data)
}

// 删除角色
export function deleteRole(id) {
  return request.delete(`/role/${id}`)
}