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
    redirect: '/dashboard',  // 重定向到首页仪表盘
    children: [
      { path: '403', name: 'NoAuth', meta: { name: '无权限' }, component: () => import('../views/manager/403') },
      { path: 'dashboard', name: 'Dashboard', meta: { name: '首页仪表盘' }, component: () => import('../views/manager/Dashboard') },
      { path: 'project', name: 'Project', meta: { name: '项目管理', roles: ['ADMIN', 'DEV'] }, component: () => import('../views/manager/Project') },
      { path: 'server', name: 'Server', meta: { name: '服务器管理', roles: ['ADMIN', 'OPS'] }, component: () => import('../views/manager/Server') },
      { path: 'app', name: 'App', meta: { name: '应用管理' }, component: () => import('../views/manager/App') },
      { path: 'deploy', name: 'Deploy', meta: { name: '部署管理' }, component: () => import('../views/manager/Deploy') },
      { path: 'deployLog', redirect: '/deploy' },
      { path: 'alarm', name: 'Alarm', meta: { name: '告警管理' }, component: () => import('../views/manager/Alarm') },
      { path: 'analysis', name: 'Analysis', meta: { name: '运维分析' }, component: () => import('../views/manager/Analysis') },
      { path: 'dev', name: 'Dev', meta: { name: '开发人员', roles: ['ADMIN'] }, component: () => import('../views/manager/Dev') },
      { path: 'ops', name: 'Ops', meta: { name: '运维人员', roles: ['ADMIN'] }, component: () => import('../views/manager/Ops') },
      { path: 'admin', name: 'Admin', meta: { name: '超级管理员', roles: ['ADMIN'] }, component: () => import('../views/manager/Admin') },
      { path: 'adminPerson', name: 'AdminPerson', meta: { name: '个人信息' }, component: () => import('../views/manager/AdminPerson') },
      { path: 'password', name: 'Password', meta: { name: '修改密码' }, component: () => import('../views/manager/Password') },
      { path: 'notice', name: 'Notice', meta: { name: '公告信息', roles: ['ADMIN'] }, component: () => import('../views/manager/Notice') },
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
