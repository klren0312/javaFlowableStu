<template>
  <div class="form-field-preview">
    <!-- 单行文本 -->
    <el-input
      v-if="field.type === 'text'"
      v-model="localValue"
      :placeholder="field.placeholder || '请输入' + field.label"
      :disabled="disabled || field.readonly"
      :maxlength="field.maxLength"
      clearable
    />
    
    <!-- 多行文本 -->
    <el-input
      v-else-if="field.type === 'textarea'"
      v-model="localValue"
      type="textarea"
      :placeholder="field.placeholder || '请输入' + field.label"
      :disabled="disabled || field.readonly"
      :maxlength="field.maxLength"
      :rows="3"
    />
    
    <!-- 数字 -->
    <el-input-number
      v-else-if="field.type === 'number'"
      v-model="localValue"
      :placeholder="field.placeholder || '请输入' + field.label"
      :disabled="disabled || field.readonly"
      :min="field.min"
      :max="field.max"
      :step="field.step || 1"
      style="width: 100%"
    />
    
    <!-- 下拉选择 -->
    <el-select
      v-else-if="field.type === 'select'"
      v-model="localValue"
      :placeholder="field.placeholder || '请选择' + field.label"
      :disabled="disabled || field.readonly"
      clearable
      style="width: 100%"
    >
      <el-option
        v-for="option in field.options"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
    
    <!-- 单选框 -->
    <el-radio-group
      v-else-if="field.type === 'radio'"
      v-model="localValue"
      :disabled="disabled || field.readonly"
    >
      <el-radio
        v-for="option in field.options"
        :key="option.value"
        :label="option.value"
      >
        {{ option.label }}
      </el-radio>
    </el-radio-group>
    
    <!-- 复选框 -->
    <el-checkbox-group
      v-else-if="field.type === 'checkbox'"
      v-model="checkboxValue"
      :disabled="disabled || field.readonly"
    >
      <el-checkbox
        v-for="option in field.options"
        :key="option.value"
        :label="option.value"
      >
        {{ option.label }}
      </el-checkbox>
    </el-checkbox-group>
    
    <!-- 日期 -->
    <el-date-picker
      v-else-if="field.type === 'date'"
      v-model="localValue"
      type="date"
      :placeholder="field.placeholder || '请选择日期'"
      :disabled="disabled || field.readonly"
      value-format="YYYY-MM-DD"
      style="width: 100%"
    />
    
    <!-- 日期时间 -->
    <el-date-picker
      v-else-if="field.type === 'datetime'"
      v-model="localValue"
      type="datetime"
      :placeholder="field.placeholder || '请选择日期时间'"
      :disabled="disabled || field.readonly"
      value-format="YYYY-MM-DD HH:mm:ss"
      style="width: 100%"
    />
    
    <!-- 人员选择 -->
    <el-select
      v-else-if="field.type === 'user'"
      v-model="localValue"
      :placeholder="field.placeholder || '请选择人员'"
      :disabled="disabled || field.readonly"
      filterable
      clearable
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
      v-model="localValue"
      :placeholder="field.placeholder || '请选择部门'"
      :disabled="disabled || field.readonly"
      filterable
      clearable
      style="width: 100%"
    >
      <el-option
        v-for="dept in departments"
        :key="dept.id"
        :label="dept.name"
        :value="dept.id"
      />
    </el-select>
    
    <!-- 附件上传 -->
    <el-upload
      v-else-if="field.type === 'file'"
      v-model:file-list="fileList"
      :disabled="disabled || field.readonly"
      action="#"
      :auto-upload="false"
      :limit="5"
    >
      <el-button type="primary" size="small" :disabled="disabled || field.readonly">
        点击上传
      </el-button>
      <template #tip>
        <div class="el-upload__tip" v-if="!disabled && !field.readonly">
          支持上传多个文件，单个文件不超过10MB
        </div>
      </template>
    </el-upload>
    
    <!-- 未知类型 -->
    <el-input
      v-else
      v-model="localValue"
      :placeholder="'未知字段类型: ' + field.type"
      disabled
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getAllUsers } from '@/api/user'
import { getAllDepartments } from '@/api/department'

const props = defineProps({
  field: {
    type: Object,
    required: true
  },
  modelValue: {
    type: [String, Number, Array, Date],
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

// 本地值处理（双向绑定）
const localValue = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 用户和部门列表
const users = ref([])
const departments = ref([])

// 加载用户和部门数据
onMounted(async () => {
  if (props.field.type === 'user') {
    try {
      const res = await getAllUsers()
      users.value = res.data || []
    } catch (e) {
      console.error('加载用户失败', e)
    }
  }
  if (props.field.type === 'department') {
    try {
      const res = await getAllDepartments()
      departments.value = res.data || []
    } catch (e) {
      console.error('加载部门失败', e)
    }
  }
})

// 复选框值处理（需要数组）
const checkboxValue = computed({
  get: () => {
    if (Array.isArray(props.modelValue)) {
      return props.modelValue
    }
    if (typeof props.modelValue === 'string' && props.modelValue) {
      return props.modelValue.split(',').filter(s => s.trim())
    }
    return []
  },
  set: (val) => {
    emit('update:modelValue', val)
  }
})

// 文件列表
const fileList = ref([])

// 监听文件列表变化
watch(fileList, (val) => {
  // 将文件列表转换为字符串存储
  const fileNames = val.map(f => f.name).join(',')
  emit('update:modelValue', fileNames)
}, { deep: true })
</script>

<style scoped>
.form-field-preview {
  width: 100%;
}

:deep(.el-radio-group),
:deep(.el-checkbox-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

:deep(.el-upload__tip) {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>