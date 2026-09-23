<template>
  <div>
    <div class="relation-bar" v-if="params.id || params.appId || params.serverId || params.status || finished">
      <i class="el-icon-link"></i>
      <span>{{ deployHint }}</span>
      <span class="link-text" style="margin-left: 12px" @click="reset">查看全部</span>
    </div>
    <div class="toolbar">
      <div class="search-fields">
        <el-input prefix-icon="el-icon-tickets" placeholder="请输入部署单号查询" style="width: 200px" v-model="params.deployNo" clearable></el-input>
        <el-select placeholder="请选择状态" style="width: 140px" v-model="params.status" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
        </el-select>
        <el-input prefix-icon="el-icon-user" placeholder="请输入申请人查询" style="width: 160px" v-model="params.applicant" clearable></el-input>
        <el-date-picker v-model="dateRange" type="daterange" style="width: 250px"
                        range-separator="至" start-placeholder="申请开始日期" end-placeholder="申请结束日期"
                        value-format="yyyy-MM-dd"></el-date-picker>
        <el-button type="info" plain @click="load(1)" icon="el-icon-search">查询</el-button>
        <el-button type="warning" plain @click="reset" icon="el-icon-refresh-left">重置</el-button>
      </div>
      <div class="toolbar-actions" v-if="canDev(user)">
        <el-button type="primary" plain @click="handleApply" icon="el-icon-s-promotion">发起部署申请</el-button>
      </div>
    </div>

    <div class="table">
      <el-table :data="tableData" stripe size="small">
        <el-table-column label="部署单号" width="190">
          <template slot="header"><span class="th-label"><i class="el-icon-tickets"></i>部署单号</span></template>
          <template v-slot="scope">
            <span class="cell-text cell-code link-text" @click="goAlarms(scope.row)"><i class="el-icon-tickets"></i>{{ scope.row.deployNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="应用名" min-width="140" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-s-platform"></i>应用名</span></template>
          <template v-slot="scope">
            <span v-if="scope.row.appId" class="cell-text link-text" @click="goApp(scope.row)"><i class="el-icon-s-platform"></i><span class="txt">{{ scope.row.appName }}</span></span>
            <span v-else class="cell-text"><i class="el-icon-s-platform"></i>{{ scope.row.appName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="目标服务器" width="150">
          <template slot="header"><span class="th-label"><i class="el-icon-monitor"></i>目标服务器</span></template>
          <template v-slot="scope">
            <span v-if="scope.row.serverId" class="cell-text link-text" @click="goServer(scope.row)"><i class="el-icon-monitor"></i>{{ scope.row.serverName }}</span>
            <span v-else class="cell-text"><i class="el-icon-monitor"></i>{{ scope.row.serverName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="目标版本" width="120">
          <template slot="header"><span class="th-label"><i class="el-icon-price-tag"></i>目标版本</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-price-tag"></i>{{ scope.row.version || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="部署类型" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-s-operation"></i>部署类型</span></template>
          <template v-slot="scope">
            <el-tag :type="scope.row.deployType === 'ROLLBACK' ? 'warning' : 'info'">
              <i :class="scope.row.deployType === 'ROLLBACK' ? 'el-icon-refresh-left' : 'el-icon-upload2'"></i>
              {{ typeNames[scope.row.deployType] || scope.row.deployType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-flag"></i>状态</span></template>
          <template v-slot="scope">
            <el-tag :type="statusTag[scope.row.status]"><i :class="statusIcon[scope.row.status]"></i>{{ statusNames[scope.row.status] || scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请人" width="160">
          <template slot="header"><span class="th-label"><i class="el-icon-user"></i>申请人</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-user"></i>{{ personName(scope.row.applicant) || '-' }}
              <el-tag v-if="personRoleName(scope.row.applicant, scope.row.applicantRole)" size="mini" :type="roleTag(scope.row.applicant, scope.row.applicantRole)">{{ personRoleName(scope.row.applicant, scope.row.applicantRole) }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="审批人" width="160">
          <template slot="header"><span class="th-label"><i class="el-icon-s-custom"></i>审批人</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-s-custom"></i>{{ personName(scope.row.approver) || '-' }}
              <el-tag v-if="personRoleName(scope.row.approver, scope.row.approverRole)" size="mini" :type="roleTag(scope.row.approver, scope.row.approverRole)">{{ personRoleName(scope.row.approver, scope.row.approverRole) }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template slot="header"><span class="th-label"><i class="el-icon-time"></i>申请时间</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-time"></i>{{ scope.row.applyTime || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="完成时间" width="170">
          <template slot="header"><span class="th-label"><i class="el-icon-time"></i>完成时间</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-time"></i>{{ scope.row.finishTime || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="日志" align="center" width="150">
          <template slot="header"><span class="th-label"><i class="el-icon-document"></i>日志</span></template>
          <template v-slot="scope">
            <el-button type="text" class="op-primary" @click="showLog(scope.row)">查看</el-button>
            <el-button v-if="canExecLog(scope.row)" type="text" class="op-primary" @click="showExecLog(scope.row)">执行日志</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="160">
          <template v-slot="scope">
            <div class="op-btns">
              <template v-if="scope.row.status === 'PENDING' && canOps(user)">
                <el-button type="text" class="op-ok" @click="handleApprove(scope.row, 'APPROVED')">批准</el-button>
                <el-button type="text" class="op-danger" @click="handleApprove(scope.row, 'REJECTED')">驳回</el-button>
              </template>
              <el-button v-if="scope.row.status === 'APPROVED' && canOps(user)"
                         type="text" class="op-primary" @click="startDeploy(scope.row.id)">开始</el-button>
              <template v-if="scope.row.status === 'DEPLOYING' && canOps(user)">
                <el-button type="text" class="op-ok" @click="finishDeploy(scope.row.id, 'SUCCESS')">成功</el-button>
                <el-button type="text" class="op-danger" @click="finishDeploy(scope.row.id, 'FAILED')">失败</el-button>
              </template>
              <el-button v-if="scope.row.status === 'SUCCESS' && canOps(user)"
                         type="text" class="op-warn" @click="rollback(scope.row.id)">回滚</el-button>
              <template v-if="canEditDeploy(scope.row)">
                <el-button type="text" class="op-primary" @click="handleEdit(scope.row)">编辑</el-button>
                <el-button type="text" class="op-danger" @click="del(scope.row.id)">删除</el-button>
              </template>
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

    <!--  发起申请 / 编辑  -->
    <el-dialog title="部署申请" :visible.sync="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="form" label-width="100px" style="padding-right: 50px" :rules="rules" ref="formRef">
        <el-form-item label="应用" prop="appId">
          <el-select v-model="form.appId" placeholder="请选择应用" style="width: 100%" @change="handleAppChange">
            <el-option v-for="item in appList" :key="item.id" :label="item.name + '（' + item.code + '）'" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="目标服务器" prop="serverId">
          <el-select v-model="form.serverId" placeholder="请选择目标服务器" style="width: 100%">
            <el-option v-for="item in serverList" :key="item.id"
                       :label="item.hostname + '（' + (envNames[item.env] || item.env) + '）'" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="目标版本" prop="version">
          <el-input v-model="form.version" placeholder="如 v1.0.1"></el-input>
        </el-form-item>
        <el-form-item label="制品" v-if="!form.id">
          <el-upload :action="uploadAction" :headers="uploadHeaders" :show-file-list="false" :on-success="onArtifactUploaded">
            <el-button size="small" icon="el-icon-upload2">上传制品</el-button>
          </el-upload>
          <span v-if="artifactName" style="margin-left: 8px">{{ artifactName }}</span>
        </el-form-item>
        <el-form-item label="部署类型" prop="deployType">
          <el-select v-model="form.deployType" placeholder="请选择部署类型" style="width: 100%">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="申请原因" prop="reason">
          <el-input type="textarea" :rows="4" v-model="form.reason" placeholder="申请原因"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="fromVisible = false" icon="el-icon-close">取 消</el-button>
        <el-button type="primary" @click="save" icon="el-icon-check">确 定</el-button>
      </div>
    </el-dialog>

    <!--  审批  -->
    <el-dialog :title="approveForm.result === 'APPROVED' ? '批准部署' : '驳回部署'" :visible.sync="approveVisible"
               width="35%" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" style="padding-right: 50px">
        <el-form-item label="部署单号">
          <span>{{ approveForm.deployNo }}</span>
        </el-form-item>
        <el-form-item label="审批备注">
          <el-input type="textarea" :rows="4" v-model="approveForm.remark" placeholder="审批备注"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="approveVisible = false" icon="el-icon-close">取 消</el-button>
        <el-button type="primary" @click="submitApprove" icon="el-icon-check">确 定</el-button>
      </div>
    </el-dialog>

    <el-drawer title="执行日志" :visible.sync="execVisible" size="50%" @close="closeExecLog">
      <div style="padding: 0 16px 16px">
        <p v-if="execSummary">共 {{ execSummary.totalLines }} 行，状态 {{ execSummary.status }}</p>
        <pre style="white-space: pre-wrap; background: #0f1720; color: #d6e2ee; padding: 12px; min-height: 240px">{{ execText }}</pre>
        <div v-if="!execText">暂无执行日志</div>
      </div>
    </el-drawer>

    <!--  流转记录  -->
    <el-dialog title="部署日志" :visible.sync="logVisible" width="60%" destroy-on-close>
      <el-table :data="logData" stripe>
        <el-table-column prop="id" label="序号" width="70" align="center"></el-table-column>
        <el-table-column label="动作" width="110" align="center">
          <template v-slot="scope">
            <el-tag :type="actionTag[scope.row.action]">{{ actionNames[scope.row.action] || scope.row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="150">
          <template v-slot="scope">
            {{ personName(scope.row.operator) || '-' }}
            <el-tag v-if="personRoleName(scope.row.operator)" size="mini" :type="roleTag(scope.row.operator)">{{ personRoleName(scope.row.operator) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="操作说明" show-overflow-tooltip></el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="160"></el-table-column>
      </el-table>
    </el-dialog>

  </div>
</template>

<script>
import { personName, personRoleName, roleTag, canDev, canOps, isAdmin } from '@/utils/person'

export default {
  name: "Deploy",
  data() {
    return {
      tableData: [],  // 所有的数据
      pageNum: 1,   // 当前的页码
      pageSize: 20,  // 每页显示的个数
      total: 0,
      params: { id: null, deployNo: null, status: null, applicant: null, appId: null, serverId: null },   // 搜索条件
      finished: false,
      dateRange: [],   // 申请时间范围
      fromVisible: false,
      form: {},
      approveVisible: false,
      approveForm: {},
      logVisible: false,
      logData: [],
      execVisible: false,
      execText: '',
      execSummary: null,
      execTimer: null,
      execRow: null,
      execCursor: 0,
      artifactName: '',
      artifactUrl: '',
      appList: [],      // 应用下拉数据源
      serverList: [],   // 服务器下拉数据源
      user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
      statusOptions: [
        { value: 'PENDING', label: '待审批' },
        { value: 'APPROVED', label: '已批准' },
        { value: 'REJECTED', label: '已驳回' },
        { value: 'DEPLOYING', label: '部署中' },
        { value: 'SUCCESS', label: '成功' },
        { value: 'FAILED', label: '失败' },
      ],
      typeOptions: [
        { value: 'FULL', label: '全量' },
        { value: 'INCREMENT', label: '增量' },
        { value: 'ROLLBACK', label: '回滚' },
      ],
      statusNames: {
        PENDING: '待审批', APPROVED: '已批准', REJECTED: '已驳回',
        DEPLOYING: '部署中', SUCCESS: '成功', FAILED: '失败'
      },
      statusTag: {
        PENDING: 'warning', APPROVED: 'primary', DEPLOYING: 'primary',
        REJECTED: 'danger', FAILED: 'danger', SUCCESS: 'success'
      },
      statusIcon: {
        PENDING: 'el-icon-time', APPROVED: 'el-icon-check', DEPLOYING: 'el-icon-loading',
        REJECTED: 'el-icon-close', FAILED: 'el-icon-circle-close', SUCCESS: 'el-icon-circle-check'
      },
      typeNames: { FULL: '全量', INCREMENT: '增量', ROLLBACK: '回滚' },
      actionNames: {
        APPLY: '提交申请', APPROVE: '审批通过', REJECT: '审批驳回',
        START_DEPLOY: '开始部署', FINISH: '部署成功', FAIL: '部署失败', ROLLBACK: '回滚'
      },
      actionTag: {
        APPLY: 'info', APPROVE: 'success', REJECT: 'danger',
        START_DEPLOY: 'primary', FINISH: 'success', FAIL: 'danger', ROLLBACK: 'warning'
      },
      envNames: { DEV: '开发环境', TEST: '测试环境', PROD: '生产环境' },
      rules: {
        appId: [
          {required: true, message: '请选择应用', trigger: 'change'},
        ],
        version: [
          {required: true, message: '请输入目标版本', trigger: 'blur'},
        ]
      },
    }
  },
  computed: {
    uploadAction() {
      const base = process.env.VUE_APP_API_BASE_URL || '/api'
      return base + '/files/upload'
    },
    uploadHeaders() {
      const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
      return { token: user.token || '' }
    },
    deployHint() {
      if (this.params.id) return '当前按关联部署单定位'
      if (this.params.appId) return '当前查看该应用的部署单'
      if (this.params.serverId) return '当前查看该服务器的部署单'
      if (this.finished) return '当前查看已完结部署（已交付 + 发布失败）'
      if (this.params.status) return '当前查看' + (this.statusNames[this.params.status] || this.params.status) + '的部署单'
      return ''
    }
  },
  created() {
    this.applyQuery()
    this.loadApp()
    this.loadServer()
    this.load(1)
  },
  beforeDestroy() {
    this.closeExecTimer()
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
    canDev,
    canOps,
    canEditDeploy(row) {
      if (row.status !== 'PENDING') return false
      if (isAdmin(this.user)) return true
      return this.user.role === 'DEV' && row.applicant === this.user.username
    },
    applyQuery() {
      const q = this.$route.query
      this.params.id = q.id ? Number(q.id) : null
      this.params.appId = q.appId ? Number(q.appId) : null
      this.params.serverId = q.serverId ? Number(q.serverId) : null
      this.finished = q.finished === '1'
      this.params.status = this.finished ? null : (q.status || null)
    },
    goApp(row) {
      this.$router.push({ path: '/app', query: { id: row.appId } })
    },
    goServer(row) {
      this.$router.push({ path: '/server', query: { id: row.serverId } })
    },
    goAlarms(row) {
      this.$router.push(row.appId ? { path: '/alarm', query: { appId: row.appId } } : { path: '/alarm' })
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
    handleAppChange(appId) {   // 选择应用后默认带出该应用当前部署的服务器
      const app = this.appList.find(v => v.id === appId)
      if (app) {
        this.form.serverId = app.serverId
      }
    },
    handleApply() {   // 发起部署申请
      this.form = { deployType: 'FULL' }
      this.artifactName = ''
      this.artifactUrl = ''
      this.fromVisible = true
    },
    onArtifactUploaded(res, file) {
      if (res && res.code === '200') {
        this.artifactUrl = res.data
        this.artifactName = file.name
        this.$message.success('制品已上传，保存申请时会登记到当前版本')
      } else {
        this.$message.error((res && res.msg) || '上传失败')
      }
    },
    registerArtifact() {
      if (!this.artifactUrl || !this.form.appId || !this.form.version) {
        return
      }
      this.$request.post('/deployArtifact/register', {
        appId: this.form.appId,
        version: this.form.version,
        fileName: this.artifactName,
        filePath: this.artifactUrl
      }).then(res => {
        if (res.code !== '200') {
          this.$message.error(res.msg || '制品登记失败')
        }
      })
    },
    handleEdit(row) {   // 编辑数据
      this.form = JSON.parse(JSON.stringify(row))  // 给form对象赋值  注意要深拷贝数据
      this.fromVisible = true   // 打开弹窗
    },
    save() {   // 保存按钮触发的逻辑  它会触发新增或者更新
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          this.$request({
            url: this.form.id ? '/deploy/update' : '/deploy/add',
            method: this.form.id ? 'PUT' : 'POST',
            data: this.form
          }).then(res => {
            if (res.code === '200') {  // 表示成功保存
              this.registerArtifact()
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
    handleApprove(row, result) {   // 打开审批弹窗
      this.approveForm = { id: row.id, deployNo: row.deployNo, result: result, remark: null }
      this.approveVisible = true
    },
    submitApprove() {   // 提交审批结果
      this.$request({
        url: '/deploy/approve/' + this.approveForm.id,
        method: 'PUT',
        params: { result: this.approveForm.result, remark: this.approveForm.remark }
      }).then(res => {
        if (res.code === '200') {
          this.$message.success('操作成功')
          this.load(this.pageNum)
          this.approveVisible = false
        } else {
          this.$message.error(res.msg)
        }
      })
    },
    startDeploy(id) {   // 开始部署
      this.$request.put('/deploy/startDeploy/' + id).then(res => {
        if (res.code === '200') {
          this.$message.success('操作成功')
          this.load(this.pageNum)
        } else {
          this.$message.error(res.msg)
        }
      })
    },
    finishDeploy(id, result) {   // 部署完成
      this.$request({
        url: '/deploy/finishDeploy/' + id,
        method: 'PUT',
        params: { result: result }
      }).then(res => {
        if (res.code === '200') {
          this.$message.success('操作成功')
          this.load(this.pageNum)
        } else {
          this.$message.error(res.msg)
        }
      })
    },
    rollback(id) {   // 回滚，需要二次确认
      this.$confirm('回滚后该部署单会重新进入部署中状态，您确定回滚吗？', '确认回滚', {type: "warning"}).then(response => {
        this.$request.put('/deploy/rollback/' + id).then(res => {
          if (res.code === '200') {
            this.$message.success('操作成功')
            this.load(this.pageNum)
          } else {
            this.$message.error(res.msg)
          }
        })
      }).catch(() => {
      })
    },
    canExecLog(row) {
      return row && (row.status === 'DEPLOYING' || row.status === 'SUCCESS' || row.status === 'FAILED')
    },
    showExecLog(row) {
      this.execRow = row
      this.execCursor = 0
      this.execText = ''
      this.execVisible = true
      this.pullExecLog()
      this.$request.get('/deploy/' + row.id + '/execSummary').then(res => {
        this.execSummary = res.data || null
      })
      if (row.status === 'DEPLOYING') {
        this.execTimer = setInterval(() => this.pullExecLog(), 2000)
      }
    },
    pullExecLog() {
      if (!this.execRow) return
      this.$request.get('/deploy/' + this.execRow.id + '/logs', {
        params: { cursor: this.execCursor }
      }).then(res => {
        const data = res.data || {}
        const lines = data.lines || []
        if (lines.length) {
          this.execText = this.execText ? (this.execText + '\n' + lines.join('\n')) : lines.join('\n')
        }
        this.execCursor = data.nextCursor || this.execCursor
        if (this.execRow.status === 'DEPLOYING') {
          this.$request.get('/deploy/selectById/' + this.execRow.id).then(detail => {
            if (detail.data && detail.data.status !== 'DEPLOYING') {
              this.execRow = detail.data
              this.closeExecTimer()
            }
          })
        }
      })
    },
    closeExecLog() {
      this.closeExecTimer()
      this.execRow = null
    },
    closeExecTimer() {
      if (this.execTimer) {
        clearInterval(this.execTimer)
        this.execTimer = null
      }
    },
    showLog(row) {   // 查看该部署单的流转记录
      this.$request.get('/deployLog/selectPage', {
        params: { deployId: row.id, pageNum: 1, pageSize: 100 }
      }).then(res => {
        this.logData = res.data?.list || []
        this.logVisible = true
      })
    },
    del(id) {   // 单个删除
      this.$confirm('您确定删除吗？', '确认删除', {type: "warning"}).then(response => {
        this.$request.delete('/deploy/delete/' + id).then(res => {
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
    load(pageNum) {  // 分页查询
      if (pageNum) this.pageNum = pageNum
      const params = {
        pageNum: this.pageNum,
        pageSize: this.pageSize,
        ...this.params,
        // 日期范围补全时分秒，避免结束当天的数据被漏掉
        startTime: this.dateRange && this.dateRange.length ? this.dateRange[0] + ' 00:00:00' : null,
        endTime: this.dateRange && this.dateRange.length ? this.dateRange[1] + ' 23:59:59' : null,
      }
      if (this.finished) {
        const query = { ...params, status: null }
        Promise.all([
          this.$request.get('/deploy/selectPage', { params: { ...query, status: 'SUCCESS' } }),
          this.$request.get('/deploy/selectPage', { params: { ...query, status: 'FAILED' } }),
        ]).then(([successRes, failedRes]) => {
          const successList = successRes.data?.list || []
          const failedList = failedRes.data?.list || []
          this.tableData = successList.concat(failedList)
          this.total = (successRes.data?.total || 0) + (failedRes.data?.total || 0)
        })
        return
      }
      this.$request.get('/deploy/selectPage', { params }).then(res => {
        this.tableData = res.data?.list
        this.total = res.data?.total
      })
    },
    reset() {
      this.params = { id: null, deployNo: null, status: null, applicant: null, appId: null, serverId: null }
      this.finished = false
      this.dateRange = []
      if (this.$route.query.id || this.$route.query.appId || this.$route.query.serverId || this.$route.query.status || this.$route.query.finished) {
        this.$router.replace({ path: '/deploy' })
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
