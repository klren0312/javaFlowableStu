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
      <el-button @click="handleImportXml">
        <el-icon><Upload /></el-icon>
        导入XML
      </el-button>
      <el-button @click="handleViewXml">
        <el-icon><View /></el-icon>
        查看XML
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
          :users="users"
          :roles="roles"
          @update:xml="handleXmlUpdate"
          @update:process-id="handleProcessIdUpdate"
          @update:process-name="handleProcessNameUpdate"
          @import-done="handleImportDone"
          @error="handleModelError"
        />
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
              <el-table-column prop="formKey" label="绑定表单" min-width="200">
                <template #default="{ row }">
                  <el-select v-model="row.formKey" placeholder="选择表单" size="small" style="width: 100%" @change="(val) => handleFormKeyChange(row, val)">
                    <el-option v-for="form in formList" :key="form.formKey" :label="form.formName" :value="form.formKey" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button v-if="row.formKey" type="primary" link size="small" @click="handlePreviewForm(row)">预览</el-button>
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
    
    <!-- 导入XML对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入流程XML" width="600px">
      <el-tabs v-model="importTab">
        <el-tab-pane label="上传文件" name="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xml,.bpmn,.bpmn20.xml"
            :on-change="handleFileChange"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .xml, .bpmn, .bpmn20.xml 格式的BPMN流程文件
              </div>
            </template>
          </el-upload>
        </el-tab-pane>
        <el-tab-pane label="粘贴XML" name="paste">
          <el-input
            v-model="importXmlContent"
            type="textarea"
            :rows="15"
            placeholder="请将BPMN XML内容粘贴到此处..."
          />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImportSubmit" :loading="importLoading">导入</el-button>
      </template>
    </el-dialog>
    
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
    
    <!-- 查看XML对话框 -->
    <el-dialog v-model="viewXmlDialogVisible" title="查看流程XML" width="800px">
      <div class="xml-viewer-toolbar">
        <el-button type="primary" size="small" @click="handleCopyXml">
          <el-icon><CopyDocument /></el-icon>
          复制XML
        </el-button>
      </div>
      <el-input 
        v-model="currentProcessXml" 
        type="textarea" 
        :rows="25" 
        readonly 
        class="xml-textarea"
      />
    </el-dialog>
    
    <!-- 表单预览对话框 -->
    <el-dialog v-model="previewFormDialogVisible" :title="'预览表单: ' + previewFormName" width="600px">
      <div v-if="previewFormFields.length > 0" class="form-preview">
        <el-form label-position="top" size="small">
          <el-form-item v-for="field in previewFormFields" :key="field.id" :label="field.label" :required="field.required">
            <!-- 文本输入 -->
            <el-input v-if="field.type === 'text' || field.type === 'input'" :placeholder="'请输入' + field.label" disabled />
            
            <!-- 多行文本 -->
            <el-input v-else-if="field.type === 'textarea'" type="textarea" :rows="3" :placeholder="'请输入' + field.label" disabled />
            
            <!-- 数字输入 -->
            <el-input-number v-else-if="field.type === 'number'" :min="field.min" :max="field.max" disabled />
            
            <!-- 日期选择 -->
            <el-date-picker v-else-if="field.type === 'date'" type="date" :placeholder="'请选择' + field.label" disabled />
            
            <!-- 日期时间选择 -->
            <el-date-picker v-else-if="field.type === 'datetime'" type="datetime" :placeholder="'请选择' + field.label" disabled />
            
            <!-- 下拉选择 -->
            <el-select v-else-if="field.type === 'select'" :placeholder="'请选择' + field.label" disabled>
              <el-option v-for="opt in field.options" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            
            <!-- 单选框 -->
            <el-radio-group v-else-if="field.type === 'radio'" disabled>
              <el-radio v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
            </el-radio-group>
            
            <!-- 复选框 -->
            <el-checkbox-group v-else-if="field.type === 'checkbox'" disabled>
              <el-checkbox v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</el-checkbox>
            </el-checkbox-group>
            
            <!-- 用户选择 -->
            <el-select v-else-if="field.type === 'user'" :placeholder="'请选择' + field.label" disabled />
            
            <!-- 部门选择 -->
            <el-select v-else-if="field.type === 'department'" :placeholder="'请选择' + field.label" disabled />
            
            <!-- 默认文本输入 -->
            <el-input v-else :placeholder="'请输入' + field.label" disabled />
          </el-form-item>
        </el-form>
      </div>
      <el-empty v-else description="该表单暂无字段定义" />
      <template #footer>
        <el-button @click="previewFormDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, CopyDocument } from '@element-plus/icons-vue'
import BpmnModeler from '@/components/BpmnModeler.vue'
import { parseBpmnXml, getDefaultXml } from '@/utils/bpmnXmlParser'
import { deployProcessByXml, getProcessDefinitionXml, updateProcessDefinition } from '@/api/process'
import { getAllUsers } from '@/api/user'
import { getAllRoles } from '@/api/role'
import { getFormList, createForm, updateForm, getFormByKey, getProcessFormRelations, bindFormToProcess } from '@/api/form'
import FormDesigner from '@/components/FormDesigner.vue'

const route = useRoute()
const router = useRouter()

const flowDesignerRef = ref(null)
const deployDialogVisible = ref(false)
const deployForm = reactive({ name: '' })

// 导入XML相关
const importDialogVisible = ref(false)
const importTab = ref('file')
const importXmlContent = ref('')
const importLoading = ref(false)
const uploadRef = ref(null)
let selectedFile = null

// 表单预览相关
const previewFormDialogVisible = ref(false)
const previewFormFields = ref([])
const previewFormName = ref('')

// 查看XML相关
const viewXmlDialogVisible = ref(false)
const currentProcessXml = ref('')

const activeTab = ref('process')
const formDesignerRef = ref(null)
const formFields = ref([])

const formList = ref([])
const currentForm = reactive({ id: null, formKey: '', formName: '', description: '' })
const selectedFormKey = ref('')
const bindingList = ref([])

// 用户和角色数据（传递给 BpmnModeler 组件）
const users = ref([])
const roles = ref([])

// BPMN XML
const processXml = ref('')
const isLoadingXml = ref(false)

// 流程信息
const processInfo = reactive({
  id: 'Process_1',
  name: '新建流程'
})

const isEditMode = computed(() => !!route.params.processDefinitionId)

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

// 流程ID更新
const handleProcessIdUpdate = (id) => {
  processInfo.id = id
}

// 流程名称更新
const handleProcessNameUpdate = (name) => {
  processInfo.name = name
}

// 导入完成
const handleImportDone = () => {
  updateBindingList()
}

// 处理模型错误
const handleModelError = (errorMsg) => {
  ElMessage.error(errorMsg)
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
      ElMessage.success('表单加载成功')
    } else {
      ElMessage.warning('未找到该表单，请检查表单标识是否正确')
    }
  } catch (e) {
    console.error('加载表单失败:', e)
    ElMessage.error('加载表单失败: ' + (e.message || '未知错误'))
  }
}

// 绑定相关方法
const handleFormKeyChange = async (row, val) => {
  if (val) {
    try {
      const res = await getFormByKey(val)
      if (res.data) {
        row.formName = res.data.formName
        ElMessage.success('表单选择成功')
      }
    } catch (e) {
      console.error('获取表单信息失败:', e)
    }
  }
}

const handlePreviewForm = async (row) => {
  if (!row.formKey) {
    ElMessage.warning('请先选择表单')
    return
  }
  try {
    const res = await getFormByKey(row.formKey)
    if (res.data && res.data.fields) {
      // 显示表单预览对话框
      previewFormDialogVisible.value = true
      previewFormFields.value = res.data.fields || []
      previewFormName.value = res.data.formName || ''
    } else {
      ElMessage.warning('该表单没有字段定义')
    }
  } catch (e) {
    console.error('获取表单内容失败:', e)
    ElMessage.error('获取表单内容失败')
  }
}

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
    
    // 如果是编辑模式，更新流程定义
    const processDefinitionId = route.params.processDefinitionId
    if (processDefinitionId) {
      await updateProcessDefinition(processDefinitionId, xml)
      ElMessage.success('保存成功')
    } else {
      // 新建模式，提示用户需要先部署
      ElMessage.warning('新建流程请使用"部署"按钮保存')
    }
  } catch (e) {
    console.error('保存失败:', e)
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  }
}

// 下载
const handleDownload = async () => {
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
    console.error('下载失败:', e)
    ElMessage.error('下载失败')
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


// 查看XML
const handleViewXml = async () => {
  try {
    const xml = await flowDesignerRef.value?.getXml()
    if (!xml) {
      ElMessage.error('获取流程XML失败')
      return
    }
    currentProcessXml.value = xml
    viewXmlDialogVisible.value = true
  } catch (e) {
    console.error('获取XML失败:', e)
    ElMessage.error('获取流程XML失败')
  }
}

// 复制XML
const handleCopyXml = async () => {
  try {
    await navigator.clipboard.writeText(currentProcessXml.value)
    ElMessage.success('XML已复制到剪贴板')
  } catch (e) {
    console.error('复制失败:', e)
    // 降级方案：使用传统方法复制
    const textarea = document.createElement('textarea')
    textarea.value = currentProcessXml.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('XML已复制到剪贴板')
  }
}

// 导入XML
const handleImportXml = () => {
  importDialogVisible.value = true
  importTab.value = 'file'
  importXmlContent.value = ''
  selectedFile = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

// 文件选择处理
const handleFileChange = (file) => {
  selectedFile = file.raw
}

// 导入提交
const handleImportSubmit = async () => {
  let xmlContent = ''
  
  if (importTab.value === 'file') {
    if (!selectedFile) {
      ElMessage.warning('请选择要导入的文件')
      return
    }
    
    try {
      xmlContent = await readFileContent(selectedFile)
    } catch (e) {
      ElMessage.error('读取文件失败')
      return
    }
  } else {
    if (!importXmlContent.value.trim()) {
      ElMessage.warning('请输入XML内容')
      return
    }
    xmlContent = importXmlContent.value.trim()
  }
  
  // 验证XML格式
  if (!isValidBpmnXml(xmlContent)) {
    ElMessage.error('无效的BPMN XML格式')
    return
  }
  
  importLoading.value = true
  try {
    // 加载XML到流程设计器
    await flowDesignerRef.value?.loadXml(xmlContent)
    
    // 解析XML更新流程信息
    const data = await parseBpmnXml(xmlContent)
    if (data.process) {
      processInfo.id = data.process.id
      processInfo.name = data.process.name
    }
    
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    
    // 更新绑定列表
    updateBindingList(data.nodes || [])
  } catch (e) {
    console.error('导入失败:', e)
    ElMessage.error('导入失败: ' + (e.message || '未知错误'))
  } finally {
    importLoading.value = false
  }
}

// 读取文件内容
const readFileContent = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target.result)
    reader.onerror = (e) => reject(e)
    reader.readAsText(file)
  })
}

// 验证BPMN XML格式
const isValidBpmnXml = (xml) => {
  try {
    const parser = new DOMParser()
    const doc = parser.parseFromString(xml, 'text/xml')
    const errorNode = doc.querySelector('parsererror')
    if (errorNode) return false
    
    // 检查是否包含BPMN元素
    const processEl = doc.querySelector('process') ||
                      doc.getElementsByTagNameNS('http://www.omg.org/spec/BPMN/20100524/MODEL', 'process')[0]
    if (!processEl) return false

    // 检查是否包含BPMN图表元素（bpmndi:BPMNDiagram）
    const bpmnDiagram = doc.querySelector('BPMNDiagram') ||
                        doc.querySelector('[id$="BPMNDiagram"]') ||
                        doc.getElementsByTagNameNS('http://www.omg.org/spec/BPMN/20100524/DI', 'BPMNDiagram')[0]
    return !!bpmnDiagram
  } catch (e) {
    return false
  }
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
