<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SearchBar from '@/components/SearchBar.vue'
import UserPanel from '@/components/UserPanel.vue'

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
    const query = new URLSearchParams({
      keyword: searchParams.value.keyword,
      sortField: searchParams.value.sortField === 'hot' ? '1' : '0',
      sortOrder: searchParams.value.sortOrder
    }).toString()

    const response = await fetch(`http://localhost:8050/get/diaries?${query}`)
    const data = await response.json()
    cards.value = data
  } catch (error) {
    console.error('数据获取失败:', error)
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

// 点击卡片（热度更新）
const handleCardClick = async (cardId: number) => {
  try {
    // 发送点击事件
    await fetch(`/api/cards/${cardId}/click`, {
      method: 'POST'
    })

    // 本地更新热度（+1）
    cards.value = cards.value.map(card =>
      card.id === cardId
        ? { ...card, hot: card.hot + 1 }
        : card
    )

    router.push(`/plan/${cardId}`)
  } catch (error) {
    console.error('点击记录失败:', error)
  }
}

// 生命周期
onMounted(fetchCards)
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
      <!-- 卡片内容保持不变 -->
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
</style>
