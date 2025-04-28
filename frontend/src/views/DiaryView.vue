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
      <img v-if="card.imageUrl" :src="card.imageUrl" alt="Diary Image">
      <div v-else>暂无图片</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import DiarySearchBar from '@/components/DiarySearchBar.vue';
import UserPanel from '@/components/UserPanel.vue';
import axios from "axios";

interface DiaryCard {
  id: number;
  title: string;
  description: string;
  imageIds: number[];
  hot: number;
  rating: number;
  destination: string;
  imageUrl?: string;
}

const router = useRouter();
const cards = ref<DiaryCard[]>([]);

// 请求参数
const searchParams = ref({
  keyword: '',
  searchType: 'name',
  sortField: 1, // 初始默认按热度排序，设为1
  sortOrder: 'desc'
});

const fetchCards = async () => {
  try {
    const response = await fetch(`http://localhost:8050/get/diaries?keyword=${searchParams.value.keyword}&sortField=${searchParams.value.sortField}`);
    const data = await response.json();
    console.log('后端返回的数据:', data);
    const fetchedCards = data as DiaryCard[];
    const cardsWithImages = await Promise.all(fetchedCards.map(async (card) => {
      if (card.imageIds && card.imageIds.length > 0) {
        const imageUrl = await getImageUrl(card.imageIds[0]);
        return { ...card, imageUrl };
      }
      return card;
    }));
    cards.value = cardsWithImages;
  } catch (error) {
    console.error('获取日记数据失败:', error);
  }
};

// 搜索
const handleSearch = (params: { keyword: string; type: string }) => {
  searchParams.value.keyword = params.keyword;
  searchParams.value.searchType = params.type;
  fetchCards();
};

// 处理排序
const handleSortChange = (params: {
  field: 'hot' | 'rating';
  order: 'asc' | 'desc';
}) => {
  if (params.field === 'hot') {
    searchParams.value.sortField = 1;
  } else if (params.field === 'rating') {
    searchParams.value.sortField = 0;
  }
  fetchCards();
};

// 点击卡片（热度更新）
// 后端替代
const handleCardClick = async (cardId: number) => {
  try {
    cards.value = cards.value.map(card =>
      card.id === cardId? { ...card, hot: card.hot + 1 } : card
    );
    console.log(cardId);
    router.push(`/diary/${cardId}`);
  } catch (error) {
    console.error('点击记录失败:', error);
  }
};

// 根据图片序号获取图片 URL
const getImageUrl = async (imageId: number) => {
  try {
    const response = await axios.get(`http://localhost:8050/getimage/${imageId}`, {
      responseType: 'blob'
    });
    const blob = new Blob([response.data], { type: response.headers['content-type'] });
    return URL.createObjectURL(blob);
  } catch (error) {
    console.error('获取图片失败:', error);
    return '';
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
