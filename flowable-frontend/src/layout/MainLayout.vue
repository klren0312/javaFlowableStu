<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="28"><Setting /></el-icon>
        <span class="logo-text">Flowable工作流</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="menu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/system/department">
            <el-icon><OfficeBuilding /></el-icon>
            <span>部门管理</span>
          </el-menu-item>
          <el-menu-item index="/system/role">
            <el-icon><UserFilled /></el-icon>
            <span>角色管理</span>
          </el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="process">
          <template #title>
            <el-icon><Share /></el-icon>
            <span>流程管理</span>
          </template>
          <el-menu-item index="/process/definition">
            <el-icon><Document /></el-icon>
            <span>流程定义</span>
          </el-menu-item>
          <el-menu-item index="/process/instance">
            <el-icon><List /></el-icon>
            <span>流程实例</span>
          </el-menu-item>
          <el-menu-item index="/process/task">
            <el-icon><Finished /></el-icon>
            <span>待办任务</span>
          </el-menu-item>
        </el-sub-menu>
        
        <el-menu-item index="/modeler">
          <el-icon><Edit /></el-icon>
          <span>流程设计器</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <!-- 主内容区 -->
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleUserSwitch">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ currentUsername }}</span>
              <el-tag v-if="currentUserInfo" size="small" type="success" style="margin-left: 8px;">
                {{ currentUserInfo.realName || currentUserInfo.username }}
              </el-tag>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item 
                  v-for="user in users" 
                  :key="user.id" 
                  :command="user.username"
                  :class="{ 'is-active': user.username === currentUsername }"
                >
                  <div class="user-option">
                    <span>{{ user.realName || user.username }}</span>
                    <el-tag v-if="user.departmentName" size="small" type="info">{{ user.departmentName }}</el-tag>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided disabled>
                  <span style="color: #909399; font-size: 12px;">点击切换用户（模拟登录）</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import userStore, { initUsers, switchUser, getCurrentUsername, getCurrentUser } from '@/store/user'

const route = useRoute()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '首页')

// 用户列表
const users = computed(() => userStore.users)

// 当前用户名
const currentUsername = computed(() => getCurrentUsername())

// 当前用户信息
const currentUserInfo = computed(() => getCurrentUser())

// 处理用户切换
const handleUserSwitch = (username) => {
  if (username === currentUsername.value) return
  
  if (switchUser(username)) {
    ElMessage.success(`已切换到用户: ${username}`)
    // 发送用户切换事件
    window.dispatchEvent(new CustomEvent('user-switched', { detail: { username } }))
    // 刷新页面以更新所有组件的用户状态
    window.location.reload()
  }
}

// 初始化用户列表
onMounted(async () => {
  await initUsers()
})
</script>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
}

.aside {
  background-color: #304156;
  
  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    border-bottom: 1px solid #3a4a5b;
    
    .logo-text {
      margin-left: 10px;
    }
  }
  
  .menu {
    border-right: none;
  }
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  
  .page-title {
    font-size: 18px;
    font-weight: 500;
  }
  
  .user-info {
    display: flex;
    align-items: center;
    cursor: pointer;
    
    .username {
      margin: 0 8px;
    }
  }
}

.main {
  background-color: #f0f2f5;
  padding: 20px;
}

.user-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 10px;
}

.is-active {
  background-color: #ecf5ff;
  color: #409eff;
}
</style>