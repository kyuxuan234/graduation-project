<template>
  <div>
    <div class="toolbar">
      <div class="search-fields">
        <el-input prefix-icon="el-icon-user" placeholder="请输入用户名查询" style="width: 180px" v-model="params.username" clearable></el-input>
        <el-input prefix-icon="el-icon-postcard" placeholder="请输入姓名查询" style="width: 180px" v-model="params.name" clearable></el-input>
        <el-select placeholder="请选择角色" style="width: 140px" v-model="params.role" clearable>
          <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
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
        <el-table-column label="姓名" min-width="140">
          <template slot="header"><span class="th-label"><i class="el-icon-postcard"></i>姓名</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-postcard"></i>{{ scope.row.name || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户名" width="130">
          <template slot="header"><span class="th-label"><i class="el-icon-user"></i>用户名</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-user"></i>{{ scope.row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-s-check"></i>角色</span></template>
          <template v-slot="scope">
            <el-tag size="mini" :type="tagType(scope.row.role)">{{ ROLE_NAMES[scope.row.role] || scope.row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="160">
          <template slot="header"><span class="th-label"><i class="el-icon-mobile-phone"></i>手机号</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-mobile-phone"></i>{{ scope.row.phone || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="180" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-message"></i>邮箱</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-message"></i><span class="txt">{{ scope.row.email || '-' }}</span></span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-flag"></i>状态</span></template>
          <template v-slot="scope">
            <el-tag :type="statusTag[scope.row.status]"><i :class="statusIcon[scope.row.status]"></i>{{ statusNames[scope.row.status] || scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template slot="header"><span class="th-label"><i class="el-icon-time"></i>创建时间</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-time"></i>{{ scope.row.createTime || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="136" fixed="right">
          <template slot="header"><span class="th-label"><i class="el-icon-s-operation"></i>操作</span></template>
          <template v-slot="scope">
            <div class="op-btns">
              <el-button type="primary" size="mini" icon="el-icon-edit" title="编辑" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button type="danger" size="mini" icon="el-icon-delete" title="删除" :disabled="!canDelete(scope.row)" @click="del(scope.row.id)">删除</el-button>
              <el-button type="warning" size="mini" icon="el-icon-refresh" title="重置密码" :disabled="!canReset(scope.row)" @click="resetPassword(scope.row)">重置</el-button>
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

    <el-dialog title="人员" :visible.sync="fromVisible" width="40%" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="form" label-width="100px" style="padding-right: 50px" :rules="rules" ref="formRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="用户名" :disabled="!!form.id"></el-input>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="姓名"></el-input>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="邮箱"></el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
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
import { ROLE_NAMES, ROLE_TAG, rememberPersons } from '@/utils/person'

export default {
  name: "Person",
  data() {
    return {
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      params: { username: null, name: null, role: null, status: null },
      fromVisible: false,
      form: {},
      user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
      roleOptions: [
        { value: 'ADMIN', label: '超级管理员' },
        { value: 'DEV', label: '开发人员' },
        { value: 'TL', label: '技术负责人' },
        { value: 'QA', label: '测试人员' },
        { value: 'PM', label: '产品经理' },
        { value: 'OPS', label: '运维人员' },
      ],
      statusOptions: [
        { value: 'ACTIVE', label: '正常' },
        { value: 'DISABLED', label: '禁用' },
      ],
      statusNames: { ACTIVE: '正常', DISABLED: '禁用' },
      statusTag: { ACTIVE: 'success', DISABLED: 'danger' },
      statusIcon: { ACTIVE: 'el-icon-success', DISABLED: 'el-icon-remove' },
      rules: {
        username: [
          {required: true, message: '请输入用户名', trigger: 'blur'},
        ],
        role: [
          {required: true, message: '请选择角色', trigger: 'change'},
        ],
      },
      ids: [],
      ROLE_NAMES,
    }
  },
  created() {
    this.load(1)
  },
  methods: {
    tagType(role) {
      return ROLE_TAG[role] || undefined
    },
    canDelete(row) {
      return row.status !== 'DISABLED' && row.id !== this.user.id
    },
    canReset(row) {
      return row.status !== 'DISABLED'
    },
    handleAdd() {
      this.form = { status: 'ACTIVE', role: 'DEV' }
      this.fromVisible = true
    },
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row))
      this.fromVisible = true
    },
    save() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          this.$request({
            url: this.form.id ? '/person/update' : '/person/add',
            method: this.form.id ? 'PUT' : 'POST',
            data: this.form
          }).then(res => {
            if (res.code === '200') {
              this.$message.success('保存成功')
              this.load(1)
              this.fromVisible = false
            } else {
              this.$message.error(res.msg)
            }
          })
        }
      })
    },
    resetPassword(row) {
      this.$confirm('确定将 ' + row.username + ' 的密码重置为 123456 吗？', '确认重置', {type: "warning"}).then(() => {
        this.$request.put('/person/update', { id: row.id, password: '123456' }).then(res => {
          if (res.code === '200') {
            this.$message.success('密码已重置为123456')
          } else {
            this.$message.error(res.msg)
          }
        })
      }).catch(() => {
      })
    },
    del(id) {
      this.$confirm('您确定删除吗？', '确认删除', {type: "warning"}).then(() => {
        this.$request.delete('/person/delete/' + id).then(res => {
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
    handleSelectionChange(rows) {
      this.ids = rows.map(v => v.id)
    },
    delBatch() {
      if (!this.ids.length) {
        this.$message.warning('请选择数据')
        return
      }
      this.$confirm('您确定批量删除这些数据吗？', '确认删除', {type: "warning"}).then(() => {
        this.$request.delete('/person/delete/batch', {data: this.ids}).then(res => {
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
    load(pageNum) {
      if (pageNum) this.pageNum = pageNum
      const query = {
        pageNum: this.pageNum,
        pageSize: this.pageSize,
      }
      Object.keys(this.params).forEach(key => {
        if (this.params[key]) query[key] = this.params[key]
      })
      this.$request.get('/person/selectPage', { params: query }).then(res => {
        this.tableData = res.data?.list
        this.total = res.data?.total
        rememberPersons(this.tableData)
      })
    },
    reset() {
      this.params = { username: null, name: null, role: null, status: null }
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
