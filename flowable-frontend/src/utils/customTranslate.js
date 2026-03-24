/**
 * 自定义BPMN翻译模块
 * 用于修改上下文菜单、属性面板等显示的文字
 */

// 自定义翻译映射
const translations = {
  // Change element 菜单中的元素类型名称
  'User Task': '用户任务',
  'Service Task': '服务任务',
  'Script Task': '脚本任务',
  'Business Rule Task': '业务规则任务',
  'Send Task': '发送任务',
  'Receive Task': '接收任务',
  'Manual Task': '手动任务',
  'Mail Task': '邮件任务',
  'Camel Task': 'Camel任务',
  'Http Task': 'HTTP任务',
  'External Task': '外部任务',
  'Decision Task': '决策任务',
  
  // 网关类型
  'Exclusive Gateway': '排他网关',
  'Parallel Gateway': '并行网关',
  'Inclusive Gateway': '包容网关',
  'Event-based Gateway': '事件网关',
  'Complex Gateway': '复杂网关',
  
  // 事件类型
  'Start Event': '开始事件',
  'End Event': '结束事件',
  'Intermediate Throw Event': '中间抛出事件',
  'Intermediate Catch Event': '中间捕获事件',
  'Boundary Event': '边界事件',
  
  // 子流程
  'Sub Process': '子流程',
  'Event Sub Process': '事件子流程',
  'Transaction': '事务',
  
  // 其他元素
  'Data Object': '数据对象',
  'Data Store Reference': '数据存储引用',
  'Data Object Reference': '数据对象引用',
  'Pool': '池',
  'Lane': '泳道',
  'Participant': '参与者',
  'Group': '分组',
  'Text Annotation': '文本注释',
  
  // 菜单操作
  'Change element': '更换元素类型',
  'Replace': '更换',
  'Append': '追加',
  'Delete': '删除',
  
  // 属性面板
  'Id': '标识',
  'Name': '名称',
  'General': '常规',
  'Documentation': '文档',
  'Element Documentation': '元素文档',
  'Executable': '可执行',
  'Async before': '异步前',
  'Async after': '异步后',
  
  // 开始事件类型
  'None Start Event': '空开始事件',
  'Timer Start Event': '定时开始事件',
  'Message Start Event': '消息开始事件',
  'Signal Start Event': '信号开始事件',
  'Conditional Start Event': '条件开始事件',
  'Error Start Event': '错误开始事件',
  'Escalation Start Event': '升级开始事件',
  'Compensation Start Event': '补偿开始事件',
  'Message Start Event (non-interrupting)': '消息开始事件(非中断)',
  'Timer Start Event (non-interrupting)': '定时开始事件(非中断)',
  'Signal Start Event (non-interrupting)': '信号开始事件(非中断)',
  'Conditional Start Event (non-interrupting)': '条件开始事件(非中断)',
  'Escalation Start Event (non-interrupting)': '升级开始事件(非中断)',
  
  // 结束事件类型
  'None End Event': '空结束事件',
  'Message End Event': '消息结束事件',
  'Signal End Event': '信号结束事件',
  'Error End Event': '错误结束事件',
  'Escalation End Event': '升级结束事件',
  'Compensation End Event': '补偿结束事件',
  'Terminate End Event': '终止结束事件',
  
  // 中间事件类型
  'Message Intermediate Catch Event': '消息中间捕获事件',
  'Message Intermediate Throw Event': '消息中间抛出事件',
  'Timer Intermediate Catch Event': '定时中间捕获事件',
  'Signal Intermediate Catch Event': '信号中间捕获事件',
  'Signal Intermediate Throw Event': '信号中间抛出事件',
  'Conditional Intermediate Catch Event': '条件中间捕获事件',
  'Link Intermediate Catch Event': '链接中间捕获事件',
  'Link Intermediate Throw Event': '链接中间抛出事件',
  'Compensation Intermediate Throw Event': '补偿中间抛出事件',
  'Escalation Intermediate Throw Event': '升级中间抛出事件',
  
  // 边界事件类型
  'Message Boundary Event': '消息边界事件',
  'Timer Boundary Event': '定时边界事件',
  'Signal Boundary Event': '信号边界事件',
  'Error Boundary Event': '错误边界事件',
  'Conditional Boundary Event': '条件边界事件',
  'Escalation Boundary Event': '升级边界事件',
  'Compensation Boundary Event': '补偿边界事件',
  'Message Boundary Event (non-interrupting)': '消息边界事件(非中断)',
  'Timer Boundary Event (non-interrupting)': '定时边界事件(非中断)',
  'Signal Boundary Event (non-interrupting)': '信号边界事件(非中断)',
  'Conditional Boundary Event (non-interrupting)': '条件边界事件(非中断)',
  'Escalation Boundary Event (non-interrupting)': '升级边界事件(非中断)',
}

/**
 * 翻译函数
 * @param {string} template - 需要翻译的文本
 * @param {object} replacements - 替换参数
 * @returns {string} 翻译后的文本
 */
export default function customTranslate(template, replacements) {
  // 调试：打印传入的翻译key
  console.log('BPMN Translate:', template)
  
  // 查找翻译
  let result = translations[template] || template
  
  // 处理替换参数
  if (replacements) {
    for (const key in replacements) {
      result = result.replace(new RegExp(`{${key}}`, 'g'), replacements[key])
    }
  }
  
  return result
}