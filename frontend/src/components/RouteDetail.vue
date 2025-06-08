<script setup lang="ts">
import { Location, Timer, Position, Flag, CirclePlus, Van, Bicycle, User } from '@element-plus/icons-vue'
import { computed, defineEmits, ref, watch } from 'vue'

interface RoutePoint {
  name: string
  type: string
  latitude: number
  longitude: number
  description?: string
  isElectricStart?: boolean  // 电动车起点
  isElectricEnd?: boolean    // 电动车终点
}

interface Props {
  visible: boolean
  routePoints: RoutePoint[]
  distance: number
  time: number
  transportMode: string
  areaType?: '校区' | '景区'  // 添加区域类型属性
}

const props = defineProps<Props>()

const emit = defineEmits(['nodeClick', 'transportModeChange'])

const formatDistance = (distance: number) => {
  return `${(distance / 1000).toFixed(2)} 公里`
}

const formatTime = (time: number) => {
  return `${time.toFixed(2)} 分钟`
}

// 获取站点图标
const getStationIcon = (point: RoutePoint, index: number) => {
  if (index === 0) return Location
  if (index === props.routePoints.length - 1) return Flag
  if (point.isElectricStart || point.isElectricEnd) return Van
  return CirclePlus
}

// 获取站点类型显示
const getStationType = (point: RoutePoint, index: number) => {
  if (index === 0) return '起点'
  if (index === props.routePoints.length - 1) return '终点'
  if (point.isElectricStart) return '电动车上车点'
  if (point.isElectricEnd) return '电动车下车点'
  return '途经点'
}

// 动态计算节点数量和间距
const nodeCount = computed(() => props.routePoints.length)
const nodeGap = computed(() => {
  // 根据节点数量动态调整间距，确保总高度不超过400px
  const maxHeight = 400
  const minGap = 40 // 最小间距
  const maxGap = 80 // 最大间距
  const calculatedGap = maxHeight / nodeCount.value
  return Math.min(Math.max(calculatedGap, minGap), maxGap)
})

const handleNodeClick = (point: RoutePoint) => {
  emit('nodeClick', point)
}

// 根据区域类型获取可用的交通方式
const availableTransportModes = computed(() => {
  if (props.areaType === '校区') {
    return [
      { mode: 'walking', label: '步行', icon: User },
      { mode: 'bicycle', label: '自行车', icon: Bicycle }
    ]
  } else if (props.areaType === '景区') {
    return [
      { mode: 'walking', label: '步行', icon: User },
      { mode: 'electric', label: '电动车', icon: Van }
    ]
  } else {
    // 默认提供所有交通方式
    return [
      { mode: 'walking', label: '步行', icon: User },
      { mode: 'bicycle', label: '自行车', icon: Bicycle },
      { mode: 'electric', label: '电动车', icon: Van }
    ]
  }
})

// 修改交通方式相关的状态
const showTransportMenu = ref(false)

const handleTransportSelect = (mode: string) => {
  console.log('[RouteDetail] Transport mode changed:', mode);
  showTransportMenu.value = false;
  emit('transportModeChange', mode);
}

// 监听交通方式变化
watch(() => props.transportMode, (newMode) => {
  console.log('[RouteDetail] Transport mode updated:', newMode);
  if (newMode === 'electric') {
    console.log('[RouteDetail] Electric mode activated, route points:', props.routePoints);
    // 检查是否有电动车上下车点
    const hasElectricPoints = props.routePoints.some(point => point.isElectricStart || point.isElectricEnd);
    console.log('[RouteDetail] Has electric points:', hasElectricPoints);
  }
});

// 监听路径点变化
watch(() => props.routePoints, (newPoints) => {
  console.log('[RouteDetail] Route points updated:', newPoints);
  if (props.transportMode === 'electric') {
    console.log('[RouteDetail] Checking electric points in new route');
    newPoints.forEach((point, index) => {
      console.log(`[RouteDetail] Point ${index}:`, {
        name: point.name,
        type: point.type,
        isElectricStart: point.isElectricStart,
        isElectricEnd: point.isElectricEnd
      });
    });
  }
}, { deep: true });

// 获取当前交通方式的图标
const getCurrentTransportIcon = computed(() => {
  const currentMode = availableTransportModes.value.find(m => m.mode === props.transportMode)
  return currentMode?.icon || User
})

// 获取当前交通方式的标签
const getCurrentTransportLabel = computed(() => {
  const currentMode = availableTransportModes.value.find(m => m.mode === props.transportMode)
  return currentMode?.label || '步行'
})
</script>

<template>
  <div class="route-detail" v-if="visible">
    <div class="route-header">
      <div class="header-top" style="position: relative;">
      <h3>行程路线</h3>
        <div class="transport-selector" @click="showTransportMenu = !showTransportMenu" style="position: relative; z-index: 2010;">
          <div class="transport-mode">
            <el-icon class="transport-icon">
              <component :is="getCurrentTransportIcon" />
            </el-icon>
            <span>{{ getCurrentTransportLabel }}</span>
          </div>
          <el-icon class="arrow-icon" :class="{ 'is-open': showTransportMenu }">
            <Position />
          </el-icon>
          <div class="transport-menu" v-show="showTransportMenu">
            <div 
              v-for="mode in availableTransportModes" 
              :key="mode.mode"
              class="menu-item" 
              :class="{ active: transportMode === mode.mode }" 
              @click.stop="handleTransportSelect(mode.mode)"
            >
              <el-icon><component :is="mode.icon" /></el-icon>
              <span>{{ mode.label }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="route-summary">
        <div class="summary-item">
          <el-icon><Location /></el-icon>
          <span>总距离：{{ formatDistance(distance) }}</span>
        </div>
        <div class="summary-item">
          <el-icon><Timer /></el-icon>
          <span>预计时间：{{ formatTime(time) }}</span>
        </div>
        <!-- 添加电动车模式特定信息 -->
        <template v-if="transportMode === 'electric'">
          <div class="summary-item" v-if="routePoints.some(p => p.isElectricStart)">
            <el-icon><Van /></el-icon>
            <span>电动车起点已设置</span>
          </div>
          <div class="summary-item" v-if="routePoints.some(p => p.isElectricEnd)">
            <el-icon><Van /></el-icon>
            <span>电动车终点已设置</span>
          </div>
        </template>
      </div>
    </div>
    
    <div class="route-map">
      <div class="metro-demo">
        <div class="metro-line">
          <div class="metro-vertical"></div>
          <template v-if="routePoints && routePoints.length > 0">
            <div
              v-for="(point, index) in routePoints"
              :key="index"
              class="metro-node"
              @click="handleNodeClick(point)"
            >
              <span class="metro-dot" :class="{
                'electric-start': point.isElectricStart,
                'electric-end': point.isElectricEnd
              }"></span>
              <div class="station-content" :class="{
                'electric-start': point.isElectricStart,
                'electric-end': point.isElectricEnd
              }">
                <el-icon class="station-icon" :size="20">
                  <component :is="getStationIcon(point, index)" />
                </el-icon>
                <div class="station-info">
                  <div class="station-name">{{ point.name }}</div>
                  <div class="station-type">{{ getStationType(point, index) }}</div>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="no-points">
            暂无路径点
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.route-detail {
  position: fixed;
  right: 20px;
  top: 120px;
  width: 320px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  border-radius: 8px;
  padding: 20px;
  backdrop-filter: blur(4px);
  z-index: 1000;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.route-header {
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 15px;
}

.route-header h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #fff;
}

.route-summary {
  display: flex;
  gap: 15px;
  margin-top: 10px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #ccc;
}

.summary-item .el-icon {
  color: #ff0000;
}

.route-map {
  position: relative;
  padding: 10px 0;
}

.metro-demo {
  width: 100%;
  min-height: 100px;
  max-height: 400px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  position: relative;
  padding: 10px 0;
  overflow-y: auto;
}

.metro-line {
  position: relative;
  width: 100%;
  min-height: 100px;
  padding-left: 20px;
}

.metro-vertical {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #ff0000;
  z-index: 0;
  border-radius: 2px;
}

.metro-node {
  position: relative;
  width: 100%;
  height: 55px;
  display: flex;
  align-items: center;
  z-index: 1;
  margin-bottom: 20px;
  padding: 5px 0;
}

.metro-node:last-child {
  margin-bottom: 0;
}

.station-content {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 30px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  transition: all 0.3s ease;
  flex: 1;
}

.station-content:hover {
  background: rgba(255, 255, 255, 0.2);
}

.station-icon {
  color: #ff0000;
  transition: all 0.3s ease;
}

.station-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.station-name {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  transition: all 0.3s ease;
}

.station-type {
  font-size: 12px;
  color: #ccc;
}

.metro-dot {
  position: absolute;
  left: -20px;
  width: 20px;
  height: 20px;
  border: 3px solid #ff0000;
  border-radius: 50%;
  background: #fff;
  box-sizing: border-box;
  z-index: 2;
  animation: metro-pulse 1.5s infinite;
}

@keyframes metro-pulse {
  0% { box-shadow: 0 0 0 0 rgba(255,0,0,0.4);}
  70% { box-shadow: 0 0 0 10px rgba(255,0,0,0);}
  100% { box-shadow: 0 0 0 0 rgba(255,0,0,0);}
}

/* 动画效果 */
.route-detail {
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 站点悬停效果 */
.station-content:hover .station-icon {
  transform: scale(1.1);
  box-shadow: 0 0 10px rgba(255, 0, 0, 0.5);
}

.station-content:hover .station-name {
  color: #ff0000;
}

/* 滚动条样式 */
.route-map {
  max-height: calc(100vh - 250px);
  overflow-y: auto;
  padding-right: 10px;
}

.route-map::-webkit-scrollbar {
  width: 4px;
}

.route-map::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}

.no-points {
  color: #ccc;
  text-align: center;
  padding: 20px;
  font-size: 14px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.transport-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  z-index: 2010;
}

.transport-selector:hover {
  background: rgba(255, 255, 255, 0.2);
}

.transport-mode {
  display: flex;
  align-items: center;
  gap: 6px;
}

.transport-icon {
  font-size: 16px;
  color: #ff0000;
}

.arrow-icon {
  font-size: 12px;
  color: #ccc;
  transition: transform 0.3s ease;
}

.arrow-icon.is-open {
  transform: rotate(90deg);
}

.transport-menu {
  position: absolute;
  top: 100%;
  left: 0;
  width: 120px;
  background: rgba(0, 0, 0, 0.95);
  border-radius: 4px;
  padding: 8px 0;
  z-index: 2020;
  animation: slideDown 0.3s ease-out;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.menu-item.active {
  color: #ff0000;
}

.menu-item .el-icon {
  font-size: 16px;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 添加电动车相关样式 */
.metro-dot.electric-start {
  border-color: #FF4500;
  background: #FF4500;
}

.metro-dot.electric-end {
  border-color: #FF4500;
  background: #FF4500;
}

.station-content.electric-start {
  border-left: 3px solid #FF4500;
}

.station-content.electric-end {
  border-left: 3px solid #FF4500;
}

/* 电动车模式特定样式 */
.transport-mode.electric {
  color: #FF4500;
}

.transport-icon.electric {
  color: #FF4500;
}
</style> 