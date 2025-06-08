<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import NearbyServicesDialog from './NearbyServicesDialog.vue'

interface Location {
  name: string
  description: string
  type: string
  vectorIcon: string
  latitude: number
  longitude: number
  coordinates?: {
    lat: number
    lng: number
  }
}

const props = defineProps<{
  location: Location | null
  visible: boolean
  hasStartPoint?: boolean
  isPathPoint?: boolean
  isStartPoint?: boolean
}>()

const emit = defineEmits(['goToLocation', 'deleteLocation', 'startPointRemoved', 'showNearbyServices'])

// 控制周边服务设施对话框的显示
const showNearbyServicesDialog = ref(false)

const handleGoClick = () => {
  if (!props.hasStartPoint) {
    ElMessage.warning('请先选择起始点')
    return
  }
  ElMessage({
    message: '已添加为路径点',
    type: 'success',
    duration: 3000
  })
  emit('goToLocation', props.location)
}

const handleDeleteClick = () => {
  if (props.isStartPoint) {
    ElMessage({
      message: '起始点已删除',
      type: 'info',
      duration: 3000
    })
    emit('startPointRemoved')
  } else if (props.isPathPoint) {
    ElMessage({
      message: '路径点已删除',
      type: 'info',
      duration: 3000
    })
  }
  emit('deleteLocation', props.location)
}

const handleShowNearbyServices = () => {
  if (!props.location) {
    ElMessage.warning('未选择位置')
    return
  }
  
  // 检查是否有经纬度信息
  if (!props.location.latitude || !props.location.longitude) {
    ElMessage.warning('该位置缺少经纬度信息')
    return
  }
  
  emit('showNearbyServices', {
    latitude: props.location.latitude,
    longitude: props.location.longitude
  })
}

const handleServiceSelect = (service: any) => {
  emit('showNearbyServices', service)
}
</script>

<template>
  <div v-if="visible" class="floating-window">
    <div class="floating-window-content">
      <template v-if="location">
        <h2 class="location-title">{{ location.name }}</h2>
        <div class="vector-icon" v-html="location.vectorIcon"></div>
        <p class="location-description">{{ location.description }}</p>
        <div class="button-group">
          <template v-if="isPathPoint || isStartPoint">
            <el-button
              type="danger"
              class="action-button"
              @click="handleDeleteClick"
            >删除</el-button>
          </template>
          <template v-else>
            <el-button
              type="primary"
              class="action-button"
              @click="handleGoClick"
            >去这里</el-button>
            <el-button
              type="info"
              class="action-button"
              @click="handleShowNearbyServices"
            >周围服务设施</el-button>
          </template>
        </div>
      </template>
      <template v-else>
        <div class="empty-box">
          <svg viewBox="0 0 200 200" class="empty-box-svg">
            <rect x="20" y="20" width="160" height="160" fill="none" stroke="#000000" stroke-width="2" stroke-dasharray="5,5"/>
            <path d="M40 40 L160 40 L160 160 L40 160 Z" fill="none" stroke="#000000" stroke-width="2"/>
            <circle cx="100" cy="100" r="30" fill="none" stroke="#000000" stroke-width="2"/>
            <path d="M70 100 L130 100 M100 70 L100 130" stroke="#000000" stroke-width="2"/>
          </svg>
        </div>
        <p class="empty-message">去选择一个兴趣点吧！</p>
      </template>
    </div>
  </div>

  <NearbyServicesDialog
    v-model:visible="showNearbyServicesDialog"
    :center-location="location?.coordinates || { lat: 0, lng: 0 }"
    :services="[]"
    @select-service="handleServiceSelect"
  />
</template>

<style scoped>
.floating-window {
  position: absolute;
  top: 120px;
  right: 30px;
  width: 250px;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 1000;
  overflow: hidden;
  color: white;
}

.floating-window-content {
  padding: 20px;
}

.location-title {
  margin: 0 0 16px 0;
  font-size: 20px;
  color: white;
  text-align: center;
}

.vector-icon {
  display: flex;
  justify-content: center;
  margin: 20px 0;
  color: white;
}

.location-description {
  margin: 16px 0;
  color: white;
  font-size: 14px;
  line-height: 1.6;
  text-align: center;
}

.button-group {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.action-button {
  flex: 1;
  margin: 0;
  background-color: #1890ff;
  color: white;
}

.action-button.el-button--danger {
  background-color: #F56C6C;
  color: white;
}

.action-button.el-button--info {
  background-color: #909399;
  color: white;
}

.empty-box {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}

.empty-box-svg {
  width: 150px;
  height: 150px;
  opacity: 0.5;
  animation: rotate 20s linear infinite;
}

.empty-message {
  text-align: center;
  color: white;
  font-size: 16px;
  margin: 20px 0;
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