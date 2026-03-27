<template>
  <div class="task-list">
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待办任务" name="todo" />
        <el-tab-pane label="已办任务" name="done" />
      </el-tabs>
      
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="name" label="任务名称" width="150" />
        <el-table-column prop="processDefinitionName" label="流程名称" width="150" />
        <el-table-column prop="assignee" label="受理人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column v-if="activeTab === 'done'" prop="endTime" label="完成时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <template v-if="activeTab === 'todo'">
              <el-button type="primary" link @click="handleComplete(row)">完成</el-button>
              <el-button type="warning" link @click="handleRollback(row)">回退</el-button>
              <el-button type="primary" link @click="handleViewDiagram(row)">流程图</el-button>
            </template>
            <template v-else>
              <el-button type="primary" link @click="handleViewDiagram(row)">流程图</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 完成任务对话框 -->
    <el-dialog v-model="completeDialogVisible" title="完成任务" width="700px">
      <!-- 原始表单数据（只读显示） -->
      <div v-if="originalFormData && Object.keys(originalFormData).length > 0" class="original-form-section">
        <div class="section-title">
          <el-icon><Document /></el-icon>
          <span>申请信息</span>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item 
            v-for="(value, key) in originalFormData" 
            :key="key" 
            :label="getFieldLabel(key)"
          >
            <span class="form-value">{{ formatFieldValue(value) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      
      <el-divider v-if="originalFormData && Object.keys(originalFormData).length > 0" />
      
      <!-- 审批表单 -->
      <el-form :model="completeForm" label-width="80px">
        <el-form-item label="任务名称">
          <el-input :value="currentTask?.name" disabled />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-radio-group v-model="completeForm.variables.approved">
            <el-radio :label="true">同意</el-radio>
            <el-radio :label="false">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批备注">
          <el-input 
            v-model="completeForm.variables.comment" 
            type="textarea" 
            :rows="2" 
            placeholder="请输入审批备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCompleteSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 回退对话框 -->
    <el-dialog v-model="rollbackDialogVisible" title="回退任务" width="600px">
      <el-form :model="rollbackForm" label-width="80px">
        <el-form-item label="当前任务">
          <el-input :value="currentTask?.name" disabled />
        </el-form-item>
        <el-form-item label="回退节点">
          <el-select v-model="rollbackForm.targetActivityId" placeholder="请选择回退节点" style="width: 100%">
            <el-option
              v-for="activity in historyActivities"
              :key="activity.activityId"
              :label="activity.activityName"
              :value="activity.activityId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="回退原因">
          <el-input v-model="rollbackForm.reason" type="textarea" placeholder="请输入回退原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rollbackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRollbackSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 流程图对话框 -->
    <el-dialog v-model="diagramDialogVisible" title="流程图" width="900px">
      <div class="diagram-container">
        <img :src="diagramUrl" alt="流程图" v-if="diagramUrl" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTodoTaskList, getDoneTaskList, completeTask, getHistoryActivities, rollbackTask, getProcessDiagramUrl } from '@/api/process'
import { getFormInstanceByProcessId } from '@/api/form'
import { getCurrentUsername } from '@/store/user'

const loading = ref(false)
const tableData = ref([])
const activeTab = ref('todo')
const completeDialogVisible = ref(false)
const rollbackDialogVisible = ref(false)
const diagramDialogVisible = ref(false)
const currentTask = ref(null)
const historyActivities = ref([])
const diagramUrl = ref('')
const originalFormData = ref({})

const completeForm = reactive({
  taskId: '',
  variables: { approved: true, comment: '' }
})

const rollbackForm = reactive({
  taskId: '',
  targetActivityId: '',
  reason: ''
})

// 字段名称映射
const fieldLabelMap = {
  applicant: '申请人',
  department: '所属部门',
  leaveType: '请假类型',
  startTime: '开始时间',
  endTime: '结束时间',
  days: '请假天数',
  reason: '请假原因',
  amount: '金额',
  title: '标题',
  content: '内容'
}

// 请假类型映射
const leaveTypeMap = {
  '1': '事假',
  '2': '病假',
  '3': '年假',
  '4': '调休'
}

onMounted(() => { 
  // 确保用户信息已恢复后再加载数据
  setTimeout(() => {
    fetchData()
  }, 100)
})

const fetchData = async () => {
  loading.value = true
  try {
    const currentUsername = getCurrentUsername()
    const res = activeTab.value === 'todo' 
      ? await getTodoTaskList(currentUsername)
      : await getDoneTaskList(currentUsername)
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => { fetchData() }

const handleComplete = async (row) => {
  currentTask.value = row
  completeForm.taskId = row.id
  completeForm.variables = { approved: true, comment: '' }
  originalFormData.value = {}
  
  // 加载原始表单数据
  try {
    const res = await getFormInstanceByProcessId(row.processInstanceId)
    if (res.data && res.data.formData) {
      originalFormData.value = res.data.formData
    }
  } catch (e) {
    console.error('加载表单数据失败', e)
  }
  
  completeDialogVisible.value = true
}

const handleCompleteSubmit = async () => {
  await completeTask(completeForm)
  ElMessage.success('任务完成')
  completeDialogVisible.value = false
  // 切换到已办任务tab
  activeTab.value = 'done'
  // 添加延迟，等待 Flowable 将任务移动到历史表
  setTimeout(() => {
    fetchData()
  }, 500)
}

const handleRollback = async (row) => {
  currentTask.value = row
  rollbackForm.taskId = row.id
  rollbackForm.targetActivityId = ''
  rollbackForm.reason = ''
  const res = await getHistoryActivities(row.processInstanceId)
  historyActivities.value = (res.data || []).filter(a => !a.endTime || a.activityId !== row.taskDefinitionKey)
  rollbackDialogVisible.value = true
}

const handleRollbackSubmit = async () => {
  if (!rollbackForm.targetActivityId) {
    ElMessage.warning('请选择回退节点')
    return
  }
  await rollbackTask(rollbackForm)
  ElMessage.success('回退成功')
  rollbackDialogVisible.value = false
  fetchData()
}

const handleViewDiagram = (row) => {
  diagramUrl.value = getProcessDiagramUrl(row.processInstanceId)
  diagramDialogVisible.value = true
}

// 获取字段显示名称
const getFieldLabel = (key) => {
  return fieldLabelMap[key] || key
}

// 格式化字段值显示
const formatFieldValue = (value) => {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return value
}

// 获取请假类型名称
const getLeaveTypeName = (value) => {
  return leaveTypeMap[value] || value
}
</script>

<style scoped>
.diagram-container {
  text-align: center;
}
.diagram-container img {
  max-width: 100%;
}

.original-form-section {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 500;
  color: #303133;
  font-size: 15px;
}

.form-value {
  color: #606266;
}

:deep(.el-descriptions__label) {
  font-weight: 500;
  background-color: #f5f7fa;
}
</style>