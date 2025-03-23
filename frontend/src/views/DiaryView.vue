<template>
  <header class="search-header">
    <DiarySearchBar
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
      <h3>{{ card.title }}</h3>
      <p>{{ card.description }}</p>
      <p>热度: {{ card.hot }}</p>
      <p>评分: {{ card.rating }}</p>
      <p>目的地: {{ card.destination }}</p>
      <!-- 显示图片 -->
      <img v-for="(image, index) in card.images" :key="index" :src="image" alt="Diary Image">
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import DiarySearchBar from '@/components/DiarySearchBar.vue';
import UserPanel from '@/components/UserPanel.vue';

interface DiaryCard {
  id: number;
  title: string;
  description: string;
  images: string[];
  hot: number;
  rating: number;
  destination: string;
}

const router = useRouter();
const cards = ref<DiaryCard[]>([]);

// 请求参数
const searchParams = ref({
  keyword: '',
  searchType: 'name',
  sortField: 'hot',
  sortOrder: 'desc'
});

// 获取数据
// 使用模拟数据替代从后端获取
const fetchCards = async () => {
  // 模拟数据
  const mockData = [
    {
      id: 1,
      title: '旅行日记1',
      description: '这是一次精彩的旅行，充满了惊喜和美好的回忆。',
      images: ['https://picsum.photos/id/100/300/200', 'https://picsum.photos/id/101/300/200'],
      hot: 100,
      rating: 4,
      destination: '巴黎'
    },
    {
      id: 2,
      title: '美食日记',
      description: '品尝了各种美味的食物，每一口都令人陶醉。',
      images: ['https://picsum.photos/id/102/300/200', 'https://picsum.photos/id/103/300/200'],
      hot: 80,
      rating: 3,
      destination: '东京'
    },
    {
      id: 3,
      title: '户外探险日记',
      description: '挑战自我，探索大自然的奥秘。',
      images: ['https://picsum.photos/id/104/300/200', 'https://picsum.photos/id/105/300/200'],
      hot: 120,
      rating: 4,
      destination: '新西兰'
    }
  ];

  cards.value = mockData;
};

// 处理搜索
//与后端接轨
const handleSearch = (params: { keyword: string; type: string }) => {
  searchParams.value.keyword = params.keyword;
  searchParams.value.searchType = params.type;
  fetchCards();
};

// 处理排序
//与后端接轨
const handleSortChange = (params: {
  field: 'hot' | 'rating';
  order: 'asc' | 'desc';
}) => {
  searchParams.value.sortField = params.field;
  searchParams.value.sortOrder = params.order;
  fetchCards();
};
// 点击卡片（热度更新）
//后端替代
const handleCardClick = async (cardId: number) => {
  try {
    // 发送点击事件（模拟）
    // await fetch(`/api/cards/${cardId}/click`, {
    //   method: 'POST'
    // })

    // 本地更新热度（+1）
    cards.value = cards.value.map(card =>
      card.id === cardId? { ...card, hot: card.hot + 1 } : card
    );

    router.push(`/diary/${cardId}`);
  } catch (error) {
    console.error('点击记录失败:', error);
  }
};

// 生命周期
onMounted(fetchCards);
</script>

<style scoped>
.search-header {
  width: calc(100% - 70px);
  display: flex;
  position: fixed;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0px 15px 10px;
  margin-left: 70px;
  box-shadow: 0px 5px 5px -1px #DCDCDC;
  z-index: 10;
}

/* Pinterest瀑布流布局 */
.masonry-container {
  margin-left: 70px;
  margin-top: 70px; /* 增加上边距，确保卡片在搜索栏下方 */
  columns: 4; /* 简化列设置，让浏览器自动计算合适宽度 */
  column-gap: 20px;
}

.masonry-item {
  break-inside: avoid;
  margin-bottom: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.3s;
  width: 100%; /* 让卡片占满列宽 */
  max-width: 300px; /* 设置最大宽度 */
  padding: 15px; /* 增加内边距 */

  &:hover {
    transform: translateY(-5px);
  }
}

h3 {
  margin-bottom: 8px;
  color: #696969;
}

p {
  font-size: 0.9em;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

img {
  width: 100%;
  height: auto;
  margin-top: 10px;
  border-radius: 8px;
}
</style>
