import { reactive } from 'vue'
import { getAllUsers } from '@/api/user'

// 用户状态管理
const userStore = reactive({
  currentUser: null,
  users: [],
  loading: false
})

// 初始化用户列表
export async function initUsers() {
  if (userStore.users.length > 0) return
  
  userStore.loading = true
  try {
    const res = await getAllUsers()
    if (res.code === 200 && res.data) {
      userStore.users = res.data
      // 默认选择admin用户
      const adminUser = userStore.users.find(u => u.username === 'admin')
      userStore.currentUser = adminUser || userStore.users[0] || null
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    userStore.loading = false
  }
}

// 切换当前用户
export function switchUser(username) {
  const user = userStore.users.find(u => u.username === username)
  if (user) {
    userStore.currentUser = user
    // 存储到localStorage
    localStorage.setItem('currentUser', JSON.stringify(user))
    return true
  }
  return false
}

// 从localStorage恢复用户
export function restoreUser() {
  const saved = localStorage.getItem('currentUser')
  if (saved) {
    try {
      userStore.currentUser = JSON.parse(saved)
    } catch (e) {
      console.error('恢复用户信息失败:', e)
    }
  }
}

// 获取当前用户名
export function getCurrentUsername() {
  return userStore.currentUser?.username || 'admin'
}

// 获取当前用户
export function getCurrentUser() {
  return userStore.currentUser
}

export default userStore