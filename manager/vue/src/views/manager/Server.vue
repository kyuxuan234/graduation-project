<template>
  <div>
    <div class="relation-bar" v-if="params.id || params.status || params.env || params.os || params.projectId">
      <i class="el-icon-link"></i>
      <span>{{ serverHint }}</span>
      <span class="link-text" style="margin-left: 12px" @click="reset">查看全部</span>
    </div>
    <div class="toolbar">
      <div class="search-fields">
        <el-input prefix-icon="el-icon-monitor" placeholder="请输入主机名查询" style="width: 180px" v-model="params.hostname" clearable></el-input>
        <el-input prefix-icon="el-icon-location-outline" placeholder="请输入IP查询" style="width: 180px" v-model="params.ip" clearable></el-input>
        <el-select placeholder="请选择环境" style="width: 130px" v-model="params.env" clearable>
          <el-option v-for="item in envOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
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
        <el-table-column label="主机名" min-width="140">
          <template slot="header"><span class="th-label"><i class="el-icon-monitor"></i>主机名</span></template>
          <template v-slot="scope">
            <span class="cell-text link-text" @click="goApps(scope.row)"><i class="el-icon-monitor"></i>{{ scope.row.hostname }}</span>
          </template>
        </el-table-column>
        <el-table-column label="IP" width="160">
          <template slot="header"><span class="th-label"><i class="el-icon-location-outline"></i>IP</span></template>
          <template v-slot="scope">
            <span class="cell-text cell-code"><i class="el-icon-location-outline"></i>{{ scope.row.ip }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作系统" min-width="150" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-files"></i>操作系统</span></template>
          <template v-slot="scope">
            <span class="cell-text">
              <os-logo :os="scope.row.os"></os-logo>
              <span class="txt">{{ scope.row.os || '-' }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="CPU" width="90" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-cpu"></i>CPU</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-cpu"></i>{{ scope.row.cpu || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="内存" width="100" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-coin"></i>内存</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-coin"></i>{{ scope.row.memory || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="磁盘" width="100" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-box"></i>磁盘</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-box"></i>{{ scope.row.disk || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="环境" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-orange"></i>环境</span></template>
          <template v-slot="scope">
            <el-tag :type="envTag[scope.row.env]"><i class="el-icon-orange"></i>{{ envNames[scope.row.env] || scope.row.env }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-flag"></i>状态</span></template>
          <template v-slot="scope">
            <el-tag :type="statusTag[scope.row.status]"><i :class="statusIcon[scope.row.status]"></i>{{ statusNames[scope.row.status] || scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联应用" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-connection"></i>关联应用</span></template>
          <template v-slot="scope">
            <span class="cell-text link-text" @click="goApps(scope.row)"><i class="el-icon-s-platform"></i>{{ scope.row.appCount || 0 }} 个</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-document"></i>备注</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-document"></i><span class="txt">{{ scope.row.remark || '-' }}</span></span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="100" fixed="right">
          <template slot="header"><span class="th-label"><i class="el-icon-s-operation"></i>操作</span></template>
          <template v-slot="scope">
            <div class="op-btns">
              <el-button type="primary" size="mini" icon="el-icon-edit" title="编辑" :disabled="!canEdit(scope.row)" @click="handleEdit(scope.row)">编辑</el-button>
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

    <el-dialog title="服务器信息" :visible.sync="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="form" label-width="100px" style="padding-right: 50px" :rules="rules" ref="formRef">
        <el-form-item label="主机名" prop="hostname">
          <el-input v-model="form.hostname" placeholder="主机名"></el-input>
        </el-form-item>
        <el-form-item label="IP" prop="ip">
          <el-input v-model="form.ip" placeholder="IP地址"></el-input>
        </el-form-item>
        <el-form-item label="操作系统" prop="os">
          <el-input v-model="form.os" placeholder="如 CentOS 7.9 / Ubuntu 22.04">
            <os-logo slot="prefix" :os="form.os"></os-logo>
          </el-input>
        </el-form-item>
        <el-form-item label="CPU" prop="cpu">
          <el-input v-model="form.cpu" placeholder="如 4核"></el-input>
        </el-form-item>
        <el-form-item label="内存" prop="memory">
          <el-input v-model="form.memory" placeholder="如 8G"></el-input>
        </el-form-item>
        <el-form-item label="磁盘" prop="disk">
          <el-input v-model="form.disk" placeholder="如 200G"></el-input>
        </el-form-item>
        <el-form-item label="环境" prop="env">
          <el-select v-model="form.env" placeholder="请选择环境" style="width: 100%">
            <el-option v-for="item in envOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="SSH 端口" prop="sshPort">
          <el-input v-model.number="form.sshPort" placeholder="默认 22"></el-input>
        </el-form-item>
        <el-form-item label="SSH 用户" prop="sshUser">
          <el-input v-model="form.sshUser" placeholder="登录用户名"></el-input>
        </el-form-item>
        <el-form-item label="SSH 密码" prop="sshPassword">
          <el-input v-model="form.sshPassword" show-password placeholder="留空则不修改"></el-input>
          <div style="color: #909399; font-size: 12px; line-height: 1.5; margin-top: 6px">登记密码。保存时用这个密码登录一次，生成私钥放到目标机，之后登录都用这把私钥。</div>
        </el-form-item>
        <el-form-item label="部署目录" prop="deployPath">
          <el-input v-model="form.deployPath" placeholder="如 /opt/apps"></el-input>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input type="textarea" :rows="4" v-model="form.remark" placeholder="备注"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="fromVisible = false" icon="el-icon-close">取 消</el-button>
        <el-button type="primary" @click="save" icon="el-icon-check">确 定</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import OsLogo from '@/components/OsLogo'

export default {
  name: "Server",
  components: { OsLogo },
  data() {
    return {
      tableData: [],  // 所有的数据
      pageNum: 1,   // 当前的页码
      pageSize: 20,  // 每页显示的个数
      total: 0,
      params: { id: null, hostname: null, ip: null, env: null, status: null, os: null, projectId: null },   // 搜索条件
      fromVisible: false,
      form: {},
      envOptions: [
        { value: 'DEV', label: '开发环境' },
        { value: 'TEST', label: '测试环境' },
        { value: 'PROD', label: '生产环境' },
      ],
      statusOptions: [
        { value: 'RUNNING', label: '运行中' },
        { value: 'STOPPED', label: '已停机' },
        { value: 'MAINTENANCE', label: '维护中' },
      ],
      envNames: { DEV: '开发环境', TEST: '测试环境', PROD: '生产环境' },
      envTag: { DEV: 'success', TEST: 'warning', PROD: 'danger' },
      statusNames: { RUNNING: '运行中', STOPPED: '已停机', MAINTENANCE: '维护中' },
      statusTag: { RUNNING: 'success', MAINTENANCE: 'warning', STOPPED: 'info' },
      statusIcon: { RUNNING: 'el-icon-success', MAINTENANCE: 'el-icon-setting', STOPPED: 'el-icon-remove' },
      rules: {
        hostname: [
          {required: true, message: '请输入主机名', trigger: 'blur'},
        ],
        ip: [
          {required: true, message: '请输入IP地址', trigger: 'blur'},
        ]
      },
      ids: []
    }
  },
  computed: {
    serverHint() {
      if (this.params.projectId) return '当前查看该项目关联的服务器'
      if (this.params.id) return '当前按关联服务器定位'
      if (this.params.os) return '当前查看操作系统包含「' + this.params.os + '」的服务器'
      if (this.params.env) return '当前查看' + (this.envNames[this.params.env] || this.params.env) + '服务器'
      if (this.params.status) return '当前查看' + (this.statusNames[this.params.status] || this.params.status) + '的服务器'
      return ''
    }
  },
  created() {
    this.applyQuery()
    this.load(1)
  },
    watch: {
    '$route.fullPath'() {
      this.applyQuery()
      this.load(1)
    }
  },
  methods: {
    applyQuery() {
      const q = this.$route.query
      this.params.id = q.id ? Number(q.id) : null
      this.params.status = q.status || null
      this.params.env = q.env || null
      this.params.os = q.os || null
      this.params.projectId = q.projectId ? Number(q.projectId) : null
    },
    canEdit() {
      return true
    },
    canDelete(row) {
      return row.status !== 'STOPPED' && row.status !== 'MAINTENANCE'
    },
    handleAdd() {   // 新增数据
      this.form = { status: 'RUNNING', sshPort: 22 }
      this.fromVisible = true   // 打开弹窗
    },
    handleEdit(row) {   // 编辑数据
      this.form = JSON.parse(JSON.stringify(row))  // 给form对象赋值  注意要深拷贝数据
      this.fromVisible = true   // 打开弹窗
    },
    save() {   // 保存按钮触发的逻辑  它会触发新增或者更新
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          const data = JSON.parse(JSON.stringify(this.form))
          delete data.sshAuthType
          delete data.sshPrivateKey
          this.$request({
            url: this.form.id ? '/server/update' : '/server/add',
            method: this.form.id ? 'PUT' : 'POST',
            data: data
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
        this.$request.delete('/server/delete/' + id).then(res => {
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
        this.$request.delete('/server/delete/batch', {data: this.ids}).then(res => {
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
      if (this.params.projectId) {
        this.$request.get('/app/selectPage', {
          params: { pageNum: 1, pageSize: 200, projectId: this.params.projectId }
        }).then(appRes => {
          const ids = [...new Set((appRes.data?.list || []).map(item => item.serverId).filter(Boolean))]
          if (!ids.length) {
            this.tableData = []
            this.total = 0
            return
          }
          this.$request.get('/server/selectPage', {
            params: { pageNum: 1, pageSize: 200 }
          }).then(res => {
            const list = (res.data?.list || []).filter(item => ids.includes(item.id))
            this.tableData = list
            this.total = list.length
          })
        })
        return
      }
      this.$request.get('/server/selectPage', {
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
      this.params = { id: null, hostname: null, ip: null, env: null, status: null, os: null, projectId: null }
      if (this.$route.query.id || this.$route.query.status || this.$route.query.env || this.$route.query.os || this.$route.query.projectId) {
        this.$router.replace({ path: '/server' })
      }
      this.load(1)
    },
    handleCurrentChange(pageNum) {
      this.load(pageNum)
    },
    goApps(row) {   // 跳到应用管理，按该服务器筛选
      this.$router.push({ path: '/app', query: { serverId: row.id } })
    },
  }
}
</script>

<style scoped>

</style>
