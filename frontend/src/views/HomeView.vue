<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SearchBar from '@/components/SearchBar.vue'
import UserPanel from '@/components/UserPanel.vue'
import axios from 'axios'

interface TravelCard {
  id: number
  title: string
  description: string
  images: string[]
  hot: number
  rating: number
}

const router = useRouter()
const cards = ref<TravelCard[]>([])

// 请求参数
const searchParams = ref({
  keyword: '',
  sortField: 'hot',
  sortOrder: 'desc'
})

// 获取数据
const fetchCards = async () => {
  try {
    console.log('开始获取日记列表...')
    const response = await axios.get('/api/diaries')
    console.log('获取到的原始数据:', response.data)
    
    if (!Array.isArray(response.data)) {
      console.error('返回数据格式错误，期望数组格式:', response.data)
      return
    }
    
    cards.value = response.data.map((diary: any) => {
      console.log('处理日记数据:', diary)
      return {
        id: diary.id,
        title: diary.title || '无标题',
        description: diary.content || '无内容',
        images: Array.isArray(diary.images) ? diary.images : [],
        hot: typeof diary.hot === 'number' ? diary.hot : 0,
        rating: 0
      }
    })
    console.log('处理后的数据:', cards.value)
  } catch (error: any) {
    console.error('数据获取失败:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status,
      config: {
        url: error.config?.url,
        method: error.config?.method,
        baseURL: axios.defaults.baseURL
      }
    })
  }
}

// 处理搜索
const handleSearch = (keyword: string) => {
  searchParams.value.keyword = keyword
  fetchCards()
}

// 处理排序
const handleSortChange = (params: {
  field: 'hot' | 'rating'
  order: 'asc' | 'desc'
}) => {
  searchParams.value.sortField = params.field
  searchParams.value.sortOrder = params.order
  fetchCards()
}

// 点击卡片（直接跳转到规划页面）
const handleCardClick = async (cardId: number) => {
  router.push(`/plan/${cardId}`)
}

// 生命周期
onMounted(() => {
  // 不再获取日记列表
})
</script>

<template>
  <header class="search-header">
    <SearchBar
      @search="handleSearch"
      @sort-change="handleSortChange"
    />
    <UserPanel />
  </header>
  <!-- 瀑布流容器 -->
  <div class="masonry-container">
    <div
      v-for="card in cards"
      :key="card.id"
      class="masonry-item"
      @click="handleCardClick(card.id)"
    >
      <div class="card-content">
        <img v-if="card.images && card.images.length > 0" :src="card.images[0]" class="card-image" />
        <div class="card-info">
          <h3>{{ card.title }}</h3>
          <p>{{ card.description }}</p>
          <div class="card-stats">
            <span class="hot">
              <el-icon><View /></el-icon>
              {{ card.hot }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-header {
  width: calc(100% - 70px);
  display: flex;
  position: fixed;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0px 15px 10px;
  margin-left: 70px;
  box-shadow: 0px 5px 5px -1px#DCDCDC;
  z-index: 10;
}

/* Pinterest瀑布流布局 */
.masonry-container {
  margin-left: 70px;
  margin-top: 40px;
  columns: 4 240px;
  column-gap: 20px;
}

.masonry-item {
  break-inside: avoid;
  margin-bottom: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: transform 0.3s;

  &:hover {
    transform: translateY(-5px);
  }
}

  h3 {
    margin-bottom: 8px;
    color: 	#696969;
  }

  p {
    font-size: 0.9em;
    color: #666;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

.card-content {
  padding: 12px;
}

.card-image {
  width: 100%;
  border-radius: 8px 8px 0 0;
  object-fit: cover;
  aspect-ratio: 16/9;
}

.card-info {
  padding: 12px;
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
  color: #666;
  font-size: 0.9em;
}

.hot {
  display: flex;
  align-items: center;
  gap: 4px;
}

.el-icon {
  font-size: 1.2em;
}
</style>
