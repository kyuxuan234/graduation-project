<template>
  <div>
    <div class="relation-bar" v-if="relationHint">
      <i class="el-icon-link"></i>
      <span>{{ relationHint }}</span>
      <span class="link-text" style="margin-left: 12px" @click="reset">查看全部</span>
    </div>
    <div class="toolbar">
      <div class="search-fields">
        <el-input prefix-icon="el-icon-search" placeholder="请输入应用名查询" style="width: 180px" v-model="params.name" clearable></el-input>
        <el-select placeholder="请选择所属项目" style="width: 180px" v-model="params.projectId" clearable>
          <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id"></el-option>
        </el-select>
        <el-select placeholder="请选择部署服务器" style="width: 180px" v-model="params.serverId" clearable>
          <el-option v-for="item in serverList" :key="item.id" :label="item.hostname" :value="item.id"></el-option>
        </el-select>
        <el-select placeholder="请选择状态" style="width: 130px" v-model="params.status" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
        </el-select>
        <el-button type="info" plain @click="load(1)" icon="el-icon-search">查询</el-button>
        <el-button type="warning" plain @click="reset" icon="el-icon-refresh-left">重置</el-button>
      </div>
      <div class="toolbar-actions">
        <el-button type="primary" plain @click="handleAdd" icon="el-icon-plus">新增</el-button>
        <el-button type="danger" plain @click="delBatch" icon="el-icon-delete">批量删除</el-button>
      </div>
    </div>

    <div class="table">
      <el-table :data="tableData" stripe size="small" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" align="center" :selectable="canDelete"></el-table-column>
        <el-table-column label="应用编号" width="130">
          <template slot="header"><span class="th-label"><i class="el-icon-postcard"></i>应用编号</span></template>
          <template v-slot="scope">
            <span class="cell-text cell-code link-text" @click="goDeploys(scope.row)"><i class="el-icon-postcard"></i>{{ scope.row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column label="应用名" min-width="140" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-s-platform"></i>应用名</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-s-platform"></i><span class="txt">{{ scope.row.name }}</span></span>
          </template>
        </el-table-column>
        <el-table-column label="所属项目" min-width="140" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-folder"></i>所属项目</span></template>
          <template v-slot="scope">
            <span v-if="scope.row.projectId" class="cell-text link-text" @click="goProject(scope.row)"><i class="el-icon-folder"></i><span class="txt">{{ scope.row.projectName }}</span></span>
            <span v-else class="cell-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="部署服务器" width="150">
          <template slot="header"><span class="th-label"><i class="el-icon-monitor"></i>部署服务器</span></template>
          <template v-slot="scope">
            <span v-if="scope.row.serverId" class="cell-text link-text" @click="goServer(scope.row)"><i class="el-icon-monitor"></i>{{ scope.row.serverName }}</span>
            <span v-else class="cell-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="环境" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-orange"></i>环境</span></template>
          <template v-slot="scope">
            <el-tag :type="envTag[scope.row.serverEnv]" v-if="scope.row.serverEnv">
              <i class="el-icon-orange"></i>{{ envNames[scope.row.serverEnv] || scope.row.serverEnv }}
            </el-tag>
            <span v-else class="cell-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="端口" width="90" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-connection"></i>端口</span></template>
          <template v-slot="scope">
            <span class="cell-text cell-code"><i class="el-icon-connection"></i>{{ scope.row.port || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="当前版本" width="120">
          <template slot="header"><span class="th-label"><i class="el-icon-price-tag"></i>当前版本</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-price-tag"></i>{{ scope.row.version || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="技术栈" min-width="120" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-cpu"></i>技术栈</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-cpu"></i><span class="txt">{{ scope.row.techStack || '-' }}</span></span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-flag"></i>状态</span></template>
          <template v-slot="scope">
            <el-tag :type="statusTag[scope.row.status]"><i :class="statusIcon[scope.row.status]"></i>{{ statusNames[scope.row.status] || scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="部署单" width="100" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-s-promotion"></i>部署单</span></template>
          <template v-slot="scope">
            <span class="cell-text link-text" @click="goDeploys(scope.row)"><i class="el-icon-s-promotion"></i>{{ scope.row.deployCount || 0 }} 单</span>
          </template>
        </el-table-column>
        <el-table-column label="告警" width="90" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-warning-outline"></i>告警</span></template>
          <template v-slot="scope">
            <span class="cell-text link-text" @click="goAlarms(scope.row)"><i class="el-icon-warning-outline"></i>{{ scope.row.alarmCount || 0 }} 条</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="100" fixed="right">
          <template slot="header"><span class="th-label"><i class="el-icon-s-operation"></i>操作</span></template>
          <template v-slot="scope">
            <div class="op-btns">
              <el-button v-if="canEdit(scope.row)" type="primary" size="mini" icon="el-icon-edit" title="编辑" @click="handleEdit(scope.row)">编辑</el-button>
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

    <el-dialog :title="formReadonly ? '查看应用' : '应用信息'" :visible.sync="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="form" label-width="110px" style="padding-right: 50px" :rules="rules" ref="formRef" :disabled="formReadonly">
        <el-form-item label="应用名" prop="name">
          <el-input v-model="form.name" placeholder="应用名"></el-input>
        </el-form-item>
        <el-form-item label="应用编号" prop="code">
          <el-input v-model="form.code" placeholder="应用编号"></el-input>
        </el-form-item>
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择所属项目" style="width: 100%">
            <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="部署服务器" prop="serverId">
          <el-select v-model="form.serverId" placeholder="请选择部署服务器" style="width: 100%">
            <el-option v-for="item in serverList" :key="item.id"
                       :label="item.hostname + '（' + (envNames[item.env] || item.env) + '）'" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="当前版本" prop="version">
          <el-input v-model="form.version" placeholder="如 v1.0.0"></el-input>
        </el-form-item>
        <el-form-item label="技术栈" prop="techStack">
          <el-input v-model="form.techStack" placeholder="技术栈"></el-input>
        </el-form-item>
        <el-form-item label="代码仓库地址" prop="repoUrl">
          <el-input v-model="form.repoUrl" placeholder="代码仓库地址"></el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
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
import { canOps } from '@/utils/person'

export default {
  name: "App",
  data() {
    return {
      user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
      tableData: [],  // 所有的数据
      pageNum: 1,   // 当前的页码
      pageSize: 20,  // 每页显示的个数
      total: 0,
      params: { id: null, name: null, projectId: null, serverId: null, status: null },   // 搜索条件
      fromVisible: false,
      formReadonly: false,
      form: {},
      projectList: [],   // 项目下拉数据源
      serverList: [],    // 服务器下拉数据源
      statusOptions: [
        { value: 'ONLINE', label: '在线' },
        { value: 'DEPLOYING', label: '部署中' },
        { value: 'OFFLINE', label: '离线' },
      ],
      statusNames: { ONLINE: '在线', DEPLOYING: '部署中', OFFLINE: '离线' },
      statusTag: { ONLINE: 'success', DEPLOYING: 'warning', OFFLINE: 'info' },
      statusIcon: { ONLINE: 'el-icon-success', DEPLOYING: 'el-icon-loading', OFFLINE: 'el-icon-remove' },
      envNames: { DEV: '开发环境', TEST: '测试环境', PROD: '生产环境' },
      envTag: { DEV: 'success', TEST: 'warning', PROD: 'danger' },
      rules: {
        name: [
          {required: true, message: '请输入应用名', trigger: 'blur'},
        ],
        code: [
          {required: true, message: '请输入应用编号', trigger: 'blur'},
        ]
      },
      ids: []
    }
  },
  computed: {
    relationHint() {
      if (this.params.id) return '当前按关联应用筛选'
      if (this.params.projectId) {
        const p = this.projectList.find(v => v.id === this.params.projectId)
        return '当前查看项目「' + (p ? p.name : this.params.projectId) + '」下的应用'
      }
      if (this.params.serverId) {
        const s = this.serverList.find(v => v.id === this.params.serverId)
        return '当前查看服务器「' + (s ? s.hostname : this.params.serverId) + '」上的应用'
      }
      if (this.params.status === 'ONLINE') return '当前查看在线应用'
      if (this.params.status === 'DEPLOYING') return '当前查看部署中应用'
      if (this.params.status === 'OFFLINE') return '当前查看离线应用'
      if (this.params.status) return '当前按应用状态筛选'
      return ''
    }
  },
  created() {
    this.applyQuery()
    this.loadProject()
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
    canEdit(row) {
      // 在线/部署中只读查看，避免误改；离线才允许编辑
      return row.status === 'OFFLINE'
    },
    canDelete(row) {
      return row.status !== 'ONLINE'
    },
    applyQuery() {   // 从项目/服务器/部署/告警页跳转过来时带上外键筛选
      const q = this.$route.query
      this.params.id = q.id ? Number(q.id) : null
      this.params.projectId = q.projectId ? Number(q.projectId) : null
      this.params.serverId = q.serverId ? Number(q.serverId) : null
      this.params.status = q.status || null
    },
    goProject(row) {
      this.$router.push({ path: '/project', query: { id: row.projectId } })
    },
    goServer(row) {
      if (!canOps(this.user)) {
        this.$message.info('服务器由运维管理')
        return
      }
      this.$router.push({ path: '/server', query: { id: row.serverId } })
    },
    goDeploys(row) {
      this.$router.push({ path: '/deploy', query: { appId: row.id } })
    },
    goAlarms(row) {
      this.$router.push({ path: '/alarm', query: { appId: row.id } })
    },
    loadProject() {   // 预加载项目下拉数据源
      this.$request.get('/project/selectAll').then(res => {
        this.projectList = res.data || []
      })
    },
    loadServer() {   // 预加载服务器下拉数据源，接口只返回未停机的服务器
      this.$request.get('/server/selectAll').then(res => {
        this.serverList = res.data || []
      })
    },
    handleAdd() {   // 新增数据
      this.formReadonly = false
      this.form = { status: 'OFFLINE' }  // 新增数据的时候清空数据
      this.fromVisible = true   // 打开弹窗
    },
    handleEdit(row) {   // 编辑数据
      this.formReadonly = false
      this.form = JSON.parse(JSON.stringify(row))  // 给form对象赋值  注意要深拷贝数据
      this.fromVisible = true   // 打开弹窗
    },
    handleView(row) {   // 在线/部署中：只读查看
      this.formReadonly = true
      this.form = JSON.parse(JSON.stringify(row))
      this.fromVisible = true
    },
    save() {   // 保存按钮触发的逻辑  它会触发新增或者更新
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          this.$request({
            url: this.form.id ? '/app/update' : '/app/add',
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
        this.$request.delete('/app/delete/' + id).then(res => {
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
        this.$request.delete('/app/delete/batch', {data: this.ids}).then(res => {
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
      this.$request.get('/app/selectPage', {
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
      this.params = { id: null, name: null, projectId: null, serverId: null, status: null }
      if (this.$route.query.id || this.$route.query.projectId || this.$route.query.serverId || this.$route.query.status) {
        this.$router.replace({ path: '/app' })
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
