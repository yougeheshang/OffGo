<template>
  <div class="diary-detail-container">
    <!-- 左侧部分 -->
    <div class="left-section">
      <!-- 日记名称 -->
      <div class="diary-title">
        <h3>日记名称</h3>
        <span>{{ diaryData.title }}</span>
      </div>
      <!-- 作者 -->
      <div class="diary-author">
        <h3>作者</h3>
        <span>{{ diaryData.userID }}</span>
      </div>
      <!-- 目的地 -->
      <div class="diary-destination">
        <h3>目的地</h3>
        <span>{{ diaryData.destination }}</span>
      </div>
      <!-- 旅行图片 -->
      <div class="diary-images">
        <h3>旅行图片</h3>
        <img
          v-for="(imageId, index) in diaryData.image"
          :key="index"
          :src="getImageUrl(imageId)"
          alt="diary image"
        />
      </div>
    </div>
    <!-- 右侧部分 -->
    <div class="right-section">
      <!-- 日记内容 -->
      <div class="diary-content">
        <h3>日记内容</h3>
        <p>{{ diaryData.content }}</p>
      </div>
      <!-- 显示热度 -->
      <div class="diary-hot">
        <h3>热度: {{ diaryData.hot }}</h3>
      </div>
      <!-- 评分板块 -->
      <div class="diary-rating">
        <h3>评分</h3>
        <div class="star-rating">
          <span
            v-for="(star, index) in 5"
            :key="index"
            :class="{ 'star-filled': index < submittedRating }"
            :style="{ cursor: hasRated ? 'default' : 'pointer' }"
            @mouseenter="!hasRated ? handleMouseEnter(index + 1) : null"
            @mouseleave="!hasRated ? handleMouseLeave : null"
            @click="!hasRated ? submitRating(index + 1) : null"
          >★</span>
        </div>
        <div v-if="hasRated">
          <p>您已评分为: {{ submittedRating }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';

// 定义 DiaryData 类型，与后端结构一致
interface DiaryData {
  id: number;
  title: string;
  description: string;
  image: number[];
  hot: number;
  destination: string;
  rating: number;
  rate_sum: number;
  rate_counts: number;
  userID: number;
  content: string; // content 为实际的 String 类型
}

const route = useRoute();
const diaryData = ref<DiaryData>({
  id: 0,
  title: '',
  description: '',
  image: [],
  hot: 0,
  destination: '',
  rating: 0,
  rate_sum: 0,
  rate_counts: 0,
  userID: 0,
  content: ''
});

// 鼠标悬停时的评分
const hoverRating = ref(0);
// 用户提交的评分
const submittedRating = ref(0);
// 用户是否已经评分
const hasRated = ref(false);

// 从后端获取日记数据
const fetchDiaryData = async () => {
  const diaryId = Number(route.params.id);
  try {
    const response = await axios.get(`http://localhost:8050/get/diarydata/${diaryId}`);
    diaryData.value = response.data;
    // 请求查看用户是否已经打过分及分数
    const ratingResponse = await axios.get(`http://localhost:8050/get/diarydata/${diaryId}/checkRating`);
    const { hasRated: backendHasRated, rating: backendRating } = ratingResponse.data;
    hasRated.value = backendHasRated;
    if (backendHasRated) {
      submittedRating.value = backendRating;
    }
    await axios.get(`http://localhost:8050/diary/add_hot/${diaryId}`);
  } catch (error) {
    console.error('获取日记数据或评分状态失败:', error);
  }
};

// 获取图片 URL
const getImageUrl = (imageId: number) => {
  return `http://localhost:8050/getimage/${imageId}`;
};

// 鼠标悬停处理
const handleMouseEnter = (rating: number) => {
  if (!hasRated.value) {
    hoverRating.value = rating;
  }
};

// 鼠标离开处理
const handleMouseLeave = () => {
  if (!hasRated.value) {
    hoverRating.value = 0;
  }
};

// 提交评分
const submitRating = async (rating: number) => {
  const diaryId = Number(route.params.id);
  if (!hasRated.value) {
    try {
      //const response = await axios.post(`http://localhost:8050/get/diarydata/${diaryId}/rating`, { rating });
      if (true) {
        submittedRating.value = rating;
        hasRated.value = true;
        // 更新日记数据中的评分信息
        diaryData.value.rating = (diaryData.value.rate_sum + rating) / (diaryData.value.rate_counts + 1);
        diaryData.value.rate_sum += rating;
        diaryData.value.rate_counts++;
      }
    } catch (error) {
      console.error('提交评分失败:', error);
    }
  }
};

onMounted(() => {
  fetchDiaryData();
});
</script>

<style scoped>
.diary-detail-container {
  display: flex;
  padding: 30px;
  margin-top: 70px;
  gap: 30px;
  margin-left: 100px;
}

.left-section {
  width: 40%;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.1);
  padding: 20px;
  left: 50px;
}

.right-section {
  width: 60%;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.diary-title h3,
.diary-author h3,
.diary-destination h3,
.diary-images h3,
.diary-content h3,
.diary-hot h3,
.diary-rating h3 {
  margin-bottom: 5px;
  color: #333;
  font-size: 1.1em;
}

.diary-title span,
.diary-author span,
.diary-destination span {
  color: #666;
  display: block;
  margin-bottom: 15px;
}

.diary-images img {
  width: 100%;
  height: auto;
  margin-bottom: 10px;
  border-radius: 5px;
}

.diary-content p {
  line-height: 1.6;
  color: #666;
}

.star-rating {
  font-size: 24px;
  color: #ccc;
}

.star-filled {
  color: gold;
}
</style>
