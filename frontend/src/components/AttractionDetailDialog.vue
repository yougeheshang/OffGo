<script setup lang="ts">
import { Star, View, Location, ZoomOut, Refresh, ZoomIn, Close } from '@element-plus/icons-vue'
import { ref, watch } from 'vue'
import { ElRate, ElMessage } from 'element-plus'

interface TravelCard {
  id: number
  title: string
  description: string
  image: string
  hot: number
  rating: number
  style?: Record<string, string>
  areaType: '校区' | '景区'
}

const props = defineProps<{
  visible: boolean
  attraction: TravelCard | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'go-to-plan', id: number): void
  (e: 'rate', value: number): void
}>()

const handleClose = () => {
  emit('update:visible', false)
}

const handleGoToPlan = (id: number) => {
  emit('go-to-plan', id)
}

const handleImageError = (e: Event) => {
  const imgElement = e.target as HTMLImageElement
  console.error('图片加载失败:', {
    src: imgElement.src,
    alt: imgElement.alt,
    timestamp: new Date().toISOString()
  })
}

const handleImageLoad = (imageUrl: string) => {
  console.log('图片加载成功:', {
    url: imageUrl,
    timestamp: new Date().toISOString()
  })
}

// 图片预览相关
const showImageViewer = ref(false)
const scale = ref(1)
const isDragging = ref(false)
const startX = ref(0)
const startY = ref(0)
const translateX = ref(0)
const translateY = ref(0)

const handleImageClick = () => {
  showImageViewer.value = true
  resetImageState()
}
const resetImageState = () => {
  scale.value = 1
  translateX.value = 0
  translateY.value = 0
}
const handleZoom = (delta: number) => {
  const newScale = scale.value + delta
  if (newScale >= 0.5 && newScale <= 3) {
    scale.value = newScale
  }
}
const handleWheel = (e: WheelEvent) => {
  e.preventDefault()
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  handleZoom(delta)
}
const handleMouseDown = (e: MouseEvent) => {
  isDragging.value = !isDragging.value
  if (isDragging.value) {
    startX.value = e.clientX - translateX.value
    startY.value = e.clientY - translateY.value
  }
}
const handleMouseMove = (e: MouseEvent) => {
  if (isDragging.value) {
    translateX.value = e.clientX - startX.value
    translateY.value = e.clientY - startY.value
  }
}
const handleMouseUp = () => {}
const handleMouseLeave = () => {
  isDragging.value = false
}
const closeImageViewer = () => {
  showImageViewer.value = false
  resetImageState()
  isDragging.value = false
}

const userHasRated = ref(false)
const userRating = ref(0)

const handleUserRate = (value: number) => {
  if (userHasRated.value) {
    ElMessage.info('您已评分，不能重复评分')
    return
  }
  userRating.value = value
  userHasRated.value = true
  emit('rate', value)
}

// 添加评分状态检查
const fetchRatingStatus = async () => {
  if (!props.attraction) return;
  
  const userId = localStorage.getItem('userId');
  if (!userId) {
    userHasRated.value = false;
    userRating.value = 0;
    return;
  }

  try {
    const response = await fetch(`http://localhost:8050/api/attraction/${props.attraction.id}/checkRating?userId=${userId}`);
    if (!response.ok) {
      throw new Error('Failed to fetch rating status');
    }
    const data = await response.json();
    userHasRated.value = data.hasRated;
    userRating.value = data.rating || 0;
  } catch (error) {
    console.error('获取评分状态失败:', error);
    userHasRated.value = false;
    userRating.value = 0;
  }
};

// 监听弹窗显示，获取评分状态
watch(() => props.visible, (newVal) => {
  if (newVal) {
    fetchRatingStatus();
  } else {
    userHasRated.value = false;
    userRating.value = 0;
  }
});
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    width="50%"
    class="attraction-detail-dialog-bg"
    @close="handleClose"
  >
    <div v-if="attraction" class="attraction-detail">
      <div class="attraction-image">
        <img 
          :src="attraction.image" 
          :alt="attraction.title"
          @error="handleImageError"
          @load="handleImageLoad(attraction.image)"
          @click="handleImageClick"
          style="cursor: zoom-in;"
        >
        <div class="area-type-tag" :class="attraction.areaType === '校区' ? 'campus' : 'scenic'">
          {{ attraction.areaType }}
        </div>
      </div>
      
      <div class="attraction-content">
        <div class="attraction-header">
          <h2>{{ attraction.title }}</h2>
          <div class="attraction-stats">
            <span class="stat-item">
              <el-icon><Star /></el-icon>
              评分: {{ attraction.rating?.toFixed(1) || '0.0' }}
            </span>
            <span class="stat-item">
              <el-icon><View /></el-icon>
              热度: {{ attraction.hot }}
            </span>
          </div>
          <div class="attraction-rate-bar" style="margin: 12px 0; text-align: center;">
            <el-rate
              v-model="userRating"
              :disabled="userHasRated"
              @change="handleUserRate"
              :max="5"
              style="font-size: 28px;"
            />
            <span v-if="userHasRated" style="color: #67C23A; margin-left: 10px; font-size: 15px;">已评分</span>
          </div>
        </div>
        
        <div class="attraction-description">
          <h3>景点介绍</h3>
          <p>{{ attraction.description }}</p>
        </div>

        <div class="attraction-actions">
          <el-button 
            type="primary" 
            size="large" 
            class="go-here-btn"
            @click="handleGoToPlan(attraction.id)"
          >
            <el-icon><Location /></el-icon>
            去这里
          </el-button>
        </div>
      </div>
    </div>
    <el-dialog
      v-model="showImageViewer"
      :show-close="false"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      class="image-viewer-dialog"
      @close="closeImageViewer"
    >
      <div class="image-viewer-container" 
        @wheel="handleWheel"
        @mousedown="handleMouseDown"
        @mousemove="handleMouseMove"
        @mouseup="handleMouseUp"
        @mouseleave="handleMouseLeave"
      >
        <img 
          :src="attraction?.image"
          :alt="attraction?.title"
          class="preview-image"
          :style="{
            transform: `scale(${scale}) translate(${translateX}px, ${translateY}px)`,
            cursor: isDragging ? 'grabbing' : 'grab'
          }"
        >
      </div>
      <div class="image-controls">
        <el-button-group>
          <el-button @click="handleZoom(-0.1)" :disabled="scale <= 0.5">
            <el-icon><ZoomOut /></el-icon>
          </el-button>
          <el-button @click="resetImageState">
            <el-icon><Refresh /></el-icon>
          </el-button>
          <el-button @click="handleZoom(0.1)" :disabled="scale >= 3">
            <el-icon><ZoomIn /></el-icon>
          </el-button>
        </el-button-group>
        <span class="zoom-level">{{ Math.round(scale * 100) }}%</span>
        <el-button @click="closeImageViewer">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<style scoped>
.attraction-detail-dialog-bg :deep(.el-dialog) {
  --el-dialog-width: 50% !important;
  max-width: 50% !important;
  width: 50% !important;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18), 0 2px 8px rgba(0, 0, 0, 0.10) !important;
  background: rgba(255, 255, 255, 0.55) !important;
  border: none;
  backdrop-filter: blur(18px) !important;
  -webkit-backdrop-filter: blur(18px) !important;
}

.attraction-detail-dialog-bg :deep(.el-dialog__body) {
  background: transparent !important;
  padding: 0 !important;
}

.attraction-detail-dialog-bg :deep(.el-overlay) {
  background: rgba(0,0,0,0.18) !important;
}

.attraction-image {
  width: 90%;
  max-width: 420px;
  height: 220px;
  overflow: hidden;
  position: relative;
  border-radius: 18px;
  margin: 18px auto 0 auto;
  box-shadow: 0 2px 8px rgba(64,158,255,0.08);
  background: #f6f8fa;
  transition: box-shadow 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.attraction-image::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: linear-gradient(to top, rgba(0,0,0,0.10), transparent);
  pointer-events: none;
  border-radius: 0 0 18px 18px;
}

.attraction-detail-dialog-bg :deep(.attraction-image img) {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
  border-radius: 18px !important;
  display: block;
}

.attraction-content {
  padding: 16px 20px 14px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #ffffff00;
  border-radius: 0 0 24px 24px;
  margin-top: 4px;
  width: 100%;
  align-items: center;
}

.attraction-header {
  text-align: center;
  padding: 0;
  background: none;
  box-shadow: none;
}

.attraction-header h2 {
  font-size: 22px;
  color: #1a1a1a;
  margin: 0 0 8px 0;
  line-height: 1.3;
  font-weight: 700;
  letter-spacing: -0.5px;
  text-align: center;
  text-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.attraction-stats {
  display: flex;
  gap: 12px;
  color: #606266;
  justify-content: center;
  flex-wrap: wrap;
}

.attraction-stats .stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px;
  background: #f8f9fa;
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  font-size: 14px;
}

.attraction-stats .stat-item:hover {
  background: #e6f7ff;
  color: #409EFF;
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(64,158,255,0.15);
}

.attraction-stats .stat-item .el-icon {
  font-size: 18px;
  color: #409EFF;
}

.attraction-description {
  background: #f8f9fa;
  padding: 10px 12px;
  border-radius: 10px;
  margin-top: 2px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  font-size: 14px;
}

.attraction-description:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

.attraction-description h3 {
  font-size: 15px;
  color: #1a1a1a;
  margin: 0 0 6px 0;
  font-weight: 600;
  position: relative;
  padding-left: 12px;
}

.attraction-description h3::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  background: #409EFF;
  border-radius: 2px;
}

.attraction-description p {
  color: #4a4a4a;
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
  letter-spacing: 0.3px;
}

.attraction-actions {
  display: flex;
  justify-content: center;
  margin-top: 6px;
  padding: 0;
  border-top: none;
}

.go-here-btn {
  padding: 8px 18px;
  font-size: 14px;
  border-radius: 10px;
  background: linear-gradient(135deg, #409EFF 0%, #36cfc9 100%);
  border: none;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.25);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 600;
  letter-spacing: 0.5px;
}

.go-here-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.35);
  background: linear-gradient(135deg, #66b1ff 0%, #5cdbd3 100%);
}

.go-here-btn .el-icon {
  margin-right: 8px;
  font-size: 18px;
}

.attraction-detail-dialog-bg .el-dialog__headerbtn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255,255,255,0.9);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  top: 14px;
  right: 14px;
  backdrop-filter: blur(8px);
}

.attraction-detail-dialog-bg .el-dialog__header {
  padding-right: 48px;
}

.attraction-detail-dialog-bg .el-dialog__close {
  color: #409EFF;
  font-size: 22px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.attraction-detail-dialog-bg .el-dialog__headerbtn:hover {
  background: #ffffff;
  box-shadow: 0 6px 16px rgba(0,0,0,0.15);
  transform: scale(1.1) rotate(90deg);
}

.attraction-detail-dialog-bg .el-dialog__close:hover {
  color: #1d7be6;
  transform: scale(1.1) rotate(90deg);
}

@media (max-width: 768px) {
  .attraction-detail-dialog-bg :deep(.el-dialog),
  .attraction-detail-dialog-bg :deep(.el-dialog[role='dialog']) {
    max-width: 98vw !important;
    width: 98vw !important;
    border-radius: 16px;
  }
  .attraction-image {
    width: 98%;
    max-width: 98vw;
    height: 160px;
    border-radius: 12px;
    margin: 10px auto 0 auto;
  }
  .attraction-detail-dialog-bg :deep(.attraction-image img) {
    border-radius: 12px !important;
  }
  .attraction-content {
    padding: 10px 6px 10px 6px;
    border-radius: 0 0 16px 16px;
  }
}

.image-viewer-dialog :deep(.el-dialog) {
  margin: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  max-width: none !important;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 0;
}
.image-viewer-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
}
.image-viewer-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  position: relative;
  padding: 20px;
}
.preview-image {
  max-width: 95%;
  max-height: 95%;
  object-fit: contain;
  transition: transform 0.1s ease;
  user-select: none;
  -webkit-user-drag: none;
}
.image-controls {
  position: fixed;
  top: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(0, 0, 0, 0.5);
  padding: 8px 16px;
  border-radius: 8px;
  z-index: 1000;
}
.zoom-level {
  color: white;
  font-size: 14px;
  min-width: 60px;
  text-align: center;
}
.image-controls .el-button {
  background: transparent;
  border: none;
  color: white;
  padding: 8px;
}
.image-controls .el-button:hover {
  background: rgba(255, 255, 255, 0.1);
}
.image-controls .el-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.area-type-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: white;
  backdrop-filter: blur(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1;
}

.area-type-tag.campus {
  background: linear-gradient(135deg, #409EFF 0%, #36a3ff 100%);
}

.area-type-tag.scenic {
  background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
}
</style> 