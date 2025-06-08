<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Loading } from '@element-plus/icons-vue'

interface Service {
  id: number
  name: string
  type: string
  latitude: number
  longitude: number
  distance: number
  icon: string
  actualDistance?: number
  isVerifying?: boolean
}

interface Props {
  visible: boolean
  centerLocation: {
    lat: number
    lng: number
  }
  services: Service[]
}

const props = defineProps<Props>()
const emit = defineEmits(['update:visible', 'select-service'])

// 拖动相关状态
const dialogRef = ref<HTMLElement | null>(null)
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
const initialPosition = ref({ x: 0, y: 0 })

// 筛选相关状态
const selectedTypes = ref<string[]>([])
const serviceTypes = computed(() => {
  const types = new Set(props.services.map(service => service.type))
  return Array.from(types)
})

// 验证服务设施的实际距离
const verifyServiceDistance = async (service: Service) => {
  try {
    service.isVerifying = true
    const response = await fetch('http://localhost:8050/api/route/planMulti', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        startPoint: {
          latitude: props.centerLocation.lat,
          longitude: props.centerLocation.lng
        },
        pathPoints: [{
          latitude: service.latitude,
          longitude: service.longitude
        }],
        allowReturn: false,
        transportMode: 'walking'
      })
    })

    const data = await response.json()
    if (data && data.distancePath) {
      service.actualDistance = data.distancePath.totalDistance
      console.log(`服务设施 ${service.name} 的实际距离: ${service.actualDistance}米`)
    }
  } catch (error) {
    console.error('验证服务设施距离失败:', error)
    ElMessage.error('验证服务设施距离失败')
  } finally {
    service.isVerifying = false
  }
}

// 验证所有服务设施的距离
const verifyAllServices = async () => {
  console.log('开始验证所有服务设施距离')
  const promises = props.services.map(service => verifyServiceDistance(service))
  await Promise.all(promises)
  console.log('所有服务设施距离验证完成')
}

// 筛选后的服务列表
const filteredServices = computed(() => {
  let services = props.services
  if (selectedTypes.value.length > 0) {
    services = services.filter(service => selectedTypes.value.includes(service.type))
  }
  
  // 只显示实际距离在250米内的服务设施
  return services.filter(service => {
    // 如果还没有验证完成，使用直线距离
    if (service.actualDistance === undefined) {
      return service.distance <= 250
    }
    // 如果已经验证完成，使用实际步行距离
    return service.actualDistance <= 250
  })
})

// 添加验证状态
const isVerifying = ref(true)

// 拖动处理函数
const handleMouseDown = (e: MouseEvent) => {
  if (!dialogRef.value) return
  isDragging.value = true
  const rect = dialogRef.value.getBoundingClientRect()
  
  dragOffset.value = {
    x: e.clientX - rect.left,
    y: e.clientY - rect.top
  }
  
  initialPosition.value = {
    x: rect.left,
    y: rect.top
  }
}

const handleMouseMove = (e: MouseEvent) => {
  if (!isDragging.value || !dialogRef.value) return
  
  const x = e.clientX - dragOffset.value.x
  const y = e.clientY - dragOffset.value.y
  
  dialogRef.value.style.left = `${x}px`
  dialogRef.value.style.top = `${y}px`
  dialogRef.value.style.transform = 'none'
}

const handleMouseUp = () => {
  isDragging.value = false
}

// 关闭弹窗
const handleClose = () => {
  emit('update:visible', false)
}

// 选择服务
const handleServiceSelect = (service: Service) => {
  emit('select-service', service)
}

// 添加事件监听
const addEventListeners = () => {
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

// 移除事件监听
const removeEventListeners = () => {
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
}

// 组件挂载时开始验证
onMounted(async () => {
  addEventListeners()
  isVerifying.value = true
  await verifyAllServices()
  isVerifying.value = false
})

onUnmounted(() => {
  removeEventListeners()
})
</script>

<template>
  <div v-if="visible" class="nearby-services-dialog" ref="dialogRef">
    <div class="dialog-header" @mousedown="handleMouseDown">
      <h3>周边服务设施</h3>
      <el-button class="close-button" @click="handleClose">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>
    
    <div class="dialog-content">
      <!-- 类型筛选器 -->
      <div class="type-filter">
        <el-checkbox-group v-model="selectedTypes">
          <el-checkbox 
            v-for="type in serviceTypes" 
            :key="type" 
            :value="type"
          >
            {{ type }}
          </el-checkbox>
        </el-checkbox-group>
      </div>

      <!-- 加载状态 -->
      <div v-if="isVerifying" class="verifying-status">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>正在计算实际步行距离...</span>
      </div>

      <!-- 服务列表 -->
      <div v-else class="services-list">
        <div v-if="filteredServices.length === 0" class="no-services">
          250米范围内没有可用的服务设施
        </div>
        <div 
          v-for="service in filteredServices" 
          :key="service.id"
          class="service-item"
          @click="handleServiceSelect(service)"
        >
          <div class="service-icon" v-html="service.icon"></div>
          <div class="service-info">
            <div class="service-name">{{ service.name }}</div>
            <div class="service-distance">
              <template v-if="service.isVerifying">
                正在计算距离...
              </template>
              <template v-else-if="service.actualDistance !== undefined">
                步行距离: {{ (service.actualDistance).toFixed(0) }}米
              </template>
              <template v-else>
              实际距离: {{ service.distance.toFixed(0) }}米
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.nearby-services-dialog {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 400px;
  background: rgba(0, 0, 0, 0.8);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 2000;
  color: white;
  backdrop-filter: blur(4px);
  transition: none; /* 移除过渡效果，使拖动更流畅 */
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  cursor: move;
}

.dialog-header h3 {
  margin: 0;
  font-size: 18px;
  color: white;
}

.close-button {
  background: none;
  border: none;
  color: white;
  padding: 4px;
  cursor: pointer;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.dialog-content {
  padding: 20px;
  max-height: 500px;
  overflow-y: auto;
}

.type-filter {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.type-filter :deep(.el-checkbox) {
  margin-right: 16px;
  margin-bottom: 8px;
  color: white;
}

.type-filter :deep(.el-checkbox__label) {
  color: white;
}

.services-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.service-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.service-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.service-icon {
  width: 40px;
  height: 40px;
  margin-right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.service-info {
  flex: 1;
}

.service-name {
  font-size: 16px;
  margin-bottom: 4px;
}

.service-distance {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

/* 自定义滚动条样式 */
.dialog-content::-webkit-scrollbar {
  width: 6px;
}

.dialog-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.dialog-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.dialog-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.verifying-status {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: white;
  gap: 10px;
}

.loading-icon {
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

.no-services {
  text-align: center;
  padding: 20px;
  color: white;
  font-size: 16px;
}
</style> 