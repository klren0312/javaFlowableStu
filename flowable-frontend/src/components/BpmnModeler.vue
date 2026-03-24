<template>
  <div class="bpmn-modeler">
    <div class="bpmn-container" ref="modelerContainer"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import { is, isAny } from 'bpmn-js/lib/util/ModelUtil'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import { getDefaultXml } from '@/utils/bpmnXmlParser'

const props = defineProps({
  processId: {
    type: String,
    default: 'Process_1'
  },
  processName: {
    type: String,
    default: '新建流程'
  },
  xml: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:xml', 'element-selected', 'element-changed', 'import-done'])

const modelerContainer = ref(null)
let modeler = null

// 自定义模块 - 将Palette/ContextPad中的Task改为UserTask，并修改Change element菜单
const CustomBpmnModule = {
  __depends__: [],
  __init__: ['customPaletteProvider', 'customReplaceMenuProvider', 'contextPadProvider'],
  
  // 覆盖默认的contextPadProvider
  contextPadProvider: ['type', function(config, injector, eventBus, contextPad, modeling, elementFactory, connect, create, popupMenu, canvas, rules, translate, appendPreview) {
    var autoPlace = injector.get('autoPlace', false)
    var assign = Object.assign

    // 获取替换菜单位置
    function getReplaceMenuPosition(element) {
      var pad = contextPad.getPad(element).html
      var padRect = pad.getBoundingClientRect()
      return { x: padRect.right + 5, y: padRect.top }
    }

    // 检查事件类型
    function isEventType(eventBo, type, definitionType) {
      var isType = eventBo.$instanceOf(type)
      var isDefinition = false
      var definitions = eventBo.eventDefinitions || []
      definitions.forEach(function(def) {
        if (def.$type === definitionType) {
          isDefinition = true
        }
      })
      return isType && isDefinition
    }

    // append action
    function appendAction(type, className, title, options) {
      function appendStart(event, element) {
        var shape = elementFactory.createShape(assign({ type: type }, options))
        create.start(event, shape, { source: element })
      }

      var append = autoPlace ? function(_, element) {
        var shape = elementFactory.createShape(assign({ type: type }, options))
        autoPlace.append(element, shape)
      } : appendStart

      return {
        group: 'model',
        className: className,
        title: title,
        action: {
          dragstart: appendStart,
          click: append
        }
      }
    }

    // start connect
    function startConnect(event, element) {
      connect.start(event, element)
    }

    // 注册provider
    contextPad.registerProvider(1000, this) // 高优先级

    this.getContextPadEntries = function(element) {
      var actions = {}
      var businessObject = element.businessObject

      // 开始事件
      if (businessObject.$type === 'bpmn:StartEvent') {
        assign(actions, {
          'append.end-event': appendAction('bpmn:EndEvent', 'bpmn-icon-end-event-none', translate('Append end event')),
          'append.gateway': appendAction('bpmn:ExclusiveGateway', 'bpmn-icon-gateway-none', translate('Append gateway')),
          'append.append-task': appendAction('bpmn:UserTask', 'bpmn-icon-user-task', translate('Append user task')),
          'append.intermediate-event': appendAction('bpmn:IntermediateThrowEvent', 'bpmn-icon-intermediate-event-none', translate('Append intermediate/boundary event'))
        })
      }

      // 其他FlowNode（非结束事件）
      if (businessObject.$instanceOf('bpmn:FlowNode') && 
          businessObject.$type !== 'bpmn:EndEvent' &&
          businessObject.$type !== 'bpmn:StartEvent' &&
          !businessObject.isForCompensation) {
        assign(actions, {
          'append.end-event': appendAction('bpmn:EndEvent', 'bpmn-icon-end-event-none', translate('Append end event')),
          'append.gateway': appendAction('bpmn:ExclusiveGateway', 'bpmn-icon-gateway-none', translate('Append gateway')),
          'append.append-task': appendAction('bpmn:UserTask', 'bpmn-icon-user-task', translate('Append user task')),
          'append.intermediate-event': appendAction('bpmn:IntermediateThrowEvent', 'bpmn-icon-intermediate-event-none', translate('Append intermediate/boundary event'))
        })
      }

      // 替换菜单
      if (!popupMenu.isEmpty(element, 'bpmn-replace')) {
        assign(actions, {
          'replace': {
            group: 'edit',
            className: 'bpmn-icon-screw-wrench',
            title: translate('Change element'),
            action: {
              click: function(event, element) {
                var position = assign(getReplaceMenuPosition(element), {
                  cursor: { x: event.x, y: event.y }
                })
                popupMenu.open(element, 'bpmn-replace', position, {
                  title: translate('Change element'),
                  width: 300,
                  search: true
                })
              }
            }
          }
        })
      }

      // 连接
      if (businessObject.$instanceOf('bpmn:FlowNode') && 
          businessObject.$type !== 'bpmn:EndEvent') {
        assign(actions, {
          'connect': {
            group: 'connect',
            className: 'bpmn-icon-connection-multi',
            title: translate('Connect using sequence flow'),
            action: {
              click: startConnect,
              dragstart: startConnect
            }
          }
        })
      }

      return actions
    }
  }],
  
  // 自定义Palette提供者 - 让Task按钮直接创建UserTask
  customPaletteProvider: ['type', function(paletteProvider, create, elementFactory) {
    const originalGetPaletteEntries = paletteProvider.getPaletteEntries.bind(paletteProvider)
    
    paletteProvider.getPaletteEntries = function(element) {
      const entries = originalGetPaletteEntries(element)
      
      if (entries['create.task']) {
        entries['create.task'] = {
          group: 'activity',
          className: 'bpmn-icon-user-task',
          title: 'Create User Task',
          action: {
            dragstart: createTask,
            click: createTask
          }
        }
      }
      
      function createTask(event) {
        const shape = elementFactory.createShape({ type: 'bpmn:UserTask' })
        create.start(event, shape)
      }
      
      return entries
    }
  }],
  
  // 自定义替换菜单提供者
  customReplaceMenuProvider: ['type', function(popupMenu, bpmnReplace, translate) {
    const customTaskOptions = [
      { label: 'User task', actionName: 'replace-with-user-task', className: 'bpmn-icon-user', target: { type: 'bpmn:UserTask' } },
      { label: 'Task', actionName: 'replace-with-task', className: 'bpmn-icon-task', target: { type: 'bpmn:Task' } },
      { label: 'Service task', actionName: 'replace-with-service-task', className: 'bpmn-icon-service', target: { type: 'bpmn:ServiceTask' } },
      { label: 'Send task', actionName: 'replace-with-send-task', className: 'bpmn-icon-send', target: { type: 'bpmn:SendTask' } },
      { label: 'Receive task', actionName: 'replace-with-receive-task', className: 'bpmn-icon-receive', target: { type: 'bpmn:ReceiveTask' } },
      { label: 'Manual task', actionName: 'replace-with-manual-task', className: 'bpmn-icon-manual', target: { type: 'bpmn:ManualTask' } },
      { label: 'Business rule task', actionName: 'replace-with-rule-task', className: 'bpmn-icon-business-rule', target: { type: 'bpmn:BusinessRuleTask' } },
      { label: 'Script task', actionName: 'replace-with-script-task', className: 'bpmn-icon-script', target: { type: 'bpmn:ScriptTask' } },
      { label: 'Call activity', actionName: 'replace-with-call-activity', className: 'bpmn-icon-call-activity', target: { type: 'bpmn:CallActivity' } },
      { label: 'Sub-process (collapsed)', actionName: 'replace-with-collapsed-subprocess', className: 'bpmn-icon-subprocess-collapsed', target: { type: 'bpmn:SubProcess', isExpanded: false } },
      { label: 'Sub-process (expanded)', actionName: 'replace-with-expanded-subprocess', className: 'bpmn-icon-subprocess-expanded', target: { type: 'bpmn:SubProcess', isExpanded: true } }
    ]

    popupMenu.registerProvider('bpmn-replace', {
      getPopupMenuEntries: function(target) {
        const entries = {}
        const businessObject = target.businessObject
        const taskTypes = ['bpmn:Task', 'bpmn:UserTask', 'bpmn:ServiceTask', 'bpmn:SendTask', 'bpmn:ReceiveTask', 'bpmn:ManualTask', 'bpmn:BusinessRuleTask', 'bpmn:ScriptTask', 'bpmn:CallActivity']

        if (businessObject && taskTypes.includes(businessObject.$type)) {
          customTaskOptions.forEach(option => {
            if (businessObject.$type !== option.target.type) {
              entries[option.actionName] = {
                label: translate(option.label),
                className: option.className,
                action: function() {
                  return bpmnReplace.replaceElement(target, option.target)
                }
              }
            }
          })
        }
        return entries
      }
    })
  }]
}

// 初始化模型器
const initModeler = () => {
  modeler = new BpmnModeler({
    container: modelerContainer.value,
    keyboard: {
      bindTo: document
    },
    moddleExtensions: {},
    additionalModules: [
      CustomBpmnModule
    ]
  })

  // 监听元素选中
  modeler.on('selection.changed', (e) => {
    const element = e.newSelection[0]
    if (element) {
      const props = getElementProperties(element)
      console.log('selection.changed element:', element.id, 'props:', props, 'bo.assignee:', element.businessObject?.assignee)
      emit('element-selected', {
        id: element.id,
        type: element.type,
        name: element.businessObject?.name || '',
        properties: props
      })
    } else {
      emit('element-selected', null)
    }
  })

  // 监听元素变化
  modeler.on('element.changed', (e) => {
    emit('element-changed', e.element)
    emitXml()
  })

  // 监听流程对象变化
  modeler.on('commandStack.changed', () => {
    emitXml()
  })

  // 加载默认流程
  if (!props.xml) {
    const defaultXml = getDefaultXml(props.processId, props.processName)
    loadXml(defaultXml)
  }
}

// 获取元素属性
const getElementProperties = (element) => {
  const bo = element.businessObject
  if (!bo) return {}

  const props = {}
  
  // 用户任务属性 - 尝试从多个位置读取
  if (bo.$type === 'bpmn:UserTask') {
    if (bo.assignee) props.assignee = bo.assignee
    else if (bo.$attrs?.assignee) props.assignee = bo.$attrs.assignee
    if (bo.candidateUsers) props.candidateUsers = bo.candidateUsers
    else if (bo.$attrs?.candidateUsers) props.candidateUsers = bo.$attrs.candidateUsers
    if (bo.candidateGroups) props.candidateGroups = bo.candidateGroups
    else if (bo.$attrs?.candidateGroups) props.candidateGroups = bo.$attrs.candidateGroups
  }

  // 顺序流条件
  if (bo.$type === 'bpmn:SequenceFlow') {
    if (bo.conditionExpression) {
      props.conditionExpression = bo.conditionExpression.body
    }
  }

  return props
}

// 加载XML
const loadXml = async (xml) => {
  try {
    await modeler.importXML(xml)
    const canvas = modeler.get('canvas')
    canvas.zoom('fit-viewport')
    emit('import-done')
  } catch (err) {
    console.error('加载XML失败:', err)
  }
}

// 导出XML
const emitXml = async () => {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    emit('update:xml', xml)
  } catch (err) {
    console.error('导出XML失败:', err)
  }
}

// 获取XML
const getXml = async () => {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    return xml
  } catch (err) {
    console.error('获取XML失败:', err)
    return ''
  }
}

// 更新元素属性
const updateElement = (elementId, properties) => {
  const element = modeler.get('elementRegistry').get(elementId)
  if (!element) {
    console.warn('updateElement: element not found', elementId)
    return
  }

  console.log('updateElement:', elementId, properties, 'element.type:', element.type)

  const moddle = modeler.get('moddle')
  const update = {}

  // 更新名称
  if (properties.name !== undefined) {
    update.name = properties.name
  }

  // 用户任务属性
  if (element.type === 'bpmn:UserTask') {
    if (properties.assignee !== undefined) {
      update.assignee = properties.assignee || undefined
    }
    if (properties.candidateUsers !== undefined) {
      const val = properties.candidateUsers
      update.candidateUsers = val && typeof val === 'string' ? val.split(',') : val
    }
    if (properties.candidateGroups !== undefined) {
      const val = properties.candidateGroups
      update.candidateGroups = val && typeof val === 'string' ? val.split(',') : val
    }
  }

  // 顺序流条件
  if (element.type === 'bpmn:SequenceFlow') {
    if (properties.conditionExpression !== undefined) {
      update.conditionExpression = moddle.create('bpmn:FormalExpression', {
        body: properties.conditionExpression
      })
    }
  }

  console.log('updateElement update:', update)
  if (Object.keys(update).length > 0) {
    const modelingModule = modeler.get('modeling')
    modelingModule.updateProperties(element, update)
    
    // Debug: 检查更新后的 businessObject
    const bo = element.businessObject
    console.log('After update - bo:', bo, 'bo.assignee:', bo?.assignee, 'bo.$attrs:', bo?.$attrs)
    
    emit('element-selected', {
      id: element.id,
      type: element.type,
      name: element.businessObject?.name || '',
      properties: getElementProperties(element)
    })
  }
}

// 添加元素
const addElement = (type, parentId) => {
  // bpmn-js 通过 palette 添加元素，这里可以添加快捷操作
}

// 删除选中元素
const deleteElement = (elementId) => {
  const element = modeler.get('elementRegistry').get(elementId)
  if (element) {
    const modeling = modeler.get('modeling')
    modeling.removeElements([element])
  }
}

// 缩放
const zoomIn = () => {
  const canvas = modeler.get('canvas')
  canvas.zoom(canvas.zoom() + 0.1)
}

const zoomOut = () => {
  const canvas = modeler.get('canvas')
  canvas.zoom(canvas.zoom() - 0.1)
}

const zoomReset = () => {
  const canvas = modeler.get('canvas')
  canvas.zoom(1)
}

const zoomFit = () => {
  const canvas = modeler.get('canvas')
  canvas.zoom('fit-viewport')
}

// 获取图形数据
const getGraphData = async () => {
  const { xml } = await modeler.saveXML({ format: true })
  return xml
}

// 撤销
const undo = () => {
  modeler.get('commandStack').undo()
}

// 重做
const redo = () => {
  modeler.get('commandStack').redo()
}

// 选中元素
const selectElement = (elementId) => {
  const element = modeler.get('elementRegistry').get(elementId)
  if (element) {
    modeler.get('selection').select(element)
  }
}

// 暴露方法给父组件
defineExpose({
  loadXml,
  getXml,
  updateElement,
  deleteElement,
  zoomIn,
  zoomOut,
  zoomReset,
  zoomFit,
  getGraphData,
  undo,
  redo,
  selectElement
})

onMounted(() => {
  initModeler()
})

onBeforeUnmount(() => {
  if (modeler) {
    modeler.destroy()
  }
})

// 监听外部XML变化
watch(() => props.xml, (newXml) => {
  if (newXml && modeler) {
    loadXml(newXml)
  }
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

:deep(.bjs-container) {
  height: 100%;
}

:deep(.djs-palette) {
  left: 20px;
  top: 20px;
}

:deep(.djs-element) {
  cursor: pointer;
}

:deep(.djs-element.selected .djs-outline) {
  stroke: #409eff;
  stroke-width: 2px;
}
</style>