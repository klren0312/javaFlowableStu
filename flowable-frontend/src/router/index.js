import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      // 系统管理
      {
        path: 'system/user',
        name: 'UserList',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/department',
        name: 'DepartmentList',
        component: () => import('@/views/system/DepartmentList.vue'),
        meta: { title: '部门管理' }
      },
      {
        path: 'system/role',
        name: 'RoleList',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { title: '角色管理' }
      },
      // 流程管理
      {
        path: 'process/definition',
        name: 'ProcessDefinition',
        component: () => import('@/views/process/ProcessDefinition.vue'),
        meta: { title: '流程定义' }
      },
      {
        path: 'process/instance',
        name: 'ProcessInstance',
        component: () => import('@/views/process/ProcessInstance.vue'),
        meta: { title: '流程实例' }
      },
      {
        path: 'process/task',
        name: 'TaskList',
        component: () => import('@/views/process/TaskList.vue'),
        meta: { title: '待办任务' }
      },
      // 流程设计器
      {
        path: 'modeler',
        name: 'Modeler',
        component: () => import('@/views/modeler/Modeler.vue'),
        meta: { title: '流程设计器' }
      },
      {
        path: 'modeler/:processDefinitionId',
        name: 'ModelerEdit',
        component: () => import('@/views/modeler/Modeler.vue'),
        meta: { title: '编辑流程' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router