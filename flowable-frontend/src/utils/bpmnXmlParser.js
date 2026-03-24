/**
 * BPMN XML 解析器
 * 将 BPMN XML 转换为 X6 图形数据格式
 */

/**
 * 解析 BPMN XML 字符串
 * @param {string} xmlString - BPMN XML 字符串
 * @returns {Promise<{nodes: Array, edges: Array, process: Object}>}
 */
export async function parseBpmnXml(xmlString) {
  const parser = new DOMParser()
  const xmlDoc = parser.parseFromString(xmlString, 'text/xml')
  
  const result = {
    nodes: [],
    edges: [],
    process: {
      id: '',
      name: '',
      documentation: ''
    }
  }
  
  // 解析流程定义
  const processEl = xmlDoc.querySelector('process') || 
                    xmlDoc.getElementsByTagNameNS('http://www.omg.org/spec/BPMN/20100524/MODEL', 'process')[0]
  
  if (processEl) {
    result.process.id = processEl.getAttribute('id') || 'Process_1'
    result.process.name = processEl.getAttribute('name') || '新建流程'
    const docEl = processEl.querySelector('documentation') ||
                  processEl.getElementsByTagNameNS('http://www.omg.org/spec/BPMN/20100524/MODEL', 'documentation')[0]
    if (docEl) {
      result.process.documentation = docEl.textContent
    }
  }
  
  // 获取图形信息
  const boundsMap = {}
  const diagramEl = xmlDoc.querySelector('bpmndi\\:BPMNDiagram, BPMNDiagram') ||
                    xmlDoc.getElementsByTagNameNS('http://www.omg.org/spec/BPMN/20100524/DI', 'BPMNDiagram')[0]
  
  if (diagramEl) {
    const shapes = diagramEl.querySelectorAll('bpmndi\\:BPMNShape, BPMNShape') ||
                   xmlDoc.getElementsByTagNameNS('http://www.omg.org/spec/BPMN/20100524/DI', 'BPMNShape')
    
    shapes.forEach(shape => {
      const bpmnElement = shape.getAttribute('bpmnElement')
      const bounds = shape.querySelector('dc\\:Bounds, Bounds') ||
                     shape.getElementsByTagNameNS('http://www.omg.org/spec/DD/20100524/DC', 'Bounds')[0]
      if (bounds) {
        boundsMap[bpmnElement] = {
          x: parseFloat(bounds.getAttribute('x')) || 0,
          y: parseFloat(bounds.getAttribute('y')) || 0,
          width: parseFloat(bounds.getAttribute('width')) || 100,
          height: parseFloat(bounds.getAttribute('height')) || 80
        }
      }
    })
  }
  
  // 解析所有元素
  const processElements = (tagName, nodeType) => {
    const elements = xmlDoc.getElementsByTagName(tagName)
    for (let i = 0; i < elements.length; i++) {
      const el = elements[i]
      const id = el.getAttribute('id')
      const name = el.getAttribute('name') || ''
      const bounds = boundsMap[id] || { x: 100 + i * 150, y: 100, width: 100, height: 80 }
      
      const node = {
        id,
        type: nodeType,
        shape: 'bpmn-node',
        x: bounds.x + bounds.width / 2,
        y: bounds.y + bounds.height / 2,
        width: bounds.width,
        height: bounds.height,
        data: {
          bpmnType: tagName,
          name,
          properties: extractProperties(el)
        }
      }
      
      result.nodes.push(node)
    }
  }
  
  processElements('startEvent', 'bpmn:startEvent')
  processElements('endEvent', 'bpmn:endEvent')
  processElements('userTask', 'bpmn:userTask')
  processElements('serviceTask', 'bpmn:serviceTask')
  processElements('scriptTask', 'bpmn:scriptTask')
  processElements('exclusiveGateway', 'bpmn:exclusiveGateway')
  processElements('parallelGateway', 'bpmn:parallelGateway')
  processElements('inclusiveGateway', 'bpmn:inclusiveGateway')
  processElements('task', 'bpmn:task')
  processElements('receiveTask', 'bpmn:receiveTask')
  processElements('sendTask', 'bpmn:sendTask')
  processElements('manualTask', 'bpmn:manualTask')
  processElements('businessRuleTask', 'bpmn:businessRuleTask')
  processElements('subProcess', 'bpmn:subProcess')
  processElements('callActivity', 'bpmn:callActivity')
  processElements('intermediateCatchEvent', 'bpmn:intermediateCatchEvent')
  processElements('intermediateThrowEvent', 'bpmn:intermediateThrowEvent')
  processElements('boundaryEvent', 'bpmn:boundaryEvent')
  
  // 解析顺序流
  const sequenceFlows = xmlDoc.getElementsByTagName('sequenceFlow')
  for (let i = 0; i < sequenceFlows.length; i++) {
    const flow = sequenceFlows[i]
    const id = flow.getAttribute('id')
    const name = flow.getAttribute('name') || ''
    const sourceRef = flow.getAttribute('sourceRef')
    const targetRef = flow.getAttribute('targetRef')
    
    const conditionEl = flow.querySelector('conditionExpression') ||
                        flow.getElementsByTagNameNS('http://www.omg.org/spec/BPMN/20100524/MODEL', 'conditionExpression')[0]
    const conditionExpression = conditionEl ? conditionEl.textContent : ''
    
    result.edges.push({
      id,
      shape: 'bpmn-edge',
      source: { cell: sourceRef },
      target: { cell: targetRef },
      data: {
        bpmnType: 'sequenceFlow',
        name,
        conditionExpression,
        properties: extractProperties(flow)
      },
      attrs: {
        line: {
          stroke: '#5F95FF',
          strokeWidth: 2,
          targetMarker: { name: 'classic', size: 8 }
        }
      },
      labels: name ? [{ attrs: { text: { text: name } } }] : []
    })
  }
  
  return result
}

function extractProperties(element) {
  const properties = {}
  
  const docEl = element.querySelector('documentation') ||
                element.getElementsByTagNameNS('http://www.omg.org/spec/BPMN/20100524/MODEL', 'documentation')[0]
  if (docEl) {
    properties.documentation = docEl.textContent
  }
  
  const attrs = element.attributes
  for (let i = 0; i < attrs.length; i++) {
    const attr = attrs[i]
    if (attr.name.startsWith('flowable:')) {
      const key = attr.name.replace('flowable:', '')
      properties[key] = attr.value
    }
  }
  
  return properties
}

export function exportToBpmnXml(graphData, processInfo = {}) {
  const { nodes, edges } = graphData
  const processId = processInfo.id || 'Process_1'
  const processName = processInfo.name || '新建流程'
  
  const XML_DECL = String.fromCharCode(60) + '?xml version="1.0" encoding="UTF-8"?' + String.fromCharCode(62) + '\n'
  let xml = XML_DECL
  xml += '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"\n'
  xml += '             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"\n'
  xml += '             xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"\n'
  xml += '             xmlns:di="http://www.omg.org/spec/DD/20100524/DI"\n'
  xml += '             xmlns:flowable="http://flowable.org/bpmn"\n'
  xml += '             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n'
  xml += '             targetNamespace="http://flowable.org/demo">\n'
  xml += '  <bpmn:process id="' + escapeXml(processId) + '" name="' + escapeXml(processName) + '" isExecutable="true">\n'
  
  nodes.forEach(node => {
    const nodeType = getBpmnElementType(node.type || node.data?.bpmnType)
    const nodeAttrs = generateNodeAttributes(node)
    xml += '    <bpmn:' + nodeType + ' id="' + escapeXml(node.id) + '"' + nodeAttrs + '/>\n'
  })
  
  edges.forEach(edge => {
    const sourceId = typeof edge.source === 'object' ? edge.source.cell : edge.source
    const targetId = typeof edge.target === 'object' ? edge.target.cell : edge.target
    const name = edge.data?.name || ''
    const condition = edge.data?.conditionExpression || ''
    
    const nameAttr = name ? ' name="' + escapeXml(name) + '"' : ''
    
    if (condition) {
      xml += '    <bpmn:sequenceFlow id="' + escapeXml(edge.id) + '" sourceRef="' + escapeXml(sourceId) + '" targetRef="' + escapeXml(targetId) + '"' + nameAttr + '>\n'
      xml += '      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">' + escapeXml(condition) + '</bpmn:conditionExpression>\n'
      xml += '    </bpmn:sequenceFlow>\n'
    } else {
      xml += '    <bpmn:sequenceFlow id="' + escapeXml(edge.id) + '" sourceRef="' + escapeXml(sourceId) + '" targetRef="' + escapeXml(targetId) + '"' + nameAttr + '/>\n'
    }
  })
  
  xml += '  </bpmn:process>\n'
  
  xml += '  <bpmndi:BPMNDiagram id="BPMNDiagram_' + escapeXml(processId) + '">\n'
  xml += '    <bpmndi:BPMNPlane id="BPMNPlane_' + escapeXml(processId) + '" bpmnElement="' + escapeXml(processId) + '">\n'
  
  nodes.forEach(node => {
    const x = node.x - (node.width || 100) / 2
    const y = node.y - (node.height || 80) / 2
    const width = node.width || 100
    const height = node.height || 80
    
    xml += '      <bpmndi:BPMNShape id="BPMNShape_' + escapeXml(node.id) + '" bpmnElement="' + escapeXml(node.id) + '">\n'
    xml += '        <dc:Bounds x="' + x + '" y="' + y + '" width="' + width + '" height="' + height + '"/>\n'
    xml += '      </bpmndi:BPMNShape>\n'
  })
  
  edges.forEach(edge => {
    xml += '      <bpmndi:BPMNEdge id="BPMNEdge_' + escapeXml(edge.id) + '" bpmnElement="' + escapeXml(edge.id) + '">\n'
    xml += '      </bpmndi:BPMNEdge>\n'
  })
  
  xml += '    </bpmndi:BPMNPlane>\n'
  xml += '  </bpmndi:BPMNDiagram>\n'
  xml += '</bpmn:definitions>'
  
  return xml
}

function getBpmnElementType(type) {
  const typeMap = {
    'bpmn:startEvent': 'startEvent',
    'bpmn:endEvent': 'endEvent',
    'bpmn:userTask': 'userTask',
    'bpmn:serviceTask': 'serviceTask',
    'bpmn:scriptTask': 'scriptTask',
    'bpmn:exclusiveGateway': 'exclusiveGateway',
    'bpmn:parallelGateway': 'parallelGateway',
    'bpmn:inclusiveGateway': 'inclusiveGateway',
    'bpmn:task': 'task',
    'bpmn:receiveTask': 'receiveTask',
    'bpmn:sendTask': 'sendTask',
    'bpmn:manualTask': 'manualTask',
    'bpmn:businessRuleTask': 'businessRuleTask',
    'bpmn:subProcess': 'subProcess',
    'bpmn:callActivity': 'callActivity',
    'bpmn:intermediateCatchEvent': 'intermediateCatchEvent',
    'bpmn:intermediateThrowEvent': 'intermediateThrowEvent',
    'bpmn:boundaryEvent': 'boundaryEvent'
  }
  
  return typeMap[type] || 'task'
}

function generateNodeAttributes(node) {
  const attrs = []
  const name = node.data?.name || ''
  
  if (name) {
    attrs.push(' name="' + escapeXml(name) + '"')
  }
  
  const props = node.data?.properties || {}
  if (props.assignee) {
    attrs.push(' flowable:assignee="' + escapeXml(props.assignee) + '"')
  }
  if (props.candidateUsers) {
    attrs.push(' flowable:candidateUsers="' + escapeXml(props.candidateUsers) + '"')
  }
  if (props.candidateGroups) {
    attrs.push(' flowable:candidateGroups="' + escapeXml(props.candidateGroups) + '"')
  }
  if (props.async) {
    attrs.push(' flowable:async="true"')
  }
  if (props.exclusive) {
    attrs.push(' flowable:exclusive="true"')
  }
  
  return attrs.join('')
}

function escapeXml(str) {
  if (!str) return ''
  return String(str)
    .replace(/&/g, '\x26amp;')
    .replace(/</g, '\x26lt;')
    .replace(/>/g, '\x26gt;')
    .replace(/"/g, '\x26quot;')
    .replace(/'/g, '\x26apos;')
}

export function generateId(prefix = 'node') {
  return prefix + '_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

export function getDefaultXml(processId, processName) {
  processId = processId || 'Process_1'
  processName = processName || '新建流程'
  const XML_DECL = String.fromCharCode(60) + '?xml version="1.0" encoding="UTF-8"?' + String.fromCharCode(62) + '\n'
  let xml = XML_DECL
  xml += '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"\n'
  xml += '             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"\n'
  xml += '             xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"\n'
  xml += '             xmlns:flowable="http://flowable.org/bpmn"\n'
  xml += '             targetNamespace="http://flowable.org/demo">\n'
  xml += '  <bpmn:process id="' + processId + '" name="' + processName + '" isExecutable="true">\n'
  xml += '    <bpmn:startEvent id="StartEvent_1" name="开始"/>\n'
  xml += '  </bpmn:process>\n'
  xml += '  <bpmndi:BPMNDiagram id="BPMNDiagram_1">\n'
  xml += '    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="' + processId + '">\n'
  xml += '      <bpmndi:BPMNShape id="BPMNShape_StartEvent_1" bpmnElement="StartEvent_1">\n'
  xml += '        <dc:Bounds x="152" y="102" width="36" height="36"/>\n'
  xml += '      </bpmndi:BPMNShape>\n'
  xml += '    </bpmndi:BPMNPlane>\n'
  xml += '  </bpmndi:BPMNDiagram>\n'
  xml += '</bpmn:definitions>'
  return xml
}

export const bpmnNodeTypes = {
  startEvent: {
    type: 'bpmn:startEvent',
    name: '开始事件',
    icon: '▶',
    defaultSize: { width: 36, height: 36 },
    color: '#52c41a'
  },
  endEvent: {
    type: 'bpmn:endEvent',
    name: '结束事件',
    icon: '●',
    defaultSize: { width: 36, height: 36 },
    color: '#ff4d4f'
  },
  userTask: {
    type: 'bpmn:userTask',
    name: '用户任务',
    icon: '👤',
    defaultSize: { width: 100, height: 80 },
    color: '#1890ff'
  },
  serviceTask: {
    type: 'bpmn:serviceTask',
    name: '服务任务',
    icon: '⚙',
    defaultSize: { width: 100, height: 80 },
    color: '#722ed1'
  },
  scriptTask: {
    type: 'bpmn:scriptTask',
    name: '脚本任务',
    icon: '📜',
    defaultSize: { width: 100, height: 80 },
    color: '#13c2c2'
  },
  exclusiveGateway: {
    type: 'bpmn:exclusiveGateway',
    name: '排他网关',
    icon: '✕',
    defaultSize: { width: 50, height: 50 },
    color: '#faad14'
  },
  parallelGateway: {
    type: 'bpmn:parallelGateway',
    name: '并行网关',
    icon: '+',
    defaultSize: { width: 50, height: 50 },
    color: '#52c41a'
  },
  inclusiveGateway: {
    type: 'bpmn:inclusiveGateway',
    name: '包容网关',
    icon: '○',
    defaultSize: { width: 50, height: 50 },
    color: '#eb2f96'
  },
  task: {
    type: 'bpmn:task',
    name: '任务',
    icon: '☐',
    defaultSize: { width: 100, height: 80 },
    color: '#8c8c8c'
  }
}
