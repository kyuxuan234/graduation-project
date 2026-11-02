<template>
  <div>
    <div class="relation-bar" v-if="params.appId || params.serverId || params.status">
      <i class="el-icon-link"></i>
      <span>{{ alarmHint }}</span>
      <span class="link-text" style="margin-left: 12px" @click="reset">查看全部</span>
    </div>
    <div class="toolbar">
      <div class="search-fields">
        <el-input prefix-icon="el-icon-warning-outline" placeholder="请输入告警编号查询" style="width: 200px" v-model="params.alarmNo" clearable></el-input>
        <el-select placeholder="请选择级别" style="width: 130px" v-model="params.level" clearable>
          <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
        </el-select>
        <el-select placeholder="请选择状态" style="width: 130px" v-model="params.status" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" style="width: 250px"
                        range-separator="至" start-placeholder="告警开始日期" end-placeholder="告警结束日期"
                        value-format="yyyy-MM-dd"></el-date-picker>
        <el-button type="info" plain @click="load(1)" icon="el-icon-search">查询</el-button>
        <el-button type="warning" plain @click="reset" icon="el-icon-refresh-left">重置</el-button>
      </div>
      <div class="toolbar-actions" v-if="canOps(user)">
        <el-button type="primary" plain @click="handleAdd" icon="el-icon-bell">登记告警</el-button>
        <el-button type="danger" plain @click="delBatch" icon="el-icon-delete">批量删除</el-button>
      </div>
    </div>

    <div class="table">
      <el-table :data="tableData" stripe size="small" @selection-change="handleSelectionChange">
        <el-table-column v-if="canOps(user)" type="selection" width="48" align="center" :selectable="canDelete"></el-table-column>
        <el-table-column label="告警编号" width="190">
          <template slot="header"><span class="th-label"><i class="el-icon-warning-outline"></i>告警编号</span></template>
          <template v-slot="scope">
            <span class="cell-text cell-code"><i class="el-icon-warning-outline"></i>{{ scope.row.alarmNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="应用名" min-width="140" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-s-platform"></i>应用名</span></template>
          <template v-slot="scope">
            <span v-if="scope.row.appId" class="cell-text link-text" @click="goApp(scope.row)"><i class="el-icon-s-platform"></i><span class="txt">{{ scope.row.appName }}</span></span>
            <span v-else class="cell-text"><i class="el-icon-s-platform"></i>{{ scope.row.appName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="服务器" width="150">
          <template slot="header"><span class="th-label"><i class="el-icon-monitor"></i>服务器</span></template>
          <template v-slot="scope">
            <span v-if="scope.row.serverId" class="cell-text link-text" @click="goServer(scope.row)"><i class="el-icon-monitor"></i>{{ scope.row.serverName }}</span>
            <span v-else class="cell-text"><i class="el-icon-monitor"></i>{{ scope.row.serverName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-bell"></i>级别</span></template>
          <template v-slot="scope">
            <el-tag :type="levelTag[scope.row.level]"><i :class="levelIcon[scope.row.level]"></i>{{ levelNames[scope.row.level] || scope.row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="告警内容" min-width="160" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-chat-dot-square"></i>告警内容</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-chat-dot-square"></i><span class="txt">{{ scope.row.content }}</span></span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-flag"></i>状态</span></template>
          <template v-slot="scope">
            <el-tag :type="statusTag[scope.row.status]"><i :class="statusIcon[scope.row.status]"></i>{{ statusNames[scope.row.status] || scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理人" width="160">
          <template slot="header"><span class="th-label"><i class="el-icon-user"></i>处理人</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-user"></i>{{ personName(scope.row.handler) || '-' }}
              <el-tag v-if="personRoleName(scope.row.handler, scope.row.handlerRole)" size="mini" :type="roleTag(scope.row.handler, scope.row.handlerRole)">{{ personRoleName(scope.row.handler, scope.row.handlerRole) }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="接手时间" width="170">
          <template slot="header"><span class="th-label"><i class="el-icon-time"></i>接手时间</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-time"></i>{{ scope.row.handleTime || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="解决时间" width="170">
          <template slot="header"><span class="th-label"><i class="el-icon-time"></i>解决时间</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-time"></i>{{ scope.row.resolveTime || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="172" fixed="right">
          <template slot="header"><span class="th-label"><i class="el-icon-s-operation"></i>操作</span></template>
          <template v-slot="scope">
            <div class="op-btns">
              <el-button v-if="canHandle(scope.row)" type="warning" size="mini" icon="el-icon-user" title="接手" @click="handle(scope.row.id)">接手</el-button>
              <el-button v-if="canResolve(scope.row)" type="success" size="mini" icon="el-icon-circle-check" title="解决" @click="resolve(scope.row.id)">解决</el-button>
              <el-button v-if="canEdit(scope.row)" type="primary" size="mini" icon="el-icon-edit" title="编辑" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button v-else type="info" size="mini" icon="el-icon-view" title="查看" @click="handleView(scope.row)">查看</el-button>
              <el-button v-if="canDelete(scope.row)" type="danger" size="mini" icon="el-icon-delete" title="删除" @click="del(scope.row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
            background
            @current-change="handleCurrentChange"
            :current-page="pageNum"
            :page-sizes="[5, 10, 20]"
            :page-size="pageSize"
            layout="total, prev, pager, next"
            :total="total">
        </el-pagination>
      </div>
    </div>

    <el-dialog :title="formReadonly ? '查看告警' : '告警信息'" :visible.sync="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="form" label-width="100px" style="padding-right: 50px" :rules="rules" ref="formRef" :disabled="formReadonly">
        <el-form-item label="应用" prop="appId">
          <el-select v-model="form.appId" placeholder="请选择应用" style="width: 100%" @change="handleAppChange">
            <el-option v-for="item in appList" :key="item.id" :label="item.name + '（' + item.code + '）'" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="服务器" prop="serverId">
          <el-select v-model="form.serverId" placeholder="请选择服务器" style="width: 100%">
            <el-option v-for="item in serverList" :key="item.id"
                       :label="item.hostname + '（' + (envNames[item.env] || item.env) + '）'" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-select v-model="form.level" placeholder="请选择级别" style="width: 100%">
            <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="告警内容" prop="content">
          <el-input type="textarea" :rows="4" v-model="form.content" placeholder="告警内容"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="fromVisible = false" icon="el-icon-close">{{ formReadonly ? '关 闭' : '取 消' }}</el-button>
        <el-button v-if="!formReadonly" type="primary" @click="save" icon="el-icon-check">确 定</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { personName, personRoleName, roleTag, canOps, isAdmin } from '@/utils/person'

export default {
  name: "Alarm",
  data() {
    return {
      user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
      tableData: [],  // 所有的数据
      ids: [],
      pageNum: 1,   // 当前的页码
      pageSize: 20,  // 每页显示的个数
      total: 0,
      params: { alarmNo: null, level: null, status: null, appId: null, serverId: null },   // 搜索条件
      dateRange: [],   // 告警时间范围
      fromVisible: false,
      formReadonly: false,
      form: {},
      appList: [],      // 应用下拉数据源
      serverList: [],   // 服务器下拉数据源
      levelOptions: [
        { value: 'INFO', label: '提示' },
        { value: 'WARNING', label: '警告' },
        { value: 'CRITICAL', label: '严重' },
      ],
      statusOptions: [
        { value: 'UNHANDLED', label: '未处理' },
        { value: 'HANDLING', label: '处理中' },
        { value: 'RESOLVED', label: '已解决' },
      ],
      levelNames: { INFO: '提示', WARNING: '警告', CRITICAL: '严重' },
      levelTag: { INFO: 'info', WARNING: 'warning', CRITICAL: 'danger' },
      levelIcon: { INFO: 'el-icon-info', WARNING: 'el-icon-warning', CRITICAL: 'el-icon-error' },
      statusNames: { UNHANDLED: '未处理', HANDLING: '处理中', RESOLVED: '已解决' },
      statusTag: { UNHANDLED: 'danger', HANDLING: 'warning', RESOLVED: 'success' },
      statusIcon: { UNHANDLED: 'el-icon-bell', HANDLING: 'el-icon-loading', RESOLVED: 'el-icon-circle-check' },
      envNames: { DEV: '开发环境', TEST: '测试环境', PROD: '生产环境' },
      rules: {
        level: [
          {required: true, message: '请选择级别', trigger: 'change'},
        ],
        content: [
          {required: true, message: '请输入告警内容', trigger: 'blur'},
        ],
        appId: [
          {validator: (rule, value, callback) => this.requireAsset(callback), trigger: 'change'},
        ],
        serverId: [
          {validator: (rule, value, callback) => this.requireAsset(callback), trigger: 'change'},
        ]
      },
    }
  },
  computed: {
    alarmHint() {
      if (this.params.appId) return '当前查看该应用的告警'
      if (this.params.serverId) return '当前查看该服务器的告警'
      if (this.params.status) return '当前查看' + (this.statusNames[this.params.status] || this.params.status) + '的告警'
      return ''
    }
  },
  created() {
    this.applyQuery()
    this.loadApp()
    this.loadServer()
    this.load(1)
  },
    watch: {
    '$route.fullPath'() {
      this.applyQuery()
      this.load(1)
    }
  },
  methods: {
    personName,
    personRoleName,
    roleTag,
    canOps,
    canHandle(row) {
      return canOps(this.user) && row.status === 'UNHANDLED'
    },
    canResolve(row) {
      if (!canOps(this.user) || row.status !== 'HANDLING') return false
      return isAdmin(this.user) || row.handler === this.user.username
    },
    canEdit(row) {
      return canOps(this.user) && row.status === 'UNHANDLED'
    },
    canDelete(row) {
      return canOps(this.user) && row.status === 'UNHANDLED'
    },
    applyQuery() {
      const q = this.$route.query
      this.params.appId = q.appId ? Number(q.appId) : null
      this.params.serverId = q.serverId ? Number(q.serverId) : null
      this.params.status = q.status || null
    },
    goApp(row) {
      this.$router.push({ path: '/app', query: { id: row.appId } })
    },
    goServer(row) {
      if (!canOps(this.user)) {
        this.$message.info('服务器由运维管理')
        return
      }
      this.$router.push({ path: '/server', query: { id: row.serverId } })
    },
    loadApp() {   // 预加载应用下拉数据源
      this.$request.get('/app/selectAll').then(res => {
        this.appList = res.data || []
      })
    },
    loadServer() {   // 预加载服务器下拉数据源
      this.$request.get('/server/selectAll').then(res => {
        this.serverList = res.data || []
      })
    },
    requireAsset(callback) {
      if (!this.form.appId && !this.form.serverId) {
        callback(new Error('请选择应用或部署环境'))
        return
      }
      callback()
    },
    handleAppChange() {
    },
    handleAdd() {   // 登记告警
      this.formReadonly = false
      this.form = { level: 'WARNING', status: 'UNHANDLED' }
      this.fromVisible = true
    },
    handleEdit(row) {   // 编辑数据
      this.formReadonly = false
      this.form = JSON.parse(JSON.stringify(row))  // 给form对象赋值  注意要深拷贝数据
      this.fromVisible = true   // 打开弹窗
    },
    handleView(row) {   // 处理中/已解决或无权限：只读查看
      this.formReadonly = true
      this.form = JSON.parse(JSON.stringify(row))
      this.fromVisible = true
    },
    save() {   // 保存按钮触发的逻辑  它会触发新增或者更新
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          this.$request({
            url: this.form.id ? '/alarm/update' : '/alarm/add',
            method: this.form.id ? 'PUT' : 'POST',
            data: this.form
          }).then(res => {
            if (res.code === '200') {  // 表示成功保存
              this.$message.success('保存成功')
              this.load(1)
              this.fromVisible = false
            } else {
              this.$message.error(res.msg)  // 弹出错误的信息
            }
          })
        }
      })
    },
    handle(id) {   // 接手处理
      this.$request.put('/alarm/handle/' + id).then(res => {
        if (res.code === '200') {
          this.$message.success('操作成功')
          this.load(this.pageNum)
        } else {
          this.$message.error(res.msg)
        }
      })
    },
    resolve(id) {   // 标记解决
      this.$request.put('/alarm/resolve/' + id).then(res => {
        if (res.code === '200') {
          this.$message.success('操作成功')
          this.load(this.pageNum)
        } else {
          this.$message.error(res.msg)
        }
      })
    },
    del(id) {   // 单个删除
      this.$confirm('您确定删除吗？', '确认删除', {type: "warning"}).then(response => {
        this.$request.delete('/alarm/delete/' + id).then(res => {
          if (res.code === '200') {   // 表示操作成功
            this.$message.success('操作成功')
            this.load(1)
          } else {
            this.$message.error(res.msg)  // 弹出错误的信息
          }
        })
      }).catch(() => {
      })
    },
    handleSelectionChange(rows) {
      this.ids = rows.map(v => v.id)
    },
    delBatch() {
      if (!this.ids.length) {
        this.$message.warning('请选择数据')
        return
      }
      this.$confirm('您确定批量删除这些数据吗？', '确认删除', {type: "warning"}).then(response => {
        this.$request.delete('/alarm/delete/batch', {data: this.ids}).then(res => {
          if (res.code === '200') {
            this.$message.success('操作成功')
            this.load(1)
          } else {
            this.$message.error(res.msg)
          }
        })
      }).catch(() => {
      })
    },
    load(pageNum) {  // 分页查询
      if (pageNum) this.pageNum = pageNum
      this.$request.get('/alarm/selectPage', {
        params: {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          ...this.params,
          // 日期范围补全时分秒，避免结束当天的数据被漏掉
          startTime: this.dateRange && this.dateRange.length ? this.dateRange[0] + ' 00:00:00' : null,
          endTime: this.dateRange && this.dateRange.length ? this.dateRange[1] + ' 23:59:59' : null,
        }
      }).then(res => {
        this.tableData = res.data?.list
        this.total = res.data?.total
      })
    },
    reset() {
      this.params = { alarmNo: null, level: null, status: null, appId: null, serverId: null }
      this.dateRange = []
      if (this.$route.query.appId || this.$route.query.serverId || this.$route.query.status) {
        this.$router.replace({ path: '/alarm' })
      }
      this.load(1)
    },
    handleCurrentChange(pageNum) {
      this.load(pageNum)
    },
  }
}
</script>

<style scoped>
</style>
