<template>
  <div class="diary-container">
    <!-- 列表模式 -->
    <div v-if="!route.params.id" class="diary-list">
      <div class="diary-grid">
        <div 
          v-for="diary in diaries" 
          :key="diary.id"
          class="diary-card"
          @click="goToDetail(diary.id)"
        >
          <div class="diary-card-header">
            <h3>{{ diary.title }}</h3>
            <div class="diary-meta">
              <span class="time">{{ formatTime(diary.createTime) }}</span>
              <span class="stats">
                <el-icon><View /></el-icon> {{ diary.hot || 0 }}
              </span>
            </div>
          </div>
          <div class="diary-card-content">
            <p>{{ diary.content }}</p>
          </div>
          <div class="diary-card-images" v-if="diary.images && diary.images.length > 0">
            <el-image 
              :src="getImageUrl(diary.images[0])"
              fit="cover"
              class="preview-image"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 详情模式 -->
    <div v-else class="diary-detail-container">
      <div class="diary-header">
        <el-button @click="goBack" link>
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
      </div>

      <div v-if="diary" class="diary-content">
        <h1>{{ diary.title }}</h1>
        <div class="diary-meta">
          <span class="time">{{ formatTime(diary.createTime) }}</span>
          <span class="stats">
            <el-icon><View /></el-icon> {{ diary.hot || 0 }}
          </span>
        </div>

        <div class="content-text">
          <pre>{{ diary.content }}</pre>
        </div>

        <div class="waterfall-gallery" v-if="diary.images && diary.images.length > 0">
          <div 
            v-for="(image, index) in diary.images" 
            :key="index"
            class="waterfall-item"
          >
            <el-image 
              :src="getImageUrl(image)"
              :preview-src-list="diary.images.map(img => getImageUrl(img))"
              fit="cover"
              class="diary-image"
              @click="incrementHot"
            />
          </div>
        </div>
      </div>

      <div v-else class="loading-state">
        <el-empty description="日记加载中..." v-if="loading" />
        <el-empty description="未找到日记" v-else />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, View } from '@element-plus/icons-vue'
import axios from 'axios'
import { IMAGE_BASE_URL } from '../config'

interface Diary {
  id: number
  title: string
  content: string
  images: string[]
  hot: number
  createTime: string
}

const router = useRouter()
const route = useRoute()
const diary = ref<Diary | null>(null)
const diaries = ref<Diary[]>([])
const loading = ref(true)

const getImageUrl = (url: string) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  // 如果url已经包含/uploads/，去掉重复的部分
  if (url.startsWith('/uploads/')) {
    return `${IMAGE_BASE_URL}${url.substring('/uploads/'.length)}`
  }
  return `${IMAGE_BASE_URL}${url}`
}

// 获取日记列表
const fetchDiaries = async () => {
  try {
    loading.value = true
    const response = await axios.get<Diary[]>('/api/diaries')
    diaries.value = response.data.map(diary => ({
      ...diary,
      images: diary.images.map(img => getImageUrl(img))
    }))
    loading.value = false
  } catch (error: any) {
    if (error.response) {
      ElMessage.error(`获取日记列表失败: ${error.response.data.message || '服务器错误'}`)
    } else if (error.request) {
      ElMessage.error('网络错误，请检查网络连接')
    } else {
      ElMessage.error('获取日记列表失败，请重试')
    }
    loading.value = false
  }
}

// 获取日记详情
const fetchDiary = async () => {
  try {
    loading.value = true
    const diaryId = Number(route.params.id)
    console.log('正在获取日记详情，ID:', diaryId)
    
    if (!diaryId || isNaN(diaryId)) {
      ElMessage.error('无效的日记ID')
      loading.value = false
      return
    }

    const response = await axios.get<Diary>(`/api/diaries/${diaryId}`)
    console.log('获取到的日记详情:', response.data)
    
    if (!response.data) {
      ElMessage.error('未找到日记')
      loading.value = false
      return
    }

    diary.value = {
      ...response.data,
      images: (response.data.images || []).map(img => getImageUrl(img))
    }
    loading.value = false
  } catch (error: any) {
    console.error('获取日记详情失败:', error)
    if (error.response?.status === 404) {
      ElMessage.error('未找到日记')
    } else if (error.response) {
      ElMessage.error(`获取日记详情失败: ${error.response.data.message || '服务器错误'}`)
    } else if (error.request) {
      ElMessage.error('网络错误，请检查网络连接')
    } else {
      ElMessage.error('获取日记详情失败，请重试')
    }
    loading.value = false
  }
}

// 增加热度
const incrementHot = async () => {
  if (!diary.value) return
  
  try {
    await axios.post(`/api/diaries/${diary.value.id}/hot`)
    if (diary.value) {
      diary.value.hot = (diary.value.hot || 0) + 1
    }
  } catch (error) {
    console.error('增加热度失败:', error)
  }
}

// 跳转到详情页
const goToDetail = (id: number) => {
  console.log('跳转到日记详情，ID:', id)
  router.push(`/diary/${id}`)
}

// 返回上一页
const goBack = () => {
  if (route.params.id) {
    router.push('/diary')
  } else {
    router.push('/')
  }
}

const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

// 监听路由参数变化
watch(() => route.params.id, (newId) => {
  if (newId) {
    fetchDiary()
  } else {
    fetchDiaries()
  }
})

onMounted(() => {
  if (route.params.id) {
    fetchDiary()
  } else {
    fetchDiaries()
  }
})
</script>

<style scoped>
.diary-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 列表模式样式 */
.diary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.diary-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  cursor: pointer;
  transition: transform 0.3s;

  &:hover {
    transform: translateY(-5px);
  }
}

.diary-card-header {
  margin-bottom: 15px;
}

.diary-card-header h3 {
  margin: 0 0 10px 0;
  color: #303133;
}

.diary-card-content {
  color: #606266;
  margin-bottom: 15px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.diary-card-images {
  height: 200px;
  overflow: hidden;
  border-radius: 4px;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 详情模式样式 */
.diary-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.diary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.diary-content {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

h1 {
  margin: 0 0 20px 0;
  color: #303133;
}

.diary-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  margin-bottom: 20px;
}

.stats {
  display: flex;
  align-items: center;
  gap: 4px;
}

.content-text {
  line-height: 1.8;
  color: #606266;
  margin-bottom: 20px;
  white-space: pre-wrap;
  font-size: 16px;
}

.content-text pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  margin: 0;
  padding: 0;
}

.waterfall-gallery {
  column-count: 3;
  column-gap: 16px;
  margin-top: 20px;
}

.waterfall-item {
  break-inside: avoid;
  margin-bottom: 16px;
}

.diary-image {
  width: 100%;
  border-radius: 4px;
  transition: transform 0.3s;
  cursor: pointer;
  display: block;

  &:hover {
    transform: scale(1.02);
  }
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

@media (max-width: 768px) {
  .waterfall-gallery {
    column-count: 2;
  }
  .diary-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  }
}

@media (max-width: 480px) {
  .waterfall-gallery {
    column-count: 1;
  }
  .diary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
