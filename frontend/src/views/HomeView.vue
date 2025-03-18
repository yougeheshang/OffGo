<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

interface TravelCard {
  id: number
  title: string
  description: string
  images: string[]
  hot: number
  rating: number
}

const router = useRouter()
const searchKeyword = ref('')

// 模拟数据（后续替换为API请求）
const generateRandomCard = (index: number): TravelCard => {
  const cardId = Date.now() + index + Math.floor(Math.random() * 1000)
  const imageWidth = 300
  const imageHeight = 200

  return {
    id: cardId,
    title: `景点 ${index + 1}`,
    description: '随机生成的景点描述',
    images: [
      // 使用 Picsum Photos 的随机图片（添加随机参数避免浏览器缓存）
      `https://picsum.photos/${imageWidth}/${imageHeight}?random=${cardId}`
    ],
    hot: Math.floor(Math.random() * 4500) + 500,    // 500-4999 热度
    rating: Number((4 + Math.random()).toFixed(1)) // 4.0-5.0 评分
  }
}
// 生成 200 个卡片
const cards = ref<TravelCard[]>(
  Array.from({ length: 200 }, (_, index) => generateRandomCard(index))
)
const handleSearch = () => {
  // 搜索逻辑
}

const navigateToPlan = (cardId: number) => {
  router.push(`/plan/${cardId}`)
}
</script>

<template>
  <div class="home-container">
    <!-- 顶部搜索栏 -->
    <header class="search-header">
      <div class="search-box">
        <input
          type="text"
          placeholder="搜索旅游目的地..."
          v-model="searchKeyword"
        >
        <button @click="handleSearch">搜索</button>
      </div>
      <div class="user-avatar">[AVATAR]</div>
    </header>

    <!-- Pinterest风格瀑布流 -->
    <div class="masonry-container">
      <div
        v-for="(card, index) in cards"
        :key="index"
        class="masonry-item"
        @click="navigateToPlan(card.id)"
      >
        <div class="card-image">
          <img
            :src="card.images[0]"
            loading="lazy"
            :alt="card.title"
          >
          <div class="image-overlay">
            <span class="hot">🔥 {{ card.hot }}</span>
            <span class="rating">⭐ {{ card.rating }}</span>
          </div>
        </div>
        <div class="card-content">
          <h3>{{ card.title }}</h3>
          <p>{{ card.description }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-container {
  margin-left: 70px; /* 与导航栏宽度一致 */
  padding: 20px;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.search-box {
  flex: 1;
  max-width: 600px;
  display: flex;
  gap: 10px;

  input {
    flex: 1;
    padding: 12px;
    border: 1px solid #ddd;
    border-radius: 24px;
  }

  button {
    padding: 12px 24px;
    background: #f5f5f5;
    color: white;
    border: none;
    border-radius: 24px;
    cursor: pointer;
  }
}

/* Pinterest瀑布流布局 */
.masonry-container {
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

.card-image {
  position: relative;
  img {
    width: 100%;
    border-radius: 12px 12px 0 0;
  }
}

.image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px;
  background: linear-gradient(transparent, rgba(0,0,0,0.7));
  color: white;
  display: flex;
  justify-content: space-between;
}

.card-content {
  padding: 16px;

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
}
</style>
