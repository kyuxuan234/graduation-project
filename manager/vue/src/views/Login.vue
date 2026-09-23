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
            <div>DevOps 发布管理系统<small>RELEASE & OPS</small></div>
          </div>
          <div class="live"><i></i>系统在线</div>
        </div>

        <div class="main">
          <div class="side">
            <h2>按发布链路管控<br><span>项目 → 应用 → 部署 → 告警</span></h2>
            <p>开发提交发布申请，运维审批并自动或手动执行；探活与告警在同一控制台闭环。</p>
            <div class="tags">
              <span>发布申请</span>
              <span>审批执行</span>
              <span>自动部署</span>
              <span>告警闭环</span>
            </div>
            <div class="stats">
              <div class="stat"><b>3</b><em>角色协同</em></div>
              <div class="stat"><b>2</b><em>执行方式</em></div>
              <div class="stat"><b>1</b><em>发布看板</em></div>
            </div>
          </div>

          <div class="form-box">
            <h1>欢迎登录</h1>
            <div class="sub">当前身份：{{ roleName }}，请输入账号信息</div>

            <div class="roles">
              <button v-for="item in roles" :key="item.value" type="button" class="role" :class="{ active: role === item.value }" @click="pickRole(item)">
                {{ item.name }}
              </button>
            </div>

            <div class="field">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="4"></circle><path d="M4 21c0-4 4-6 8-6s8 2 8 6"></path></svg>
              <input v-model="form.username" placeholder="用户名 / 手机号" @keyup.enter="login">
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
              演示账号：<b>{{ demoText }}</b>
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
  { value: 'ADMIN', name: '超级管理员', username: 'admin', password: 'admin', text: 'admin / admin' },
  { value: 'DEV', name: '开发', username: 'dev1', password: '123456', text: 'dev1 / 123456' },
  { value: 'OPS', name: '运维', username: 'ops1', password: '123456', text: 'ops1 / 123456' },
]

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
    roleName() {
      const item = ROLES.find(v => v.value === this.role)
      return item ? item.name : ''
    },
    demoText() {
      const item = ROLES.find(v => v.value === this.role)
      return item ? item.text : ''
    }
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
    pickRole(item) {
      this.role = item.value
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
  background: linear-gradient(135deg, #2ee6c8, #e8c36a);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.side p {
  color: #7ea8a4;
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.7;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
}
.tags span {
  font-size: 11px;
  color: #9ed8d0;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(46, 230, 200, .08);
  border: 1px solid rgba(46, 230, 200, .14);
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 16px;
}
.stat {
  padding: 10px 8px;
  border-radius: 10px;
  background: rgba(3, 16, 18, .35);
  border: 1px solid rgba(90, 190, 180, .12);
  text-align: center;
}
.stat b {
  display: block;
  font-size: 20px;
  font-weight: 800;
  color: #2ee6c8;
  line-height: 1;
}
.stat em {
  display: block;
  margin-top: 5px;
  font-style: normal;
  font-size: 11px;
  color: #7ea8a4;
}

.form-box {
  padding: 6px 6px 2px 22px;
}

h1 { font-size: 20px; font-weight: 700; margin-bottom: 4px; color: #e6f4f2; }
.sub { color: #7ea8a4; font-size: 12px; margin-bottom: 14px; }

.roles {
  display: flex;
  gap: 6px;
  margin-bottom: 14px;
  background: rgba(3, 16, 18, .55);
  padding: 4px;
  border-radius: 11px;
  border: 1px solid rgba(90, 190, 180, .14);
}

.role {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  font-size: 13px;
  border-radius: 8px;
  cursor: pointer;
  color: #7ea8a4;
  transition: .22s;
  background: transparent;
  border: none;
}
.role.active {
  color: #041114;
  font-weight: 600;
  background: linear-gradient(135deg, #2ee6c8, #3d8bff);
  box-shadow: 0 6px 16px rgba(46, 230, 200, .28);
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
