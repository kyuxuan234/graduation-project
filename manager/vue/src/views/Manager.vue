<template>
  <div class="manager-container" :class="{ 'is-ops': isOps }">
    <!--  头部  -->
    <div class="manager-header">
      <div class="manager-header-left">
        <img src="@/assets/imgs/logo.png" />
        <div>
          <div class="title">运维管理系统</div>
          <div class="ops-clock" v-if="isOps">{{ nowText }}</div>
        </div>
      </div>

      <div class="manager-header-center">
        <div class="ops-main-title" v-if="isOps"><i class="el-icon-monitor"></i>DevOps 运维管理系统监控</div>
        <el-breadcrumb v-else separator-class="el-icon-arrow-right">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: $route.path }">{{ $route.meta.name }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="manager-header-right">
        <el-dropdown v-if="inboxItems.length" trigger="click" @command="goInbox">
          <span class="inbox-btn">
            <i class="el-icon-bell"></i>
            <em v-if="inboxItems.length">{{ inboxItems.length > 9 ? '9+' : inboxItems.length }}</em>
          </span>
          <el-dropdown-menu slot="dropdown" class="inbox-menu">
            <el-dropdown-item v-for="item in inboxItems.slice(0, 6)" :key="item.key" :command="item">
              <span class="inbox-dot" :class="'is-' + item.tone"></span>{{ item.text }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <el-dropdown placement="bottom">
          <div class="avatar">
            <img :src="avatarSrc" @error="useDefaultAvatar" />
            <div>{{ user.name || user.username }}（{{ roleName }}）</div>
          </div>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item v-if="user.role === 'ADMIN'" icon="el-icon-user" @click.native="$router.push('/adminPerson')">个人信息</el-dropdown-item>
            <el-dropdown-item icon="el-icon-key" @click.native="$router.push('/password')">修改密码</el-dropdown-item>
            <el-dropdown-item icon="el-icon-switch-button" @click.native="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </div>

    <!--  主体  -->
    <div class="manager-main">
      <!--  侧边栏，按当前登录角色过滤菜单  -->
      <div class="manager-main-left">
        <el-menu :default-openeds="['ops', 'user']" router style="border: none" :default-active="$route.path">
          <template v-for="menu in menus">
            <!--  一级菜单  -->
            <el-menu-item v-if="!menu.children" :key="menu.index" :index="menu.index">
              <i :class="menu.icon"></i>
              <span slot="title">{{ menu.name }}</span>
            </el-menu-item>
            <!--  带子菜单  -->
            <el-submenu v-else :key="menu.index" :index="menu.index">
              <template slot="title">
                <i :class="menu.icon"></i><span>{{ menu.name }}</span>
              </template>
              <el-menu-item v-for="child in menu.children" :key="child.index" :index="child.index">
                <i :class="child.icon"></i>
                <span slot="title">{{ child.name }}</span>
              </el-menu-item>
            </el-submenu>
          </template>
        </el-menu>
      </div>

      <!--  数据表格  -->
      <div class="manager-main-right">
        <div class="page-head" v-if="$route.path !== '/dashboard'">
          <i :class="pageIcon"></i>
          <span>{{ $route.meta.name }}</span>
        </div>
        <router-view @update:user="updateUser" />
      </div>
    </div>

  </div>
</template>

<script>
import { rememberPerson, rememberPersons } from '@/utils/person'
import { buildInbox } from '@/utils/inbox'
import { defaultAvatar, resolveAvatar } from '@/utils/avatar'

// 全部菜单，roles 表示哪些角色可见
const ALL_MENUS = [
  { index: '/dashboard', name: '首页仪表盘', icon: 'el-icon-s-home', roles: ['ADMIN', 'DEV', 'OPS'] },
  {
    index: 'ops', name: '运维管理', icon: 'el-icon-s-operation',
    children: [
      { index: '/project', name: '项目管理', icon: 'el-icon-folder', roles: ['ADMIN', 'DEV'] },
      { index: '/server', name: '服务器管理', icon: 'el-icon-monitor', roles: ['ADMIN', 'DEV', 'OPS'] },
      { index: '/app', name: '应用管理', icon: 'el-icon-s-platform', roles: ['ADMIN', 'DEV', 'OPS'] },
      { index: '/deploy', name: '部署管理', icon: 'el-icon-s-promotion', roles: ['ADMIN', 'DEV', 'OPS'] },
      { index: '/alarm', name: '告警管理', icon: 'el-icon-warning-outline', roles: ['ADMIN', 'DEV', 'OPS'] },
      { index: '/analysis', name: '运维分析', icon: 'el-icon-data-line', roles: ['ADMIN', 'DEV', 'OPS'] },
    ]
  },
  {
    index: 'user', name: '用户管理', icon: 'el-icon-user-solid',
    children: [
      { index: '/dev', name: '开发人员', icon: 'el-icon-user', roles: ['ADMIN'] },
      { index: '/ops', name: '运维人员', icon: 'el-icon-s-custom', roles: ['ADMIN'] },
      { index: '/notice', name: '系统公告', icon: 'el-icon-bell', roles: ['ADMIN'] },
    ]
  },
]

const PAGE_ICONS = {
  '/project': 'el-icon-folder',
  '/server': 'el-icon-monitor',
  '/app': 'el-icon-s-platform',
  '/deploy': 'el-icon-s-promotion',
  '/alarm': 'el-icon-warning-outline',
  '/analysis': 'el-icon-data-line',
  '/dev': 'el-icon-user',
  '/ops': 'el-icon-s-custom',
  '/adminPerson': 'el-icon-user',
  '/password': 'el-icon-key',
  '/admin': 'el-icon-s-custom',
  '/notice': 'el-icon-bell',
}

const ROLE_NAMES = { ADMIN: '超级管理员', DEV: '开发人员', OPS: '运维人员' }

export default {
  name: "Manager",
  data() {
    return {
      user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
      nowText: '',
      clockTimer: null,
      inboxItems: [],
      avatarFailed: false,
    }
  },
  computed: {
    avatarSrc() {
      if (this.avatarFailed) {
        return defaultAvatar
      }
      return resolveAvatar(this.user.avatar)
    },
    isOps() {
      return this.$route.path === '/dashboard'
    },
    roleName() {
      return ROLE_NAMES[this.user.role] || this.user.role
    },
    menus() {  // 按角色过滤菜单，子菜单被过滤空了就把整个分组也隐藏
      const role = this.user.role
      return ALL_MENUS
          .map(menu => menu.children
              ? { ...menu, children: menu.children.filter(child => child.roles.includes(role)) }
              : menu)
          .filter(menu => menu.children ? menu.children.length : menu.roles.includes(role))
    },
    pageIcon() {
      return PAGE_ICONS[this.$route.path] || 'el-icon-document'
    }
  },
  created() {
    if (!this.user.id) {
      this.$router.push('/login')
      return
    }
    rememberPerson(this.user.username, this.user.name)
    if (this.user.role === 'ADMIN') {
      this.$request.get('/dev/selectAll').then(res => rememberPersons(res.data))
      this.$request.get('/ops/selectAll').then(res => rememberPersons(res.data))
    }
    this.loadInbox(true)
  },
  watch: {
    isOps: {
      immediate: true,
      handler(val) {
        this.toggleClock(val)
      }
    },
    '$route.fullPath'() {
      this.loadInbox(false)
    }
  },
  beforeDestroy() {
    this.toggleClock(false)
  },
  methods: {
    tickClock() {
      const d = new Date()
      const pad = n => String(n).padStart(2, '0')
      this.nowText = d.getFullYear() + '/' + pad(d.getMonth() + 1) + '/' + pad(d.getDate())
          + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
    },
    toggleClock(on) {
      if (this.clockTimer) {
        clearInterval(this.clockTimer)
        this.clockTimer = null
      }
      if (on) {
        this.tickClock()
        this.clockTimer = setInterval(this.tickClock, 1000)
      }
    },
    updateUser() {
      this.user = JSON.parse(localStorage.getItem('xm-user') || '{}')   // 重新获取下用户的最新信息
      this.avatarFailed = false
    },
    useDefaultAvatar() {
      this.avatarFailed = true
    },
    loadInbox(notify) {
      if (!this.user.id || this.user.role === 'ADMIN') {
        this.inboxItems = []
        return
      }
      Promise.all([
        this.$request.get('/deploy/selectPage', { params: { pageNum: 1, pageSize: 20 } }),
        this.$request.get('/alarm/selectPage', { params: { pageNum: 1, pageSize: 20 } })
      ]).then(([deployRes, alarmRes]) => {
        this.inboxItems = buildInbox({
          user: this.user,
          deploys: (deployRes.data && deployRes.data.list) || [],
          alarms: (alarmRes.data && alarmRes.data.list) || []
        })
        if (notify) this.notifyInbox()
      }).catch(() => {
        this.inboxItems = []
      })
    },
    notifyInbox() {
      const key = 'xm-inbox-notified:' + this.user.username
      if (sessionStorage.getItem(key) || !this.inboxItems.length) return
      sessionStorage.setItem(key, '1')
      this.inboxItems.slice(0, 2).forEach((item, index) => {
        setTimeout(() => {
          this.$notify({
            title: item.title,
            message: item.text,
            type: item.notifyType || 'info',
            duration: 8000,
            onClick: () => this.goInbox(item)
          })
        }, index * 400)
      })
    },
    goInbox(item) {
      if (!item || !item.path) return
      this.$router.push(item.query ? { path: item.path, query: item.query } : item.path)
    },
    logout() {
      if (this.user.role) {
        localStorage.setItem('xm-login-role', this.user.role)
      }
      localStorage.removeItem('xm-user')
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
@import "@/assets/css/manager.css";
</style>
