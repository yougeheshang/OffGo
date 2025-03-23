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
        <span>{{ diaryData.author }}</span>
      </div>
      <!-- 目的地 -->
      <div class="diary-destination" @click="handleDestinationClick">
        <h3>目的地</h3>
        <span>{{ diaryData.destination }}</span>
      </div>
      <!-- 旅行图片 -->
      <div class="diary-images">
        <h3>旅行图片</h3>
        <img v-for="(image, index) in diaryData.images" :key="index" :src="image" alt="diary image">
      </div>
    </div>
    <!-- 右侧部分 -->
    <div class="right-section">
      <!-- 日记内容 -->
      <div class="diary-content">
        <h3>日记内容</h3>
        <p>{{ diaryData.content }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

// 定义 DiaryData 类型
interface DiaryData {
  id: number;
  title: string;
  author: string;
  images: string[];
  content: string;
  destination: string;
}

// 模拟从后端获取日记数据的函数，实际使用时需替换为真实的 API 调用
const fetchDiaryData = (diaryId: number): DiaryData => {
  // 这里返回模拟数据，实际应通过 API 获取
  return {
    id: diaryId,
    title: '示例日记标题',
    author: '示例作者',
    images: ['https://picsum.photos/id/100/300/200', 'https://picsum.photos/id/101/300/200'],
    content: '这是示例日记的具体内容。',
    destination: '巴黎'
  };
};

const router = useRouter();
const route = useRoute();
// 使用具体的 DiaryData 类型
const diaryData = ref<DiaryData>({
  id: 0,
  title: '',
  author: '',
  images: [],
  content: '',
  destination: ''
});

onMounted(() => {
  const diaryId = Number(route.params.id);
  diaryData.value = fetchDiaryData(diaryId);
});

// 处理目的地点击事件
const handleDestinationClick = () => {
  // 这里暂时跳转到 #，后续可根据实际情况定义跳转路径
  router.push('#');
};
</script>

<style scoped>
.diary-detail-container {
  display: flex;
  padding: 30px;
  margin-top: 70px;
  gap: 30px;
  margin-left: 120px; /* 新增，根据侧边栏宽度调整，这里假设侧边栏宽120px */
}

.left-section {
  width: 40%;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.1);
  padding: 20px;
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
.diary-content h3 {
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

.diary-destination {
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.diary-destination:hover {
  background-color: #f0f5ff;
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
</style>
