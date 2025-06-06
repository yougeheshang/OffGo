<script setup lang="ts">
import { ref } from 'vue'
import LoginRegisterForm from './LoginRegisterForm.vue'

const showDialog = ref(false)
const handleLoginClick = () => {
  showDialog.value = true
}
const handleDialogClose = () => {
  showDialog.value = false
}
</script>

<template>
  <div class="user-panel">
    <el-button type="primary" size="small" @click="handleLoginClick">登录</el-button>
    <el-dialog v-model="showDialog" title="用户登录/注册" width="350px" @close="handleDialogClose">
      <LoginRegisterForm @close="handleDialogClose" />
    </el-dialog>
  </div>
</template>

<style scoped>
  .user-panel {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: #eee;
    display: flex;
    align-items: center;
    justify-content: center;
  }
</style>
