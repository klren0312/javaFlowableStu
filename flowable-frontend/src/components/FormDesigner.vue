<template>
  <div class="form-designer">
    <div class="form-toolbar">
      <el-dropdown @command="handleAddField" trigger="click">
        <el-button type="primary" size="small">
          <el-icon><Plus /></el-icon>
          添加字段
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="text">
              <el-icon><Edit /></el-icon> 单行文本
            </el-dropdown-item>
            <el-dropdown-item command="textarea">
              <el-icon><Document /></el-icon> 多行文本
            </el-dropdown-item>
            <el-dropdown-item command="number">
              <el-icon><Star /></el-icon> 数字
            </el-dropdown-item>
            <el-dropdown-item command="select">
              <el-icon><ArrowDown /></el-icon> 下拉选择
            </el-dropdown-item>
            <el-dropdown-item command="radio">
              <el-icon><Select /></el-icon> 单选框
            </el-dropdown-item>
            <el-dropdown-item command="checkbox">
              <el-icon><Check /></el-icon> 复选框
            </el-dropdown-item>
            <el-dropdown-item command="date">
              <el-icon><Calendar /></el-icon> 日期
            </el-dropdown-item>
            <el-dropdown-item command="datetime">
              <el-icon><Clock /></el-icon> 日期时间
            </el-dropdown-item>
            <el-dropdown-item command="user">
              <el-icon><User /></el-icon> 人员选择
            </el-dropdown-item>
            <el-dropdown-item command="department">
              <el-icon><OfficeBuilding /></el-icon> 部门选择
            </el-dropdown-item>
            <el-dropdown-item command="file">
              <el-icon><Folder /></el-icon> 附件上传
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button size="small" @click="handlePreview">
        <el-icon><View /></el-icon>
        预览
      </el-button>
      <el-button size="small" @click="handleClear" type="danger">
        <el-icon><Delete /></el-icon>
        清空
      </el-button>
    </div>
    
    <div class="form-content">
      <!-- 字段列表 -->
      <div v-if="fields.length === 0" class="form-empty">
        <el-empty description="暂无表单字段，请点击上方按钮添加" :image-size="100" />
      </div>
      
      <draggable 
        v-else
        v-model="fields" 
        item-key="id"
        handle=".drag-handle"
        animation="200"
        class="field-list"
      >
        <template #item="{ element, index }">
          <div class="field-item" :class="{ 'is-editing': editingIndex === index }">
            <div class="field-header">
              <el-icon class="drag-handle"><Rank /></el-icon>
              <span class="field-type">{{ getFieldTypeLabel(element.type) }}</span>
              <span class="field-name">{{ element.label }}</span>
              <div class="field-actions">
                <el-button type="primary" link size="small" @click="handleEditField(index)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button type="danger" link size="small" @click="handleDeleteField(index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <div class="field-preview">
              <form-field-preview :field="element" :disabled="true" />
            </div>
          </div>
        </template>
      </draggable>
    </div>
    
    <!-- 字段编辑对话框 -->
    <el-dialog v-model="fieldDialogVisible" :title="isEditField ? '编辑字段' : '添加字段'" width="500px">
      <el-form :model="currentField" :rules="fieldRules" ref="fieldFormRef" label-width="100px">
        <el-form-item label="字段类型">
          <el-input :value="getFieldTypeLabel(currentField.type)" disabled />
        </el-form-item>
        <el-form-item label="字段名称" prop="name">
          <el-input v-model="currentField.name" placeholder="请输入字段名称（英文标识）" />
        </el-form-item>
        <el-form-item label="显示标签" prop="label">
          <el-input v-model="currentField.label" placeholder="请输入显示标签" />
        </el-form-item>
        <el-form-item label="默认值">
          <el-input v-model="currentField.defaultValue" placeholder="请输入默认值" />
        </el-form-item>
        <el-form-item label="占位提示">
          <el-input v-model="currentField.placeholder" placeholder="请输入占位提示" />
        </el-form-item>
        <el-form-item label="是否必填">
          <el-switch v-model="currentField.required" />
        </el-form-item>
        <el-form-item label="是否只读">
          <el-switch v-model="currentField.readonly" />
        </el-form-item>
        <el-form-item label="是否隐藏">
          <el-switch v-model="currentField.hidden" />
        </el-form-item>
        
        <!-- 选项配置（下拉选择、单选、复选） -->
        <template v-if="['select', 'radio', 'checkbox'].includes(currentField.type)">
          <el-form-item label="选项配置">
            <div class="options-config">
              <div v-for="(option, optIndex) in currentField.options" :key="optIndex" class="option-item">
                <el-input v-model="option.label" placeholder="选项名称" style="width: 120px" />
                <el-input v-model="option.value" placeholder="选项值" style="width: 100px; margin-left: 8px" />
                <el-button type="danger" link @click="removeOption(optIndex)" style="margin-left: 8px">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button type="primary" link @click="addOption">
                <el-icon><Plus /></el-icon> 添加选项
              </el-button>
            </div>
          </el-form-item>
        </template>
        
        <!-- 数字配置 -->
        <template v-if="currentField.type === 'number'">
          <el-form-item label="最小值">
            <el-input-number v-model="currentField.min" :controls="false" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最大值">
            <el-input-number v-model="currentField.max" :controls="false" style="width: 100%" />
          </el-form-item>
          <el-form-item label="步长">
            <el-input-number v-model="currentField.step" :min="0.01" :controls="false" style="width: 100%" />
          </el-form-item>
        </template>
        
        <!-- 文本配置 -->
        <template v-if="['text', 'textarea'].includes(currentField.type)">
          <el-form-item label="最大长度">
            <el-input-number v-model="currentField.maxLength" :min="1" :controls="false" style="width: 100%" />
          </el-form-item>
        </template>
        
        <el-form-item label="校验规则">
          <el-input v-model="currentField.pattern" placeholder="正则表达式，如: ^1[3-9]\d{9}$" />
        </el-form-item>
        <el-form-item label="校验提示">
          <el-input v-model="currentField.patternMessage" placeholder="校验失败时的提示信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fieldDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleFieldSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 表单预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="表单预览" width="600px">
      <el-form :model="previewData" label-width="120px">
        <template v-for="field in fields" :key="field.id">
          <el-form-item 
            :label="field.label" 
            :required="field.required"
            :prop="field.name"
          >
            <form-field-preview 
              :field="field" 
              v-model="previewData[field.name]" 
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleValidatePreview">验证表单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import draggable from 'vuedraggable'
import FormFieldPreview from './FormFieldPreview.vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

// 深度比较两个数组是否相等
const isEqual = (a, b) => {
  return JSON.stringify(a) === JSON.stringify(b)
}

// 字段列表
const fields = ref([])
let isInternalUpdate = false

// 监听外部值变化
watch(() => props.modelValue, (val) => {
  if (isInternalUpdate) return
  if (val && Array.isArray(val)) {
    if (!isEqual(val, fields.value)) {
      isInternalUpdate = true
      fields.value = JSON.parse(JSON.stringify(val))
      isInternalUpdate = false
    }
  }
}, { immediate: true })

// 监听内部值变化，同步到外部
watch(fields, (val) => {
  if (!isInternalUpdate) {
    emit('update:modelValue', val)
  }
})

// 字段编辑相关
const fieldDialogVisible = ref(false)
const fieldFormRef = ref(null)
const editingIndex = ref(-1)
const isEditField = computed(() => editingIndex.value >= 0)

const currentField = reactive({
  id: '',
  type: '',
  name: '',
  label: '',
  defaultValue: '',
  placeholder: '',
  required: false,
  readonly: false,
  hidden: false,
  options: [],
  min: undefined,
  max: undefined,
  step: 1,
  maxLength: 255,
  pattern: '',
  patternMessage: ''
})

const fieldRules = {
  name: [
    { required: true, message: '请输入字段名称', trigger: 'blur' },
    { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '字段名称只能包含字母、数字和下划线，且不能以数字开头', trigger: 'blur' }
  ],
  label: [
    { required: true, message: '请输入显示标签', trigger: 'blur' }
  ]
}

// 字段类型映射
const fieldTypeMap = {
  text: '单行文本',
  textarea: '多行文本',
  number: '数字',
  select: '下拉选择',
  radio: '单选框',
  checkbox: '复选框',
  date: '日期',
  datetime: '日期时间',
  user: '人员选择',
  department: '部门选择',
  file: '附件上传'
}

const getFieldTypeLabel = (type) => {
  return fieldTypeMap[type] || type
}

// 生成唯一ID
const generateId = () => {
  return 'field_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

// 添加字段
const handleAddField = (type) => {
  editingIndex.value = -1
  resetCurrentField()
  currentField.id = generateId()
  currentField.type = type
  currentField.name = ''
  currentField.label = ''
  
  // 根据类型设置默认选项
  if (['select', 'radio', 'checkbox'].includes(type)) {
    currentField.options = [
      { label: '选项1', value: '1' },
      { label: '选项2', value: '2' }
    ]
  }
  
  fieldDialogVisible.value = true
}

// 编辑字段
const handleEditField = (index) => {
  editingIndex.value = index
  const field = fields.value[index]
  Object.keys(currentField).forEach(key => {
    if (field[key] !== undefined) {
      currentField[key] = JSON.parse(JSON.stringify(field[key]))
    }
  })
  fieldDialogVisible.value = true
}

// 删除字段
const handleDeleteField = async (index) => {
  try {
    await ElMessageBox.confirm('确定要删除该字段吗？', '提示', {
      type: 'warning'
    })
    fields.value.splice(index, 1)
  } catch (e) {
    // 取消删除
  }
}

// 清空表单
const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有字段吗？', '提示', {
      type: 'warning'
    })
    fields.value = []
  } catch (e) {
    // 取消清空
  }
}

// 提交字段
const handleFieldSubmit = async () => {
  try {
    await fieldFormRef.value.validate()
    
    // 检查字段名是否重复
    const existingIndex = fields.value.findIndex(f => f.name === currentField.name && f.id !== currentField.id)
    if (existingIndex >= 0) {
      ElMessage.error('字段名称已存在')
      return
    }
    
    const fieldData = JSON.parse(JSON.stringify(currentField))
    
    if (isEditField.value) {
      fields.value[editingIndex.value] = fieldData
    } else {
      fields.value.push(fieldData)
    }
    
    fieldDialogVisible.value = false
  } catch (e) {
    // 验证失败
  }
}

// 重置当前字段
const resetCurrentField = () => {
  Object.keys(currentField).forEach(key => {
    if (key === 'options') {
      currentField[key] = []
    } else if (['required', 'readonly', 'hidden'].includes(key)) {
      currentField[key] = false
    } else if (key === 'step') {
      currentField[key] = 1
    } else if (key === 'maxLength') {
      currentField[key] = 255
    } else {
      currentField[key] = ''
    }
  })
}

// 添加选项
const addOption = () => {
  currentField.options.push({
    label: `选项${currentField.options.length + 1}`,
    value: String(currentField.options.length + 1)
  })
}

// 删除选项
const removeOption = (index) => {
  currentField.options.splice(index, 1)
}

// 预览相关
const previewDialogVisible = ref(false)
const previewData = reactive({})

const handlePreview = () => {
  if (fields.value.length === 0) {
    ElMessage.warning('请先添加表单字段')
    return
  }
  // 重置预览数据
  Object.keys(previewData).forEach(key => delete previewData[key])
  // 设置默认值
  fields.value.forEach(field => {
    if (field.defaultValue) {
      previewData[field.name] = field.defaultValue
    }
  })
  previewDialogVisible.value = true
}

const handleValidatePreview = () => {
  // 验证必填字段
  for (const field of fields.value) {
    if (field.required && !previewData[field.name]) {
      ElMessage.warning(`请填写${field.label}`)
      return
    }
  }
  ElMessage.success('表单验证通过')
}

// 暴露方法给父组件
defineExpose({
  getFields: () => fields.value,
  setFields: (newFields) => {
    fields.value = JSON.parse(JSON.stringify(newFields))
  }
})
</script>

<style scoped>
.form-designer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.form-toolbar {
  padding: 10px;
  border-bottom: 1px solid #eee;
  display: flex;
  gap: 8px;
}

.form-content {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.form-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-item {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
  background: #fafafa;
  transition: all 0.3s;
}

.field-item:hover {
  border-color: #409eff;
}

.field-item.is-editing {
  border-color: #409eff;
  background: #ecf5ff;
}

.field-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.drag-handle {
  cursor: move;
  color: #909399;
}

.drag-handle:hover {
  color: #409eff;
}

.field-type {
  font-size: 12px;
  color: #909399;
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 2px;
}

.field-name {
  font-weight: 500;
  flex: 1;
}

.field-actions {
  opacity: 0;
  transition: opacity 0.3s;
}

.field-item:hover .field-actions {
  opacity: 1;
}

.field-preview {
  padding: 8px;
  background: #fff;
  border-radius: 4px;
}

.options-config {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  align-items: center;
}
</style>
