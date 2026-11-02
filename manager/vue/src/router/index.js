import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

// 解决导航栏或者底部导航tabBar中的vue-router在3.0版本以上频繁点击菜单报错的问题。
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push (location) {
  return originalPush.call(this, location).catch(err => err)
}

// 管理端角色，只有这三种角色可以进入后台
const MANAGER_ROLES = ['ADMIN', 'DEV', 'TL', 'QA', 'PM', 'OPS']
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
      { path: 'dashboard', name: 'Dashboard', meta: { name: '发布看板' }, component: () => import('../views/manager/Dashboard') },
      { path: 'project', name: 'Project', meta: { name: '业务项目', hint: '开发维护项目信息，开发负责人由技术负责人分配', roles: ['ADMIN', 'DEV', 'TL'] }, component: () => import('../views/manager/Project') },
      { path: 'environment', name: 'Environment', meta: { name: '运行环境', hint: '开发、测试、生产。运维维护环境说明，服务器归入其中之一', roles: ['ADMIN', 'OPS'] }, component: () => import('../views/manager/Environment') },
      { path: 'server', name: 'Server', meta: { name: '服务器', hint: '每台服务器只属于一个环境。保存时开通密钥，供自动发布与探活', roles: ['ADMIN', 'OPS'] }, component: () => import('../views/manager/Server') },
      { path: 'app', name: 'App', meta: { name: '应用服务', hint: '应用本身不绑定服务器。按环境挂实例后，版本从开发推进到测试，再进入生产', roles: ['ADMIN', 'DEV', 'OPS'] }, component: () => import('../views/manager/App') },
      { path: 'deploy', name: 'Deploy', meta: { name: '发布单', hint: '开发环境直接交运维；测试环境先测试验收；生产环境走技术审查和产品验收' }, component: () => import('../views/manager/Deploy') },
      { path: 'deployLog', redirect: '/deploy' },
      { path: 'alarm', name: 'Alarm', meta: { name: '发布告警', hint: '接手后处置，解决后计入闭环率', roles: ['ADMIN', 'OPS'] }, component: () => import('../views/manager/Alarm') },
      { path: 'analysis', name: 'Analysis', meta: { name: '发布报表', hint: '按时间窗看发布成败、错误 TOP 与告警走势', roles: ['ADMIN', 'OPS'] }, component: () => import('../views/manager/Analysis') },
      { path: 'person', name: 'Person', meta: { name: '团队成员', hint: '管理员、开发、运维账号在同一张表里，按角色区分', roles: ['ADMIN'] }, component: () => import('../views/manager/Person') },
      { path: 'dev', redirect: '/person' },
      { path: 'ops', redirect: '/person' },
      { path: 'admin', redirect: '/person' },
      { path: 'adminPerson', name: 'AdminPerson', meta: { name: '个人信息' }, component: () => import('../views/manager/AdminPerson') },
      { path: 'password', name: 'Password', meta: { name: '修改密码' }, component: () => import('../views/manager/Password') },
      { path: 'notice', name: 'Notice', meta: { name: '发布通知', hint: '面向全员的发布通知', roles: ['ADMIN'] }, component: () => import('../views/manager/Notice') },
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
