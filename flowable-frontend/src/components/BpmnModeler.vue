<template>
  <div class="bpmn-modeler">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button @click="handleZoomIn" title="放大">
        <el-icon><ZoomIn /></el-icon>
      </el-button>
      <el-button @click="handleZoomOut" title="缩小">
        <el-icon><ZoomOut /></el-icon>
      </el-button>
      <el-button @click="handleZoomFit" title="适应画布">
        <el-icon><FullScreen /></el-icon>
      </el-button>
      <el-divider direction="vertical" />
      <el-button @click="handleUndo" title="撤销">
        <el-icon><RefreshLeft /></el-icon>
      </el-button>
      <el-button @click="handleRedo" title="重做">
        <el-icon><RefreshRight /></el-icon>
      </el-button>
    </div>
    
    <!-- 主体区域 -->
    <div class="main-content">
      <div class="bpmn-container" ref="modelerContainer"></div>
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
    </div>
  </div>
</template>

<script setup>
/**
 * BPMN 流程设计器组件
 * 基于 bpmn-js 封装，支持：
 * - 自定义左侧工具栏（Task 按钮创建 UserTask）
 * - 自定义右键菜单（任务类型替换为 UserTask）
 * - Flowable 扩展属性支持
 * - 集成右侧属性面板
 * - 集成顶部工具栏（缩放、撤销/重做）
 */
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import { getDefaultXml } from '@/utils/bpmnXmlParser'
import flowableModdle from '@/utils/flowableModdle.json'
import { Setting, ZoomIn, ZoomOut, FullScreen, RefreshLeft, RefreshRight } from '@element-plus/icons-vue'

// ==================== Props & Emits ====================
const props = defineProps({
  processId: { type: String, default: 'Process_1' },
  processName: { type: String, default: '新建流程' },
  xml: { type: String, default: '' },
  // 用户列表（用于任务分配）
  users: { type: Array, default: () => [] },
  // 角色列表（用于候选组）
  roles: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:xml', 'update:processId', 'update:processName', 'import-done', 'error'])

// ==================== 响应式变量 ====================
const modelerContainer = ref(null)
let modeler = null

// 属性面板相关
const selectedElement = ref(null)
const activeCollapse = ref(['basic', 'assignment', 'process'])

// 防止属性更新时触发选择变化
let isUpdatingProperties = false

// 流程信息（内部维护）
const processInfo = reactive({
  id: props.processId,
  name: props.processName
})

// 元素属性
const elementProperties = reactive({
  id: '', 
  name: '', 
  assignType: 'assignee', 
  assignee: '',
  candidateUsers: [], 
  candidateGroups: [], 
  processId: '', 
  processName: '', 
  conditionExpression: ''
})

// 计算属性
const isUserTask = computed(() => selectedElement.value?.type === 'bpmn:UserTask')
const isProcess = computed(() => !selectedElement.value || selectedElement.value?.type === 'bpmn:Process' || selectedElement.value?.type === 'bpmn:startEvent')
const isSequenceFlow = computed(() => selectedElement.value?.type === 'bpmn:SequenceFlow')

// ==================== 常量定义 ====================
/** 需要显示替换按钮的任务类型 */
const REPLACEABLE_TASK_TYPES = [
  'bpmn:Task', 'bpmn:ServiceTask', 'bpmn:ScriptTask', 
  'bpmn:BusinessRuleTask', 'bpmn:ManualTask', 
  'bpmn:ReceiveTask', 'bpmn:SendTask'
]

// ==================== 自定义 Bpmn 模块 ====================
/**
 * 自定义 bpmn-js 模块
 * - customPaletteProvider: 覆盖左侧工具栏，让 Task 按钮创建 UserTask
 * - contextPadProvider: 覆盖右键菜单，自定义元素操作按钮
 */
const CustomBpmnModule = {
  __depends__: [],
  __init__: ['customPaletteProvider', 'contextPadProvider'],

  // ==================== 自定义左侧工具栏 ====================
  /**
   * 精简的工具栏，只包含：
   * - 起始事件 (StartEvent)
   * - 结束事件 (EndEvent)  
   * - 用户任务 (UserTask)
   * - 条件网关 (ExclusiveGateway)
   */
  customPaletteProvider: ['type', function(paletteProvider, create, elementFactory) {
    // 完全覆盖默认的工具栏，只返回我们需要的工具
    paletteProvider.getPaletteEntries = function() {
      /** 创建拖拽创建元素的 action */
      const createAction = (type, className, title) => {
        const createElement = (event) => {
          const shape = elementFactory.createShape({ type })
          create.start(event, shape)
        }
        return {
          group: type.includes('Event') ? 'event' : 'activity',
          className,
          title,
          action: { dragstart: createElement, click: createElement }
        }
      }

      return {
        // 起始事件
        'create.start-event': createAction('bpmn:StartEvent', 'bpmn-icon-start-event-none', '创建起始事件'),
        // 结束事件
        'create.end-event': createAction('bpmn:EndEvent', 'bpmn-icon-end-event-none', '创建结束事件'),
        // 用户任务
        'create.user-task': createAction('bpmn:UserTask', 'bpmn-icon-user-task', '创建用户任务'),
        // 条件网关
        'create.exclusive-gateway': createAction('bpmn:ExclusiveGateway', 'bpmn-icon-gateway-xor', '创建条件网关'),
      }
    }
  }],

  // ==================== 自定义右键菜单（ContextPad）====================
  contextPadProvider: ['type', function(config, injector, contextPad, elementFactory, connect, create) {
    const autoPlace = injector.get('autoPlace', false)
    const translate = injector.get('translate')
    const bpmnReplace = injector.get('bpmnReplace')

    // 高优先级注册，覆盖默认的 contextPadProvider
    contextPad.registerProvider(1000, this)

    /**
     * 创建"追加元素"操作
     * @param {string} type - 元素类型，如 'bpmn:UserTask'
     * @param {string} className - 图标类名
     * @param {string} title - 提示文字
     */
    const appendAction = (type, className, title) => {
      const appendStart = (event, element) => {
        const shape = elementFactory.createShape({ type })
        create.start(event, shape, { source: element })
      }
      const append = autoPlace 
        ? (_, element) => autoPlace.append(element, elementFactory.createShape({ type }))
        : appendStart
      return {
        group: 'model',
        className,
        title,
        action: { dragstart: appendStart, click: append }
      }
    }

    /** 开始连线操作 */
    const startConnect = (event, element) => connect.start(event, element)

    /**
     * 获取 ContextPad 按钮配置
     * 返回的 actions 对象决定了右键菜单显示哪些按钮
     */
    this.getContextPadEntries = function(element) {
      const bo = element.businessObject
      const actions = {}

      // 1. 开始事件：可追加结束事件、网关、任务、中间事件、连线
      if (bo.$type === 'bpmn:StartEvent') {
        Object.assign(actions, {
          'append.end-event': appendAction('bpmn:EndEvent', 'bpmn-icon-end-event-none', 'Append end event'),
          'append.gateway': appendAction('bpmn:ExclusiveGateway', 'bpmn-icon-gateway-none', 'Append gateway'),
          'append.task': appendAction('bpmn:UserTask', 'bpmn-icon-user-task', 'Append user task'),
          'append.event': appendAction('bpmn:IntermediateThrowEvent', 'bpmn-icon-intermediate-event-none', 'Append event'),
          'connect': {
            group: 'connect',
            className: 'bpmn-icon-connection-multi',
            title: translate('Connect'),
            action: { click: startConnect, dragstart: startConnect }
          }
        })
      }

      // 2. 其他 FlowNode（非开始/结束事件）：可追加元素 + 连线
      if (bo.$instanceOf('bpmn:FlowNode') && !['bpmn:StartEvent', 'bpmn:EndEvent'].includes(bo.$type) && !bo.isForCompensation) {
        Object.assign(actions, {
          'append.end-event': appendAction('bpmn:EndEvent', 'bpmn-icon-end-event-none', 'Append end event'),
          'append.gateway': appendAction('bpmn:ExclusiveGateway', 'bpmn-icon-gateway-none', 'Append gateway'),
          'append.task': appendAction('bpmn:UserTask', 'bpmn-icon-user-task', 'Append user task'),
          'append.event': appendAction('bpmn:IntermediateThrowEvent', 'bpmn-icon-intermediate-event-none', 'Append event'),
          'connect': {
            group: 'connect',
            className: 'bpmn-icon-connection-multi',
            title: translate('Connect'),
            action: { click: startConnect, dragstart: startConnect }
          }
        })
      }

      // 3. 任务类型：显示替换按钮（点击直接替换为 UserTask）
      if (REPLACEABLE_TASK_TYPES.includes(bo.$type)) {
        actions['replace'] = {
          group: 'edit',
          className: 'bpmn-icon-screw-wrench',
          title: 'Replace with User Task',
          action: {
            click: () => bpmnReplace.replaceElement(element, { type: 'bpmn:UserTask' })
          }
        }
      }

      return actions
    }
  }]
}

// ==================== 初始化模型器 ====================
const initModeler = () => {
  modeler = new BpmnModeler({
    container: modelerContainer.value,
    keyboard: { bindTo: document },
    moddleExtensions: { flowable: flowableModdle },
    additionalModules: [CustomBpmnModule]
  })

  // 监听元素选中，更新属性面板
  modeler.on('selection.changed', (e) => {
    // 如果正在更新属性，忽略选择变化事件
    if (isUpdatingProperties) {
      return
    }
    
    const element = e.newSelection[0]
    if (element) {
      selectedElement.value = {
        id: element.id,
        type: element.type,
        name: element.businessObject?.name || '',
        properties: getElementProperties(element)
      }
      updateElementPropertiesPanel(element)
    } else {
      selectedElement.value = null
    }
  })

  // 监听元素创建，限制开始和结束节点只能存在一个
  modeler.on('shape.added', (e) => {
    const element = e.element
    const elementRegistry = modeler.get('elementRegistry')
    const modeling = modeler.get('modeling')
    
    // 检查是否是开始事件
    if (element.type === 'bpmn:StartEvent') {
      // 查找所有开始事件
      const startEvents = elementRegistry.filter(el => el.type === 'bpmn:StartEvent' && el.id !== element.id)
      if (startEvents.length > 0) {
        // 已经存在开始事件，删除新创建的
        setTimeout(() => {
          modeling.removeShape(element)
          emit('error', '只能存在一个开始事件')
        }, 0)
        return
      }
    }
    
    // 检查是否是结束事件
    if (element.type === 'bpmn:EndEvent') {
      // 查找所有结束事件
      const endEvents = elementRegistry.filter(el => el.type === 'bpmn:EndEvent' && el.id !== element.id)
      if (endEvents.length > 0) {
        // 已经存在结束事件，删除新创建的
        setTimeout(() => {
          modeling.removeShape(element)
          emit('error', '只能存在一个结束事件')
        }, 0)
        return
      }
    }
  })

  // 监听命令栈变化，检查并限制开始和结束节点数量
  modeler.on('commandStack.changed', (e) => {
    const elementRegistry = modeler.get('elementRegistry')
    const modeling = modeler.get('modeling')
    
    // 检查是否是创建元素的命令（而不是移动、删除等操作）
    const commandStack = modeler.get('commandStack')
    const currentCommand = commandStack._currentCommand
    
    // 只在创建元素时检查，避免移动节点时误删
    if (currentCommand && currentCommand.handler && 
        (currentCommand.handler.type === 'shape.create' || 
         currentCommand.handler.type === 'element.create')) {
      
      // 检查开始事件数量
      const startEvents = elementRegistry.filter(el => el.type === 'bpmn:StartEvent')
      if (startEvents.length > 1) {
        // 删除多余的开始事件，只保留第一个
        for (let i = 1; i < startEvents.length; i++) {
          modeling.removeShape(startEvents[i])
        }
        emit('error', '只能存在一个开始事件')
      }
      
      // 检查结束事件数量
      const endEvents = elementRegistry.filter(el => el.type === 'bpmn:EndEvent')
      if (endEvents.length > 1) {
        // 删除多余的结束事件，只保留第一个
        for (let i = 1; i < endEvents.length; i++) {
          modeling.removeShape(endEvents[i])
        }
        emit('error', '只能存在一个结束事件')
      }
    }
    
    emitXml()
  })

  // 监听元素变化，导出 XML
  modeler.on('element.changed', () => emitXml())

  // 加载默认流程
  if (!props.xml) {
    loadXml(getDefaultXml(props.processId, props.processName))
  }
}

// ==================== 工具栏操作 ====================
/** 放大 */
const handleZoomIn = () => {
  const canvas = modeler.get('canvas')
  canvas.zoom(canvas.zoom() + 0.1)
}

/** 缩小 */
const handleZoomOut = () => {
  const canvas = modeler.get('canvas')
  canvas.zoom(canvas.zoom() - 0.1)
}

/** 适应画布 */
const handleZoomFit = () => {
  modeler.get('canvas').zoom('fit-viewport')
}

/** 撤销 */
const handleUndo = () => {
  modeler.get('commandStack').undo()
}

/** 重做 */
const handleRedo = () => {
  modeler.get('commandStack').redo()
}

// ==================== 属性面板操作 ====================
/** 更新属性面板显示 */
const updateElementPropertiesPanel = (element) => {
  const bo = element.businessObject
  if (!bo) return
  
  elementProperties.id = element.id || ''
  elementProperties.name = bo.name || ''
  
  // 用户任务
  if (element.type === 'bpmn:UserTask') {
    const props = getElementProperties(element)
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
    elementProperties.conditionExpression = bo.conditionExpression?.body || ''
  }
  
  // 流程级别属性
  if (element.type === 'bpmn:Process' || !element.type) {
    elementProperties.processId = processInfo.id
    elementProperties.processName = processInfo.name
  }
}

/** 更新元素名称 */
const updateElementName = () => {
  if (!selectedElement.value) return
  const element = modeler.get('elementRegistry').get(elementProperties.id)
  if (element) {
    modeler.get('modeling').updateProperties(element, { name: elementProperties.name })
  }
}

/** 处理分配类型变化 */
const handleAssignTypeChange = () => {
  elementProperties.assignee = ''
  elementProperties.candidateUsers = []
  elementProperties.candidateGroups = []
}

/** 更新受理人 */
const updateAssignee = () => {
  if (!selectedElement.value) return
  const element = modeler.get('elementRegistry').get(elementProperties.id)
  if (element) {
    modeler.get('modeling').updateProperties(element, {
      'flowable:assignee': elementProperties.assignee || null,
      'flowable:candidateUsers': null,
      'flowable:candidateGroups': null
    })
  }
}

/** 更新候选用户 */
const updateCandidateUsers = () => {
  if (!selectedElement.value) return
  const element = modeler.get('elementRegistry').get(elementProperties.id)
  if (element) {
    modeler.get('modeling').updateProperties(element, {
      'flowable:candidateUsers': elementProperties.candidateUsers.join(',') || null,
      'flowable:assignee': null,
      'flowable:candidateGroups': null
    })
  }
}

/** 更新候选组 */
const updateCandidateGroups = () => {
  if (!selectedElement.value) return
  const element = modeler.get('elementRegistry').get(elementProperties.id)
  if (element) {
    // 设置标志，防止选择变化事件清空属性面板
    isUpdatingProperties = true
    
    modeler.get('modeling').updateProperties(element, {
      'flowable:candidateGroups': elementProperties.candidateGroups.join(',') || null,
      'flowable:assignee': null,
      'flowable:candidateUsers': null
    })
    
    // 延迟重置标志，确保所有事件都处理完毕
    setTimeout(() => {
      isUpdatingProperties = false
    }, 100)
  }
}

/** 更新流程ID */
const updateProcessId = () => {
  processInfo.id = elementProperties.processId
  emit('update:processId', processInfo.id)
}

/** 更新流程名称 */
const updateProcessName = () => {
  processInfo.name = elementProperties.processName
  emit('update:processName', processInfo.name)
}

/** 更新条件表达式 */
const updateConditionExpression = () => {
  if (!selectedElement.value) return
  const element = modeler.get('elementRegistry').get(elementProperties.id)
  if (element) {
    const conditionExpression = elementProperties.conditionExpression
      ? modeler.get('moddle').create('bpmn:FormalExpression', {
          body: elementProperties.conditionExpression
        })
      : null
    modeler.get('modeling').updateProperties(element, { conditionExpression })
  }
}

// ==================== XML 加载/导出 ====================
/** 加载 XML 到设计器 */
const loadXml = async (xml) => {
  try {
    await modeler.importXML(xml)
    modeler.get('canvas').zoom('fit-viewport')
    emit('import-done')
  } catch (err) {
    console.error('加载 XML 失败:', err)
    emit('error', err.message || '加载XML失败')
  }
}

/** 导出设计器 XML */
const emitXml = async () => {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    emit('update:xml', xml)
  } catch (err) {
    console.error('导出 XML 失败:', err)
  }
}

/** 获取当前 XML */
const getXml = async () => {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    return xml
  } catch {
    return ''
  }
}

// ==================== 元素属性操作 ====================
/** 获取元素的 Flowable 扩展属性 */
const getElementProperties = (element) => {
  const bo = element.businessObject
  if (!bo) return {}

  const props = {}
  
  if (bo.$type === 'bpmn:UserTask') {
    // 读取 flowable 命名空间的属性（兼容多种存储位置）
    const getAttr = (name) => bo[name] || bo[`flowable:${name}`] || bo.$attrs?.[`flowable:${name}`] || bo.$attrs?.[name] || ''
    props.assignee = getAttr('assignee')
    props.candidateUsers = getAttr('candidateUsers')
    props.candidateGroups = getAttr('candidateGroups')
  }

  if (bo.$type === 'bpmn:SequenceFlow' && bo.conditionExpression) {
    props.conditionExpression = bo.conditionExpression.body
  }

  return props
}

/** 更新元素属性（外部调用接口） */
const updateElement = (elementId, properties) => {
  const element = modeler.get('elementRegistry').get(elementId)
  if (!element) return

  const update = {}

  // 更新名称
  if (properties.name !== undefined) {
    update.name = properties.name
  }

  // 更新 UserTask 的 flowable 属性
  if (element.type === 'bpmn:UserTask') {
    const setAttr = (name, value) => {
      if (properties[name] !== undefined) {
        update[`flowable:${name}`] = Array.isArray(value) ? value.join(',') : (value || null)
      }
    }
    setAttr('assignee', properties.assignee)
    setAttr('candidateUsers', properties.candidateUsers)
    setAttr('candidateGroups', properties.candidateGroups)
  }

  // 更新顺序流条件
  if (element.type === 'bpmn:SequenceFlow' && properties.conditionExpression !== undefined) {
    update.conditionExpression = modeler.get('moddle').create('bpmn:FormalExpression', {
      body: properties.conditionExpression
    })
  }

  if (Object.keys(update).length > 0) {
    modeler.get('modeling').updateProperties(element, update)
  }
}

// ==================== 辅助操作 ====================
/** 删除元素 */
const deleteElement = (elementId) => {
  const element = modeler.get('elementRegistry').get(elementId)
  if (element) modeler.get('modeling').removeElements([element])
}

/** 缩放操作 */
const zoom = (delta) => modeler.get('canvas').zoom(delta)
const zoomFit = () => modeler.get('canvas').zoom('fit-viewport')

/** 撤销/重做 */
const undo = () => modeler.get('commandStack').undo()
const redo = () => modeler.get('commandStack').redo()

/** 选中元素 */
const selectElement = (elementId) => {
  const element = modeler.get('elementRegistry').get(elementId)
  if (element) modeler.get('selection').select(element)
}

/** 获取流程信息 */
const getProcessInfo = () => ({ ...processInfo })

// ==================== 暴露给父组件 ====================
defineExpose({
  loadXml, getXml, updateElement, deleteElement,
  zoomIn: () => zoom(modeler.get('canvas').zoom() + 0.1),
  zoomOut: () => zoom(modeler.get('canvas').zoom() - 0.1),
  zoomReset: () => zoom(1),
  zoomFit, getGraphData: getXml, undo, redo, selectElement,
  getProcessInfo
})

// ==================== 生命周期 ====================
onMounted(() => initModeler())
onBeforeUnmount(() => modeler?.destroy())

// 监听外部 XML 变化
watch(() => props.xml, (newXml) => {
  if (newXml && modeler) loadXml(newXml)
})

// 监听外部 processId/processName 变化
watch(() => props.processId, (newVal) => {
  processInfo.id = newVal
})
watch(() => props.processName, (newVal) => {
  processInfo.name = newVal
})
</script>

<style scoped>
.bpmn-modeler {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar {
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 4px;
  background: #fff;
}

.toolbar .el-button {
  padding: 6px 10px;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.bpmn-container {
  flex: 1;
  height: 100%;
  background: #fff;
}

.properties-panel {
  width: 320px;
  border-left: 1px solid #eee;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  background: #fff;
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

:deep(.bjs-container) { height: 100%; }
:deep(.djs-palette) { left: 20px; top: 20px; }
:deep(.djs-element) { cursor: pointer; }
:deep(.djs-element.selected .djs-outline) {
  stroke: #409eff;
  stroke-width: 2px;
}
</style>
