<template>
  <div class="x6-flow-designer">
    <!-- 左侧工具栏 -->
    <div class="stencil-container">
      <div class="stencil-title">流程节点</div>
      <div class="stencil-groups">
        <div class="stencil-group">
          <div class="group-title">事件</div>
          <div class="group-items">
            <div 
              class="stencil-item" 
              draggable="true" 
              @dragstart="onDragStart($event, 'bpmn:startEvent')"
            >
              <div class="item-icon start-event"></div>
              <span>开始事件</span>
            </div>
            <div 
              class="stencil-item" 
              draggable="true" 
              @dragstart="onDragStart($event, 'bpmn:endEvent')"
            >
              <div class="item-icon end-event"></div>
              <span>结束事件</span>
            </div>
          </div>
        </div>
        <div class="stencil-group">
          <div class="group-title">任务</div>
          <div class="group-items">
            <div 
              class="stencil-item" 
              draggable="true" 
              @dragstart="onDragStart($event, 'bpmn:userTask')"
            >
              <div class="item-icon user-task"></div>
              <span>用户任务</span>
            </div>
            <div 
              class="stencil-item" 
              draggable="true" 
              @dragstart="onDragStart($event, 'bpmn:serviceTask')"
            >
              <div class="item-icon service-task"></div>
              <span>服务任务</span>
            </div>
            <div 
              class="stencil-item" 
              draggable="true" 
              @dragstart="onDragStart($event, 'bpmn:scriptTask')"
            >
              <div class="item-icon script-task"></div>
              <span>脚本任务</span>
            </div>
          </div>
        </div>
        <div class="stencil-group">
          <div class="group-title">网关</div>
          <div class="group-items">
            <div 
              class="stencil-item" 
              draggable="true" 
              @dragstart="onDragStart($event, 'bpmn:exclusiveGateway')"
            >
              <div class="item-icon exclusive-gateway"></div>
              <span>排他网关</span>
            </div>
            <div 
              class="stencil-item" 
              draggable="true" 
              @dragstart="onDragStart($event, 'bpmn:parallelGateway')"
            >
              <div class="item-icon parallel-gateway"></div>
              <span>并行网关</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 画布容器 -->
    <div class="graph-container" ref="graphRef"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, markRaw } from 'vue'
import { Graph } from '@antv/x6'
import { generateId } from '@/utils/bpmnXmlParser'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({ nodes: [], edges: [] })
  }
})

const emit = defineEmits(['update:modelValue', 'node-selected', 'node-change'])

const graphRef = ref(null)
let graph = null
let dragType = null

// 注册自定义节点
const registerNodes = () => {
  // 开始事件节点
  Graph.registerNode('bpmn-start-event', {
    inherit: 'circle',
    width: 36,
    height: 36,
    attrs: {
      body: {
        strokeWidth: 2,
        stroke: '#52c41a',
        fill: '#f6ffed'
      }
    },
    ports: {
      groups: {
        right: {
          position: 'right',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#52c41a',
              strokeWidth: 1,
              fill: '#fff'
            }
          }
        }
      },
      items: [{ group: 'right', id: 'port-right' }]
    },
    data: {
      bpmnType: 'startEvent',
      name: '开始',
      properties: {}
    }
  }, true)

  // 结束事件节点
  Graph.registerNode('bpmn-end-event', {
    inherit: 'circle',
    width: 36,
    height: 36,
    attrs: {
      body: {
        strokeWidth: 4,
        stroke: '#ff4d4f',
        fill: '#fff2f0'
      }
    },
    ports: {
      groups: {
        left: {
          position: 'left',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#ff4d4f',
              strokeWidth: 1,
              fill: '#fff'
            }
          }
        }
      },
      items: [{ group: 'left', id: 'port-left' }]
    },
    data: {
      bpmnType: 'endEvent',
      name: '结束',
      properties: {}
    }
  }, true)

  // 用户任务节点
  Graph.registerNode('bpmn-user-task', {
    inherit: 'rect',
    width: 100,
    height: 80,
    attrs: {
      body: {
        strokeWidth: 2,
        stroke: '#1890ff',
        fill: '#e6f7ff',
        rx: 6,
        ry: 6
      }
    },
    ports: {
      groups: {
        top: {
          position: 'top',
          attrs: { circle: { r: 4, magnet: true, stroke: '#1890ff', strokeWidth: 1, fill: '#fff' } }
        },
        bottom: {
          position: 'bottom',
          attrs: { circle: { r: 4, magnet: true, stroke: '#1890ff', strokeWidth: 1, fill: '#fff' } }
        },
        left: {
          position: 'left',
          attrs: { circle: { r: 4, magnet: true, stroke: '#1890ff', strokeWidth: 1, fill: '#fff' } }
        },
        right: {
          position: 'right',
          attrs: { circle: { r: 4, magnet: true, stroke: '#1890ff', strokeWidth: 1, fill: '#fff' } }
        }
      },
      items: [
        { group: 'top', id: 'port-top' },
        { group: 'bottom', id: 'port-bottom' },
        { group: 'left', id: 'port-left' },
        { group: 'right', id: 'port-right' }
      ]
    },
    data: {
      bpmnType: 'userTask',
      name: '用户任务',
      properties: {}
    }
  }, true)

  // 服务任务节点
  Graph.registerNode('bpmn-service-task', {
    inherit: 'rect',
    width: 100,
    height: 80,
    attrs: {
      body: {
        strokeWidth: 2,
        stroke: '#722ed1',
        fill: '#f9f0ff',
        rx: 6,
        ry: 6
      }
    },
    ports: {
      groups: {
        top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#722ed1', strokeWidth: 1, fill: '#fff' } } },
        bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#722ed1', strokeWidth: 1, fill: '#fff' } } },
        left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#722ed1', strokeWidth: 1, fill: '#fff' } } },
        right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#722ed1', strokeWidth: 1, fill: '#fff' } } }
      },
      items: [
        { group: 'top', id: 'port-top' },
        { group: 'bottom', id: 'port-bottom' },
        { group: 'left', id: 'port-left' },
        { group: 'right', id: 'port-right' }
      ]
    },
    data: { bpmnType: 'serviceTask', name: '服务任务', properties: {} }
  }, true)

  // 脚本任务节点
  Graph.registerNode('bpmn-script-task', {
    inherit: 'rect',
    width: 100,
    height: 80,
    attrs: {
      body: {
        strokeWidth: 2,
        stroke: '#13c2c2',
        fill: '#e6fffb',
        rx: 6,
        ry: 6
      }
    },
    ports: {
      groups: {
        top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#13c2c2', strokeWidth: 1, fill: '#fff' } } },
        bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#13c2c2', strokeWidth: 1, fill: '#fff' } } },
        left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#13c2c2', strokeWidth: 1, fill: '#fff' } } },
        right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#13c2c2', strokeWidth: 1, fill: '#fff' } } }
      },
      items: [
        { group: 'top', id: 'port-top' },
        { group: 'bottom', id: 'port-bottom' },
        { group: 'left', id: 'port-left' },
        { group: 'right', id: 'port-right' }
      ]
    },
    data: { bpmnType: 'scriptTask', name: '脚本任务', properties: {} }
  }, true)

  // 排他网关节点
  Graph.registerNode('bpmn-exclusive-gateway', {
    inherit: 'polygon',
    width: 50,
    height: 50,
    attrs: {
      body: {
        refPoints: '25,0 50,25 25,50 0,25',
        strokeWidth: 2,
        stroke: '#faad14',
        fill: '#fffbe6'
      }
    },
    ports: {
      groups: {
        top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#faad14', strokeWidth: 1, fill: '#fff' } } },
        bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#faad14', strokeWidth: 1, fill: '#fff' } } },
        left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#faad14', strokeWidth: 1, fill: '#fff' } } },
        right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#faad14', strokeWidth: 1, fill: '#fff' } } }
      },
      items: [
        { group: 'top', id: 'port-top' },
        { group: 'bottom', id: 'port-bottom' },
        { group: 'left', id: 'port-left' },
        { group: 'right', id: 'port-right' }
      ]
    },
    data: { bpmnType: 'exclusiveGateway', name: '排他网关', properties: {} }
  }, true)

  // 并行网关节点
  Graph.registerNode('bpmn-parallel-gateway', {
    inherit: 'polygon',
    width: 50,
    height: 50,
    attrs: {
      body: {
        refPoints: '25,0 50,25 25,50 0,25',
        strokeWidth: 2,
        stroke: '#52c41a',
        fill: '#f6ffed'
      }
    },
    ports: {
      groups: {
        top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#52c41a', strokeWidth: 1, fill: '#fff' } } },
        bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#52c41a', strokeWidth: 1, fill: '#fff' } } },
        left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#52c41a', strokeWidth: 1, fill: '#fff' } } },
        right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#52c41a', strokeWidth: 1, fill: '#fff' } } }
      },
      items: [
        { group: 'top', id: 'port-top' },
        { group: 'bottom', id: 'port-bottom' },
        { group: 'left', id: 'port-left' },
        { group: 'right', id: 'port-right' }
      ]
    },
    data: { bpmnType: 'parallelGateway', name: '并行网关', properties: {} }
  }, true)
}

// 节点类型配置
const nodeTypeConfig = {
  'bpmn:startEvent': { shape: 'bpmn-start-event', name: '开始', width: 36, height: 36 },
  'bpmn:endEvent': { shape: 'bpmn-end-event', name: '结束', width: 36, height: 36 },
  'bpmn:userTask': { shape: 'bpmn-user-task', name: '用户任务', width: 100, height: 80 },
  'bpmn:serviceTask': { shape: 'bpmn-service-task', name: '服务任务', width: 100, height: 80 },
  'bpmn:scriptTask': { shape: 'bpmn-script-task', name: '脚本任务', width: 100, height: 80 },
  'bpmn:exclusiveGateway': { shape: 'bpmn-exclusive-gateway', name: '排他网关', width: 50, height: 50 },
  'bpmn:parallelGateway': { shape: 'bpmn-parallel-gateway', name: '并行网关', width: 50, height: 50 }
}

// 初始化图
const initGraph = () => {
  registerNodes()
  
  graph = markRaw(new Graph({
    container: graphRef.value,
    grid: {
      visible: true,
      type: 'dot',
      size: 20,
      args: { color: '#d0d0d0', thickness: 1 }
    },
    panning: true,
    mousewheel: true,
    connecting: {
      anchor: 'center',
      connectionPoint: 'anchor',
      allowBlank: false,
      allowLoop: false,
      allowNode: true,
      allowEdge: false,
      highlight: true,
      snap: true,
      createEdge() {
        return graph.createEdge({
          shape: 'edge',
          attrs: {
            line: {
              stroke: '#5F95FF',
              strokeWidth: 2,
              targetMarker: { name: 'classic', size: 8 }
            }
          },
          data: {
            bpmnType: 'sequenceFlow',
            name: '',
            conditionExpression: '',
            properties: {}
          },
          zIndex: 0
        })
      }
    },
    highlighting: {
      magnetAvailable: {
        name: 'stroke',
        args: { attrs: { fill: '#fff', stroke: '#47C769' } }
      }
    }
  }))

  // 监听节点选中
  graph.on('node:selected', ({ node }) => {
    emit('node-selected', {
      id: node.id,
      type: node.data?.bpmnType || 'task',
      name: node.label || node.data?.name || '',
      properties: node.data?.properties || {}
    })
  })

  // 监听边选中
  graph.on('edge:selected', ({ edge }) => {
    emit('node-selected', {
      id: edge.id,
      type: 'sequenceFlow',
      name: edge.data?.name || '',
      conditionExpression: edge.data?.conditionExpression || '',
      properties: edge.data?.properties || {}
    })
  })

  // 监听空白区域点击
  graph.on('blank:click', () => {
    emit('node-selected', null)
  })

  // 监听节点变化
  graph.on('node:changed', () => {
    emitGraphChange()
  })

  graph.on('edge:connected', () => {
    emitGraphChange()
  })

  graph.on('edge:removed', () => {
    emitGraphChange()
  })

  // 拖放支持
  graphRef.value.addEventListener('dragover', (e) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  })

  graphRef.value.addEventListener('drop', (e) => {
    e.preventDefault()
    if (dragType) {
      const config = nodeTypeConfig[dragType]
      if (config) {
        const rect = graphRef.value.getBoundingClientRect()
        const x = e.clientX - rect.left
        const y = e.clientY - rect.top
        const pos = graph.clientToLocal(x, y)
        
        const node = graph.addNode({
          id: generateId(dragType.split(':')[1]),
          shape: config.shape,
          x: pos.x,
          y: pos.y,
          width: config.width,
          height: config.height,
          label: config.name,
          data: {
            bpmnType: dragType.split(':')[1],
            name: config.name,
            properties: {}
          }
        })
        
        graph.cleanSelection()
        graph.select(node)
      }
    }
    dragType = null
  })
}

// 拖拽开始
const onDragStart = (e, type) => {
  dragType = type
  e.dataTransfer.effectAllowed = 'move'
}

// 导出图形变化
const emitGraphChange = () => {
  if (!graph) return
  const nodes = []
  const edges = []
  
  graph.getNodes().forEach(node => {
    const pos = node.position()
    const size = node.size()
    nodes.push({
      id: node.id,
      type: node.data?.bpmnType ? `bpmn:${node.data.bpmnType}` : 'bpmn:task',
      x: pos.x + size.width / 2,
      y: pos.y + size.height / 2,
      width: size.width,
      height: size.height,
      label: node.label,
      data: node.data
    })
  })
  
  graph.getEdges().forEach(edge => {
    edges.push({
      id: edge.id,
      source: { cell: edge.getSourceCell()?.id },
      target: { cell: edge.getTargetCell()?.id },
      data: edge.data
    })
  })
  
  emit('node-change', { nodes, edges })
}

// 加载数据
const loadData = (data) => {
  if (!graph || !data) return
  
  graph.clearCells()
  
  if (data.nodes && data.nodes.length > 0) {
    data.nodes.forEach(nodeData => {
      const config = nodeTypeConfig[nodeData.type] || nodeTypeConfig['bpmn:userTask']
      if (config) {
        graph.addNode({
          id: nodeData.id,
          shape: config.shape,
          x: nodeData.x - (nodeData.width || config.width) / 2,
          y: nodeData.y - (nodeData.height || config.height) / 2,
          width: nodeData.width || config.width,
          height: nodeData.height || config.height,
          label: nodeData.data?.name || nodeData.label || config.name,
          data: nodeData.data || { bpmnType: nodeData.type?.split(':')[1], name: config.name, properties: {} }
        })
      }
    })
  }
  
  if (data.edges && data.edges.length > 0) {
    data.edges.forEach(edgeData => {
      const sourceId = typeof edgeData.source === 'object' ? edgeData.source.cell : edgeData.source
      const targetId = typeof edgeData.target === 'object' ? edgeData.target.cell : edgeData.target
      
      graph.addEdge({
        id: edgeData.id,
        source: sourceId,
        target: targetId,
        attrs: {
          line: {
            stroke: '#5F95FF',
            strokeWidth: 2,
            targetMarker: { name: 'classic', size: 8 }
          }
        },
        data: edgeData.data || { bpmnType: 'sequenceFlow', name: '', conditionExpression: '', properties: {} },
        labels: edgeData.data?.name ? [{ attrs: { text: { text: edgeData.data.name } } }] : []
      })
    })
  }
}

// 获取图形数据
const getGraphData = () => {
  if (!graph) return { nodes: [], edges: [] }
  
  const nodes = []
  const edges = []
  
  graph.getNodes().forEach(node => {
    const pos = node.position()
    const size = node.size()
    nodes.push({
      id: node.id,
      type: node.data?.bpmnType ? `bpmn:${node.data.bpmnType}` : 'bpmn:task',
      x: pos.x + size.width / 2,
      y: pos.y + size.height / 2,
      width: size.width,
      height: size.height,
      label: node.label,
      data: node.data
    })
  })
  
  graph.getEdges().forEach(edge => {
    edges.push({
      id: edge.id,
      source: { cell: edge.getSourceCell()?.id },
      target: { cell: edge.getTargetCell()?.id },
      data: edge.data
    })
  })
  
  return { nodes, edges }
}

// 更新节点属性
const updateNodeData = (nodeId, data) => {
  if (!graph) return
  const node = graph.getCellById(nodeId)
  if (node) {
    node.setData(data)
    if (data.name !== undefined) {
      node.setLabel(data.name)
    }
  }
}

// 删除选中元素
const deleteSelected = () => {
  if (!graph) return
  const cells = graph.getSelectedCells()
  if (cells.length) {
    graph.removeCells(cells)
  }
}

// 缩放
const zoomIn = () => {
  if (graph) graph.zoom(0.1)
}

const zoomOut = () => {
  if (graph) graph.zoom(-0.1)
}

const zoomReset = () => {
  if (graph) graph.zoomTo(1)
}

// 暴露方法给父组件
defineExpose({
  loadData,
  getGraphData,
  updateNodeData,
  deleteSelected,
  zoomIn,
  zoomOut,
  zoomReset
})

onMounted(() => {
  initGraph()
})

onBeforeUnmount(() => {
  if (graph) {
    graph.dispose()
  }
})

// 监听外部数据变化
watch(() => props.modelValue, (newVal) => {
  if (newVal && graph) {
    loadData(newVal)
  }
}, { deep: true })
</script>

<style scoped>
.x6-flow-designer {
  display: flex;
  height: 100%;
  width: 100%;
}

.stencil-container {
  width: 200px;
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
}

.stencil-title {
  padding: 12px 16px;
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
}

.stencil-groups {
  padding: 8px;
}

.stencil-group {
  margin-bottom: 12px;
}

.group-title {
  padding: 8px;
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.group-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stencil-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: grab;
  transition: all 0.2s;
}

.stencil-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.stencil-item:active {
  cursor: grabbing;
}

.item-icon {
  width: 24px;
  height: 24px;
  border-radius: 4px;
}

.item-icon.start-event {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f6ffed;
  border: 2px solid #52c41a;
}

.item-icon.end-event {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #fff2f0;
  border: 4px solid #ff4d4f;
}

.item-icon.user-task {
  background: #e6f7ff;
  border: 2px solid #1890ff;
  border-radius: 4px;
}

.item-icon.service-task {
  background: #f9f0ff;
  border: 2px solid #722ed1;
  border-radius: 4px;
}

.item-icon.script-task {
  background: #e6fffb;
  border: 2px solid #13c2c2;
  border-radius: 4px;
}

.item-icon.exclusive-gateway {
  width: 24px;
  height: 24px;
  background: #fffbe6;
  border: 2px solid #faad14;
  transform: rotate(45deg);
}

.item-icon.parallel-gateway {
  width: 24px;
  height: 24px;
  background: #f6ffed;
  border: 2px solid #52c41a;
  transform: rotate(45deg);
}

.stencil-item span {
  font-size: 13px;
  color: #606266;
}

.graph-container {
  flex: 1;
  height: 100%;
  background: #fff;
}
</style>
