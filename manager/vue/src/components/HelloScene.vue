<template>
  <div class="hello-scene">
    <div class="hello-title">HELLO.</div>
    <img src="@/assets/imgs/hello-scene.png" alt="" />
    <div class="brand">
      <span class="logo"><i class="el-icon-monitor"></i></span>
      <span>DevOps 运维管理系统</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HelloScene'
}
</script>

<style scoped>
.hello-scene {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.hello-title {
  font-size: 42px;
  font-weight: 700;
  color: #6b7280;
  letter-spacing: 1px;
  line-height: 1;
  margin-bottom: 2px;
}

.hello-scene img {
  width: 300px;
  display: block;
}

.brand {
  display: flex;
  align-items: center;
  margin-top: 2px;
  color: #4b5563;
  font-size: 14px;
  font-weight: 600;
}

.logo {
  width: 24px;
  height: 24px;
  margin-right: 8px;
  border-radius: 50%;
  background: #2b7bff;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}
</style>
