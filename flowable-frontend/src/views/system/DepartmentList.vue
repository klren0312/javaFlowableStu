<template>
  <div class="department-list">
    <el-card>
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd(null)">
          <el-icon><Plus /></el-icon>
          新增部门
        </el-button>
      </div>
      
      <el-table :data="tableData" v-loading="loading" row-key="id" border default-expand-all>
        <el-table-column prop="name" label="部门名称" />
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleAdd(row)">新增</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="上级部门">
          <el-input v-model="parentName" disabled />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="form.leader" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const parentName = ref('')

const form = reactive({
  id: null,
  name: '',
  parentId: 0,
  leader: '',
  phone: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => form.id ? '编辑部门' : '新增部门')

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/department/tree')
    tableData.value = buildTree(res.data || [])
  } finally {
    loading.value = false
  }
}

const buildTree = (data) => {
  const map = {}
  const roots = []
  data.forEach(item => map[item.id] = { ...item, children: [] })
  data.forEach(item => {
    if (item.parentId === 0) {
      roots.push(map[item.id])
    } else if (map[item.parentId]) {
      map[item.parentId].children.push(map[item.id])
    }
  })
  return roots
}

const handleAdd = (row) => {
  Object.assign(form, { id: null, name: '', parentId: row?.id || 0, leader: '', phone: '', status: 1 })
  parentName.value = row?.name || '顶级部门'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, row)
  parentName.value = row.parentId === 0 ? '顶级部门' : tableData.value.find(d => d.id === row.parentId)?.name || ''
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该部门吗？', '提示', { type: 'warning' })
  await request.delete(`/department/${row.id}`)
  ElMessage.success('删除成功')
  fetchData()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (form.id) {
    await request.put('/department', form)
    ElMessage.success('更新成功')
  } else {
    await request.post('/department', form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  fetchData()
}
</script>