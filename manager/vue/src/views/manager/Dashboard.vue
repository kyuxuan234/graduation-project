<template>
  <div class="ops">
    <div class="role-tip">{{ roleTip }}</div>
    <div class="notice-bar" v-if="tickerItem" :class="['is-' + tickerItem.tone, { clickable: !!tickerItem.path }]" @click="goTicker">
      <i :class="tickerItem.icon"></i>
      <em>{{ tickerItem.label }}</em>
      <span class="notice-text">{{ tickerItem.text }}</span>
    </div>
    <div class="ops-grid">
      <section class="ops-panel p-overview">
        <h3><i class="el-icon-data-analysis"></i>一览</h3>
        <div class="ov-grid">
          <div v-for="item in cards" :key="item.key" class="ov" @click="go(item.path, item.query)">
            <span><i :class="item.icon"></i>{{ item.name }}</span>
            <b :class="item.tone">{{ count[item.key] || 0 }}</b>
          </div>
        </div>
      </section>

      <section class="ops-panel p-asset">
        <h3><i class="el-icon-s-grid"></i>资产分布</h3>
        <div class="asset-row">
          <div class="asset-item" @click="go('/server', { os: 'CentOS' })">
            <os-logo os="CentOS"></os-logo>
            <b>{{ osCount.centos }}</b>
            <span>CentOS</span>
          </div>
          <div class="asset-item" @click="go('/server', { os: 'Ubuntu' })">
            <os-logo os="Ubuntu"></os-logo>
            <b class="tone-warn">{{ osCount.ubuntu }}</b>
            <span>Ubuntu</span>
          </div>
          <div class="asset-item" @click="go('/server', { status: 'RUNNING' })">
            <i class="el-icon-success"></i>
            <b class="tone-ok">{{ serverStatus.RUNNING }}</b>
            <span>运行中</span>
          </div>
          <div class="asset-item" @click="go('/app')">
            <i class="el-icon-s-platform"></i>
            <b>{{ count.appCount || 0 }}</b>
            <span>应用</span>
          </div>
          <div class="asset-item" @click="go('/project')">
            <i class="el-icon-folder"></i>
            <b class="tone-cyan">{{ count.projectCount || 0 }}</b>
            <span>项目</span>
          </div>
        </div>
        <div class="env-row">
          <div v-for="env in envItems" :key="env.value" class="env-chip" @click="go('/server', { env: env.value })">
            <i :class="env.icon"></i>{{ env.name }} {{ envCount[env.value] || 0 }}
          </div>
        </div>
      </section>

      <section class="ops-panel p-kpi">
        <h3><i class="el-icon-odometer"></i>交付与风险</h3>
        <div class="kpi-line clickable" @click="go('/deploy', { finished: '1' })">
          <span><i class="el-icon-s-promotion"></i>交付成功率</span>
          <em class="tone-ok">{{ deploySuccessRate }}%（{{ deploySuccessCount }}/{{ deployFinishedCount }}）</em>
        </div>
        <div class="rank">
          <div class="bar"><i class="ok" :style="{ width: deploySuccessRate + '%' }"></i></div>
        </div>
        <div class="kpi-line clickable" @click="go('/alarm')">
          <span><i class="el-icon-warning-outline"></i>告警闭环率</span>
          <em class="tone-warn">{{ alarmResolveRate }}%（{{ alarmResolvedCount }}/{{ alarmTotalCount }}）</em>
        </div>
        <div class="rank">
          <div class="bar"><i class="warn" :style="{ width: alarmResolveRate + '%' }"></i></div>
        </div>
        <div class="kpi-line clickable" @click="go('/deploy', { status: 'PENDING' })">
          <span><i class="el-icon-time"></i>待审批部署</span>
          <em>{{ count.pendingDeployCount || 0 }}</em>
        </div>
        <div class="kpi-line clickable" @click="go('/alarm', { status: 'UNHANDLED' })">
          <span><i class="el-icon-bell"></i>未处理告警</span>
          <em class="tone-danger">{{ count.unhandledAlarmCount || 0 }}</em>
        </div>
      </section>

      <section class="ops-panel p-trend">
        <h3><i class="el-icon-data-line"></i>近14天部署</h3>
        <div ref="trend14" class="trend-box"></div>
      </section>

      <section class="ops-panel p-deploy">
        <h3><i class="el-icon-s-promotion"></i>部署状态</h3>
        <div v-for="item in deployBars" :key="item.value" class="rank clickable" @click="$router.push({ path: '/deploy', query: { status: item.value } })">
          <span><i :class="item.icon"></i>{{ item.name }}</span>
          <div class="bar"><i :class="item.tone" :style="{ width: barWidth(item.count, deployMax) }"></i></div>
          <em>{{ item.count }}</em>
        </div>
      </section>

      <section class="ops-panel p-apps">
        <div class="panel-head">
          <h3><i class="el-icon-s-platform"></i>应用风险</h3>
          <a class="panel-more" href="javascript:;" @click.prevent="go('/app')">更多</a>
        </div>
        <table class="ops-table">
          <thead>
            <tr>
              <th><i class="el-icon-s-platform"></i>应用</th>
              <th><i class="el-icon-s-promotion"></i>部署单</th>
              <th><i class="el-icon-warning-outline"></i>告警</th>
              <th><i class="el-icon-time"></i>24h告警</th>
              <th><i class="el-icon-circle-check"></i>完成</th>
              <th><i class="el-icon-aim"></i>风险</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in appRiskPreview" :key="row.id">
              <td class="clickable" @click="go('/app', { id: row.id })"><i class="el-icon-s-platform"></i>{{ row.name }}</td>
              <td class="clickable" @click="go('/deploy', { appId: row.id })">{{ num(row.deployCount) }}</td>
              <td class="clickable" @click="go('/alarm', { appId: row.id })">{{ num(row.alarmCount) }}</td>
              <td class="clickable" @click="go('/alarm', { appId: row.id })">{{ num(row.alarmAfterSuccess) }}</td>
              <td class="clickable" @click="go('/alarm', { appId: row.id, status: 'RESOLVED' })">{{ resolvedAlarm(row) }}</td>
              <td class="clickable" @click="go('/alarm', { appId: row.id })">
                <span class="risk" :class="riskLevel(row).tone"><i :class="riskLevel(row).icon"></i>{{ riskLevel(row).name }}</span>
              </td>
            </tr>
            <tr v-if="!appRiskPreview.length"><td colspan="6" class="empty">暂无应用数据</td></tr>
          </tbody>
        </table>
      </section>

      <section class="ops-panel p-alarm">
        <h3><i class="el-icon-warning-outline"></i>告警状态</h3>
        <div v-for="item in alarmBars" :key="item.value" class="rank clickable" @click="$router.push({ path: '/alarm', query: { status: item.value } })">
          <span><i :class="item.icon"></i>{{ item.name }}</span>
          <div class="bar"><i :class="item.tone" :style="{ width: barWidth(item.count, alarmMax) }"></i></div>
          <em>{{ item.count }}</em>
        </div>
      </section>

    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import OsLogo from '@/components/OsLogo'
import { canOps } from '@/utils/person'

const DEPLOY_STATUS = [
  { value: 'REVIEW', name: '待技术审查', tone: 'warn', icon: 'el-icon-view' },
  { value: 'TESTING', name: '待测试验收', tone: 'warn', icon: 'el-icon-s-check' },
  { value: 'ACCEPTING', name: '待产品验收', tone: 'warn', icon: 'el-icon-s-claim' },
  { value: 'PENDING', name: '待运维审批', tone: 'warn', icon: 'el-icon-time' },
  { value: 'APPROVED', name: '已批准', tone: 'cyan', icon: 'el-icon-check' },
  { value: 'DEPLOYING', name: '部署中', tone: 'cyan', icon: 'el-icon-loading' },
  { value: 'SUCCESS', name: '已交付', tone: 'ok', icon: 'el-icon-circle-check' },
  { value: 'FAILED', name: '发布失败', tone: 'danger', icon: 'el-icon-circle-close' },
  { value: 'REJECTED', name: '已驳回', tone: '', icon: 'el-icon-close' },
]
const ALARM_STATUS = [
  { value: 'UNHANDLED', name: '未处理', tone: 'danger', icon: 'el-icon-bell' },
  { value: 'HANDLING', name: '处理中', tone: 'warn', icon: 'el-icon-loading' },
  { value: 'RESOLVED', name: '已解决', tone: 'ok', icon: 'el-icon-circle-check' },
]
export default {
  name: "Dashboard",
  components: { OsLogo },
  data() {
    return {
      count: {},
      cards: [
        { key: 'projectCount', name: '项目总数', path: '/project', tone: '', icon: 'el-icon-folder' },
        { key: 'serverCount', name: '服务器总数', path: '/server', tone: 'tone-cyan', icon: 'el-icon-monitor' },
        { key: 'appCount', name: '应用总数', path: '/app', tone: '', icon: 'el-icon-s-platform' },
        { key: 'onlineAppCount', name: '在线应用', path: '/app', query: { status: 'ONLINE' }, tone: 'tone-ok', icon: 'el-icon-circle-check' },
        { key: 'pendingDeployCount', name: '待审批部署', path: '/deploy', query: { status: 'PENDING' }, tone: 'tone-warn', icon: 'el-icon-s-promotion' },
        { key: 'unhandledAlarmCount', name: '未处理告警', path: '/alarm', query: { status: 'UNHANDLED' }, tone: 'tone-danger', icon: 'el-icon-warning-outline' },
      ],
      deployBars: [],
      alarmBars: [],
      appRisk: [],
      trendChart: null,
      recentDeploys: [],
      osCount: { centos: 0, ubuntu: 0 },
      envCount: { DEV: 0, TEST: 0, PROD: 0 },
      serverStatus: { RUNNING: 0, STOPPED: 0, MAINTENANCE: 0 },
      envItems: [
        { value: 'DEV', name: '开发', icon: 'el-icon-edit-outline' },
        { value: 'TEST', name: '测试', icon: 'el-icon-cpu' },
        { value: 'PROD', name: '生产', icon: 'el-icon-s-flag' },
      ],
      deploySuccessRate: 0,
      deploySuccessCount: 0,
      deployFinishedCount: 0,
      alarmResolveRate: 0,
      alarmResolvedCount: 0,
      alarmTotalCount: 0,
      user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
      notices: [],
      tickerIndex: 0,
      tickerTimer: null,
    }
  },
  computed: {
    tickerItems() {
      const items = []
      const role = this.user.role
      const unhandled = this.num(this.count.unhandledAlarmCount)
      const pending = this.num(this.count.pendingDeployCount)
      const failed = this.num((this.deployBars.find(v => v.value === 'FAILED') || {}).count)
      const highRisk = this.appRisk.filter(row => this.riskLevel(row).name === '高').length
      const mine = this.recentDeploys.filter(row => row.applicant === this.user.username)
      if (role === 'OPS') {
        if (unhandled) {
          items.push({ tone: 'danger', icon: 'el-icon-warning-outline', label: '待你接手', text: '有 ' + unhandled + ' 条未处理告警，请运维接手。', path: '/alarm', query: { status: 'UNHANDLED' } })
        }
        if (pending) {
          items.push({ tone: 'warn', icon: 'el-icon-time', label: '待你审批', text: '有 ' + pending + ' 张部署单等待你审批。', path: '/deploy', query: { status: 'PENDING' } })
        }
      } else if (role === 'DEV') {
        const rejected = mine.find(row => row.status === 'REJECTED')
        const approved = mine.find(row => row.status === 'APPROVED')
        const pendingMine = mine.find(row => row.status === 'PENDING')
        const failedMine = mine.find(row => row.status === 'FAILED')
        if (rejected) {
          items.push({ tone: 'danger', icon: 'el-icon-close', label: '已驳回', text: '你的部署单 ' + rejected.deployNo + ' 已被驳回，请重新申请。', path: '/deploy', query: { status: 'REJECTED' } })
        }
        if (failedMine) {
          items.push({ tone: 'danger', icon: 'el-icon-circle-close', label: '失败', text: '你的部署单 ' + failedMine.deployNo + ' 发布失败。', path: '/deploy', query: { status: 'FAILED' } })
        }
        if (approved) {
          items.push({ tone: 'warn', icon: 'el-icon-check', label: '已批准', text: '你的部署单 ' + approved.deployNo + ' 已批准，等待运维执行。', path: '/deploy', query: { status: 'APPROVED' } })
        }
        if (pendingMine) {
          items.push({ tone: 'cyan', icon: 'el-icon-time', label: '审批中', text: '你的部署单 ' + pendingMine.deployNo + ' 正在等待运维审批。', path: '/deploy', query: { status: 'PENDING' } })
        }
      } else {
        if (unhandled) {
          items.push({ tone: 'danger', icon: 'el-icon-warning-outline', label: '告警', text: '未处理告警 ' + unhandled + ' 条，请运维及时接手。', path: '/alarm', query: { status: 'UNHANDLED' } })
        }
        if (pending) {
          items.push({ tone: 'warn', icon: 'el-icon-time', label: '部署', text: '待审批部署 ' + pending + ' 张，等待运维审批。', path: '/deploy', query: { status: 'PENDING' } })
        }
      }
      if (failed && role !== 'DEV') {
        items.push({ tone: 'danger', icon: 'el-icon-circle-close', label: '失败', text: '发布失败 ' + failed + ' 张，相关应用风险升高。', path: '/deploy', query: { status: 'FAILED' } })
      }
      if (highRisk) {
        items.push({ tone: 'warn', icon: 'el-icon-aim', label: '风险', text: '高风险应用 ' + highRisk + ' 个，请查看应用变更与风险。' })
      }
      if (!items.length && this.alarmTotalCount) {
        items.push({ tone: 'ok', icon: 'el-icon-circle-check', label: '正常', text: '当前没有需要你处理的单据，告警闭环率 ' + this.alarmResolveRate + '%。' })
      }
      this.notices.forEach(row => {
        items.push({
          tone: this.noticeTone(row),
          icon: 'el-icon-bell',
          label: '公告',
          text: row.title + (row.content ? '：' + row.content : '')
        })
      })
      return items
    },
    tickerItem() {
      if (!this.tickerItems.length) return null
      return this.tickerItems[this.tickerIndex % this.tickerItems.length]
    },
    roleTip() {
      const map = {
        DEV: '开发环境提交后直接等运维；测试环境先由测试验收；生产环境依次经过技术审查、测试验收和产品验收',
        TL: '你只处理生产环境的技术审查，通过后进入测试验收',
        QA: '测试环境验收通过后交给运维；生产环境验收通过后交给产品经理',
        PM: '你只处理生产环境、且测试已通过的发布单',
        OPS: '各环境完成本环境要求的审查验收后，由你审批并执行',
        ADMIN: '全流程可见：项目 / 服务器 / 应用 / 部署 / 告警 / 人员'
      }
      return map[this.user.role] || '发布管理系统工作台'
    },
    deployMax() {
      return Math.max(1, ...this.deployBars.map(v => v.count))
    },
    alarmMax() {
      return Math.max(1, ...this.alarmBars.map(v => v.count))
    },
    appRiskPreview() {
      return (this.appRisk || []).slice(0, 5)
    }
  },
  created() {
    if (this.user.role === 'ADMIN') {
      this.loadCount()
      this.loadDeployStatus()
      this.loadAlarmStatus()
      this.loadAppRisk()
      this.loadServers()
      this.loadTrend14()
    } else {
      this.loadOwnBoard()
    }
    this.loadRecentDeploys()
    this.loadNotices()
    this.tickerTimer = setInterval(() => {
      if (this.tickerItems.length) {
        this.tickerIndex = (this.tickerIndex + 1) % this.tickerItems.length
      }
    }, 4000)
  },
  mounted() {
    window.addEventListener('resize', this.resizeTrend)
  },
  beforeDestroy() {
    if (this.tickerTimer) clearInterval(this.tickerTimer)
    window.removeEventListener('resize', this.resizeTrend)
    if (this.trendChart) this.trendChart.dispose()
  },
  methods: {
    go(path, query) {
      if (path === '/alarm' && !canOps(this.user)) {
        this.$message.info('发布告警由运维处置')
        return
      }
      if (path === '/analysis' && !canOps(this.user)) {
        this.$message.info('发布报表由运维查看')
        return
      }
      if (path === '/server' && !canOps(this.user)) {
        this.$message.info('服务器由运维管理，开发可在应用中选择部署机')
        return
      }
      if (path === '/project' && this.user.role !== 'DEV' && this.user.role !== 'ADMIN') {
        this.$message.info('业务项目由开发维护')
        return
      }
      this.$router.push(query ? { path, query } : path)
    },
    goTicker() {
      if (this.tickerItem && this.tickerItem.path) {
        this.go(this.tickerItem.path, this.tickerItem.query)
      }
    },
    num(v) {
      return Number(v || 0)
    },
    barWidth(count, max) {
      return Math.max(6, Math.round(this.num(count) * 100 / max)) + '%'
    },
    resolvedAlarm(row) {
      return Math.max(0, this.num(row.alarmCount) - this.num(row.openAlarmCount))
    },
    riskLevel(row) {
      const open = this.num(row.openAlarmCount)
      const critical = this.num(row.criticalOpenCount)
      const failed = this.num(row.failedDeployCount)
      if (critical >= 1 || failed >= 1 || open >= 2) return { name: '高', tone: 'danger', icon: 'el-icon-warning' }
      if (open >= 1) return { name: '中', tone: 'warn', icon: 'el-icon-info' }
      return { name: '低', tone: 'ok', icon: 'el-icon-success' }
    },
    loadOwnBoard() {
      const role = this.user.role
      const jobs = [this.$request.get('/deploy/selectAll')]
      if (role === 'OPS') {
        jobs.push(this.$request.get('/server/selectPage', { params: { pageNum: 1, pageSize: 500 } }))
        jobs.push(this.$request.get('/app/selectAll'))
        jobs.push(this.$request.get('/alarm/selectAll'))
        jobs.push(this.$request.get('/project/selectAll'))
      } else if (role === 'DEV') {
        jobs.push(this.$request.get('/server/selectAll'))
        jobs.push(this.$request.get('/app/selectAll'))
        jobs.push(this.$request.get('/project/selectAll'))
      } else {
        jobs.push(this.$request.get('/server/selectAll'))
        jobs.push(this.$request.get('/app/selectAll'))
      }
      Promise.all(jobs).then(responses => {
        const deploys = responses[0].data || []
        const servers = role === 'OPS'
          ? ((responses[1].data && responses[1].data.list) || [])
          : (responses[1].data || [])
        const apps = (responses[2] && responses[2].data) || []
        const alarms = role === 'OPS' ? ((responses[3] && responses[3].data) || []) : []
        const projects = role === 'OPS'
          ? ((responses[4] && responses[4].data) || [])
          : ((responses[3] && responses[3].data) || [])
        this.applyOwnDeploys(deploys)
        this.applyOwnAssets(deploys, servers, apps, projects, alarms)
      }).catch(() => {})
    },
    applyOwnDeploys(list) {
      const countMap = {}
      DEPLOY_STATUS.forEach(item => { countMap[item.value] = 0 })
      list.forEach(row => {
        if (countMap[row.status] != null) countMap[row.status] += 1
      })
      this.deployBars = DEPLOY_STATUS.map(item => ({ ...item, count: countMap[item.value] || 0 }))
      this.deploySuccessCount = countMap.SUCCESS || 0
      this.deployFinishedCount = this.deploySuccessCount + (countMap.FAILED || 0)
      this.deploySuccessRate = this.deployFinishedCount
        ? Math.round(this.deploySuccessCount * 100 / this.deployFinishedCount) : 0
      const role = this.user.role
      let pending = list.length
      if (role === 'DEV') {
        pending = list.filter(row => !['SUCCESS', 'FAILED', 'REJECTED'].includes(row.status)).length
      } else if (role === 'OPS') {
        pending = list.filter(row => ['PENDING', 'APPROVED', 'DEPLOYING'].includes(row.status)).length
      }
      this.count = Object.assign({}, this.count, { pendingDeployCount: pending })
      this.renderTrend(this.trendFromDeploys(list))
    },
    trendFromDeploys(list) {
      const days = []
      const now = new Date()
      for (let i = 13; i >= 0; i--) {
        const date = new Date(now.getFullYear(), now.getMonth(), now.getDate() - i)
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        days.push(date.getFullYear() + '-' + month + '-' + day)
      }
      const countMap = {}
      days.forEach(day => { countMap[day] = 0 })
      list.forEach(row => {
        const day = String(row.applyTime || '').slice(0, 10)
        if (countMap[day] != null) countMap[day] += 1
      })
      return days.map(day => ({ date: day, count: countMap[day] }))
    },
    applyOwnAssets(deploys, servers, apps, projects, alarms) {
      const role = this.user.role
      const serverIds = {}
      const appIds = {}
      deploys.forEach(row => {
        if (row.serverId) serverIds[row.serverId] = true
        if (row.appId) appIds[row.appId] = true
      })
      let serverList = servers
      let appList = apps
      if (role === 'TL' || role === 'QA' || role === 'PM' || role === 'DEV') {
        serverList = servers.filter(row => serverIds[row.id])
      }
      if (role === 'TL' || role === 'QA' || role === 'PM') {
        appList = apps.filter(row => appIds[row.id])
      }
      const osCount = { centos: 0, ubuntu: 0 }
      const envCount = { DEV: 0, TEST: 0, PROD: 0 }
      const serverStatus = { RUNNING: 0, STOPPED: 0, MAINTENANCE: 0 }
      serverList.forEach(row => {
        const os = String(row.os || '').toLowerCase()
        if (os.includes('ubuntu')) osCount.ubuntu += 1
        else if (os.includes('centos')) osCount.centos += 1
        if (envCount[row.env] != null) envCount[row.env] += 1
        if (serverStatus[row.status] != null) serverStatus[row.status] += 1
      })
      this.osCount = osCount
      this.envCount = envCount
      this.serverStatus = serverStatus
      const projectIds = {}
      appList.forEach(row => {
        if (row.projectId) projectIds[row.projectId] = true
      })
      const projectCount = role === 'DEV' ? projects.length : Object.keys(projectIds).length
      const onlineAppCount = appList.filter(row => row.status === 'ONLINE').length
      const alarmCountMap = { UNHANDLED: 0, HANDLING: 0, RESOLVED: 0 }
      ;(alarms || []).forEach(row => {
        if (alarmCountMap[row.status] != null) alarmCountMap[row.status] += 1
      })
      this.alarmBars = ALARM_STATUS.map(item => ({ ...item, count: alarmCountMap[item.value] || 0 }))
      this.alarmResolvedCount = alarmCountMap.RESOLVED
      this.alarmTotalCount = this.alarmBars.reduce((sum, item) => sum + item.count, 0)
      this.alarmResolveRate = this.alarmTotalCount
        ? Math.round(this.alarmResolvedCount * 100 / this.alarmTotalCount) : 0
      this.count = Object.assign({}, this.count, {
        projectCount: projectCount,
        serverCount: serverList.length,
        appCount: appList.length,
        onlineAppCount: onlineAppCount,
        unhandledAlarmCount: alarmCountMap.UNHANDLED
      })
      const byApp = {}
      deploys.forEach(row => {
        if (!row.appId) return
        if (!byApp[row.appId]) {
          byApp[row.appId] = { id: row.appId, name: row.appName || ('应用' + row.appId), deployCount: 0, failedDeployCount: 0, alarmCount: 0, openAlarmCount: 0, alarmAfterSuccess: 0, criticalOpenCount: 0 }
        }
        byApp[row.appId].deployCount += 1
        if (row.status === 'FAILED') byApp[row.appId].failedDeployCount += 1
      })
      this.appRisk = Object.keys(byApp).map(key => byApp[key])
    },
    loadCount() {
      this.$request.get('/statistics/count').then(res => {
        this.count = res.data || {}
      })
    },
    loadDeployStatus() {
      this.$request.get('/statistics/deployStatus').then(res => {
        const countMap = {}
        ;(res.data || []).forEach(v => { countMap[v.status] = this.num(v.count) })
        this.deployBars = DEPLOY_STATUS.map(v => ({ ...v, count: countMap[v.value] || 0 }))
        this.deploySuccessCount = countMap.SUCCESS || 0
        this.deployFinishedCount = this.deploySuccessCount + (countMap.FAILED || 0)
        this.deploySuccessRate = this.deployFinishedCount ? Math.round(this.deploySuccessCount * 100 / this.deployFinishedCount) : 0
      })
    },
    loadAlarmStatus() {
      this.$request.get('/statistics/alarmStatus').then(res => {
        const countMap = {}
        ;(res.data || []).forEach(v => { countMap[v.status] = this.num(v.count) })
        this.alarmBars = ALARM_STATUS.map(v => ({ ...v, count: countMap[v.value] || 0 }))
        this.alarmResolvedCount = countMap.RESOLVED || 0
        this.alarmTotalCount = this.alarmBars.reduce((sum, v) => sum + v.count, 0)
        this.alarmResolveRate = this.alarmTotalCount ? Math.round(this.alarmResolvedCount * 100 / this.alarmTotalCount) : 0
      })
    },
    loadAppRisk() {
      this.$request.get('/statistics/appRisk').then(res => {
        this.appRisk = res.data || []
      })
    },
    resizeTrend() {
      if (this.trendChart) this.trendChart.resize()
    },
    loadTrend14() {
      this.$request.get('/statistics/deployTrend14').then(res => {
        this.renderTrend(res.data || [])
      })
    },
    renderTrend(rows) {
      this.$nextTick(() => {
        if (!this.$refs.trend14) return
        if (!this.trendChart) this.trendChart = echarts.init(this.$refs.trend14)
        this.trendChart.setOption({
          backgroundColor: 'transparent',
          grid: { left: 8, right: 12, top: 16, bottom: 4, containLabel: true },
          tooltip: { trigger: 'axis' },
          xAxis: {
            type: 'category',
            data: rows.map(v => String(v.date).slice(5)),
            axisLine: { lineStyle: { color: 'rgba(61, 231, 255, 0.25)' } },
            axisLabel: { color: '#7aa8b8', fontSize: 11 }
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            splitLine: { lineStyle: { color: 'rgba(61, 231, 255, 0.1)' } },
            axisLabel: { color: '#7aa8b8', fontSize: 11 }
          },
          series: [{
            type: 'line',
            data: rows.map(v => v.count),
            smooth: true,
            symbolSize: 6,
            itemStyle: { color: '#3de7ff' },
            lineStyle: { color: '#3de7ff', width: 2 },
            areaStyle: {
              color: {
                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: 'rgba(61, 231, 255, 0.35)' },
                  { offset: 1, color: 'rgba(61, 231, 255, 0.02)' }
                ]
              }
            }
          }]
        })
        this.trendChart.resize()
      })
    },
    loadServers() {
      // 开发无服务器管理页权限；资产统计改走下拉接口（未停机机）
      const asOps = canOps(this.user)
      const req = asOps
        ? this.$request.get('/server/selectPage', { params: { pageNum: 1, pageSize: 200 } })
        : this.$request.get('/server/selectAll')
      req.then(res => {
        const list = asOps ? (res.data && res.data.list) || [] : (res.data || [])
        const osCount = { centos: 0, ubuntu: 0 }
        const envCount = { DEV: 0, TEST: 0, PROD: 0 }
        const serverStatus = { RUNNING: 0, STOPPED: 0, MAINTENANCE: 0 }
        list.forEach(row => {
          const os = String(row.os || '').toLowerCase()
          if (os.includes('ubuntu')) osCount.ubuntu += 1
          else if (os.includes('centos')) osCount.centos += 1
          if (envCount[row.env] != null) envCount[row.env] += 1
          if (serverStatus[row.status] != null) serverStatus[row.status] += 1
        })
        this.osCount = osCount
        this.envCount = envCount
        this.serverStatus = serverStatus
      })
    },
    loadRecentDeploys() {
      this.$request.get('/deploy/selectPage', { params: { pageNum: 1, pageSize: 6 } }).then(res => {
        this.recentDeploys = res.data?.list || []
      })
    },
    noticeTone(row) {
      const text = String((row && row.title || '') + (row && row.content || ''))
      if (/告警|失败|严重|故障|中断/.test(text)) return 'danger'
      if (/审批|待处理|风险|注意|回滚/.test(text)) return 'warn'
      if (/成功|闭环|正常|就绪/.test(text)) return 'ok'
      return 'cyan'
    },
    loadNotices() {
      this.$request.get('/notice/selectAll').then(res => {
        this.notices = (res.data || []).slice(0, 3)
      }).catch(() => {
        this.notices = []
      })
    },
  }
}
</script>

<style scoped>
.ops {
  color: #c8eef8;
  min-height: calc(100vh - 88px);
}

.role-tip {
  margin: 0 0 10px;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid rgba(61, 231, 255, 0.28);
  background: rgba(12, 40, 66, 0.72);
  color: #9edfff;
  font-size: 13px;
}

.notice-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 8px 14px;
  background: linear-gradient(180deg, rgba(12, 40, 66, 0.88), rgba(6, 18, 30, 0.9));
  border: 1px solid rgba(61, 231, 255, 0.38);
  border-radius: 6px;
  box-shadow: inset 0 0 28px rgba(0, 190, 255, 0.08), 0 0 18px rgba(0, 180, 255, 0.16);
  font-size: 13px;
}

.notice-bar > i,
.notice-bar em {
  flex-shrink: 0;
}

.notice-bar em {
  font-style: normal;
  font-weight: 600;
  letter-spacing: 1px;
}

.notice-text {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notice-bar.is-danger,
.notice-bar.is-danger em,
.notice-bar.is-danger .notice-text { color: #ff6b81; }
.notice-bar.is-danger { border-color: rgba(255, 107, 129, 0.55); }

.notice-bar.is-warn,
.notice-bar.is-warn em,
.notice-bar.is-warn .notice-text { color: #ffd15c; }
.notice-bar.is-warn { border-color: rgba(255, 209, 92, 0.5); }

.notice-bar.is-ok,
.notice-bar.is-ok em,
.notice-bar.is-ok .notice-text { color: #5dff9a; }
.notice-bar.is-ok { border-color: rgba(93, 255, 154, 0.45); }

.notice-bar.is-cyan,
.notice-bar.is-cyan em,
.notice-bar.is-cyan .notice-text { color: #3de7ff; }
.notice-bar.is-cyan { border-color: rgba(61, 231, 255, 0.45); }

.notice-bar.clickable {
  cursor: pointer;
}

.ops-grid {
  display: grid;
  grid-template-columns: 1.05fr 1.5fr 1fr;
  grid-template-areas:
    "overview asset kpi"
    "trend trend trend"
    "deploy apps alarm";
  gap: 12px;
}

.p-overview { grid-area: overview; }
.p-asset { grid-area: asset; }
.p-kpi { grid-area: kpi; }
.p-trend { grid-area: trend; }
.p-deploy { grid-area: deploy; }
.p-apps { grid-area: apps; overflow-x: auto; }
.p-alarm { grid-area: alarm; }

.ops-panel {
  position: relative;
  min-width: 0;
  padding: 12px 14px 14px;
  background: linear-gradient(180deg, rgba(12, 40, 66, 0.88), rgba(6, 18, 30, 0.9));
  border: 1px solid rgba(61, 231, 255, 0.38);
  border-radius: 6px;
  box-shadow: inset 0 0 28px rgba(0, 190, 255, 0.08), 0 0 18px rgba(0, 180, 255, 0.16);
}

.ops-panel h3 {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: #7af0ff;
  letter-spacing: 1px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.panel-head h3 {
  margin: 0;
}

.panel-more {
  flex-shrink: 0;
  color: #7af0ff;
  font-size: 12px;
  text-decoration: none;
  opacity: 0.85;
}

.panel-more:hover {
  opacity: 1;
  text-decoration: underline;
}

.ops-panel h3 i,
.ov span i,
.kpi-line span i,
.rank span i,
.ops-table i,
.env-chip i,
.risk i {
  margin-right: 5px;
}

.asset-item > i,
.asset-item >>> .os-logo {
  display: inline-flex;
  margin: 0 auto 4px;
  color: #7af0ff;
  font-size: 18px;
}

.ov-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.ov {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  min-width: 0;
  padding: 8px 10px;
  cursor: pointer;
  color: #8ec8e0;
  font-size: 12px;
  background: rgba(61, 231, 255, 0.05);
  border: 1px solid rgba(61, 231, 255, 0.16);
  border-radius: 4px;
}

.ov span {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ov b {
  color: #e8fbff;
  font-size: 20px;
  line-height: 1.2;
}

.trend-box {
  height: 168px;
  overflow: hidden;
}

.asset-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  text-align: center;
}

.asset-item {
  flex: 1;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.asset-item b {
  display: block;
  font-size: 22px;
  color: #7af0ff;
  text-shadow: 0 0 8px rgba(61, 231, 255, 0.35);
}

.asset-item span {
  color: #7aa8b8;
  font-size: 12px;
  white-space: nowrap;
}

.env-row {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.env-chip {
  flex: 1;
  text-align: center;
  padding: 6px 0;
  border: 1px solid rgba(61, 231, 255, 0.2);
  color: #9be7ff;
  font-size: 12px;
  cursor: pointer;
}

.kpi-line {
  display: flex;
  justify-content: space-between;
  margin: 8px 0 4px;
  color: #8ec8e0;
  font-size: 13px;
}

.kpi-line.clickable {
  cursor: pointer;
}

.kpi-line em {
  font-style: normal;
  color: #e8fbff;
  font-weight: 700;
}

.rank {
  display: grid;
  grid-template-columns: 86px 1fr 28px;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #9ad6ea;
  font-size: 12px;
}

.rank span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank.clickable {
  cursor: pointer;
}

.rank .bar {
  height: 8px;
  background: rgba(61, 231, 255, 0.08);
  border: 1px solid rgba(61, 231, 255, 0.12);
  overflow: hidden;
}

.rank .bar i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #1677a8, #3de7ff);
  box-shadow: 0 0 8px rgba(61, 231, 255, 0.65);
}

.rank .bar i.ok { background: linear-gradient(90deg, #1f8f4e, #5dff9a); box-shadow: 0 0 8px rgba(93, 255, 154, 0.5); }
.rank .bar i.warn { background: linear-gradient(90deg, #9a6b12, #ffd15c); box-shadow: 0 0 8px rgba(255, 209, 92, 0.5); }
.rank .bar i.danger { background: linear-gradient(90deg, #9a2433, #ff6b81); box-shadow: 0 0 8px rgba(255, 107, 129, 0.5); }
.rank .bar i.cyan { background: linear-gradient(90deg, #1677a8, #3de7ff); }

.ops-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.ops-table th,
.ops-table td {
  padding: 7px 6px;
  text-align: left;
  border-bottom: 1px solid rgba(61, 231, 255, 0.1);
  white-space: nowrap;
}

.ops-table th {
  color: #7af0ff;
  font-weight: 600;
}

.ops-table tbody tr:hover {
  background: rgba(61, 231, 255, 0.06);
}

.ops-table td.clickable {
  cursor: pointer;
}

.risk {
  color: #9ad6ea;
}

.risk.ok { color: #5dff9a; }
.risk.warn { color: #ffd15c; }
.risk.danger { color: #ff6b81; }
.risk.cyan { color: #3de7ff; }

.tone-ok { color: #5dff9a !important; }
.tone-warn { color: #ffd15c !important; }
.tone-danger { color: #ff6b81 !important; }
.tone-cyan { color: #3de7ff !important; }

.empty {
  text-align: center;
  color: #5b7f90;
  padding: 16px 0;
}

@media (max-width: 1280px) {
  .ops-grid {
    grid-template-columns: 1fr 1fr;
    grid-template-areas:
      "overview asset"
      "kpi deploy"
      "trend trend"
      "apps alarm";
  }
}
</style>
