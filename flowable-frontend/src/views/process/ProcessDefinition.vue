<template>
  <div class="process-definition">
    <el-card>
      <div class="table-toolbar">
        <el-button type="primary" @click="handleDeploy">
          <el-icon><Upload /></el-icon>
          部署流程
        </el-button>
        <el-button type="primary" @click="$router.push('/modeler')">
          <el-icon><Plus /></el-icon>
          创建流程
        </el-button>
      </div>
      
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="key" label="流程KEY" width="180" />
        <el-table-column prop="name" label="流程名称" width="180" />
        <el-table-column prop="version" label="版本" width="80">
          <template #default="{ row }">V{{ row.version }}</template>
        </el-table-column>
        <el-table-column prop="deploymentTime" label="部署时间" width="180" />
        <el-table-column prop="suspended" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.suspended ? 'danger' : 'success'">
              {{ row.suspended ? '挂起' : '激活' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleStart(row)">启动</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleViewXml(row)">查看XML</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 部署对话框 -->
    <el-dialog v-model="deployDialogVisible" title="部署流程定义" width="500px">
      <el-form :model="deployForm" label-width="80px">
        <el-form-item label="流程名称">
          <el-input v-model="deployForm.name" placeholder="请输入流程名称" />
        </el-form-item>
        <el-form-item label="BPMN文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".bpmn,.bpmn20.xml"
            :on-change="handleFileChange"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">只能上传 bpmn/bpmn20.xml 文件</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deployDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDeploySubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 启动流程对话框 -->
    <el-dialog v-model="startDialogVisible" title="启动流程" width="600px">
      <el-form :model="startForm" label-width="100px">
        <el-form-item label="流程定义">
          <el-input :value="currentProcess?.name" disabled />
        </el-form-item>
        
        <!-- 动态表单字段 -->
        <template v-if="startFormFields.length > 0">
          <el-divider content-position="left">表单信息</el-divider>
          <el-form-item 
            v-for="field in startFormFields" 
            :key="field.id"
            :label="field.label"
            :required="field.required"
          >
            <!-- 文本输入 -->
            <el-input 
              v-if="field.type === 'text'"
              v-model="startForm.variables[field.name]"
              :placeholder="field.placeholder || '请输入' + field.label"
              :maxlength="field.maxLength"
            />
            <!-- 多行文本 -->
            <el-input 
              v-else-if="field.type === 'textarea'"
              v-model="startForm.variables[field.name]"
              type="textarea"
              :rows="3"
              :placeholder="field.placeholder || '请输入' + field.label"
              :maxlength="field.maxLength"
            />
            <!-- 数字输入 -->
            <el-input-number 
              v-else-if="field.type === 'number'"
              v-model="startForm.variables[field.name]"
              :min="field.min"
              :max="field.max"
              :step="field.step || 1"
              style="width: 100%"
            />
            <!-- 下拉选择 -->
            <el-select 
              v-else-if="field.type === 'select'"
              v-model="startForm.variables[field.name]"
              :placeholder="field.placeholder || '请选择'"
              style="width: 100%"
            >
              <el-option 
                v-for="opt in field.options" 
                :key="opt.value" 
                :label="opt.label" 
                :value="opt.value" 
              />
            </el-select>
            <!-- 单选框 -->
            <el-radio-group 
              v-else-if="field.type === 'radio'"
              v-model="startForm.variables[field.name]"
            >
              <el-radio 
                v-for="opt in field.options" 
                :key="opt.value" 
                :label="opt.value"
              >
                {{ opt.label }}
              </el-radio>
            </el-radio-group>
            <!-- 复选框 -->
            <el-checkbox-group 
              v-else-if="field.type === 'checkbox'"
              v-model="startForm.variables[field.name]"
            >
              <el-checkbox 
                v-for="opt in field.options" 
                :key="opt.value" 
                :label="opt.value"
              >
                {{ opt.label }}
              </el-checkbox>
            </el-checkbox-group>
            <!-- 日期 -->
            <el-date-picker 
              v-else-if="field.type === 'date'"
              v-model="startForm.variables[field.name]"
              type="date"
              :placeholder="field.placeholder || '请选择日期'"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
            <!-- 日期时间 -->
            <el-date-picker 
              v-else-if="field.type === 'datetime'"
              v-model="startForm.variables[field.name]"
              type="datetime"
              :placeholder="field.placeholder || '请选择日期时间'"
              style="width: 100%"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
            <!-- 人员选择 -->
            <el-select 
              v-else-if="field.type === 'user'"
              v-model="startForm.variables[field.name]"
              :placeholder="field.placeholder || '请选择人员'"
              filterable
              :disabled="field.readonly"
              style="width: 100%"
            >
              <el-option 
                v-for="user in users" 
                :key="user.id" 
                :label="user.realName || user.username" 
                :value="user.username" 
              />
            </el-select>
            <!-- 部门选择 -->
            <el-select 
              v-else-if="field.type === 'department'"
              v-model="startForm.variables[field.name]"
              :placeholder="field.placeholder || '请选择部门'"
              filterable
              style="width: 100%"
            >
              <el-option 
                v-for="dept in departments" 
                :key="dept.id" 
                :label="dept.name" 
                :value="dept.name" 
              />
            </el-select>
            <!-- 默认文本输入 -->
            <el-input 
              v-else
              v-model="startForm.variables[field.name]"
              :placeholder="field.placeholder || '请输入' + field.label"
            />
          </el-form-item>
        </template>
        
        <!-- 无表单时显示默认字段 -->
        <template v-else>
          <el-form-item label="申请人">
            <el-input v-model="startForm.variables.applicant" placeholder="请输入申请人" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="startDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStartSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- XML查看对话框 -->
    <el-dialog v-model="xmlDialogVisible" title="流程XML" width="800px">
      <el-input v-model="processXml" type="textarea" :rows="20" readonly />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProcessDefinitionList, deleteDeployment, getProcessDefinitionXml, startProcess, deployProcess } from '@/api/process'
import { getStartForm, saveFormInstance } from '@/api/form'
import { getAllUsers } from '@/api/user'
import { getAllDepartments } from '@/api/department'
import { getCurrentUsername } from '@/store/user'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const deployDialogVisible = ref(false)
const startDialogVisible = ref(false)
const xmlDialogVisible = ref(false)
const currentProcess = ref(null)
const processXml = ref('')
const uploadRef = ref(null)
const selectedFile = ref(null)
const startFormFields = ref([])
const users = ref([])
const departments = ref([])

const deployForm = reactive({ name: '' })
const startForm = reactive({ 
  processDefinitionKey: '', 
  variables: {} 
})

onMounted(() => { fetchData() })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getProcessDefinitionList()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleDeploy = () => {
  deployForm.name = ''
  selectedFile.value = null
  deployDialogVisible.value = true
}

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleDeploySubmit = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择BPMN文件')
    return
  }
  await deployProcess(deployForm.name, selectedFile.value)
  ElMessage.success('部署成功')
  deployDialogVisible.value = false
  fetchData()
}

const handleStart = async (row) => {
  currentProcess.value = row
  startForm.processDefinitionKey = row.key
  startForm.variables = { applicant: getCurrentUsername() }
  startFormFields.value = []
  
  // 加载用户和部门数据
  try {
    const [userRes, deptRes] = await Promise.all([
      getAllUsers(),
      getAllDepartments()
    ])
    users.value = userRes.data || []
    departments.value = deptRes.data || []
  } catch (e) {
    console.error('加载用户部门数据失败', e)
  }
  
  // 加载启动表单
  try {
    const res = await getStartForm(row.key)
    if (res.data && res.data.fields) {
      startFormFields.value = res.data.fields
      // 初始化变量
      res.data.fields.forEach(field => {
        // 如果字段已有值（如 applicant 已设置为当前用户），则不覆盖
        if (startForm.variables[field.name] !== undefined && startForm.variables[field.name] !== '') {
          return
        }
        if (field.defaultValue) {
          startForm.variables[field.name] = field.defaultValue
        } else if (field.type === 'checkbox') {
          startForm.variables[field.name] = []
        } else {
          startForm.variables[field.name] = ''
        }
      })
      // 设置申请人只读
      const applicantField = res.data.fields.find(f => f.name === 'applicant')
      if (applicantField) {
        applicantField.readonly = true
      }
    }
  } catch (e) {
    console.log('未找到启动表单，使用默认表单')
  }
  
  startDialogVisible.value = true
}

const handleStartSubmit = async () => {
  // 验证必填字段
  for (const field of startFormFields.value) {
    if (field.required) {
      const value = startForm.variables[field.name]
      if (value === undefined || value === null || value === '' || 
          (Array.isArray(value) && value.length === 0)) {
        ElMessage.warning(`请填写${field.label}`)
        return
      }
    }
  }
  
  submitting.value = true
  try {
    // 启动流程
    const res = await startProcess(startForm)
    const processInstanceId = res.data?.id
    
    // 保存表单实例数据
    if (startFormFields.value.length > 0 && processInstanceId) {
      await saveFormInstance({
        processInstanceId: processInstanceId,
        formKey: currentProcess.value.key + '_start',
        formData: { ...startForm.variables },
        submitter: getCurrentUsername()
      })
    }
    
    ElMessage.success('启动成功')
    startDialogVisible.value = false
  } catch (e) {
    ElMessage.error('启动失败')
  } finally {
    submitting.value = false
  }
}

const handleEdit = (row) => {
  router.push(`/modeler/${row.id}`)
}

const handleViewXml = async (row) => {
  const res = await getProcessDefinitionXml(row.id)
  processXml.value = res.data
  xmlDialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该流程定义吗？', '提示', { type: 'warning' })
  await deleteDeployment(row.deploymentId, true)
  ElMessage.success('删除成功')
  fetchData()
}
</script>

<style scoped>
.table-toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}
</style>