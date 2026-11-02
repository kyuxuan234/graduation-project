<template>
  <div>
    <div class="relation-bar">
      <i class="el-icon-orange"></i>
      <span class="node"><i class="el-icon-s-opportunity"></i>开发</span>
      <span class="arrow">→</span>
      <span class="node"><i class="el-icon-s-check"></i>测试</span>
      <span class="arrow">→</span>
      <span class="node"><i class="el-icon-s-flag"></i>生产</span>
      <span style="margin-left: 12px">编码固定，由运维维护名称与说明。服务器归入其中一个环境；应用在各环境各挂一条实例。</span>
    </div>

    <div class="env-grid" v-loading="loading">
      <div
        v-for="item in tableData"
        :key="item.code"
        class="env-card"
        :class="'is-' + (item.code || '').toLowerCase()">
        <div class="env-card-top">
          <div class="env-badge">
            <i :class="envIcon[item.code] || 'el-icon-orange'"></i>
            <span>{{ item.code }}</span>
          </div>
          <el-tag size="mini" :type="envTag[item.code]">{{ envStage[item.code] || '环境' }}</el-tag>
        </div>

        <div class="env-name">{{ item.name || envNames[item.code] || item.code }}</div>
        <p class="env-remark">{{ item.remark || '暂无说明，可补充该环境的用途与准入要求。' }}</p>

        <div class="env-stats">
          <button type="button" class="stat" @click="goServers(item)">
            <em>{{ item.serverCount || 0 }}</em>
            <span>服务器</span>
          </button>
          <button type="button" class="stat" @click="goApps(item)">
            <em>{{ item.instanceCount || 0 }}</em>
            <span>应用实例</span>
          </button>
        </div>

        <div class="env-flow">{{ envFlow[item.code] || '按环境走对应审批链路' }}</div>

        <div class="env-actions">
          <el-button type="primary" size="mini" icon="el-icon-edit" @click="edit(item)">编辑说明</el-button>
          <el-button size="mini" icon="el-icon-monitor" @click="goServers(item)">服务器</el-button>
          <el-button size="mini" icon="el-icon-s-order" @click="goDeploys(item)">发布单</el-button>
        </div>
      </div>
    </div>

    <div class="table" v-if="tableData.length">
      <el-table :data="tableData" stripe size="small">
        <el-table-column label="编码" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-price-tag"></i>编码</span></template>
          <template v-slot="scope">
            <el-tag size="mini" :type="envTag[scope.row.code]">{{ scope.row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="名称" min-width="140">
          <template slot="header"><span class="th-label"><i class="el-icon-orange"></i>名称</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-orange"></i><span class="txt">{{ scope.row.name }}</span></span>
          </template>
        </el-table-column>
        <el-table-column label="服务器" width="110" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-monitor"></i>服务器</span></template>
          <template v-slot="scope">
            <span class="cell-text link-text" @click="goServers(scope.row)">
              <i class="el-icon-monitor"></i>{{ scope.row.serverCount || 0 }} 台
            </span>
          </template>
        </el-table-column>
        <el-table-column label="应用实例" width="120" align="center">
          <template slot="header"><span class="th-label"><i class="el-icon-s-platform"></i>应用实例</span></template>
          <template v-slot="scope">
            <span class="cell-text link-text" @click="goApps(scope.row)">
              <i class="el-icon-s-platform"></i>{{ scope.row.instanceCount || 0 }} 个
            </span>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="220" show-overflow-tooltip>
          <template slot="header"><span class="th-label"><i class="el-icon-document"></i>说明</span></template>
          <template v-slot="scope">
            <span class="cell-text"><i class="el-icon-document"></i><span class="txt">{{ scope.row.remark || '-' }}</span></span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template slot="header"><span class="th-label"><i class="el-icon-s-operation"></i>操作</span></template>
          <template v-slot="scope">
            <div class="op-btns">
              <el-button type="primary" size="mini" icon="el-icon-edit" title="编辑" @click="edit(scope.row)">编辑</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog title="编辑环境说明" :visible.sync="visible" width="480px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="form" label-width="80px" style="padding-right: 24px" :rules="rules" ref="formRef">
        <el-form-item label="编码">
          <div class="code-row">
            <el-tag :type="envTag[form.code]">{{ form.code }}</el-tag>
            <span>{{ envNames[form.code] || form.code }}</span>
          </div>
          <div class="form-tip">编码不可改，发布审批与版本晋升都按编码识别环境。</div>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="32" show-word-limit placeholder="环境显示名称"></el-input>
        </el-form-item>
        <el-form-item label="说明" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="4" maxlength="200" show-word-limit
                    placeholder="补充用途、准入要求或注意事项"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="visible = false" icon="el-icon-close">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="save" icon="el-icon-check">保 存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Environment',
  data() {
    return {
      loading: false,
      saving: false,
      tableData: [],
      visible: false,
      form: { code: '', name: '', remark: '' },
      envNames: { DEV: '开发环境', TEST: '测试环境', PROD: '生产环境' },
      envTag: { DEV: 'success', TEST: 'warning', PROD: 'danger' },
      envIcon: {
        DEV: 'el-icon-s-opportunity',
        TEST: 'el-icon-s-check',
        PROD: 'el-icon-s-flag'
      },
      envStage: { DEV: '开发态', TEST: '验证态', PROD: '线上态' },
      envFlow: {
        DEV: '提交后直接交运维审批并执行',
        TEST: '测试验收通过后，由运维审批并执行',
        PROD: '技术审查 → 产品验收 → 运维审批并执行'
      },
      rules: {
        name: [{ required: true, message: '请填写环境名称', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      this.$request.get('/environment/selectAll').then(res => {
        const list = res.data || []
        const order = { DEV: 1, TEST: 2, PROD: 3 }
        this.tableData = list.slice().sort((a, b) => (order[a.code] || 9) - (order[b.code] || 9))
      }).finally(() => {
        this.loading = false
      })
    },
    edit(row) {
      this.form = {
        id: row.id,
        code: row.code,
        name: row.name || '',
        remark: row.remark || ''
      }
      this.visible = true
      this.$nextTick(() => {
        if (this.$refs.formRef) this.$refs.formRef.clearValidate()
      })
    },
    save() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        this.saving = true
        this.$request.put('/environment/update', this.form).then(res => {
          if (res.code === '200') {
            this.$message.success('已保存')
            this.visible = false
            this.load()
          } else {
            this.$message.error(res.msg)
          }
        }).finally(() => {
          this.saving = false
        })
      })
    },
    goServers(row) {
      this.$router.push({ path: '/server', query: { env: row.code } })
    },
    goApps() {
      this.$router.push({ path: '/app' })
    },
    goDeploys(row) {
      this.$router.push({ path: '/deploy', query: { envCode: row.code } })
    }
  }
}
</script>

<style scoped>
.env-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
  margin-bottom: 12px;
}

.env-card {
  background: #fff;
  border: 1px solid #e9eef5;
  border-radius: 12px;
  box-shadow: 0 6px 18px rgba(31, 42, 68, 0.05);
  padding: 18px 18px 16px;
  border-top: 3px solid #909399;
  min-height: 268px;
  display: flex;
  flex-direction: column;
}

.env-card.is-dev { border-top-color: #67c23a; }
.env-card.is-test { border-top-color: #e6a23c; }
.env-card.is-prod { border-top-color: #f56c6c; }

.env-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.env-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f5f7fa;
  color: #606266;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.env-card.is-dev .env-badge { background: #f0f9eb; color: #67c23a; }
.env-card.is-test .env-badge { background: #fdf6ec; color: #e6a23c; }
.env-card.is-prod .env-badge { background: #fef0f0; color: #f56c6c; }

.env-name {
  font-size: 20px;
  font-weight: 700;
  color: #1f2a44;
  line-height: 1.3;
  margin-bottom: 8px;
}

.env-remark {
  margin: 0;
  min-height: 40px;
  color: #5b6472;
  font-size: 13px;
  line-height: 1.55;
}

.env-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin: 16px 0 12px;
}

.stat {
  border: 1px solid #e9eef5;
  background: #f8fafc;
  border-radius: 10px;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
  transition: border-color .15s ease, background .15s ease;
}

.stat:hover {
  border-color: #c9d8f5;
  background: #f3f7ff;
}

.stat em {
  display: block;
  font-style: normal;
  font-size: 22px;
  font-weight: 700;
  color: #1f2a44;
  line-height: 1.2;
}

.stat span {
  color: #909399;
  font-size: 12px;
}

.env-flow {
  color: #606266;
  font-size: 12px;
  line-height: 1.5;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f7faff;
  border: 1px dashed #d9e4f5;
  margin-bottom: 14px;
}

.env-actions {
  margin-top: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.code-row {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 14px;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 6px;
}

</style>
