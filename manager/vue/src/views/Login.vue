<template>
  <div class="page">
    <div class="bg">
      <div class="grid"></div>
      <div class="orb a"></div>
      <div class="orb b"></div>
      <div class="orb c"></div>
      <div class="vignette"></div>
    </div>

    <div class="shell">
      <div class="panel">
        <div class="brand">
          <div class="brand-left">
            <div class="logo">
              <svg viewBox="0 0 24 24" fill="none" stroke="#041114" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 12h4l3 8 4-16 3 8h4"></path>
              </svg>
            </div>
            <div>DevOps 发布管理系统<small>公司项目发布</small></div>
          </div>
          <div class="live"><i></i>内部使用</div>
        </div>

        <div class="main">
          <div class="side">
            <h2>发布链路<br><span class="chain">项目 → 应用 → 环境 → 发布单</span></h2>
            <div class="flow" aria-label="发布流程">
              <div class="flow-head" :class="{ on: hit('submit') }"><span class="node start" :class="{ on: hit('submit') }">开发提交</span></div>
              <div class="fork" :class="{ 'to-dev': hit('dev'), 'to-test': hit('test'), 'to-prod': hit('prod'), stem: hit('submit') }"></div>
              <div class="lanes">
                <div class="lane" :class="{ on: hit('dev') }">
                  <div class="env">开发环境</div>
                  <div class="steps" :class="{ lit: hit('dev-run') }">
                    <span class="node" :class="{ on: hit('dev-ops'), link: link('dev-ops', 'dev-run') }">运维审批</span>
                  </div>
                  <span class="node end" :class="{ on: hit('dev-run') }">执行</span>
                </div>
                <div class="lane" :class="{ on: hit('test') }">
                  <div class="env">测试环境</div>
                  <div class="steps" :class="{ lit: hit('test-run') }">
                    <span class="node" :class="{ on: hit('test-qa'), link: link('test-qa', 'test-ops') }">测试验收</span>
                    <span class="node" :class="{ on: hit('test-ops'), link: link('test-ops', 'test-run') }">运维审批</span>
                  </div>
                  <span class="node end" :class="{ on: hit('test-run') }">执行</span>
                </div>
                <div class="lane" :class="{ on: hit('prod') }">
                  <div class="env">生产环境</div>
                  <div class="steps" :class="{ lit: hit('prod-run') }">
                    <span class="node" :class="{ on: hit('prod-tl'), link: link('prod-tl', 'prod-pm') }">技术审查</span>
                    <span class="node" :class="{ on: hit('prod-pm'), link: link('prod-pm', 'prod-ops') }">产品验收</span>
                    <span class="node" :class="{ on: hit('prod-ops'), link: link('prod-ops', 'prod-run') }">运维审批</span>
                  </div>
                  <span class="node end" :class="{ on: hit('prod-run') }">执行</span>
                </div>
              </div>
            </div>
            <p class="duty">{{ roleDuty }}</p>
          </div>

          <div class="form-box">
            <h1>登录发布平台</h1>
            <div class="sub">按岗位登录，左侧会标出你负责的环节</div>

            <label class="label" for="login-role">当前身份</label>
            <div class="field select-field">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4z"></path><path d="M4 20c1.2-3.2 4-5 8-5s6.8 1.8 8 5"></path></svg>
              <select id="login-role" v-model="role" @change="applyRole">
                <option v-for="item in roles" :key="item.value" :value="item.value">{{ item.name }}</option>
              </select>
            </div>

            <div class="field">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="4"></circle><path d="M4 21c0-4 4-6 8-6s8 2 8 6"></path></svg>
              <input v-model="form.username" placeholder="用户名" @keyup.enter="login">
            </div>
            <div class="field">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="10" width="16" height="11" rx="2"></rect><path d="M8 10V7a4 4 0 0 1 8 0v3"></path></svg>
              <input v-model="form.password" :type="showPass ? 'text' : 'password'" placeholder="密码" @keyup.enter="login">
              <button type="button" class="eye" @click="showPass = !showPass">{{ showPass ? '隐藏' : '显示' }}</button>
            </div>

            <div class="row">
              <label><input type="checkbox" v-model="remember"> 记住我</label>
              <a v-if="role !== 'ADMIN'" href="javascript:;" @click.prevent="forgot">忘记密码？</a>
            </div>

            <button class="btn" type="button" :disabled="loading" @click="login">{{ loading ? '登录中…' : '登 录' }}</button>
            <div class="foot">
              岗位账号：<b>{{ demoText }}</b>
              <span> · </span>
              <router-link to="/register">注册账号</router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const ROLES = [
  { value: 'ADMIN', name: '超级管理员', username: 'admin', password: 'admin', text: 'admin / admin', duty: '查看项目、服务器、应用、发布单和告警的全部进度。' },
  { value: 'DEV', name: '开发人员', username: 'dev1', password: '123456', text: 'dev1 / 123456', duty: '提交发布申请。开发环境直接交运维，测试环境先测试验收，生产环境先技术审查和产品验收。' },
  { value: 'TL', name: '技术负责人', username: 'tl1', password: '123456', text: 'tl1 / 123456', duty: '审查生产发布，通过后进入产品验收；并在业务项目中分配开发负责人。' },
  { value: 'QA', name: '测试人员', username: 'qa1', password: '123456', text: 'qa1 / 123456', duty: '验收测试环境的发布单，通过后交给运维审批并执行。' },
  { value: 'PM', name: '产品经理', username: 'pm1', password: '123456', text: 'pm1 / 123456', duty: '验收生产环境中已通过技术审查的发布单，通过后交给运维。' },
  { value: 'OPS', name: '运维人员', username: 'ops1', password: '123456', text: 'ops1 / 123456', duty: '审批并执行各环境已完成前置环节的发布，接手发布告警。' },
]

const ROLE_PATH = {
  ADMIN: ['submit', 'dev', 'dev-ops', 'dev-run', 'test', 'test-qa', 'test-ops', 'test-run', 'prod', 'prod-tl', 'prod-pm', 'prod-ops', 'prod-run'],
  DEV: ['submit', 'dev', 'test', 'prod'],
  TL: ['submit', 'prod', 'prod-tl'],
  QA: ['submit', 'test', 'test-qa'],
  PM: ['submit', 'prod', 'prod-tl', 'prod-pm'],
  OPS: ['dev', 'dev-ops', 'dev-run', 'test', 'test-ops', 'test-run', 'prod', 'prod-ops', 'prod-run'],
}

export default {
  name: "Login",
  data() {
    return {
      role: 'ADMIN',
      remember: true,
      showPass: false,
      loading: false,
      form: { username: 'admin', password: 'admin' },
      roles: ROLES,
    }
  },
  computed: {
    demoText() {
      const item = ROLES.find(v => v.value === this.role)
      return item ? item.text : ''
    },
    activePath() {
      return ROLE_PATH[this.role] || []
    },
    roleDuty() {
      const item = ROLES.find(v => v.value === this.role)
      return item ? item.duty : ''
    },
  },
  created() {
    const savedRole = localStorage.getItem('xm-login-role')
    const savedUser = localStorage.getItem('xm-login-remember')
    const byRole = ROLES.find(v => v.value === savedRole)
    const byName = ROLES.find(v => v.username === savedUser)
    const item = byRole || byName
    if (item) {
      this.role = item.value
      this.form.username = savedUser || item.username
      this.form.password = item.password
    }
    if (savedUser) this.remember = true
  },
  methods: {
    hit(key) {
      return this.activePath.indexOf(key) >= 0
    },
    link(from, to) {
      return this.hit(from) && this.hit(to)
    },
    applyRole() {
      const item = ROLES.find(v => v.value === this.role)
      if (!item) return
      this.form.username = item.username
      this.form.password = item.password
    },
    forgot() {
      this.$message.info('请联系超级管理员重置密码')
    },
    login() {
      if (!this.form.username || !this.form.password) {
        this.$message.warning('请输入用户名和密码')
        return
      }
      this.loading = true
      this.$request.post('/login', this.form).then(res => {
        if (res.code === '200') {
          if (this.remember) {
            localStorage.setItem('xm-login-remember', this.form.username)
          } else {
            localStorage.removeItem('xm-login-remember')
          }
          if (res.data && res.data.role) {
            localStorage.setItem('xm-login-role', res.data.role)
          }
          localStorage.setItem("xm-user", JSON.stringify(res.data))
          this.$router.push('/dashboard')
          this.$message.success('登录成功')
        } else {
          this.$message.error(res.msg)
        }
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #041114;
  color: #e6f4f2;
  overflow: hidden;
  font-family: "PingFang SC", "Microsoft YaHei", -apple-system, Segoe UI, Roboto, sans-serif;
}

.bg { position: fixed; inset: 0; z-index: 0; overflow: hidden; }
.bg::before {
  content: "";
  position: absolute;
  inset: -20%;
  background:
    radial-gradient(38% 38% at 16% 20%, rgba(46, 230, 200, .14), transparent 62%),
    radial-gradient(42% 42% at 84% 16%, rgba(232, 195, 106, .10), transparent 62%),
    radial-gradient(48% 48% at 60% 90%, rgba(61, 139, 255, .10), transparent 62%),
    linear-gradient(160deg, #0b2c34, #041114);
  filter: blur(10px);
  animation: drift 18s ease-in-out infinite alternate;
}

@keyframes drift {
  from { transform: translate3d(-2%, -1%, 0) scale(1.02); }
  to { transform: translate3d(2%, 2%, 0) scale(1.06); }
}

.grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(90, 180, 170, .12) 1px, transparent 1px), linear-gradient(90deg, rgba(90, 180, 170, .12) 1px, transparent 1px);
  background-size: 46px 46px;
  -webkit-mask-image: radial-gradient(circle at 50% 45%, #000 28%, transparent 76%);
  mask-image: radial-gradient(circle at 50% 45%, #000 28%, transparent 76%);
  opacity: .45;
}

.vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 50% 48%, transparent 42%, rgba(4, 17, 20, .55) 100%);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(6px);
  opacity: .35;
  animation: float 14s ease-in-out infinite;
}
.orb.a { width: 140px; height: 140px; left: 8%; top: 72%; background: radial-gradient(circle, #2ee6c8, transparent 70%); }
.orb.b { width: 100px; height: 100px; right: 10%; top: 12%; background: radial-gradient(circle, #e8c36a, transparent 70%); animation-delay: -4s; }
.orb.c { width: 80px; height: 80px; right: 38%; bottom: 10%; background: radial-gradient(circle, #3d8bff, transparent 70%); animation-delay: -8s; }

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-18px); }
}

.shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.panel {
  width: 860px;
  max-width: 100%;
  padding: 20px 20px 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(10, 42, 46, .82), rgba(5, 26, 30, .86));
  border: 1px solid rgba(120, 220, 210, .22);
  backdrop-filter: blur(18px);
  box-shadow:
    0 28px 70px rgba(0, 0, 0, .46),
    0 0 0 1px rgba(46, 230, 200, .06),
    0 0 80px rgba(46, 230, 200, .08);
}

.brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(90, 190, 180, .14);
}

.brand-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  letter-spacing: .4px;
}

.logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2ee6c8, #3d8bff);
  box-shadow: 0 0 16px rgba(46, 230, 200, .38);
  display: grid;
  place-items: center;
  flex-shrink: 0;
}
.logo svg { width: 18px; height: 18px; }

.brand small {
  display: block;
  color: #7ea8a4;
  font-weight: 400;
  font-size: 11px;
  letter-spacing: 2px;
}

.live {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #8fd9cf;
  font-size: 12px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(46, 230, 200, .08);
  border: 1px solid rgba(46, 230, 200, .18);
}
.live i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2ee6c8;
  box-shadow: 0 0 8px #2ee6c8;
  animation: pulse 1.6s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .4; }
}

.main {
  display: grid;
  grid-template-columns: 1fr 352px;
  gap: 0;
  align-items: stretch;
}

.side {
  padding: 8px 22px 6px 8px;
  border-right: 1px solid rgba(90, 190, 180, .12);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.side h2 {
  font-size: 24px;
  line-height: 1.35;
  font-weight: 800;
  letter-spacing: .3px;
}
.side h2 span {
  display: block;
  margin-top: 8px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 1.45;
  background: linear-gradient(135deg, #2ee6c8, #e8c36a);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.flow { margin-top: 16px; }
.flow-head { text-align: center; }
.flow-head::after {
  content: "";
  display: block;
  width: 1px;
  height: 10px;
  margin: 0 auto;
  background: rgba(46, 230, 200, .2);
}
.flow-head.on::after { background: rgba(46, 230, 200, .9); }
.node {
  display: block;
  text-align: center;
  font-size: 12px;
  line-height: 1.2;
  padding: 6px 4px;
  border-radius: 8px;
  color: #e6f4f2;
  background: rgba(46, 230, 200, .1);
  border: 1px solid rgba(46, 230, 200, .22);
  opacity: .38;
  transition: opacity .2s, box-shadow .2s, border-color .2s;
}
.node.on {
  opacity: 1;
  border-color: rgba(46, 230, 200, .75);
  box-shadow: 0 0 14px rgba(46, 230, 200, .28);
}
.node.start {
  display: inline-block;
  min-width: 96px;
  padding: 7px 18px;
  font-weight: 700;
  background: linear-gradient(135deg, rgba(46, 230, 200, .22), rgba(61, 139, 255, .2));
}
.fork {
  height: 14px;
  margin: 0 16.6% 0;
  border-top: 1px solid rgba(46, 230, 200, .2);
  border-left: 1px solid rgba(46, 230, 200, .2);
  border-right: 1px solid rgba(46, 230, 200, .2);
  position: relative;
}
.fork.stem { border-top-color: rgba(46, 230, 200, .9); }
.fork.to-dev { border-left-color: rgba(46, 230, 200, .9); }
.fork.to-prod { border-right-color: rgba(46, 230, 200, .9); }
.fork::before {
  content: "";
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  border-left: 1px solid rgba(46, 230, 200, .2);
}
.fork.to-test::before { border-left-color: rgba(46, 230, 200, .9); }
.lanes {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.lane {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 6px;
  border-radius: 12px;
  background: rgba(3, 16, 18, .35);
  border: 1px solid rgba(90, 190, 180, .14);
}
.lane.on {
  border-color: rgba(46, 230, 200, .5);
  background: rgba(46, 230, 200, .06);
}
.env {
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  color: #e8c36a;
  opacity: .4;
  transition: opacity .2s;
}
.lane.on .env { opacity: 1; }
.steps {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
}
.steps .node { position: relative; }
.steps .node:not(:last-child)::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: -11px;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid rgba(46, 230, 200, .25);
  transform: translateX(-50%);
}
.steps .node.link::after { border-top-color: #2ee6c8; }
.steps::after {
  content: "";
  width: 1px;
  flex: 1;
  min-height: 12px;
  margin: 2px auto 0;
  background: rgba(46, 230, 200, .2);
}
.steps.lit::after { background: rgba(46, 230, 200, .9); }
.node.end.on { box-shadow: 0 0 16px rgba(46, 230, 200, .4); }
.node.end {
  margin-top: auto;
  color: #041114;
  font-weight: 700;
  background: linear-gradient(135deg, #2ee6c8, #3d8bff);
  border-color: transparent;
}

.duty {
  margin: 14px 0 0;
  color: #9ed8d0;
  font-size: 13px;
  line-height: 1.7;
}

.form-box {
  padding: 6px 6px 2px 22px;
}

h1 { font-size: 20px; font-weight: 700; margin-bottom: 4px; color: #e6f4f2; }
.sub { color: #7ea8a4; font-size: 12px; margin-bottom: 14px; }
.label {
  display: block;
  margin: 0 2px 6px;
  font-size: 12px;
  color: #9ed8d0;
}

.field select {
  width: 100%;
  appearance: none;
  -webkit-appearance: none;
  padding: 11px 36px 11px 38px;
  font-size: 14px;
  color: #e6f4f2;
  background: rgba(3, 16, 18, .62);
  border: 1px solid rgba(90, 190, 180, .16);
  border-radius: 10px;
  outline: none;
  cursor: pointer;
}
.field select:focus { border-color: #2ee6c8; box-shadow: 0 0 0 3px rgba(46, 230, 200, .14); }
.field select option { color: #041114; background: #f4fbfa; }
.select-field::after {
  content: "";
  position: absolute;
  right: 14px;
  top: 50%;
  width: 7px;
  height: 7px;
  border-right: 1.5px solid #8ec8c2;
  border-bottom: 1.5px solid #8ec8c2;
  transform: translateY(-70%) rotate(45deg);
  pointer-events: none;
}

.field { position: relative; margin-bottom: 10px; color: #8ec8c2; }
.field svg { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); width: 17px; height: 17px; opacity: .7; }
.field input {
  width: 100%;
  padding: 11px 52px 11px 38px;
  font-size: 14px;
  color: #e6f4f2;
  background: rgba(3, 16, 18, .62);
  border: 1px solid rgba(90, 190, 180, .16);
  border-radius: 10px;
  outline: none;
}
.field input::placeholder { color: #4f7a76; }
.field input:focus { border-color: #2ee6c8; box-shadow: 0 0 0 3px rgba(46, 230, 200, .14); }

.eye {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: none;
  color: #7ea8a4;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 20px;
  font-size: 12.5px;
  color: #7ea8a4;
  margin: 2px 2px 14px;
}
.row label { display: flex; align-items: center; gap: 7px; cursor: pointer; }
.row input { accent-color: #2ee6c8; }
.row a { color: #2ee6c8; text-decoration: none; }

.btn {
  width: 100%;
  padding: 11px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #041114;
  cursor: pointer;
  background: linear-gradient(135deg, #2ee6c8, #3d8bff);
  box-shadow: 0 10px 24px rgba(46, 230, 200, .28);
  letter-spacing: 1px;
}
.btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 14px 30px rgba(46, 230, 200, .4); }
.btn:disabled { opacity: .7; cursor: default; }

.foot { margin-top: 12px; text-align: center; font-size: 12px; color: #7ea8a4; }
.foot b { color: #e8c36a; font-weight: 600; }
.foot a { color: #2ee6c8; text-decoration: none; }

@media (max-width: 860px) {
  .panel { width: min(400px, 100%); }
  .main { grid-template-columns: 1fr; }
  .side { display: none; }
  .form-box { padding: 2px; }
  .live { display: none; }
}
</style>
