<template>
  <div class="process-instance">
    <el-card>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="实例ID" width="280" />
        <el-table-column prop="processDefinitionName" label="流程名称" width="150" />
        <el-table-column prop="startUserId" label="发起人" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="suspended" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.suspended ? 'warning' : 'success'">
              {{ row.suspended ? '挂起' : '运行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDiagram(row)">流程图</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 流程图对话框 -->
    <el-dialog v-model="diagramDialogVisible" title="流程图" width="900px">
      <div class="diagram-container">
        <img :src="diagramUrl" alt="流程图" v-if="diagramUrl" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProcessInstanceList, deleteProcessInstance, getProcessDiagramUrl } from '@/api/process'

const loading = ref(false)
const tableData = ref([])
const diagramDialogVisible = ref(false)
const diagramUrl = ref('')

onMounted(() => { fetchData() })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getProcessInstanceList()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleViewDiagram = (row) => {
  diagramUrl.value = getProcessDiagramUrl(row.id)
  diagramDialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该流程实例吗？', '提示', { type: 'warning' })
  await deleteProcessInstance(row.id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>

<style scoped>
.diagram-container {
  text-align: center;
}
.diagram-container img {
  max-width: 100%;
}
</style>