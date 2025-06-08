<script setup lang="ts">
import { Star, View, Location, VideoCamera, ZoomOut, Refresh, ZoomIn, Close } from '@element-plus/icons-vue'
import { ref, computed, watch } from 'vue'
import { ElRate, ElMessage } from 'element-plus'

const props = defineProps<{
  visible: boolean
  diary: any
  videoUrl?: string | null
  imageUrls?: Record<number, string>
  userRating?: number
  hasRated?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'go-to-plan', id: number): void
  (e: 'rate', value: number): void
  (e: 'rating-updated', value: { diaryId: number, newRating: number }): void
}>()

const handleClose = () => {
  emit('update:visible', false)
}

const handleRate = (value: number) => {
  emit('rate', value)
}

// 多媒体切换
const showType = ref<'image' | 'video'>('image')
const hasVideo = computed(() => !!props.videoUrl)
const imageIdList = computed(() => props.diary?.image ? props.diary.image.split(',').map((id:string) => parseInt(id)) : [])

// 切换多媒体
const switchToImage = () => { showType.value = 'image' }
const switchToVideo = () => { showType.value = 'video' }

// 图片预览相关
const showImageViewer = ref(false)
const currentImageIndex = ref(0)
const scale = ref(1)
const isDragging = ref(false)
const startX = ref(0)
const startY = ref(0)
const translateX = ref(0)
const translateY = ref(0)

const handleImageClick = (index: number) => {
  currentImageIndex.value = index
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
const imageUrlList = computed(() => {
  if (!props.diary || !props.imageUrls) return []
  if (!props.diary.image) return []
  return props.diary.image.split(',').map((id:string) => props.imageUrls?.[parseInt(id)]).filter(Boolean)
})

const userHasRated = ref(props.hasRated || false)
const userRating = ref(props.userRating || 0)

// 添加评分状态检查
const fetchRatingStatus = async () => {
  if (!props.diary) return;
  
  const userId = localStorage.getItem('userId');
  if (!userId) {
    userHasRated.value = false;
    userRating.value = 0;
    return;
  }

  try {
    console.log('检查评分状态:', {
      diaryId: props.diary.id,
      userId: userId
    });

    const response = await fetch(`http://localhost:8050/api/diary/get/diarydata/${props.diary.id}/checkRating?userId=${userId}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch rating status: ${response.status}`);
    }
    const data = await response.json();
    console.log('评分状态响应:', data);
    
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

const handleUserRate = async (value: number) => {
  if (userHasRated.value) {
    ElMessage.info('您已评分，不能重复评分')
    return
  }

  const userId = localStorage.getItem('userId');
  if (!userId) {
    ElMessage.warning('请先登录后再评分');
    return;
  }

  try {
    console.log('提交评分:', {
      diaryId: props.diary.id,
      userId: userId,
      rating: value
    });

    const response = await fetch('http://localhost:8050/api/diary/rate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        diaryId: props.diary.id,
        userId: parseInt(userId),
        rating: value
      })
    });

    if (!response.ok) {
      const errorData = await response.text();
      throw new Error(`评分失败: ${errorData}`);
    }

    const data = await response.json();
    console.log('评分响应:', data);

    userHasRated.value = true;
    userRating.value = value;
    props.diary.rating = data.newRating;
    
    // 发送评分更新事件
    emit('rating-updated', {
      diaryId: props.diary.id,
      newRating: data.newRating
    });
    
    ElMessage.success('评分成功！');
  } catch (error) {
    console.error('评分失败:', error);
    ElMessage.error(error instanceof Error ? error.message : '评分失败，请稍后重试');
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    width="700px"
    class="diary-detail-dialog-bg"
    @close="handleClose"
  >
    <div v-if="diary" class="diary-detail">
      <div class="media-switch-bar" v-if="(imageIdList.length > 0 && imageUrls) || hasVideo">
        <el-button
          v-if="imageIdList.length > 0 && imageUrls"
          :type="showType === 'image' ? 'primary' : 'default'"
          size="small"
          @click="switchToImage"
          >图片</el-button>
        <el-button
          v-if="hasVideo"
          :type="showType === 'video' ? 'primary' : 'default'"
          size="small"
          @click="switchToVideo"
        >视频</el-button>
      </div>
      <div class="diary-media-area">
        <el-carousel
          v-if="showType === 'image' && imageIdList.length > 0 && imageUrls"
          height="260px"
          indicator-position="outside"
          arrow="always"
          class="diary-carousel"
        >
          <el-carousel-item v-for="(imgId, idx) in imageIdList" :key="imgId">
            <img
              :src="imageUrls[imgId]"
              :alt="diary.title"
              class="carousel-image"
              @click="handleImageClick(idx)"
              style="cursor: zoom-in;"
            >
          </el-carousel-item>
        </el-carousel>
        <div v-if="showType === 'video' && hasVideo" class="diary-video-area">
          <video
            v-if="videoUrl"
            :src="videoUrl"
            controls
            class="video-player"
            :poster="imageUrls && imageIdList.length > 0 ? imageUrls[imageIdList[0]] : ''"
          />
        </div>
      </div>
      <div class="diary-content">
        <div class="diary-header">
          <h2>{{ diary.title }}</h2>
          <div class="diary-meta">
            <span class="destination">
              <el-icon><Location /></el-icon>
              {{ diary.destination }}
            </span>
            <span class="hot">
              <el-icon><View /></el-icon>
              热度: {{ diary.hot }}
            </span>
            <span class="stat-item">
              <el-icon><Star /></el-icon>
              评分: {{ diary.rating?.toFixed(1) || '0.0' }}
            </span>
          </div>
          <div class="diary-rate-bar" style="margin: 12px 0; text-align: center;">
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
        <div class="diary-description">
          <h3>简介</h3>
          <p>{{ diary.description }}</p>
        </div>
        <div class="diary-main-content">
          <h3>正文</h3>
          <p style="white-space: pre-wrap;">{{ diary.content }}</p>
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
          v-if="imageUrlList.length > 0"
          :src="imageUrlList[currentImageIndex]"
          :alt="diary.title"
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
        <el-button-group v-if="imageUrlList.length > 1">
          <el-button @click="currentImageIndex = (currentImageIndex - 1 + imageUrlList.length) % imageUrlList.length">
            上一张
          </el-button>
          <el-button @click="currentImageIndex = (currentImageIndex + 1) % imageUrlList.length">
            下一张
          </el-button>
        </el-button-group>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<style scoped>
.diary-detail-dialog-bg :deep(.el-dialog) {
  max-width: 700px;
  margin: 4vh auto;
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.16), 0 4px 12px rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.78);
  border: none;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}
.diary-detail {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #ffffff00;
}
.media-switch-bar {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin: 10px 0 0 0;
}
.diary-media-area {
  width: 92%;
  max-width: 520px;
  min-height: 260px;
  margin: 18px auto 0 auto;
  border-radius: 18px;
  background: #f6f8fa;
  box-shadow: 0 4px 16px rgba(64,158,255,0.08);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.diary-carousel {
  width: 100%;
  height: 260px;
  background: transparent;
}
.carousel-image {
  width: 100%;
  height: 260px;
  object-fit: cover;
  border-radius: 18px;
  display: block;
  background: #f6f8fa;
}
.diary-video-area {
  width: 100%;
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
  border-radius: 18px;
  overflow: hidden;
}
.video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
  border-radius: 18px;
}
.diary-content {
  padding: 24px 28px 18px 28px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background: #ffffff00;
  border-radius: 0 0 28px 28px;
  margin-top: 8px;
  width: 100%;
  align-items: center;
}
.diary-header h2 {
  font-size: 26px;
  margin: 0 0 12px 0;
}
.diary-meta {
  display: flex;
  gap: 12px;
  color: #606266;
  justify-content: center;
  flex-wrap: wrap;
}
.diary-meta .destination, .diary-meta .hot, .diary-meta .stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: #f8f9fa;
  border-radius: 14px;
  font-size: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.diary-description, .diary-main-content {
  background: #f8f9fa;
  padding: 16px 18px;
  border-radius: 14px;
  margin-top: 4px;
  font-size: 15px;
  width: 100%;
}
.diary-description h3, .diary-main-content h3 {
  font-size: 17px;
  margin-bottom: 8px;
}
.diary-actions {
  margin-top: 10px;
  padding: 0;
  border-top: none;
}
.go-here-btn {
  padding: 10px 28px;
  font-size: 15px;
  border-radius: 14px;
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
</style> 