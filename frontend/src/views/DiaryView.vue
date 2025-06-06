<template>
  <header class="search-header">
    <div class="header-content">
      <DiarySearchBar
        @search="handleSearch"
        @sort-change="handleSort"
      />
    </div>
    <div class="nav-user-area">
      <button
        v-if="!isLoggedIn"
        class="login-nav-btn"
        @click="openLoginDialog"
      >登录</button>
      <div v-else class="avatar-wrapper" @click="toggleMenu" @blur="closeMenu" tabindex="0">
        <div class="avatar">{{ getAvatarLetter() }}</div>
        <div v-if="showMenu" class="dropdown-menu">
          <div class="dropdown-item" @click.stop="handleSwitchAccount">切换账号</div>
          <div class="dropdown-item" @click.stop="handleLogout">退出登录</div>
          <div class="dropdown-item" @click.stop="handleModifyInterest">修改兴趣</div>
        </div>
      </div>
    </div>
    <!-- 登录弹窗 -->
    <Transition name="fade">
      <div v-if="showLoginDialog" class="main-login-dialog">
        <LoginRegisterForm @login-success="handleLoginSuccess" @close="showLoginDialog = false" />
      </div>
    </Transition>
    <InterestSelectDialog
      v-if="showInterestDialog"
      :visible="showInterestDialog"
      :userInterests="userInterests"
      @close="showInterestDialog = false"
      @submit="handleInterestSubmit"
    />
  </header>
  
  <div class="diary-grid">
    <div
      v-for="diary in cards" 
      :key="diary.id" 
      class="diary-card"
      @click="handleCardClick(diary.id)"
    >
      <div class="diary-image-container">
        <img 
          v-if="diary.imageUrl" 
          :src="diary.imageUrl" 
          :alt="diary.title" 
          class="diary-image"
          @error="handleImageError"
        >
        <div v-else class="diary-image-placeholder">
          <el-icon :size="32"><Picture /></el-icon>
          <span>暂无图片</span>
        </div>
      </div>
      <div class="diary-content">
        <h3 class="diary-title" v-html="diary.title || '无标题'"></h3>
        <div class="diary-destination">
          <el-icon><Location /></el-icon>
          <span>{{ diary.destination || '未知地点' }}</span>
        </div>
        <p class="diary-description" v-html="diary.description || '暂无描述'"></p>
        <div class="diary-tags" v-if="diary.tags && diary.tags.length > 0">
          <span 
            v-for="tag in diary.tags" 
            :key="tag" 
            class="tag-item"
          >
            {{ tag }}
          </span>
        </div>
      </div>
      <div class="diary-footer">
        <div class="diary-stats">
          <span class="stat-item">
            <el-icon><Star /></el-icon>
            {{ diary.rating?.toFixed(1) || '0.0' }}
          </span>
          <span class="stat-item">
            <el-icon><View /></el-icon>
            {{ diary.hot || 0 }}
          </span>
        </div>
        <el-button
          class="storage-btn"
          type="primary"
          :icon="Download"
          circle
          @click.stop="handleStorage(diary)"
          :loading="storingDiaryId === diary.id"
        />
      </div>
    </div>
  </div>

  <!-- 加载状态指示器 -->
  <div v-if="loading" class="loading-indicator">
    <el-icon class="is-loading"><Loading /></el-icon>
    <span>加载中...</span>
  </div>

  <!-- 日记详情对话框 -->
  <transition name="fade-dialog">
    <DiaryDetailDialog
      v-model:visible="showDetailDialog"
      :diary="currentDiary"
      :video-url="videoUrl"
      :image-urls="imageUrls"
      :hasRated="userHasRatedDiary"
      :userRating="userDiaryRating"
      @rate="handleDiaryRate"
      @rating-updated="handleRatingUpdated"
    />
  </transition>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, shallowRef, reactive } from 'vue';
import { useRouter } from 'vue-router';
import DiarySearchBar from '@/components/DiarySearchBar.vue';
import LoginRegisterForm from '@/components/LoginRegisterForm.vue';
import InterestSelectDialog from '@/components/InterestSelectDialog.vue';
import axios from "axios";
import { ElMessage, ElMessageBox } from 'element-plus';
import { Location, View, Picture, Star, Loading, Download } from '@element-plus/icons-vue';
import JSZip from 'jszip';
import DiaryDetailDialog from '@/components/DiaryDetailDialog.vue'
import { useUserStore } from '@/stores/user';

interface DiaryCard {
  id: number;
  title: string;
  description: string;
  content?: string;
  imageIds: string;
  image?: string;
  hot: number;
  rating: number;
  destination: string;
  imageUrl?: string | null;
  tags?: string[];
}

const router = useRouter();
const cards = ref<DiaryCard[]>([]);
const loading = ref(false);
const hasMore = ref(true);
const retryCount = ref(0);
const MAX_RETRIES = 3;
const LOAD_SIZE = 20; // 每次加载的数量
const userId = ref<number | null>(null);

// 排序选项
const sortOptions = [
  { label: '按热度排序', value: 1 },
  { label: '按评分排序', value: 2 },
  { label: '随机排序', value: 3 }
]

// 排序字段和顺序
const sortField = ref(1) // 1: 热度, 2: 评分, 3: 随机
const sortOrder = ref('desc')

// 搜索参数
const searchParams = ref({
  keyword: '',      // 搜索关键词
  searchType: 'name', // 搜索类型：name(标题), destination(目的地), content_text(内容)
  sortField: 1,     // 排序字段：1(热度), 2(评分), 3(随机)
  sortOrder: 'desc', // 排序顺序：desc(降序), asc(升序)
  userId: null,
  offset: 0         // 添加offset属性
});

// 添加已加载日记ID的集合，用于去重
const loadedDiaryIds = ref(new Set<number>());

// 添加用户角色响应式变量
const state = reactive({
  // 删除 userRole
});

// 添加用户兴趣相关的响应式变量
const userInterests = ref<string[]>([]);

// 添加用户登录相关的响应式变量
const isLoggedIn = ref(false);
const username = ref('');
const showLoginDialog = ref(false);
const showInterestDialog = ref(false);
const showMenu = ref(false);
const userStore = useUserStore();

// 设置 axios 默认配置
axios.defaults.baseURL = 'http://localhost:8050';  // 设置正确的后端服务地址
axios.defaults.timeout = 10000;  // 设置超时时间
axios.defaults.headers.common['Content-Type'] = 'application/json';
axios.defaults.withCredentials = true;  // 允许跨域请求携带凭证

// 添加请求拦截器用于调试
axios.interceptors.request.use(
  config => {
    // 确保每个请求都包含正确的 headers
    if (config.headers) {
      config.headers['Content-Type'] = 'application/json';
      config.headers['Accept'] = 'application/json';
    }
    console.log('发送请求:', {
      url: config.url,
      method: config.method,
      baseURL: config.baseURL,
      fullUrl: `${config.baseURL || ''}${config.url}`,
      headers: config.headers,
      data: config.data,
      timestamp: new Date().toISOString()
    });
    return config;
  },
  error => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

// 添加响应拦截器用于调试
axios.interceptors.response.use(
  response => {
    console.log('收到响应:', {
      status: response.status,
      statusText: response.statusText,
      headers: response.headers,
      data: response.data,
      config: {
        url: response.config.url,
        method: response.config.method,
        baseURL: response.config.baseURL,
        fullUrl: `${response.config.baseURL || ''}${response.config.url}`
      },
      timestamp: new Date().toISOString()
    });
    return response;
  },
  error => {
    console.error('响应错误:', {
      message: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      config: {
        url: error.config?.url,
        method: error.config?.method,
        baseURL: error.config?.baseURL,
        fullUrl: `${error.config?.baseURL || ''}${error.config?.url}`
      },
      timestamp: new Date().toISOString()
    });
    return Promise.reject(error);
  }
);

// 获取日记卡片数据
const fetchDiaryCards = async (isLoadMore = false) => {
  if (loading.value || (!isLoadMore && !hasMore.value)) return;
  
  try {
    loading.value = true;
    console.log('开始加载日记卡片数据:', {
      isLoadMore,
      currentLoaded: cards.value.length,
      searchParams: searchParams.value
    });

    const response = await axios.get('/api/diary/getdiarys', {
      params: searchParams.value
    });

    if (response.data.code === 200) {
      const diaries = response.data.data.data as DiaryCard[];
      console.log('获取到的日记数据:', {
        total: diaries.length,
        searchType: searchParams.value.searchType,
        keyword: searchParams.value.keyword
      });

      // 处理新数据
      const newDiaries = diaries.filter((diary: DiaryCard) => 
        !cards.value.some(card => card.id === diary.id)
      );

      if (newDiaries.length > 0) {
        // 处理图片URL
        const processedDiaries = newDiaries.map((diary: DiaryCard) => ({
          ...diary,
          imageUrl: diary.image ? `http://localhost:8050/api/diary/getimage/${diary.image.split(',')[0]}` : null,
          tags: Array.isArray(diary.tags)
            ? diary.tags
            : (diary.tags ? String(diary.tags).split(',') : [])
        })) as DiaryCard[];

        if (isLoadMore) {
          cards.value.push(...processedDiaries);
        } else {
          cards.value = processedDiaries;
        }

        console.log('成功加载新数据:', {
          newDiariesCount: newDiaries.length,
          totalLoaded: cards.value.length
        });

        hasMore.value = newDiaries.length > 0;
        retryCount.value = 0;
      } else {
        console.log('没有获取到新数据:', {
          searchType: searchParams.value.searchType,
          keyword: searchParams.value.keyword
        });
        
        if (retryCount.value < MAX_RETRIES) {
          retryCount.value++;
          console.log(`尝试重新加载 (${retryCount.value}/${MAX_RETRIES})...`);
          setTimeout(() => {
            fetchDiaryCards(isLoadMore);
          }, 1000);
        } else {
          console.log('达到最大重试次数，停止加载');
          hasMore.value = false;
          retryCount.value = 0;
        }
      }
    } else {
      console.error('获取日记卡片失败:', {
        code: response.data.code,
        message: response.data.msg,
        searchType: searchParams.value.searchType,
        keyword: searchParams.value.keyword
      });
      hasMore.value = false;
    }
  } catch (error: any) {
    console.error('获取日记卡片出错:', {
      error: error.message,
      searchType: searchParams.value.searchType,
      keyword: searchParams.value.keyword,
      status: error.response?.status,
      data: error.response?.data
    });
    hasMore.value = false;
  } finally {
    loading.value = false;
  }
};

// 处理搜索
const handleSearch = (params: { keyword: string; type: string }) => {
  // 更新搜索参数
  searchParams.value.keyword = params.keyword.trim();
  searchParams.value.searchType = params.type;
  
  // 重置列表状态
  cards.value = [];
  hasMore.value = true;
  retryCount.value = 0;
  
  // 添加搜索日志
  console.log('执行搜索:', {
    keyword: searchParams.value.keyword,
    searchType: searchParams.value.searchType,
    timestamp: new Date().toISOString()
  });
  
  // 重新加载数据
  fetchDiaryCards();
};

// 处理排序
const handleSort = (params: { field: number; order: string }) => {
  searchParams.value.sortField = params.field;
  searchParams.value.sortOrder = params.order;
  
  if (params.field === 4) { // 智能排序
    const interests = userInterests.value;
    cards.value.sort((a, b) => {
      // 1. 兴趣重合度
      const aMatch = a.tags ? a.tags.filter(tag => interests.includes(tag)).length : 0;
      const bMatch = b.tags ? b.tags.filter(tag => interests.includes(tag)).length : 0;
      // 2. 评分
      const aRating = a.rating || 0;
      const bRating = b.rating || 0;
      // 3. 热度
      const aHot = a.hot || 0;
      const bHot = b.hot || 0;
      // 综合分数
      const aScore = aMatch * 10000 + aRating * 1000 + aHot * 10;
      const bScore = bMatch * 10000 + bRating * 1000 + bHot * 10;
      return bScore - aScore;
    });
  } else {
    cards.value = [];
    hasMore.value = true;
    retryCount.value = 0;
    fetchDiaryCards();
  }
};

// 处理滚动加载
const handleScroll = () => {
  if (loading.value || !hasMore.value) return;

  const scrollHeight = document.documentElement.scrollHeight;
  const scrollTop = document.documentElement.scrollTop;
  const clientHeight = document.documentElement.clientHeight;

  // 当距离底部300px时开始加载
  if (scrollHeight - scrollTop - clientHeight < 300) {
    console.log('触发滚动加载:', {
      totalLoaded: cards.value.length
    });
    fetchDiaryCards(true);
  }
};

// 组件挂载时
onMounted(async () => {
  console.log('DiaryView组件挂载，开始初始化...');
  console.log('Axios 配置信息:', {
    baseURL: axios.defaults.baseURL,
    headers: axios.defaults.headers,
    timeout: axios.defaults.timeout,
    withCredentials: axios.defaults.withCredentials,
    currentEnvironment: process.env.NODE_ENV,
    currentPort: window.location.port
  });
  
  // 检查登录状态
  const storedUsername = localStorage.getItem('username');
  if (storedUsername) {
    isLoggedIn.value = true;
    username.value = storedUsername;
    // 获取用户兴趣
    try {
      const res = await axios.get(`/api/user/interests?username=${storedUsername}`);
      userInterests.value = Array.isArray(res.data) ? res.data : [];
      console.log('获取到用户兴趣:', userInterests.value);
    } catch (error) {
      console.error('获取用户兴趣失败:', error);
      userInterests.value = [];
    }
  }
  
  fetchDiaryCards();
  window.addEventListener('scroll', handleScroll);
});

// 组件卸载时
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
  // 清理图片URL
  cards.value.forEach(card => {
    if (card.imageUrl) {
      URL.revokeObjectURL(card.imageUrl);
    }
  });
});

// 存储图片URL的响应式对象
const imageUrls = ref<Record<number, string>>({});

// 在 script setup 部分添加视频相关的响应式变量
const videoUrl = ref<string | null>(null);
const videoError = ref<string | null>(null);
const videoDebug = ref(true);
const videoLoadStatus = ref('未开始加载');

// 获取视频URL
const getVideoUrl = (videoId: number) => {
  const url = `http://localhost:8050/api/videos/${videoId}`;
  console.log('Generated video URL:', url);
  return url;
};

// 处理视频错误
const handleVideoError = (e: Event) => {
  const videoElement = e.target as HTMLVideoElement;
  console.error('Video error:', videoElement.error);
  videoError.value = '视频加载失败，请稍后重试';
  videoLoadStatus.value = '加载失败';
};

// 处理视频加载
const handleVideoLoaded = () => {
  videoError.value = null;
  videoLoadStatus.value = '元数据已加载';
};

const handleVideoLoadStart = () => {
  console.log('Video load started');
  videoLoadStatus.value = '开始加载';
};

const handleVideoCanPlay = () => {
  console.log('Video can play');
  videoLoadStatus.value = '可以播放';
};

const showDetailDialog = ref(false);
const currentDiary = ref<any>(null);
const userHasRatedDiary = ref(false);
const userDiaryRating = ref(0);

// 获取日记评分状态
const fetchDiaryRatingStatus = async (diaryId: number) => {
  const userId = localStorage.getItem('userId');
  if (!userId) {
    userHasRatedDiary.value = false;
    userDiaryRating.value = 0;
    return;
  }
  try {
    // 假设后端有此接口，实际接口名请替换
    const res = await axios.get(`/api/diary/get/diarydata/${diaryId}/checkRating`, { params: { userId } });
    userHasRatedDiary.value = !!res.data.hasRated;
    userDiaryRating.value = res.data.rating || 0;
  } catch (e) {
    userHasRatedDiary.value = false;
    userDiaryRating.value = 0;
  }
};

// 修改获取日记详情逻辑，弹窗前先查评分状态
const handleCardClick = async (cardId: number) => {
  // 重置评分状态
  userHasRatedDiary.value = false;
  userDiaryRating.value = 0;
  
  console.log('开始获取日记详情，卡片ID:', cardId);
  console.log('当前环境:', {
    baseURL: axios.defaults.baseURL,
    headers: axios.defaults.headers,
    timeout: axios.defaults.timeout
  });
  
  try {
    // 获取日记详情
    const apiUrl = `/api/diary/get/diary/${cardId}`;  // 正确的API路径
    console.log('请求日记详情，API URL:', apiUrl);
    console.log('完整请求URL:', `${axios.defaults.baseURL || ''}${apiUrl}`);
    
    const response = await axios.get(apiUrl);
    console.log('日记详情响应状态:', response.status);
    console.log('日记详情响应头:', response.headers);
    console.log('日记详情响应数据:', response.data);
    
    if (response.data) {
      currentDiary.value = response.data;
      await fetchDiaryRatingStatus(cardId);
      console.log('成功设置当前日记数据:', {
        id: currentDiary.value.id,
        title: currentDiary.value.title,
        contentId: currentDiary.value.contentId,
        videoId: currentDiary.value.videoId,
        image: currentDiary.value.image
      });
      
      // 获取内容
      if (response.data.contentId) {
        try {
          const contentApiUrl = `/api/diary/getcontent/${response.data.contentId}`;  // 正确的API路径
          console.log('请求日记内容，API URL:', contentApiUrl);
          console.log('完整内容请求URL:', `${axios.defaults.baseURL || ''}${contentApiUrl}`);
          
          const contentResponse = await axios.get(contentApiUrl);
          console.log('内容响应状态:', contentResponse.status);
          console.log('内容响应数据:', contentResponse.data);
          
          if (contentResponse.data) {
            currentDiary.value.content = contentResponse.data.content;
            console.log('成功设置日记内容');
          }
        } catch (error: any) {
          console.error('获取日记内容失败:', {
            error: error.message,
            status: error.response?.status,
            data: error.response?.data,
            config: error.config
          });
          ElMessage.error('获取日记内容失败');
        }
      } else {
        console.log('日记没有关联的内容ID');
      }

      // 处理视频
      if (response.data.videoId) {
        console.log('处理视频，视频ID:', response.data.videoId);
        videoUrl.value = getVideoUrl(response.data.videoId);
        console.log('生成的视频URL:', videoUrl.value);
        videoLoadStatus.value = '准备加载视频';
      } else {
        console.log('日记没有关联的视频');
        videoUrl.value = null;
      }
      
      // 预加载所有图片
      if (currentDiary.value.image) {
        const imageIds = currentDiary.value.image.split(',');
        console.log('开始预加载图片，图片ID列表:', imageIds);
        await Promise.all(
          imageIds.map(async (imageId: string) => {
            try {
              console.log('加载图片，ID:', imageId);
              const url = await getImageUrl(imageId);
              imageUrls.value[parseInt(imageId)] = url;
              console.log(`图片 ${imageId} 加载成功:`, url);
            } catch (error: any) {
              console.error(`加载图片 ${imageId} 失败:`, {
                error: error.message,
                status: error.response?.status,
                data: error.response?.data
              });
            }
          })
        );
      } else {
        console.log('日记没有关联的图片');
      }
      
      showDetailDialog.value = true;
      console.log('显示日记详情对话框');
      
      // 更新热度
      try {
        const hotApiUrl = `/api/diary/diary/add_hot/${cardId}`;  // 正确的API路径
        console.log('更新热度，API URL:', hotApiUrl);
        
        await axios.post(hotApiUrl);
        cards.value = cards.value.map(card =>
          card.id === cardId ? { ...card, hot: card.hot + 1 } : card
        );
        console.log('热度更新成功，新热度:', cards.value.find(card => card.id === cardId)?.hot);
      } catch (error: any) {
        console.error('更新热度失败:', {
          error: error.message,
          status: error.response?.status,
          data: error.response?.data
        });
      }
    } else {
      console.warn('获取到的日记数据为空');
      ElMessage.error('获取日记详情失败：数据为空');
    }
  } catch (error: any) {
    console.error('获取日记详情失败:', {
      error: error.message,
      status: error.response?.status,
      data: error.response?.data,
      config: error.config,
      url: error.config?.url,
      method: error.config?.method,
      headers: error.config?.headers
    });
    ElMessage.error(`获取日记详情失败: ${error.response?.data?.message || error.message}`);
  }
};

// 修改获取图片URL函数
const getImageUrl = (imageId: string) => {
  if (!imageId) {
    console.warn('No image ID provided')
    return ''
  }
  const url = `http://localhost:8050/api/diary/getimage/${imageId}`
  console.log('Generated image URL:', url)
  return url
}

// 修改图片错误处理函数
const handleImageError = (e: Event) => {
  const imgElement = e.target as HTMLImageElement;
  console.error('图片加载失败:', {
    src: imgElement.src,
    alt: imgElement.alt,
    naturalWidth: imgElement.naturalWidth,
    naturalHeight: imgElement.naturalHeight
  });
  
  // 隐藏失败的图片
  imgElement.style.display = 'none';
  
  // 显示占位符
  const placeholder = imgElement.parentElement?.querySelector('.diary-image-placeholder') as HTMLElement | null;
  if (placeholder) {
    placeholder.style.display = 'flex';
  }
};

// 修改图片加载成功处理函数
const handleImageLoad = (imageUrl: string) => {
  console.log('图片加载成功:', {
    url: imageUrl,
    timestamp: new Date().toISOString()
  });
};

// 添加处理评分更新的函数
const handleRatingUpdated = (data: { diaryId: number, newRating: number }) => {
  // 更新卡片列表中的评分
  cards.value = cards.value.map(card => {
    if (card.id === data.diaryId) {
      return {
        ...card,
        rating: data.newRating
      };
    }
    return card;
  });
};

// 修改评分回调函数
const handleDiaryRate = async (value: number) => {
  if (!userHasRatedDiary.value) {
    const storedUserId = localStorage.getItem('userId');
    if (!storedUserId) {
      ElMessage.warning('请先登录后再评分');
      return;
    }

    try {
      const requestData = {
        diaryId: currentDiary.value.id,
        userId: parseInt(storedUserId),
        rating: value
      };

      const response = await axios.post('/api/diary/rate', requestData);

      if (response.status === 200 && response.data) {
        // 更新当前日记的评分状态
        userDiaryRating.value = value;
        userHasRatedDiary.value = true;
        currentDiary.value.rating = response.data.newRating;
        
        // 触发评分更新事件
        handleRatingUpdated({
          diaryId: currentDiary.value.id,
          newRating: response.data.newRating
        });
        
        ElMessage.success('评分成功！');
      } else {
        throw new Error(`评分请求返回非成功状态: ${response.status}`);
      }
    } catch (error: any) {
      console.error('评分失败:', error);
      ElMessage.error(error.response?.data?.message || '评分失败，请稍后重试');
    }
  } else {
    ElMessage.info('您已评分，不能重复评分');
  }
};

// 添加存储相关的状态
const storingDiaryId = ref<number | null>(null);

// 处理存储功能
const handleStorage = async (diary: DiaryCard) => {
  if (storingDiaryId.value) return;
  
  try {
    storingDiaryId.value = diary.id;
    
    // 创建新的 ZIP 实例
    const zip = new JSZip();
    
    // 获取日记详情
    const response = await axios.get(`http://localhost:8050/api/diary/getdiary/${diary.id}`);
    const diaryDetail = response.data;
    
    // 获取日记内容
    let contentText = '';
    if (diaryDetail.contentId) {
      try {
        const contentResponse = await axios.get(`http://localhost:8050/api/diary/getcontent/${diaryDetail.contentId}`);
        contentText = contentResponse.data.content || '';
      } catch (error) {
        console.error('获取日记内容失败:', error);
        contentText = '获取内容失败';
      }
    }
    
    // 创建日记内容文件
    const content = `标题：${diaryDetail.title || '无标题'}\n\n` +
                   `目的地：${diaryDetail.destination || '未知地点'}\n\n` +
                   `描述：${diaryDetail.description || '暂无描述'}\n\n` +
                   `正文：${contentText}\n\n` +
                   `评分：${diaryDetail.rating?.toFixed(1) || '0.0'}\n` +
                   `热度：${diaryDetail.hot || 0}`;
    
    zip.file('diary.txt', content);
    
    // 处理图片
    if (diaryDetail.image) {
      const imageIds = diaryDetail.image.split(',');
      const imageFolder = zip.folder('images');
      
      for (const imageId of imageIds) {
        try {
          const imageResponse = await axios.get(`http://localhost:8050/api/diary/getimage/${parseInt(imageId)}`, {
            responseType: 'blob'
          });
          imageFolder?.file(`image_${imageId}.jpg`, imageResponse.data);
        } catch (error) {
          console.error(`获取图片 ${imageId} 失败:`, error);
        }
      }
    }
    
    // 处理视频
    if (diaryDetail.videoId) {
      try {
        const videoResponse = await axios.get(`http://localhost:8050/api/videos/${diaryDetail.videoId}`, {
          responseType: 'blob'
        });
        zip.file('video.mp4', videoResponse.data);
      } catch (error) {
        console.error('获取视频失败:', error);
      }
    }
    
    // 生成 ZIP 文件
    const blob = await zip.generateAsync({
      type: 'blob',
      compression: 'DEFLATE',
      compressionOptions: {
        level: 9 // 最高压缩级别
      }
    });
    
    // 创建下载链接
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `diary_${diary.id}_${new Date().toISOString().split('T')[0]}.zip`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    
    ElMessage.success('日记已成功压缩并下载');
  } catch (error) {
    console.error('存储日记失败:', error);
    ElMessage.error('存储日记失败，请稍后重试');
  } finally {
    storingDiaryId.value = null;
  }
};

// 添加用户登录相关的方法
const openLoginDialog = () => {
  showLoginDialog.value = true;
};

const handleLoginSuccess = () => {
  isLoggedIn.value = true;
  username.value = localStorage.getItem('username') || '';
  showLoginDialog.value = false;
};

const handleLogout = () => {
  localStorage.removeItem('username');
  localStorage.removeItem('userId');
  localStorage.removeItem('userRole');
  isLoggedIn.value = false;
  username.value = '';
  ElMessage.success('已退出登录');
};

const handleSwitchAccount = () => {
  handleLogout();
  showLoginDialog.value = true;
};

const getAvatarLetter = () => {
  if (!username.value) return '';
  return username.value.charAt(0).toUpperCase();
};

const toggleMenu = () => {
  showMenu.value = !showMenu.value;
};

const closeMenu = () => {
  showMenu.value = false;
};

const handleModifyInterest = () => {
  showMenu.value = false;
  showInterestDialog.value = true;
};

const handleInterestSubmit = async (interests: string[]) => {
  userInterests.value = interests;
  showInterestDialog.value = false;
  const username = localStorage.getItem('username');
  if (username) {
    await axios.post(`/api/user/interests?username=${username}`, interests);
    ElMessage.success('兴趣已更新');
  } else {
    ElMessage.error('未获取到用户名，无法保存兴趣');
  }
};
</script>

<style scoped>
.search-header {
  width: calc(100% - 70px);
  display: flex;
  position: fixed;
  justify-content: space-between;
  background-color: rgba(255, 255, 255, 0.95);
  align-items: center;
  padding: 15px 20px;
  margin-left: 70px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(10px);
  z-index: 10;
}

.diary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 28px;
  padding: 24px;
  margin-left: 80px;
  margin-right: 24px;
  margin-top: 80px;
}

.diary-card {
  break-inside: avoid;
  margin-bottom: 24px;
  background: white;
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  height: fit-content;
  cursor: pointer;
  position: relative;
}

.diary-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.diary-image-container {
  width: 100%;
  height: 220px;
  overflow: hidden;
  position: relative;
  background-color: #f8f9fa;
}

.diary-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.diary-card:hover .diary-image {
  transform: scale(1.08);
}

.diary-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7eb 100%);
}

.diary-image-placeholder span {
  margin-top: 12px;
  font-size: 14px;
  color: #606266;
}

.diary-content {
  padding: 20px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: white;
}

.diary-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.diary-destination {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
  padding: 4px 0;
}

.diary-destination .el-icon {
  color: #409EFF;
}

.diary-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-clamp: 2;
  overflow: hidden;
  flex-grow: 1;
  text-align: center;
}

.diary-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f8f9fa;
  border-top: 1px solid #ebeef5;
}

.diary-stats {
  display: flex;
  gap: 20px;
  color: #909399;
  font-size: 13px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  transition: color 0.3s ease;
}

.stat-item:hover {
  color: #409EFF;
}

.stat-item .el-icon {
  font-size: 16px;
}

/* 加载状态指示器样式 */
.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: #909399;
  gap: 12px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  margin: 24px auto;
  width: fit-content;
  backdrop-filter: blur(10px);
}

.loading-indicator .el-icon {
  font-size: 24px;
  color: #409EFF;
}

.loading-indicator span {
  font-size: 15px;
  color: #606266;
}

/* 高亮样式 */
:deep(.mark) {
  background-color: #ffd04b;
  padding: 2px 4px;
  border-radius: 4px;
  color: #303133;
  font-weight: 500;
}

/* 响应式布局保持不变 */
@media (max-width: 1600px) {
  .diary-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1200px) {
  .diary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .search-header {
    padding: 15px;
  }
}

@media (max-width: 768px) {
  .diary-grid {
    grid-template-columns: 1fr;
    margin-left: 20px;
    margin-right: 20px;
    margin-top: 70px;
  }
  
  .search-header {
    width: 100%;
    margin-left: 0;
    padding: 10px;
  }
  
  .diary-header h2 {
    font-size: 24px;
  }
  
  .diary-meta {
    gap: 16px;
  }
  
  .diary-meta span {
    padding: 6px 12px;
  }
}

/* 登录对话框样式 */
.login-dialog :deep(.el-dialog) {
  width: 100% !important;
  height: 100% !important;
  margin: 0 !important;
  max-width: none !important;
  border-radius: 0 !important;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: white;
}

.login-dialog :deep(.el-dialog__header) {
  padding: 20px;
  margin: 0;
  border-bottom: 1px solid #ebeef5;
  background: #f8f9fa;
}

.login-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: calc(100% - 60px);
  overflow-y: auto;
}

.login-dialog :deep(.el-dialog__headerbtn) {
  top: 20px;
  right: 20px;
  font-size: 20px;
}

.login-dialog :deep(.el-dialog__close) {
  color: #909399;
}

.login-dialog :deep(.el-dialog__close:hover) {
  color: #409EFF;
}

/* 确保父容器有相对定位 */
.diary-view-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.storage-btn {
  width: 36px;
  height: 36px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.storage-btn:hover {
  transform: scale(1.1);
}

.storage-btn .el-icon {
  font-size: 18px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.fade-dialog-enter-active, .fade-dialog-leave-active {
  transition: all 0.35s cubic-bezier(0.4,0,0.2,1);
}
.fade-dialog-enter-from, .fade-dialog-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
.fade-dialog-enter-to, .fade-dialog-leave-from {
  opacity: 1;
  transform: scale(1);
}

/* 添加标签样式 */
.diary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 12px;
  margin-bottom: 8px;
}

.tag-item {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #909399 0%, #a6a9ad 100%);
  backdrop-filter: blur(4px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.tag-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

/* 添加登录相关样式 */
.nav-user-area {
  display: flex;
  align-items: center;
  position: relative;
}

.login-nav-btn {
  margin-left: 16px;
  padding: 8px 22px;
  background: #6cb4ff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.login-nav-btn:hover {
  background: #409eff;
}

.avatar-wrapper {
  position: relative;
  outline: none;
  cursor: pointer;
}

.avatar {
  width: 40px;
  height: 40px;
  background: #6cb4ff;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  user-select: none;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
  transition: box-shadow 0.2s;
}

.avatar-wrapper:focus .avatar,
.avatar-wrapper:hover .avatar {
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.25);
}

.dropdown-menu {
  position: absolute;
  top: 48px;
  right: 0;
  min-width: 120px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  z-index: 100;
  padding: 8px 0;
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.2s;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.dropdown-item {
  padding: 10px 20px;
  color: #333;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s;
}

.dropdown-item:hover {
  background: #f5f7fa;
}

/* 登录弹窗动画 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

.main-login-dialog {
  position: fixed;
  top: 0;
  left: 70px;
  width: calc(100vw - 70px);
  height: 100vh;
  background: transparent;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .main-login-dialog {
    left: 0;
    width: 100vw;
  }
}
</style>
