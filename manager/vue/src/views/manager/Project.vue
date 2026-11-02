<template>
  <div>
    <div class="relation-bar" v-if="params.id">
      <i class="el-icon-link"></i>
      <span>当前按关联项目定位</span>
      <span class="link-text" style="margin-left: 12px" @click="reset">查看全部</span>
    </div>
    <div class="toolbar">
      <div class="search-fields">
        <el-input prefix-icon="el-icon-search" placeholder="请输入项目名查询" style="width: 200px" v-model="params.name" clearable></el-input>
        <el-select placeholder="请选择状态" style="width: 150px" v-model="params.status" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
        </el-select>
        <el-input prefix-icon="el-icon-user" placeholder="请输入开发负责人查询" style="width: 220px" v-model="params.leader" clearable></el-input>
        <el-button type="info" plain @click="load(1)" icon="el-icon-search">查询</el-button>
        <el-button type="warning" plain @click="reset" icon="el-icon-refresh-left">重置</el-button>
      </div>
      <div class="toolbar-actions">
        <el-button v-if="canMaintain" type="primary" plain @click="handleAdd" icon="el-icon-plus">新增项目</el-button>
        <el-button v-if="canMaintain" type="danger" plain @click="delBatch" icon="el-icon-delete">批量删除</el-button>
      </div>
    </div>

    <div class="table">
      <el-table :data="tableData" stripe size="small" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" align="center" :selectable="canDelete"></el-table-column>
        <el-table-column label="项目编号" width="140">
          <template slot="header"><span class="th-label"><i class="el-icon-postcard"></i>项目编号</span></template>
          <template v-slot="scope">
            <span class="cell-text cell-code" :class="{ 'link-text': canOpenApps }" @click="canOpenApps && goDown(scope.row)"><i class="el-icon-postcard"></i>{{ scope.row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column label="项目名" min-width="150" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-folder"></i>项目名</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-folder-opened"></i><span class="txt">{{ scope.row.name }}</span></span>
          </template>
        </el-table-column>
        <el-table-column label="技术栈" min-width="140" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-cpu"></i>技术栈</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-cpu"></i><span class="txt">{{ scope.row.techStack || '-' }}</span></span>
          </template>
        </el-table-column>
        <el-table-column label="开发负责人" width="180">
          <template slot="header"><span class="th-label"><i class="el-icon-user"></i>开发负责人</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-user"></i>{{ personName(scope.row.leader) || '未分配' }}
              <el-tag v-if="personRoleName(scope.row.leader, scope.row.leaderRole)" size="mini" :type="roleTag(scope.row.leader, scope.row.leaderRole)">{{ personRoleName(scope.row.leader, scope.row.leaderRole) }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="关联应用" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-connection"></i>关联应用</span></template>
          <template v-slot="scope">
            <span class="cell-text" :class="{ 'link-text': canOpenApps }" @click="canOpenApps && goApps(scope.row)"><i class="el-icon-s-platform"></i>{{ scope.row.appCount || 0 }} 个</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-flag"></i>状态</span></template>
          <template v-slot="scope">
            <el-tag :type="statusTag[scope.row.status]"><i :class="statusIcon[scope.row.status]"></i>{{ statusNames[scope.row.status] || scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始日期" width="130">
          <template slot="header"><span class="th-label"><i class="el-icon-date"></i>开始日期</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-date"></i>{{ scope.row.startDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结束日期" width="130">
          <template slot="header"><span class="th-label"><i class="el-icon-date"></i>结束日期</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-date"></i>{{ scope.row.endDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="140" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-document"></i>描述</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-document"></i><span class="txt">{{ scope.row.description || '-' }}</span></span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="100" fixed="right">
          <template slot="header"><span class="th-label"><i class="el-icon-s-operation"></i>操作</span></template>
          <template v-slot="scope">
            <div class="op-btns">
              <el-button v-if="canEdit(scope.row)" type="primary" size="mini" icon="el-icon-edit" :title="assignOnly ? '分配开发负责人' : '编辑'" @click="handleEdit(scope.row)">{{ assignOnly ? '分配' : '编辑' }}</el-button>
              <el-button v-else type="info" size="mini" icon="el-icon-view" title="查看" @click="handleView(scope.row)">查看</el-button>
              <el-button type="danger" size="mini" icon="el-icon-delete" title="删除" :disabled="!canDelete(scope.row)" @click="del(scope.row.id)">删除</el-button>
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

    <el-dialog :title="dialogTitle" :visible.sync="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="form" label-width="110px" style="padding-right: 50px" :rules="rules" ref="formRef" :disabled="formReadonly">
        <el-form-item label="项目名" prop="name">
          <el-input v-model="form.name" placeholder="项目名" :disabled="assignOnly"></el-input>
        </el-form-item>
        <el-form-item label="项目编号" prop="code">
          <el-input v-model="form.code" placeholder="项目编号" :disabled="assignOnly"></el-input>
        </el-form-item>
        <el-form-item label="技术栈" prop="techStack">
          <el-input v-model="form.techStack" placeholder="技术栈" :disabled="assignOnly"></el-input>
        </el-form-item>
        <el-form-item label="开发负责人" prop="leader">
          <el-select v-model="form.leader" placeholder="请选择开发人员" style="width: 100%" clearable :disabled="!canAssignLeader">
            <el-option v-for="item in devList" :key="item.username" :label="item.name + '（' + item.username + '）'" :value="item.username"></el-option>
          </el-select>
          <div v-if="!canAssignLeader" class="field-tip">由技术负责人分配</div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%" :disabled="assignOnly">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" placeholder="请选择开始日期"
                          value-format="yyyy-MM-dd" style="width: 100%" :disabled="assignOnly"></el-date-picker>
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" placeholder="请选择结束日期"
                          value-format="yyyy-MM-dd" style="width: 100%" :disabled="assignOnly"></el-date-picker>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" :rows="4" v-model="form.description" placeholder="描述" :disabled="assignOnly"></el-input>
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
import { personName, personRoleName, roleTag, canOps } from '@/utils/person'

export default {
  name: "Project",
  data() {
    return {
      user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
      tableData: [],  // 所有的数据
      pageNum: 1,   // 当前的页码
      pageSize: 20,  // 每页显示的个数
      total: 0,
      params: { id: null, name: null, status: null, leader: null },   // 搜索条件
      fromVisible: false,
      formReadonly: false,
      form: {},
      statusOptions: [
        { value: 'NOT_STARTED', label: '未开始' },
        { value: 'IN_PROGRESS', label: '进行中' },
        { value: 'PAUSED', label: '已暂停' },
        { value: 'FINISHED', label: '已完结' },
      ],
      statusNames: { NOT_STARTED: '未开始', IN_PROGRESS: '进行中', PAUSED: '已暂停', FINISHED: '已完结' },
      statusTag: { NOT_STARTED: '', IN_PROGRESS: 'success', PAUSED: 'warning', FINISHED: 'info' },
      statusIcon: { NOT_STARTED: 'el-icon-time', IN_PROGRESS: 'el-icon-video-play', PAUSED: 'el-icon-video-pause', FINISHED: 'el-icon-circle-check' },
      rules: {
        name: [
          {required: true, message: '请输入项目名', trigger: 'blur'},
        ],
        code: [
          {required: true, message: '请输入项目编号', trigger: 'blur'},
        ]
      },
      ids: [],
      devList: [],
    }
  },
  computed: {
    canMaintain() {
      return this.user.role === 'ADMIN' || this.user.role === 'DEV'
    },
    assignOnly() {
      return this.user.role === 'TL'
    },
    canAssignLeader() {
      return this.user.role === 'ADMIN' || this.user.role === 'TL'
    },
    canOpenApps() {
      return this.user.role === 'ADMIN' || this.user.role === 'DEV'
    },
    dialogTitle() {
      if (this.formReadonly) return '查看项目'
      if (this.assignOnly) return '分配开发负责人'
      return '项目信息'
    },
  },
  created() {
    this.applyQuery()
    this.loadDevelopers()
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
    applyQuery() {
      this.params.id = this.$route.query.id ? Number(this.$route.query.id) : null
    },
    canEdit(row) {
      return row.status !== 'FINISHED'
    },
    canDelete(row) {
      if (this.assignOnly) return false
      return row.status !== 'FINISHED' && row.status !== 'PAUSED'
    },
    loadDevelopers() {
      this.$request.get('/person/developers').then(res => {
        this.devList = res.data || []
        this.bindLeader()
      })
    },
    bindLeader() {
      const current = this.form.leader
      if (!current) return
      const found = this.devList.find(item => item.username === current || item.name === current)
      if (found) this.form.leader = found.username
    },
    handleAdd() {   // 新增数据
      this.formReadonly = false
      this.form = { status: 'NOT_STARTED' }  // 新增数据的时候清空数据
      this.fromVisible = true   // 打开弹窗
    },
    handleEdit(row) {   // 编辑数据
      this.formReadonly = false
      this.form = JSON.parse(JSON.stringify(row))
      this.bindLeader()
      this.fromVisible = true   // 打开弹窗
    },
    handleView(row) {   // 已完结等不可改状态：只读查看
      this.formReadonly = true
      this.form = JSON.parse(JSON.stringify(row))
      this.bindLeader()
      this.fromVisible = true
    },
    save() {   // 保存按钮触发的逻辑  它会触发新增或者更新
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          this.$request({
            url: this.form.id ? '/project/update' : '/project/add',
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
    del(id) {   // 单个删除
      this.$confirm('您确定删除吗？', '确认删除', {type: "warning"}).then(response => {
        this.$request.delete('/project/delete/' + id).then(res => {
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
    handleSelectionChange(rows) {   // 当前选中的所有的行数据
      this.ids = rows.map(v => v.id)
    },
    delBatch() {   // 批量删除
      if (!this.ids.length) {
        this.$message.warning('请选择数据')
        return
      }
      this.$confirm('您确定批量删除这些数据吗？', '确认删除', {type: "warning"}).then(response => {
        this.$request.delete('/project/delete/batch', {data: this.ids}).then(res => {
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
      this.$request.get('/project/selectPage', {
        params: {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          ...this.params,
        }
      }).then(res => {
        this.tableData = res.data?.list
        this.total = res.data?.total
      })
    },
    reset() {
      this.params = { id: null, name: null, status: null, leader: null }
      if (this.$route.query.id) {
        this.$router.replace({ path: '/project' })
      }
      this.load(1)
    },
    handleCurrentChange(pageNum) {
      this.load(pageNum)
    },
    goApps(row) {   // 跳到应用，按该项目筛选
      this.$router.push({ path: '/app', query: { projectId: row.id } })
    },
    goDown(row) {
      // 开发无服务器菜单：点编号进应用；运维可进服务器
      if (canOps(this.user)) {
        this.$router.push({ path: '/server', query: { projectId: row.id } })
        return
      }
      this.goApps(row)
    },
  }
}
</script>

<style scoped>
.field-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: #909399;
}
</style>
