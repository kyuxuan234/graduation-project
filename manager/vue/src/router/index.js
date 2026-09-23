import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

// 解决导航栏或者底部导航tabBar中的vue-router在3.0版本以上频繁点击菜单报错的问题。
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push (location) {
  return originalPush.call(this, location).catch(err => err)
}

// 管理端角色，只有这三种角色可以进入后台
const MANAGER_ROLES = ['ADMIN', 'DEV', 'OPS']
// 无需登录即可访问的页面
const WHITE_LIST = ['/login', '/register']

const routes = [
  {
    path: '/',
    name: 'Manager',
    component: () => import('../views/Manager.vue'),
    redirect: '/dashboard',
    children: [
      { path: '403', name: 'NoAuth', meta: { name: '无权限' }, component: () => import('../views/manager/403') },
      { path: 'dashboard', name: 'Dashboard', meta: { name: '工作台' }, component: () => import('../views/manager/Dashboard') },
      { path: 'project', name: 'Project', meta: { name: '项目', hint: '维护项目台账，点编号可下钻到应用', roles: ['ADMIN', 'DEV'] }, component: () => import('../views/manager/Project') },
      { path: 'server', name: 'Server', meta: { name: '服务器', hint: '登记主机与 SSH；保存时开通密钥，供自动部署与探活', roles: ['ADMIN', 'OPS'] }, component: () => import('../views/manager/Server') },
      { path: 'app', name: 'App', meta: { name: '应用', hint: '应用挂在项目与服务器上，发布前请确认版本与部署机' }, component: () => import('../views/manager/App') },
      { path: 'deploy', name: 'Deploy', meta: { name: '部署单', hint: '开发申请 → 运维审批 → 自动/手动执行 → 成功或回滚' }, component: () => import('../views/manager/Deploy') },
      { path: 'deployLog', redirect: '/deploy' },
      { path: 'alarm', name: 'Alarm', meta: { name: '告警', hint: '接手后处置，解决后计入闭环率' }, component: () => import('../views/manager/Alarm') },
      { path: 'analysis', name: 'Analysis', meta: { name: '发布分析', hint: '按时间窗看部署成败、错误 TOP 与告警走势' }, component: () => import('../views/manager/Analysis') },
      { path: 'dev', name: 'Dev', meta: { name: '开发人员', hint: '管理系统开发账号', roles: ['ADMIN'] }, component: () => import('../views/manager/Dev') },
      { path: 'ops', name: 'Ops', meta: { name: '运维人员', hint: '管理系统运维账号', roles: ['ADMIN'] }, component: () => import('../views/manager/Ops') },
      { path: 'admin', name: 'Admin', meta: { name: '超级管理员', roles: ['ADMIN'] }, component: () => import('../views/manager/Admin') },
      { path: 'adminPerson', name: 'AdminPerson', meta: { name: '个人信息' }, component: () => import('../views/manager/AdminPerson') },
      { path: 'password', name: 'Password', meta: { name: '修改密码' }, component: () => import('../views/manager/Password') },
      { path: 'notice', name: 'Notice', meta: { name: '系统公告', hint: '面向全员的通知公告', roles: ['ADMIN'] }, component: () => import('../views/manager/Notice') },
    ]
  },
  { path: '/login', name: 'Login', meta: { name: '登录' }, component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', meta: { name: '注册' }, component: () => import('../views/Register.vue') },
  { path: '*', name: 'NotFound', meta: { name: '无法访问' }, component: () => import('../views/404.vue') },
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 登录、注册页面直接放行
  if (WHITE_LIST.includes(to.path)) {
    return next()
  }
  let user = JSON.parse(localStorage.getItem("xm-user") || '{}')
  // 未登录，跳转登录页
  if (!user.id || !user.token) {
    return next('/login')
  }
  // 管理端只允许 ADMIN / DEV / OPS 进入
  if (!MANAGER_ROLES.includes(user.role)) {
    localStorage.removeItem('xm-user')
    return next('/login')
  }
  // 页面级角色：有 meta.roles 时，不在列表内的角色进 403（菜单隐藏不够防直链）
  const roles = to.meta && to.meta.roles
  if (roles && roles.length && !roles.includes(user.role)) {
    return next('/403')
  }
  next()
})

export default router
