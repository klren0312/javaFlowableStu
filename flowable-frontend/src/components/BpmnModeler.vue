<template>
  <div class="bpmn-modeler">
    <div class="bpmn-container" ref="modelerContainer"></div>
  </div>
</template>

<script setup>
/**
 * BPMN 流程设计器组件
 * 基于 bpmn-js 封装，支持：
 * - 自定义左侧工具栏（Task 按钮创建 UserTask）
 * - 自定义右键菜单（任务类型替换为 UserTask）
 * - Flowable 扩展属性支持
 */
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import { getDefaultXml } from '@/utils/bpmnXmlParser'
import flowableModdle from '@/utils/flowableModdle.json'

// ==================== Props & Emits ====================
const props = defineProps({
  processId: { type: String, default: 'Process_1' },
  processName: { type: String, default: '新建流程' },
  xml: { type: String, default: '' }
})

const emit = defineEmits(['update:xml', 'element-selected', 'element-changed', 'import-done', 'error'])

// ==================== 响应式变量 ====================
const modelerContainer = ref(null)
let modeler = null

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

      // 1. 开始事件：可追加结束事件、网关、任务、中间事件
      if (bo.$type === 'bpmn:StartEvent') {
        Object.assign(actions, {
          'append.end-event': appendAction('bpmn:EndEvent', 'bpmn-icon-end-event-none', 'Append end event'),
          'append.gateway': appendAction('bpmn:ExclusiveGateway', 'bpmn-icon-gateway-none', 'Append gateway'),
          'append.task': appendAction('bpmn:UserTask', 'bpmn-icon-user-task', 'Append user task'),
          'append.event': appendAction('bpmn:IntermediateThrowEvent', 'bpmn-icon-intermediate-event-none', 'Append event')
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

  // 监听元素选中，通知父组件
  modeler.on('selection.changed', (e) => {
    const element = e.newSelection[0]
    if (element) {
      emit('element-selected', {
        id: element.id,
        type: element.type,
        name: element.businessObject?.name || '',
        properties: getElementProperties(element)
      })
    } else {
      emit('element-selected', null)
    }
  })

  // 监听元素变化，导出 XML
  modeler.on('element.changed', () => emitXml())
  modeler.on('commandStack.changed', () => emitXml())

  // 加载默认流程
  if (!props.xml) {
    loadXml(getDefaultXml(props.processId, props.processName))
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

/** 更新元素属性 */
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
    // 通知父组件属性已更新
    emit('element-selected', {
      id: element.id,
      type: element.type,
      name: element.businessObject?.name || '',
      properties: getElementProperties(element)
    })
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

// ==================== 暴露给父组件 ====================
defineExpose({
  loadXml, getXml, updateElement, deleteElement,
  zoomIn: () => zoom(modeler.get('canvas').zoom() + 0.1),
  zoomOut: () => zoom(modeler.get('canvas').zoom() - 0.1),
  zoomReset: () => zoom(1),
  zoomFit, getGraphData: getXml, undo, redo, selectElement
})

// ==================== 生命周期 ====================
onMounted(() => initModeler())
onBeforeUnmount(() => modeler?.destroy())

// 监听外部 XML 变化
watch(() => props.xml, (newXml) => {
  if (newXml && modeler) loadXml(newXml)
})
</script>

<style scoped>
.bpmn-modeler {
  width: 100%;
  height: 100%;
  display: flex;
}

.bpmn-container {
  flex: 1;
  height: 100%;
  background: #fff;
}

:deep(.bjs-container) { height: 100%; }
:deep(.djs-palette) { left: 20px; top: 20px; }
:deep(.djs-element) { cursor: pointer; }
:deep(.djs-element.selected .djs-outline) {
  stroke: #409eff;
  stroke-width: 2px;
}
</style>
