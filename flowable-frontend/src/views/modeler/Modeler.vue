<template>
  <div class="modeler-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleSave">
        <el-icon><Check /></el-icon>
        保存
      </el-button>
      <el-button @click="handleDeploy">
        <el-icon><Upload /></el-icon>
        部署
      </el-button>
      <el-button @click="handleDownload">
        <el-icon><Download /></el-icon>
        下载
      </el-button>
      <el-divider direction="vertical" />
      <el-button @click="handleZoomIn">
        <el-icon><ZoomIn /></el-icon>
      </el-button>
      <el-button @click="handleZoomOut">
        <el-icon><ZoomOut /></el-icon>
      </el-button>
      <el-button @click="handleZoomFit">
        <el-icon><FullScreen /></el-icon>
      </el-button>
      <el-button @click="handleUndo">
        <el-icon><RefreshLeft /></el-icon>
      </el-button>
      <el-button @click="handleRedo">
        <el-icon><RefreshRight /></el-icon>
      </el-button>
      <el-divider direction="vertical" />
      <el-radio-group v-model="activeTab" size="small">
        <el-radio-button value="process">流程设计</el-radio-button>
        <el-radio-button value="form">表单设计</el-radio-button>
        <el-radio-button value="binding">表单绑定</el-radio-button>
      </el-radio-group>
    </div>
    <div class="modeler-content">
      <!-- 流程设计 -->
      <template v-if="activeTab === 'process'">
        <BpmnModeler 
          ref="flowDesignerRef"
          :process-id="processInfo.id"
          :process-name="processInfo.name"
          :xml="processXml"
          @update:xml="handleXmlUpdate"
          @element-selected="handleElementSelected"
          @import-done="handleImportDone"
        />
        <div class="properties-panel">
          <div class="panel-header">
            <el-icon><Setting /></el-icon>
            <span>属性面板</span>
          </div>
          <div v-if="!selectedElement" class="panel-empty">
            <el-empty description="请选择一个元素" :image-size="80" />
          </div>
          <div v-else class="panel-content">
            <el-form label-position="top" size="small">
              <el-collapse v-model="activeCollapse">
                <el-collapse-item title="基本信息" name="basic">
                  <el-form-item label="元素ID">
                    <el-input v-model="elementProperties.id" disabled />
                  </el-form-item>
                  <el-form-item label="元素名称">
                    <el-input v-model="elementProperties.name" @change="updateElementName" />
                  </el-form-item>
                </el-collapse-item>
                <el-collapse-item v-if="isUserTask" title="任务分配" name="assignment">
                  <el-form-item label="分配类型">
                    <el-select v-model="elementProperties.assignType" @change="handleAssignTypeChange" style="width: 100%">
                      <el-option label="指定人员" value="assignee" />
                      <el-option label="候选用户" value="candidateUsers" />
                      <el-option label="候选组" value="candidateGroups" />
                    </el-select>
                  </el-form-item>
                  <el-form-item v-if="elementProperties.assignType === 'assignee'" label="受理人">
                    <el-select v-model="elementProperties.assignee" @change="updateAssignee" filterable allow-create default-first-option style="width: 100%" placeholder="请选择受理人或输入流程变量">
                      <el-option v-for="user in users" :key="user.id" :label="user.realName || user.username" :value="user.username" />
                    </el-select>
                  </el-form-item>
                  <el-form-item v-if="elementProperties.assignType === 'candidateUsers'" label="候选用户">
                    <el-select v-model="elementProperties.candidateUsers" @change="updateCandidateUsers" multiple filterable style="width: 100%" placeholder="请选择候选用户">
                      <el-option v-for="user in users" :key="user.id" :label="user.realName || user.username" :value="user.username" />
                    </el-select>
                  </el-form-item>
                  <el-form-item v-if="elementProperties.assignType === 'candidateGroups'" label="候选组">
                    <el-select v-model="elementProperties.candidateGroups" @change="updateCandidateGroups" multiple filterable style="width: 100%" placeholder="请选择候选组">
                      <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.code" />
                    </el-select>
                  </el-form-item>
                </el-collapse-item>
                <el-collapse-item v-if="isProcess" title="流程属性" name="process">
                  <el-form-item label="流程ID">
                    <el-input v-model="elementProperties.processId" @change="updateProcessId" />
                  </el-form-item>
                  <el-form-item label="流程名称">
                    <el-input v-model="elementProperties.processName" @change="updateProcessName" />
                  </el-form-item>
                </el-collapse-item>
                <el-collapse-item v-if="isSequenceFlow" title="流转条件" name="condition">
                  <el-form-item label="条件表达式">
                    <el-input v-model="elementProperties.conditionExpression" @change="updateConditionExpression" type="textarea" :rows="3" placeholder="例如: ${days > 3}" />
                  </el-form-item>
                </el-collapse-item>
              </el-collapse>
            </el-form>
          </div>
        </div>
      </template>
      
      <!-- 表单设计 -->
      <template v-else-if="activeTab === 'form'">
        <div class="form-designer-wrapper">
          <div class="form-designer-header">
            <el-form :inline="true" size="small">
              <el-form-item label="表单名称">
                <el-input v-model="currentForm.formName" placeholder="请输入表单名称" style="width: 200px" />
              </el-form-item>
              <el-form-item label="表单标识">
                <el-input v-model="currentForm.formKey" placeholder="请输入表单标识" style="width: 200px" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSaveForm">保存表单</el-button>
                <el-button @click="handleNewForm">新建表单</el-button>
                <el-select v-model="selectedFormKey" @change="handleLoadForm" placeholder="选择已有表单" style="width: 200px; margin-left: 10px" clearable>
                  <el-option v-for="form in formList" :key="form.formKey" :label="form.formName" :value="form.formKey" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
          <FormDesigner ref="formDesignerRef" v-model="formFields" />
        </div>
      </template>
      
      <!-- 表单绑定 -->
      <template v-else-if="activeTab === 'binding'">
        <div class="form-binding-wrapper">
          <div class="binding-header">
            <el-alert type="info" :closable="false">
              <template #title>
                <span>为流程节点绑定表单，用户在发起流程或审批任务时将填写绑定的表单</span>
              </template>
            </el-alert>
          </div>
          <div class="binding-content">
            <el-table :data="bindingList" border stripe>
              <el-table-column prop="nodeId" label="节点ID" width="180">
                <template #default="{ row }">
                  <span>{{ row.nodeId || '启动表单' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="nodeName" label="节点名称" width="180">
                <template #default="{ row }">
                  <span>{{ row.nodeName || '流程启动' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="formType" label="表单类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.formType === 'start' ? 'success' : 'primary'" size="small">
                    {{ row.formType === 'start' ? '启动表单' : '任务表单' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="formKey" label="绑定表单" width="200">
                <template #default="{ row }">
                  <el-select v-model="row.formKey" placeholder="选择表单" size="small" style="width: 100%">
                    <el-option v-for="form in formList" :key="form.formKey" :label="form.formName" :value="form.formKey" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button type="danger" link size="small" @click="handleRemoveBinding(row)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div style="margin-top: 16px">
              <el-button type="primary" @click="handleSaveBindings">保存绑定</el-button>
            </div>
          </div>
        </div>
      </template>
    </div>
    
    <!-- 部署对话框 -->
    <el-dialog v-model="deployDialogVisible" title="部署流程" width="400px">
      <el-form :model="deployForm" label-width="80px">
        <el-form-item label="流程名称">
          <el-input v-model="deployForm.name" placeholder="请输入流程名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deployDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDeploySubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BpmnModeler from '@/components/BpmnModeler.vue'
import { parseBpmnXml, getDefaultXml } from '@/utils/bpmnXmlParser'
import { deployProcessByXml, getProcessDefinitionXml } from '@/api/process'
import { getAllUsers } from '@/api/user'
import { getAllRoles } from '@/api/role'
import { getFormList, createForm, updateForm, getFormByKey, getProcessFormRelations, bindFormToProcess } from '@/api/form'
import FormDesigner from '@/components/FormDesigner.vue'

const route = useRoute()
const router = useRouter()

const flowDesignerRef = ref(null)
const deployDialogVisible = ref(false)
const deployForm = reactive({ name: '' })

const activeTab = ref('process')
const formDesignerRef = ref(null)
const formFields = ref([])

const formList = ref([])
const currentForm = reactive({ id: null, formKey: '', formName: '', description: '' })
const selectedFormKey = ref('')
const bindingList = ref([])

const selectedElement = ref(null)
const activeCollapse = ref(['basic', 'assignment', 'process'])
const users = ref([])
const roles = ref([])

// BPMN XML
const processXml = ref('')
const isLoadingXml = ref(false)

const elementProperties = reactive({
  id: '', name: '', assignType: 'assignee', assignee: '',
  candidateUsers: [], candidateGroups: [], processId: '', processName: '', conditionExpression: ''
})

// 流程信息
const processInfo = reactive({
  id: 'Process_1',
  name: '新建流程'
})

const isEditMode = computed(() => !!route.params.processDefinitionId)

const isUserTask = computed(() => selectedElement.value?.type === 'bpmn:UserTask')
const isProcess = computed(() => !selectedElement.value || selectedElement.value?.type === 'bpmn:Process' || selectedElement.value?.type === 'bpmn:startEvent')
const isSequenceFlow = computed(() => selectedElement.value?.type === 'bpmn:SequenceFlow')

// 默认XML
const defaultXML = getDefaultXml('Process_1', '新建流程')

onMounted(async () => {
  await loadUsersAndRoles()
  await loadFormList()
  
  const processDefinitionId = route.params.processDefinitionId
  if (processDefinitionId) {
    isLoadingXml.value = true
    try {
      const res = await getProcessDefinitionXml(processDefinitionId)
      if (res.data) {
        processXml.value = res.data
      }
    } catch (e) {
      ElMessage.error('加载流程失败')
    } finally {
      isLoadingXml.value = false
    }
  } else {
    processXml.value = defaultXML
  }
})

const loadUsersAndRoles = async () => {
  try {
    const [userRes, roleRes] = await Promise.all([getAllUsers(), getAllRoles()])
    users.value = userRes.data || []
    roles.value = roleRes.data || []
  } catch (e) {
    console.error('加载用户角色失败', e)
  }
}

const loadFormList = async () => {
  try {
    const res = await getFormList()
    formList.value = res.data || []
  } catch (e) {
    console.error('加载表单列表失败', e)
  }
}

// XML更新
const handleXmlUpdate = async (xml) => {
  if (isLoadingXml.value) return
  processXml.value = xml
  
  // 解析XML获取节点信息更新绑定列表
  try {
    const data = await parseBpmnXml(xml)
    if (data.process) {
      processInfo.id = data.process.id
      processInfo.name = data.process.name
    }
    updateBindingList(data.nodes || [])
  } catch (e) {
    console.error('解析XML失败', e)
  }
}

// 处理元素选中
const handleElementSelected = (element) => {
  selectedElement.value = element
  
  if (!element) {
    return
  }
  
  elementProperties.id = element.id || ''
  elementProperties.name = element.name || ''
  
  // 用户任务
  if (element.type === 'bpmn:UserTask') {
    const props = element.properties || {}
    if (props.assignee) {
      elementProperties.assignType = 'assignee'
      elementProperties.assignee = props.assignee
    } else if (props.candidateUsers) {
      elementProperties.assignType = 'candidateUsers'
      elementProperties.candidateUsers = typeof props.candidateUsers === 'string' 
        ? props.candidateUsers.split(',').filter(s => s.trim())
        : props.candidateUsers || []
    } else if (props.candidateGroups) {
      elementProperties.assignType = 'candidateGroups'
      elementProperties.candidateGroups = typeof props.candidateGroups === 'string'
        ? props.candidateGroups.split(',').filter(s => s.trim())
        : props.candidateGroups || []
    } else {
      elementProperties.assignType = 'assignee'
      elementProperties.assignee = ''
    }
  }
  
  // 顺序流条件
  if (element.type === 'bpmn:SequenceFlow') {
    elementProperties.conditionExpression = element.properties?.conditionExpression || ''
  }
  
  // 流程级别属性
  if (element.type === 'bpmn:Process' || !element.type) {
    elementProperties.processId = processInfo.id
    elementProperties.processName = processInfo.name
  }
}

// 导入完成
const handleImportDone = () => {
  updateBindingList()
}

// 更新元素名称
const updateElementName = () => {
  if (!selectedElement.value || !flowDesignerRef.value) return
  flowDesignerRef.value.updateElement(elementProperties.id, {
    ...selectedElement.value.properties,
    name: elementProperties.name
  })
}

// 处理分配类型变化
const handleAssignTypeChange = () => {
  elementProperties.assignee = ''
  elementProperties.candidateUsers = []
  elementProperties.candidateGroups = []
}

// 更新受理人
const updateAssignee = () => {
  if (!selectedElement.value || !flowDesignerRef.value) return
  console.log('updateAssignee:', elementProperties.id, elementProperties.assignee)
  flowDesignerRef.value.updateElement(elementProperties.id, {
    assignee: elementProperties.assignee
  })
}

// 更新候选用户
const updateCandidateUsers = () => {
  if (!selectedElement.value || !flowDesignerRef.value) return
  flowDesignerRef.value.updateElement(elementProperties.id, {
    ...selectedElement.value.properties,
    candidateUsers: elementProperties.candidateUsers.join(','),
    assignee: undefined,
    candidateGroups: undefined
  })
}

// 更新候选组
const updateCandidateGroups = () => {
  if (!selectedElement.value || !flowDesignerRef.value) return
  flowDesignerRef.value.updateElement(elementProperties.id, {
    ...selectedElement.value.properties,
    candidateGroups: elementProperties.candidateGroups.join(','),
    assignee: undefined,
    candidateUsers: undefined
  })
}

// 更新流程ID
const updateProcessId = () => {
  processInfo.id = elementProperties.processId
}

// 更新流程名称
const updateProcessName = () => {
  processInfo.name = elementProperties.processName
}

// 更新条件表达式
const updateConditionExpression = () => {
  if (!selectedElement.value || !flowDesignerRef.value) return
  flowDesignerRef.value.updateElement(elementProperties.id, {
    ...selectedElement.value.properties,
    conditionExpression: elementProperties.conditionExpression
  })
}

// 更新绑定列表
const updateBindingList = (nodes = []) => {
  const bindings = []
  
  // 添加启动表单
  bindings.push({ nodeId: '', nodeName: '流程启动', formType: 'start', formKey: '' })
  
  // 添加所有用户任务节点
  nodes
    .filter(node => node.type === 'bpmn:userTask')
    .forEach(node => {
      bindings.push({
        nodeId: node.id,
        nodeName: node.data?.name || node.label || node.id,
        formType: 'task',
        formKey: ''
      })
    })
  
  bindingList.value = bindings
}

// 监听切换到表单绑定Tab
watch(activeTab, async (newVal) => {
  if (newVal === 'binding') {
    updateBindingList()
    // 加载已有的绑定关系
    if (processInfo.id) {
      try {
        const res = await getProcessFormRelations(processInfo.id)
        if (res.data) {
          res.data.forEach(relation => {
            const binding = bindingList.value.find(b => b.nodeId === relation.nodeId)
            if (binding) {
              binding.formKey = relation.formKey
              binding.id = relation.id
            }
          })
        }
      } catch (e) {
        console.error('加载绑定关系失败', e)
      }
    }
  }
})

// 表单相关方法
const handleSaveForm = async () => {
  if (!currentForm.formKey || !currentForm.formName) {
    ElMessage.warning('请填写表单名称和标识')
    return
  }
  try {
    const data = {
      formKey: currentForm.formKey,
      formName: currentForm.formName,
      description: currentForm.description,
      fields: formFields.value
    }
    if (currentForm.id) {
      data.id = currentForm.id
      await updateForm(data)
    } else {
      await createForm(data)
    }
    ElMessage.success('保存成功')
    await loadFormList()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleNewForm = () => {
  currentForm.id = null
  currentForm.formKey = ''
  currentForm.formName = ''
  currentForm.description = ''
  formFields.value = []
  selectedFormKey.value = ''
}

const handleLoadForm = async (formKey) => {
  if (!formKey) return
  try {
    const res = await getFormByKey(formKey)
    if (res.data) {
      currentForm.id = res.data.id
      currentForm.formKey = res.data.formKey
      currentForm.formName = res.data.formName
      currentForm.description = res.data.description
      formFields.value = res.data.fields || []
    }
  } catch (e) {
    ElMessage.error('加载表单失败')
  }
}

// 绑定相关方法
const handleRemoveBinding = (row) => {
  row.formKey = ''
}

const handleSaveBindings = async () => {
  if (!processInfo.id) {
    ElMessage.warning('请先设计流程')
    return
  }
  try {
    for (const binding of bindingList.value) {
      if (binding.formKey) {
        await bindFormToProcess({
          processDefinitionKey: processInfo.id,
          nodeId: binding.nodeId,
          formKey: binding.formKey,
          formType: binding.formType
        })
      }
    }
    ElMessage.success('保存绑定成功')
  } catch (e) {
    ElMessage.error('保存绑定失败')
  }
}

// 保存
const handleSave = async () => {
  try {
    const xml = await flowDesignerRef.value?.getXml()
    if (!xml) {
      ElMessage.error('获取流程XML失败')
      return
    }
    const blob = new Blob([xml], { type: 'application/xml' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${processInfo.id || 'process'}.bpmn20.xml`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (e) {
    console.error('保存失败:', e)
    ElMessage.error('保存失败')
  }
}

// 部署
const handleDeploy = () => {
  deployForm.name = processInfo.name
  deployDialogVisible.value = true
}

const handleDeploySubmit = async () => {
  try {
    const xml = await flowDesignerRef.value?.getXml()
    if (!xml) {
      ElMessage.error('获取流程XML失败')
      return
    }
    await deployProcessByXml(deployForm.name, xml)
    ElMessage.success('部署成功')
    deployDialogVisible.value = false
    router.push('/process/definition')
  } catch (e) {
    console.error('部署失败:', e)
    ElMessage.error('部署失败')
  }
}

const handleDownload = handleSave

// 缩放
const handleZoomIn = () => {
  flowDesignerRef.value?.zoomIn()
}

const handleZoomOut = () => {
  flowDesignerRef.value?.zoomOut()
}

const handleZoomFit = () => {
  flowDesignerRef.value?.zoomFit()
}

// 撤销
const handleUndo = () => {
  flowDesignerRef.value?.undo()
}

// 重做
const handleRedo = () => {
  flowDesignerRef.value?.redo()
}
</script>

<style scoped>
.modeler-container {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
}

.toolbar {
  padding: 10px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 8px;
}

.modeler-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.properties-panel {
  width: 320px;
  border-left: 1px solid #eee;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #303133;
}

.panel-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel-content {
  padding: 12px;
}

.panel-content :deep(.el-collapse) {
  border: none;
}

.panel-content :deep(.el-collapse-item__header) {
  background: #f5f7fa;
  padding: 0 12px;
  font-weight: 500;
  border-radius: 4px;
  margin-bottom: 8px;
}

.panel-content :deep(.el-collapse-item__content) {
  padding: 12px 0;
}

.panel-content :deep(.el-form-item) {
  margin-bottom: 16px;
}

.panel-content :deep(.el-form-item__label) {
  font-size: 13px;
  color: #606266;
  padding-bottom: 4px;
}

.form-designer-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.form-designer-header {
  padding: 10px;
  border-bottom: 1px solid #eee;
  background: #fafafa;
}

.form-binding-wrapper {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.binding-header {
  margin-bottom: 16px;
}

.binding-content {
  background: #fff;
}
</style>
