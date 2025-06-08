<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import type { Marker } from 'leaflet'

const props = defineProps<{
  map: any
  hasStartPoint: boolean
}>()

const emit = defineEmits(['startPointSelected', 'startPointRemoved', 'hoverLocation'])

const isSelecting = ref(false)
const startPointMarker = ref<any>(null)
const startPointIcon = ref<any>(null)
const buttonState = ref<'normal' | 'selecting' | 'selected'>('normal')

// 监听 hasStartPoint 的变化
watch(() => props.hasStartPoint, (newValue) => {
  if (newValue) {
    buttonState.value = 'selected'
  } else {
    buttonState.value = 'normal'
  }
}, { immediate: true })

// 创建起始点图标
const createStartPointIcon = () => {
  console.log('创建起始点图标')
  const icon = L.divIcon({
    html: `
      <div class="start-point-marker">
        <img src="/public/images/markers/marker-icon-2x-blue.png" style="width: 20px; height: 32px;" />
        <div class="start-point-pulse"></div>
      </div>
    `,
    className: 'start-point-icon',
    iconSize: [20, 32],
    iconAnchor: [10, 32],
    popupAnchor: [0, -32]
  })
  console.log('图标创建完成:', icon)
  return icon
}

// 创建控制按钮图标
const getButtonIcon = () => {
  switch (buttonState.value) {
    case 'selecting':
      return '✓'
    case 'selected':
      return '✕'
    default:
      return '+'
  }
}

// 获取按钮颜色
const getButtonColor = () => {
  switch (buttonState.value) {
    case 'selecting':
      return 'rgba(103, 194, 58, 0.8)'  // 绿色半透明
    case 'selected':
      return 'rgba(245, 108, 108, 0.8)'  // 红色半透明
    default:
      return 'rgba(64, 159, 255, 0.8)'  // 蓝色半透明
  }
}

// 查找最近的兴趣点
const findNearestMarker = (): Marker | null => {
  if (!startPointMarker.value) {
    console.log('起始点标记不存在，无法查找最近兴趣点')
    return null
  }
  
  const center = startPointMarker.value.getLatLng()
  let nearestMarker: Marker | null = null
  let minDistance = Infinity
  
  props.map.eachLayer((layer: any) => {
    if (layer instanceof L.Marker && layer !== startPointMarker.value) {
      const distance = center.distanceTo(layer.getLatLng())
      
      if (distance < 10) { // 10米范围内视为重叠
        if (distance < minDistance) {
          minDistance = distance
          nearestMarker = layer as Marker
          
          // 获取兴趣点数据并触发悬停事件
          const location = (layer as any)._location
          console.log('找到重叠的兴趣点数据:', location)
          
          if (location) {
            // 添加动画效果
            const icon = layer.getIcon()
            if (icon && (icon as L.DivIcon).options) {
              const divIcon = icon as L.DivIcon
              const html = typeof divIcon.options.html === 'string' ? divIcon.options.html : ''
              if (!html.includes('marker-shake')) {
                layer.setIcon(L.divIcon({
                  ...divIcon.options,
                  html: html.replace('custom-marker', 'custom-marker marker-shake')
                }))
              }
            }
            
            emit('hoverLocation', location)
          } else {
            console.log('警告：找到的标记没有关联的兴趣点数据')
          }
        } else {
          // 移除其他标记的动画效果
          const icon = layer.getIcon()
          if (icon && (icon as L.DivIcon).options) {
            const divIcon = icon as L.DivIcon
            const html = typeof divIcon.options.html === 'string' ? divIcon.options.html : ''
            if (html.includes('marker-shake')) {
              layer.setIcon(L.divIcon({
                ...divIcon.options,
                html: html.replace('custom-marker marker-shake', 'custom-marker')
              }))
            }
          }
        }
      } else {
        // 移除超出范围的标记的动画效果
        const icon = layer.getIcon()
        if (icon && (icon as L.DivIcon).options) {
          const divIcon = icon as L.DivIcon
          const html = typeof divIcon.options.html === 'string' ? divIcon.options.html : ''
          if (html.includes('marker-shake')) {
            layer.setIcon(L.divIcon({
              ...divIcon.options,
              html: html.replace('custom-marker marker-shake', 'custom-marker')
            }))
          }
        }
      }
    }
  })
  
  return nearestMarker
}

// 更新标记位置到地图视图中央
const updateMarkerPosition = () => {
  if (!startPointMarker.value || buttonState.value !== 'selecting') {
    return
  }
  const center = props.map.getCenter()
  startPointMarker.value.setLatLng(center)
  
  // 检查是否有最近的兴趣点
  findNearestMarker()
}

// 处理地图缩放
const handleZoom = () => {
  if (buttonState.value === 'selecting') {
    requestAnimationFrame(updateMarkerPosition)
  }
}

// 处理按钮点击
const handleButtonClick = () => {
  if (buttonState.value === 'normal') {
    console.log('开始选择模式')
    // 开始选择模式
    buttonState.value = 'selecting'
    isSelecting.value = true
    ElMessage({
      message: '请选择起始点位置',
      type: 'info',
      duration: 3000
    })
    
    // 清理旧的标记
    if (startPointMarker.value) {
      props.map.removeLayer(startPointMarker.value)
      startPointMarker.value = null
    }
    
    // 创建固定在地图视图中央的标记
    const center = props.map.getCenter()
    console.log('地图中心点:', center)
    
    startPointIcon.value = createStartPointIcon()
    console.log('创建标记，使用图标:', startPointIcon.value)
    
    startPointMarker.value = L.marker(center, {
      icon: startPointIcon.value,
      draggable: false,
      zIndexOffset: 9999,
      isStartPoint: true
    })
    // 添加选择状态标记
    ;(startPointMarker.value as any)._isSelectingStartPoint = true
    console.log('标记创建完成:', startPointMarker.value)
    
    startPointMarker.value.addTo(props.map)
    console.log('标记已添加到地图')
    
    // 监听地图移动和缩放事件
    props.map.on('move', updateMarkerPosition)
    props.map.on('zoomstart', handleZoom)
    props.map.on('zoom', handleZoom)
    
    // 初始更新标记位置
    updateMarkerPosition()
  } else if (buttonState.value === 'selecting') {
    // 确认选择
    buttonState.value = 'selected'
    isSelecting.value = false
    
    // 移除地图事件监听
    props.map.off('move', updateMarkerPosition)
    props.map.off('zoomstart', handleZoom)
    props.map.off('zoom', handleZoom)
    
    // 清除选择状态标记
    if (startPointMarker.value) {
      (startPointMarker.value as any)._isSelectingStartPoint = false
    }
    
    // 检查是否有最近的兴趣点
    const nearestMarker = findNearestMarker()
    
    if (nearestMarker) {
      console.log('找到最近的兴趣点，距离:', nearestMarker.getLatLng().distanceTo(startPointMarker.value.getLatLng()))
      // 移除当前的起始点标记
      if (startPointMarker.value) {
        props.map.removeLayer(startPointMarker.value)
        startPointMarker.value = null
      }
      
      // 将最近的兴趣点设置为起始点
      const position = nearestMarker.getLatLng()
      emit('startPointSelected', {
        latitude: position.lat,
        longitude: position.lng,
        isExistingPoint: true,
        markerId: (nearestMarker as any)._leaflet_id
      })
      
      ElMessage({
        message: '已自动选择最近的兴趣点作为起始点',
        type: 'success',
        duration: 3000
      })
    } else {
      // 如果没有找到最近的兴趣点，使用当前位置
      const position = startPointMarker.value.getLatLng()
      emit('startPointSelected', {
        latitude: position.lat,
        longitude: position.lng,
        isExistingPoint: false
      })
      
      ElMessage({
        message: '起始点设置成功',
        type: 'success',
        duration: 3000
      })
    }
  } else {
    // 删除起始点
    buttonState.value = 'normal'
    if (startPointMarker.value) {
      props.map.removeLayer(startPointMarker.value)
      startPointMarker.value = null
    }
    // 移除地图事件监听
    props.map.off('move', updateMarkerPosition)
    props.map.off('zoomstart', handleZoom)
    props.map.off('zoom', handleZoom)
    ElMessage({
      message: '起始点已删除',
      type: 'info',
      duration: 3000
    })
    emit('startPointRemoved')
  }
}

// 组件卸载时清理
onUnmounted(() => {
  if (startPointMarker.value) {
    props.map.removeLayer(startPointMarker.value)
  }
  props.map.off('move', updateMarkerPosition)
  props.map.off('zoomstart', handleZoom)
  props.map.off('zoom', handleZoom)
})

// 组件挂载时清理之前的起始点
onMounted(() => {
  // 移除所有现有的起始点标记
  props.map.eachLayer((layer: any) => {
    if (layer instanceof L.Marker && layer.options.isStartPoint) {
      props.map.removeLayer(layer)
    }
  })
  
  // 重置状态
  buttonState.value = 'normal'
  isSelecting.value = false
  startPointMarker.value = null
})
</script>

<template>
  <div class="start-point-selector">
    <button 
      class="control-button"
      :style="{ backgroundColor: getButtonColor() }"
      @click="handleButtonClick"
    >
      {{ getButtonIcon() }}
    </button>
  </div>
</template>

<style scoped>
.start-point-selector {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1000;
}

.control-button {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  backdrop-filter: blur(4px);
}

.control-button:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.2);
}

.start-point-marker {
  position: relative;
  transform-origin: bottom center;
}

.start-point-pulse {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background: rgba(0, 123, 255, 0.3);
  border-radius: 50%;
  z-index: -1;
  animation: pulse 2s ease-out infinite;
}

@keyframes pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(2);
    opacity: 0;
  }
}
</style> 