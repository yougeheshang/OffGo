<script setup lang="ts">
import { ElMessageBox } from 'element-plus'

const emit = defineEmits(['clear'])

const handleClear = () => {
  ElMessageBox.confirm(
    '是否删除所有路径点？',
    '提示',
    {
      confirmButtonText: '是',
      cancelButtonText: '否',
      type: 'warning',
      center: true,
      customClass: 'custom-message-box'
    }
  )
    .then(() => {
      emit('clear')
    })
    .catch(() => {
      // 用户点击取消，不做任何操作
    })
}
</script>

<template>
  <el-button
    type="danger"
    class="clear-path-points-btn"
    @click="handleClear"
  >
    删除所有路径点
  </el-button>
</template>

<style scoped>
.clear-path-points-btn {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  background: rgba(255, 77, 79, 0.8);
  border: none;
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
}

.clear-path-points-btn:hover {
  background: rgba(255, 77, 79, 1);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px 0 rgba(255, 77, 79, 0.3);
}
</style>

<style>
/* 自定义弹窗样式 */
.custom-message-box {
  background: rgba(0, 0, 0, 0.8) !important;
  backdrop-filter: blur(4px);
}

.custom-message-box .el-message-box__title {
  color: white !important;
}

.custom-message-box .el-message-box__content {
  color: white !important;
}

.custom-message-box .el-message-box__btns {
  border-top: 1px solid rgba(255, 255, 255, 0.1) !important;
}

.custom-message-box .el-button {
  background: transparent !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: white !important;
}

.custom-message-box .el-button:hover {
  background: rgba(255, 255, 255, 0.1) !important;
}

.custom-message-box .el-button--primary {
  background: rgba(255, 77, 79, 0.8) !important;
  border: none !important;
}

.custom-message-box .el-button--primary:hover {
  background: rgba(255, 77, 79, 1) !important;
}

/* 修改警告图标颜色 */
.custom-message-box .el-message-box__status {
  color: #ff4d4f !important;
}
</style> 