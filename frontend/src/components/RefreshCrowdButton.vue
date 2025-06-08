<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const emit = defineEmits(['refresh'])
const loading = ref(false)

const handleRefresh = async () => {
  try {
    loading.value = true
    const response = await fetch('http://localhost:8050/api/osm/refreshCrowdLevel', {
      method: 'POST'
    })
    
    if (!response.ok) {
      throw new Error('刷新失败')
    }
    
    emit('refresh')
    ElMessage.success('拥挤度已更新')
  } catch (error) {
    ElMessage.error('刷新失败: ' + (error as Error).message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-button
    class="refresh-crowd-btn"
    :loading="false"
    circle
    @click="handleRefresh"
  >
    <el-icon class="refresh-icon" :class="{ 'is-loading': loading }">
      <Refresh />
    </el-icon>
  </el-button>
</template>

<style scoped>
.refresh-crowd-btn {
  position: absolute;
  left: 20px;
  bottom: 20px;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.8);
  border: none;
  color: white;
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
  width: 50px;
  height: 50px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.refresh-crowd-btn:hover {
  background: rgba(0, 0, 0, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.3);
}

.refresh-icon {
  font-size: 30px;
  transition: transform 0.3s ease;
}

.refresh-icon.is-loading {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style> 