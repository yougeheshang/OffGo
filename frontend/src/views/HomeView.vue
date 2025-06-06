<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SearchBar from '@/components/SearchBar.vue'
import UserPanel from '@/components/UserPanel.vue'
import { Star, View, Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import LoginRegisterForm from '@/components/LoginRegisterForm.vue'
import Masonry from 'vue3-masonry'
import AttractionDetailDialog from '@/components/AttractionDetailDialog.vue'
import InterestSelectDialog from '@/components/InterestSelectDialog.vue'
import axios from 'axios'

interface TravelCard {
  id: number
  title: string
  description: string
  image: string
  hot: number
  rating: number
  style?: Record<string, string>
  areaType: '校区' | '景区'
  tags: string[]
  userHasRated?: boolean
  userRating?: number
}

const router = useRouter()
const cards = ref<TravelCard[]>([])
const isLoggedIn = ref(false)
const username = ref('')
const showLoginDialog = ref(false)
const showInterestDialog = ref(false)
const userInterests = ref<string[]>([])

// 添加调试日志
const debugLog = (message: string, data?: any) => {
  console.log(`[Debug] ${message}`, data || '')
}

// 请求参数
const searchParams = ref({
  keyword: '',
  sortField: 'hot',
  sortOrder: 'desc'
})

// 添加获取图片URL的函数
const getImageUrl = (imageId: string) => {
  if (!imageId) {
    console.warn('No image ID provided')
    return ''
  }
  
  // 如果已经是完整URL，直接返回
  if (imageId.startsWith('http')) {
    return imageId
  }
  
  // 如果路径以/开头，说明是后端返回的完整路径
  if (imageId.startsWith('/')) {
    return imageId
  }
  
  // 从完整路径中提取图片ID
  const imageIdMatch = imageId.match(/([^/]+\.(jpg|jpeg|png|webp))$/i)
  const finalImageId = imageIdMatch ? imageIdMatch[1] : imageId
  
  // 对文件名进行URL编码
  const encodedImageId = encodeURIComponent(finalImageId)
  
  // 使用相对路径
  return `/images/attraction/${encodedImageId}`
}

// 添加图片加载错误处理
const handleImageError = (e: Event) => {
  const imgElement = e.target as HTMLImageElement
  console.error('图片加载失败:', {
    src: imgElement.src,
    alt: imgElement.alt,
    timestamp: new Date().toISOString()
  })
}

// 添加图片加载成功处理
const handleImageLoad = (imageUrl: string) => {
  console.log('图片加载成功:', {
    url: imageUrl,
    timestamp: new Date().toISOString()
  })
}

// 获取数据
const fetchCards = async () => {
  try {
    debugLog('开始获取景点卡片数据...')
    const query = new URLSearchParams({
      ...searchParams.value,
    }).toString()
    debugLog('请求参数:', searchParams.value)

    const response = await fetch(`http://localhost:8050/api/attraction/getCards?${query}`)
    debugLog('API响应状态:', response.status)
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    const data = await response.json()
    debugLog('获取到的原始数据:', data)
    
    if (!data || !data.items) {
      throw new Error('Invalid response format')
    }
    
    // 为每个卡片添加完整的图片URL
    const processedCards = data.items.map((card: TravelCard) => {
      debugLog('处理卡片数据:', card)
      
      const processedCard = {
        ...card,
        image: getImageUrl(card.image)
      }
      
      debugLog('处理后的卡片数据:', processedCard)
      
      return processedCard
    })

    // 根据排序字段和顺序对卡片进行排序
    processedCards.sort((a: TravelCard, b: TravelCard) => {
      const field = searchParams.value.sortField
      const order = searchParams.value.sortOrder
      const multiplier = order === 'asc' ? 1 : -1
      
      if (field === 'hot') {
        return multiplier * ((a.hot || 0) - (b.hot || 0))
      } else if (field === 'rating') {
        return multiplier * ((a.rating || 0) - (b.rating || 0))
      }
      return 0
    })
    
    cards.value = processedCards
    debugLog('所有卡片处理完成，总数:', cards.value.length)
  } catch (error) {
    console.error('数据获取失败:', error)
    ElMessage.error('获取数据失败，请稍后重试')
    cards.value = [] // Clear cards on error
  }
}

// 处理搜索
const handleSearch = (keyword: string) => {
  searchParams.value.keyword = keyword
  fetchCards()
}

// 处理排序
const handleSortChange = (params: { field: string, order?: string }) => {
  searchParams.value.sortField = params.field
  searchParams.value.sortOrder = params.order || 'desc'
  if (params.field === 'smart') {
    smartSortCards()
  } else {
  fetchCards()
  }
}

// 智能排序函数
const smartSortCards = async () => {
  const interests = userInterests.value
  await fetchCards()
  cards.value.sort((a, b) => {
    // 1. 兴趣重合度
    const aMatch = a.tags ? a.tags.filter(tag => interests.includes(tag)).length : 0
    const bMatch = b.tags ? b.tags.filter(tag => interests.includes(tag)).length : 0
    // 2. 评分
    const aRating = a.rating || 0
    const bRating = b.rating || 0
    // 3. 热度
    const aHot = a.hot || 0
    const bHot = b.hot || 0
    // 综合分数
    const aScore = aMatch * 10000 + aRating * 1000 + aHot * 10
    const bScore = bMatch * 10000 + bRating * 1000 + bHot * 10
    return bScore - aScore
  })
}

// 点击卡片（热度更新）
const handleCardClick = async (cardId: number) => {
  try {
    // 重置评分状态
    if (currentAttraction.value) {
      currentAttraction.value = {
        ...currentAttraction.value,
        userHasRated: false,
        userRating: 0
      }
    }

    // 发送点击事件
    await fetch(`http://localhost:8050/api/attraction/click`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ id: cardId })
    })

    // 本地更新热度（+1）
    cards.value = cards.value.map(card =>
      card.id === cardId
        ? { ...card, hot: card.hot + 1 }
        : card
    )

    // 获取景点详情
    const response = await fetch(`http://localhost:8050/api/attraction/getCards?id=${cardId}`)
    if (!response.ok) {
      throw new Error('Failed to fetch attraction data')
    }
    const data = await response.json()
    if (data.items && data.items.length > 0) {
      const targetAttraction = data.items.find((item: TravelCard) => item.id === cardId)
      if (targetAttraction) {
        // 确保新加载的景点数据包含评分状态
        currentAttraction.value = {
          ...targetAttraction,
          userHasRated: false,
          userRating: 0
        }
        showDetailDialog.value = true
      } else {
        ElMessage.error('未找到对应的景点信息')
      }
    } else {
      ElMessage.error('未找到景点信息')
    }
  } catch (error) {
    console.error('获取景点详情失败:', error)
    ElMessage.error('获取景点详情失败')
  }
}

// 添加弹窗相关的响应式变量
const showDetailDialog = ref(false)
const currentAttraction = ref<TravelCard | null>(null)

// 添加跳转到计划页面的函数
const goToPlan = (attractionId: number) => {
  router.push(`/plan/${attractionId}`)
}

const openLoginDialog = () => {
  showLoginDialog.value = true
}

const handleLoginSuccess = () => {
  isLoggedIn.value = true
  username.value = localStorage.getItem('username') || ''
  showLoginDialog.value = false
}

const handleLogout = () => {
  localStorage.removeItem('username')
  localStorage.removeItem('userId')
  localStorage.removeItem('userRole')
  isLoggedIn.value = false
  username.value = ''
  ElMessage.success('已退出登录')
}

const handleSwitchAccount = () => {
  handleLogout()
  showLoginDialog.value = true
}

const getAvatarLetter = () => {
  if (!username.value) return ''
  return username.value.charAt(0).toUpperCase()
}

const showMenu = ref(false)
const toggleMenu = () => {
  showMenu.value = !showMenu.value
}
const closeMenu = () => {
  showMenu.value = false
}

const handleInterestSubmit = async (interests: string[]) => {
  userInterests.value = interests
  showInterestDialog.value = false
  const username = localStorage.getItem('username')
  if (username) {
    await axios.post(`/api/user/interests?username=${username}`, interests)
    ElMessage.success('兴趣已更新')
  } else {
    ElMessage.error('未获取到用户名，无法保存兴趣')
  }
}

const handleModifyInterest = () => {
  showMenu.value = false
  showInterestDialog.value = true
}

// 添加评分处理函数
const handleRating = async (rating: number) => {
  if (!currentAttraction.value) return;
  
  try {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      ElMessage.warning('请先登录后再评分');
      return;
    }

    const response = await fetch('http://localhost:8050/api/attraction/rate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        attractionId: currentAttraction.value.id,
        userId: parseInt(userId),
        rating: rating
      })
    });

    if (!response.ok) {
      const errorData = await response.text();
      console.error('评分请求失败:', response.status, errorData);
      throw new Error(`评分失败: ${errorData}`);
    }

    const data = await response.json();
    console.log('评分响应:', data);
    
    // 更新当前景点的评分状态
    if (currentAttraction.value) {
      currentAttraction.value.userHasRated = true;
      currentAttraction.value.userRating = rating;
      currentAttraction.value.rating = data.newRating;
    }

    // 更新卡片列表中的评分
    cards.value = cards.value.map(card => {
      if (card.id === currentAttraction.value?.id) {
        return {
          ...card,
          rating: data.newRating
        };
      }
      return card;
    });

    ElMessage.success('评分成功！');
  } catch (error) {
    console.error('评分失败:', error);
    ElMessage.error('评分失败，请稍后重试');
  }
};

// 添加处理日记评分更新的函数
const handleDiaryRatingUpdated = (data: { diaryId: number, newRating: number }) => {
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

// 生命周期
onMounted(async () => {
  debugLog('组件挂载')
  const storedUsername = localStorage.getItem('username')
  if (storedUsername) {
    isLoggedIn.value = true
    username.value = storedUsername
    // 自动获取用户兴趣
    try {
      const res = await axios.get(`/api/user/interests?username=${storedUsername}`)
      userInterests.value = Array.isArray(res.data) ? res.data : []
    } catch (e) {
      userInterests.value = []
    }
  }
  fetchCards()
})
</script>

<template>
  <header class="search-header">
    <SearchBar
      @search="handleSearch"
      @sort-change="handleSortChange"
    />
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
    <!-- 登录弹窗只覆盖主内容区，背景透明 -->
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
  
  <!-- 使用 vue3-masonry 替代原有组件 -->
  <div class="masonry-container">
    <div v-if="cards.length === 0" class="no-data-message">
      暂无数据，请稍后重试
    </div>
    <div v-else class="masonry-grid">
      <div
        v-for="card in cards"
        :key="card.id"
        class="masonry-item"
        @click="handleCardClick(card.id)"
      >
        <div class="card-image-container">
          <img 
            :src="card.image" 
            alt="封面图片" 
            class="card-image"
            @error="handleImageError"
            @load="handleImageLoad(card.image)"
          >
          <div class="area-type-tag" :class="card.areaType === '校区' ? 'campus' : 'scenic'">
            {{ card.areaType }}
          </div>
        </div>
        <div class="card-content">
          <h3>{{ card.title }}</h3>
          <p>{{ card.description }}</p>
          <div class="card-tags">
            <span 
              v-for="tag in card.tags" 
              :key="tag" 
              class="tag-item"
            >
              {{ tag }}
            </span>
          </div>
        </div>
        <div class="card-footer">
          <div class="card-stats">
            <span class="stat-item">
              <el-icon><Star /></el-icon>
              {{ card.rating?.toFixed(1) || '0.0' }}
            </span>
            <span class="stat-item">
              <el-icon><View /></el-icon>
              {{ card.hot || 0 }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 景点详情对话框 -->
  <transition name="fade-dialog">
    <AttractionDetailDialog
      v-model:visible="showDetailDialog"
      :attraction="currentAttraction"
      @go-to-plan="goToPlan"
      @rate="handleRating"
      @rating-updated="handleDiaryRatingUpdated"
    />
  </transition>
</template>

<style scoped>
.search-header {
  width: calc(100% - 70px);
  display: flex;
  position: fixed;
  justify-content: space-between;
  background-color: #ffffff;
  align-items: center;
  padding: 15px 20px;
  margin-left: 70px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  z-index: 10;
}

/* 使用 vue3-masonry 替代原有组件 */
.masonry-container {
  margin-left: 80px;
  margin-right: 24px;
  margin-top: 80px;
  padding: 0 12px;
}

.masonry-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 28px;
  width: 100%;
}

.masonry-item {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.65);
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(64, 158, 255, 0.10), 0 1.5px 6px rgba(0,0,0,0.04);
  overflow: hidden;
  transition: box-shadow 0.25s, transform 0.25s, background 0.25s;
  cursor: pointer;
  position: relative;
  backdrop-filter: blur(12px);
  border: 1.5px solid rgba(255,255,255,0.35);
}

.masonry-item:hover {
  box-shadow: 0 12px 32px rgba(64, 158, 255, 0.18), 0 2px 8px rgba(0,0,0,0.08);
  transform: translateY(-8px) scale(1.025);
  background: rgba(255,255,255,0.82);
}

.card-image-container {
  width: 100%;
  aspect-ratio: 4/3;
  overflow: hidden;
  background: linear-gradient(135deg, #e3f0ff 0%, #f8fafd 100%);
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.masonry-item:hover .card-image {
  transform: scale(1.045);
}

.card-content {
  padding: 20px 22px 10px 22px;
  text-align: center;
  border-bottom: 1px solid #f0f2f5;
  background: linear-gradient(135deg, #fafdff 0%, #f7faff 100%);
}

.card-content h3 {
  margin: 0 0 10px 0;
  font-size: 20px;
  font-weight: 800;
  color: #409EFF;
  line-height: 1.3;
  letter-spacing: 0.5px;
  text-shadow: 0 1px 2px rgba(64,158,255,0.06);
}

.card-content p {
  font-size: 15.5px;
  color: #555;
  line-height: 1.7;
  margin: 0;
  min-height: 38px;
  word-break: break-all;
}

.card-footer {
  padding: 12px 22px 16px 22px;
  background: linear-gradient(90deg, #f8f9fa 0%, #fafdff 100%);
  border-top: 1px solid #ebeef5;
  text-align: center;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.card-stats {
  display: flex;
  gap: 20px;
  color: #909399;
  font-size: 15px;
  flex-wrap: wrap;
  justify-content: center;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 7px;
  background: linear-gradient(90deg, #e6f7ff 0%, #fafdff 100%);
  border-radius: 16px;
  padding: 5px 16px;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(64,158,255,0.06);
  transition: background 0.2s, color 0.2s;
}

.stat-item:hover {
  color: #409EFF;
  background: #e3f0ff;
}

.stat-item .el-icon {
  font-size: 17px;
}

/* 响应式布局 */
@media (max-width: 1600px) {
  .masonry-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 1200px) {
  .masonry-grid { grid-template-columns: repeat(2, 1fr); }
  
  .search-header {
    padding: 15px;
  }
}

@media (max-width: 768px) {
  .masonry-grid { grid-template-columns: 1fr; }
  .masonry-container {
    margin-left: 12px;
    margin-right: 12px;
  }
  
  .search-header {
    width: 100%;
    margin-left: 0;
    padding: 10px;
  }
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

.nav-user-area {
  display: flex;
  align-items: center;
  position: relative;
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

.no-data-message {
  text-align: center;
  padding: 40px;
  font-size: 16px;
  color: #909399;
  background: #f5f7fa;
  border-radius: 8px;
  margin: 20px;
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

/* 添加区域类型标签样式 */
.area-type-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: white;
  backdrop-filter: blur(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1;
}

.area-type-tag.campus {
  background: linear-gradient(135deg, #409EFF 0%, #36a3ff 100%);
}

.area-type-tag.scenic {
  background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
}

.card-tags {
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
</style>
