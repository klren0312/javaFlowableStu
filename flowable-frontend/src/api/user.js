import request from '@/utils/request'

// 获取用户列表
export function getUserList(params) {
  return request.get('/user/list', { params })
}

// 获取所有用户
export function getAllUsers() {
  return request.get('/user/all')
}

// 获取用户详情
export function getUserById(id) {
  return request.get(`/user/${id}`)
}

// 新增用户
export function createUser(data) {
  return request.post('/user', data)
}

// 更新用户
export function updateUser(data) {
  return request.put('/user', data)
}

// 删除用户
export function deleteUser(id) {
  return request.delete(`/user/${id}`)
}

// 获取用户角色
export function getUserRoles(id) {
  return request.get(`/user/${id}/roles`)
}

// 分配用户角色
export function assignRoles(id, roleIds) {
  return request.post(`/user/${id}/roles`, roleIds)
}