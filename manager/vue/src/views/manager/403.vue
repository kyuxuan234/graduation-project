<template>
  <div>
    <div style="height: calc(100vh - 80px); overflow: hidden; display: flex; align-items: center; justify-content: center">
      <div style="font-size: 28px">当前角色不能打开这个页面 <router-link to="/">返回首页</router-link></div>
    </div>
  </div>
</template>

<script>
export default {
  name: "Auth",
  data() {
    return {}
  },
  created() {

  },
  methods: {}
}
</script>

<style scoped>

</style>