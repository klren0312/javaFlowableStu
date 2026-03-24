<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #409EFF;">
            <el-icon :size="32"><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.processCount }}</div>
            <div class="stat-label">流程定义</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #67C23A;">
            <el-icon :size="32"><List /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.instanceCount }}</div>
            <div class="stat-label">流程实例</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #E6A23C;">
            <el-icon :size="32"><Finished /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.taskCount }}</div>
            <div class="stat-label">待办任务</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #F56C6C;">
            <el-icon :size="32"><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.userCount }}</div>
            <div class="stat-label">系统用户</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/modeler')">
              <el-icon><Plus /></el-icon>
              创建流程
            </el-button>
            <el-button type="success" @click="$router.push('/process/definition')">
              <el-icon><Document /></el-icon>
              流程定义
            </el-button>
            <el-button type="warning" @click="$router.push('/process/task')">
              <el-icon><Finished /></el-icon>
              待办任务
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>系统信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="系统名称">Flowable工作流管理系统</el-descriptions-item>
            <el-descriptions-item label="系统版本">1.0.0</el-descriptions-item>
            <el-descriptions-item label="Flowable版本">6.8.0</el-descriptions-item>
            <el-descriptions-item label="技术栈">Spring Boot + Vue3 + Element Plus</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getProcessDefinitionList, getProcessInstanceList, getTodoTaskList } from '@/api/process'
import { getUserList } from '@/api/user'
import { getCurrentUsername } from '@/store/user'

const stats = ref({
  processCount: 0,
  instanceCount: 0,
  taskCount: 0,
  userCount: 0
})

onMounted(async () => {
  try {
    const currentUsername = getCurrentUsername()
    const [processRes, instanceRes, taskRes, userRes] = await Promise.all([
      getProcessDefinitionList(),
      getProcessInstanceList(),
      getTodoTaskList(currentUsername),
      getUserList({ pageNum: 1, pageSize: 1 })
    ])
    stats.value.processCount = processRes.data?.length || 0
    stats.value.instanceCount = instanceRes.data?.length || 0
    stats.value.taskCount = taskRes.data?.length || 0
    stats.value.userCount = userRes.data?.total || 0
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
})
</script>

<style lang="scss" scoped>
.dashboard {
  .stat-card {
    display: flex;
    align-items: center;
    padding: 20px;
    
    .stat-icon {
      width: 64px;
      height: 64px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      margin-right: 16px;
    }
    
    .stat-content {
      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
      }
      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }
  
  .quick-actions {
    display: flex;
    gap: 12px;
  }
}
</style>