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
      // 先尝试从localStorage恢复用户
      const savedUser = localStorage.getItem('currentUser')
      if (savedUser) {
        try {
          const parsedUser = JSON.parse(savedUser)
          // 检查恢复的用户是否在用户列表中
          const foundUser = userStore.users.find(u => u.id === parsedUser.id)
          if (foundUser) {
            userStore.currentUser = foundUser
            // 更新localStorage以确保数据一致
            localStorage.setItem('currentUser', JSON.stringify(foundUser))
            return
          } else {
            // 保存的用户不在列表中，清除localStorage
            localStorage.removeItem('currentUser')
          }
        } catch (e) {
          console.error('解析保存的用户信息失败:', e)
          localStorage.removeItem('currentUser')
        }
      }
      // 如果没有保存的用户或保存的用户不在列表中，则选择admin用户
      const adminUser = userStore.users.find(u => u.username === 'admin')
      userStore.currentUser = adminUser || userStore.users[0] || null
      // 如果找到了默认用户，也保存到localStorage
      if (userStore.currentUser) {
        localStorage.setItem('currentUser', JSON.stringify(userStore.currentUser))
      }
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