<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, View, Star, Document, Picture, Location, ZoomOut, Refresh, ZoomIn, Close, VideoCamera } from '@element-plus/icons-vue'
import type { UploadProps, UploadUserFile } from 'element-plus'
import axios from 'axios'

const fileList = ref<UploadUserFile[]>([])
const videoList = ref<UploadUserFile[]>([])

// 添加新的响应式变量
const sortField = ref(1) // 1: 评分, 2: 热度, 3: 时间
const sortOrder = ref('desc') // 默认降序
const loading = ref(false)
const diaries = ref<any[]>([])
const AIvideoURL = ref<string>('') // 添加AI视频URL存储
const showVideoURLDialog = ref(false) // 添加显示视频URL的对话框控制

// 写游记相关
const showWriteDialog = ref(false)
const submitting = ref(false)
const diaryForm = ref({
  title: '',
  description: '',
  content: '',
  destination: '',
  tags: [] // 新增标签字段
})

// 添加视频生成相关的响应式变量
const showVideoGenDialog = ref(false)
const videoGenForm = ref({
  title: '',
  description: '',
  style: 'travel'
})
const videoGenFiles = ref<UploadUserFile[]>([])
const videoGenLoading = ref(false)
const videoGenProgress = ref(0)

// 添加查看日记相关的响应式变量
const showViewDialog = ref(false)
const currentDiary = ref<any>(null)
const videoUrl = ref<string | null>(null)

const videoError = ref<string | null>(null)
const videoDebug = ref(true) // 开启调试信息
const videoLoadStatus = ref('未开始加载')

// 添加图片预览相关的响应式变量
const showImageViewer = ref(false)
const currentImageIndex = ref(0)
const scale = ref(1)
const isDragging = ref(false)
const startX = ref(0)
const startY = ref(0)
const translateX = ref(0)
const translateY = ref(0)

const tagOptions = [
  '自然风光', '历史人文', '休闲度假', '网红打卡', '城市地标',
  '校园探索', '校园生活', '学术资源', '科研风光', '户外探险'
]

// 获取已发布的日记
const fetchPublishedDiaries = async () => {
  loading.value = true;
  try {
    const userId = localStorage.getItem('userId');
    const userRole = localStorage.getItem('userRole');
    console.log('Fetching diaries - User ID:', userId, 'User Role:', userRole);
    
    if (!userId) {
      ElMessage.error('用户未登录');
      return;
    }
    
    // 构建请求参数
    const params = {
      sortField: sortField.value,
      sortOrder: sortOrder.value,
      keyword: '',
      searchType: 'name',
      userId: userRole === 'ADMIN' ? null : userId
    };
    
    console.log('Fetching diaries with params:', params);
    const response = await axios.get('http://localhost:8050/api/diary/getdiarys', { params });
    
    if (response.data.code === 200) {
      const diaryData = response.data.data.data;
      console.log('获取到的日记数据:', diaryData);
      
      // 处理日记数据
      const processedDiaries = diaryData.map((diary: any) => {
        // 处理图片URL
        let imageUrl = '';
        if (diary.image) {
          const imageId = diary.image.split(',')[0].trim();
          if (imageId) {
            imageUrl = `http://localhost:8050/api/diary/getimage/${imageId}`;
          }
        }
        
        return {
          ...diary,
          imageUrl,
          title: diary.title || '无标题',
          description: diary.description || '暂无描述',
          destination: diary.destination || '未知地点',
          rating: Number(diary.rating) || 0,
          hot: Number(diary.hot) || 0
        };
      });
      
      console.log('处理后的日记数据:', processedDiaries);
      console.log('当前 diaries 值:', diaries.value);
      diaries.value = processedDiaries;
      console.log('更新后的 diaries 值:', diaries.value);
    } else {
      throw new Error(response.data.msg || '获取已发布日记失败');
    }
  } catch (error: any) {
    console.error('获取日记失败:', error);
    ElMessage.error('获取已发布日记失败：' + (error.response?.data?.msg || error.message));
  } finally {
    loading.value = false;
  }
};

// 获取视频URL
const getVideoUrl = async (videoId: number) => {
  try {
    const response = await axios.get(`http://localhost:8050/api/videos/${videoId}`)
    if (response.data.type === 'remote') {
      return response.data.url
    }
    return `http://localhost:8050/api/videos/${videoId}`
  } catch (error) {
    console.error('获取视频URL失败:', error)
    return null
  }
}

// 查看日记详情
const viewDiary = async (diaryId: number) => {
  try {
    console.log('=== 开始获取日记详情 ===')
    console.log('日记ID:', diaryId)
    
    const response = await axios.get(`http://localhost:8050/api/diary/getdiary/${diaryId}`)
    console.log('API响应数据:', response.data)
    
    if (response.data) {
      currentDiary.value = response.data
      
      // 获取内容
      if (response.data.contentId) {
        try {
          const contentResponse = await axios.get(`http://localhost:8050/api/diary/getcontent/${response.data.contentId}`)
          if (contentResponse.data) {
            currentDiary.value.content = contentResponse.data.content
          }
        } catch (error: any) {
          console.error('获取内容失败:', error)
          ElMessage.error('获取日记内容失败')
        }
      }

      // 处理视频
      if (response.data.videoId) {
        console.log('=== 处理视频 ===')
        console.log('视频ID:', response.data.videoId)
        videoUrl.value = await getVideoUrl(response.data.videoId)
        console.log('生成的视频URL:', videoUrl.value)
        videoLoadStatus.value = '准备加载视频'
      } else {
        console.log('日记没有关联的视频')
        videoUrl.value = null
      }
      
      showViewDialog.value = true
    } else {
      console.error('API返回数据为空')
      ElMessage.error('获取日记详情失败')
    }
  } catch (error: any) {
    console.error('=== 获取日记详情失败 ===')
    console.error('错误信息:', error.message)
    ElMessage.error('获取日记详情失败：' + error.message)
  }
}

// 确认删除日记
const confirmDelete = async (diaryId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这篇日记吗？此操作不可恢复。',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await deleteDiary(diaryId)
  } catch (error) {
    // 用户取消删除
    console.log('取消删除')
  }
}

// 删除日记
const deleteDiary = async (diaryId: number) => {
  try {
    console.log('=== 开始删除日记 ===');
    // 获取用户信息
    const userId = localStorage.getItem('userId')
    const userRole = localStorage.getItem('userRole')
    
    console.log('用户信息:', {
      userId,
      userRole,
      diaryId
    });
    
    if (!userId) {
      console.error('用户未登录');
      ElMessage.error('请先登录')
      return
    }

    // 先检查日记是否存在
    console.log('检查日记是否存在...');
    console.log('请求URL:', `http://localhost:8050/api/diary/getdiary/${diaryId}`);
    const checkResponse = await axios.get(`http://localhost:8050/api/diary/getdiary/${diaryId}`)
    console.log('日记检查响应:', checkResponse.data);
    
    if (!checkResponse.data) {
      console.error('日记不存在');
      ElMessage.error('日记不存在')
      return
    }

    // 检查权限
    const isAdmin = userRole === 'ADMIN'
    const currentUserId = parseInt(userId)
    const diaryUserId = parseInt(checkResponse.data.userID)
    const isOwner = diaryUserId === currentUserId
    
    console.log('权限检查:', {
      isAdmin,
      isOwner,
      diaryUserId,
      currentUserId
    });
    
    if (!isAdmin && !isOwner) {
      console.error('权限不足');
      ElMessage.error('您没有权限删除这篇日记')
      return
    }

    // 发送删除请求
    const deleteUrl = `http://localhost:8050/api/diary/${diaryId}?userId=${currentUserId}`;
    console.log('发送删除请求:', deleteUrl);
    const response = await axios.delete(deleteUrl)
    console.log('删除响应:', response);
    
    if (response.status === 200) {
      console.log('删除成功');
      ElMessage.success('删除成功')
      // 刷新日记列表
      await fetchPublishedDiaries()
    }
  } catch (error: any) {
    console.error('=== 删除失败 ===');
    console.error('错误类型:', error.name);
    console.error('错误信息:', error.message);
    console.error('错误状态:', error.response?.status);
    console.error('错误数据:', error.response?.data);
    console.error('请求配置:', error.config);
    
    if (error.response?.status === 404) {
      ElMessage.error('日记不存在')
    } else if (error.response?.status === 403) {
      ElMessage.error('没有权限删除这篇日记')
    } else {
      ElMessage.error('删除失败：' + (error.response?.data?.msg || error.message))
    }
  }
}

// 获取图片URL
const getImageUrl = (imageId: string) => {
  if (!imageId) {
    console.warn('No image ID provided')
    return ''
  }
  const url = `http://localhost:8050/api/diary/getimage/${imageId}`
  console.log('Generated image URL:', url)
  return url
}

// 处理图片加载错误
const handleImageError = (e: Event) => {
  console.log('Image load error:', e)
  const imgElement = e.target as HTMLImageElement
  console.log('Failed image URL:', imgElement.src)
  imgElement.style.display = 'none'
  const placeholder = imgElement.parentElement?.querySelector('.diary-image-placeholder') as HTMLElement | null
  if (placeholder) {
    placeholder.style.display = 'flex'
  }
}

// 处理图片加载成功
const handleImageLoad = (imageUrl: string) => {
  console.log('Image loaded successfully:', imageUrl)
}

// 处理关闭对话框
const handleCloseDialog = () => {
  if (diaryForm.value.title || diaryForm.value.content) {
    ElMessageBox.confirm(
      '确定要关闭吗？未保存的内容将会丢失',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    ).then(() => {
      showWriteDialog.value = false
      resetForm()
    }).catch(() => {})
  } else {
    showWriteDialog.value = false
    resetForm()
  }
}

// 重置表单
const resetForm = () => {
  diaryForm.value = {
    title: '',
    description: '',
    content: '',
    destination: '',
    tags: []
  }
  fileList.value = []
  videoList.value = []
}

// 提交游记
const submitDiary = async () => {
  if (!diaryForm.value.title || !diaryForm.value.content || !diaryForm.value.description || !diaryForm.value.destination) {
    ElMessage.warning('请填写完整信息')
    return
  }

  if (!diaryForm.value.tags || diaryForm.value.tags.length < 1) {
    ElMessage.warning('请至少选择1个标签')
    return
  }
  if (diaryForm.value.tags.length > 3) {
    ElMessage.warning('最多只能选择3个标签')
    return
  }

  submitting.value = true
  try {
    // 获取当前登录用户的ID
    const userId = localStorage.getItem('userId')
    if (!userId) {
      ElMessage.error('用户未登录')
      return
    }

    // 首先上传所有图片
    const imageIds = []
    for (const file of fileList.value) {
      const reader = new FileReader()
      const base64Data = await new Promise<string>((resolve) => {
        reader.onload = () => {
          const base64String = reader.result as string
          const base64Data = base64String.split(',')[1]
          resolve(base64Data)
        }
        reader.readAsDataURL(file.raw!)
      })
      
      const params = new URLSearchParams()
      params.append('imageData', base64Data)
      
      const response = await fetch('http://localhost:8050/api/diary/saveImage', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: params
      })
      
      if (response.ok) {
        const imageId = await response.text()
        imageIds.push(parseInt(imageId))
      } else {
        throw new Error('上传图片失败')
      }
    }

    // 上传视频（如果有）
    let videoId = null
    if (videoList.value.length > 0) {
      const videoFile = videoList.value[0].raw
      if (videoFile) {
        const formData = new FormData()
        formData.append('video', videoFile)
        
        try {
          console.log('Uploading video file:', videoFile.name)
          const response = await fetch('http://localhost:8050/api/videos/saveVideo', {
          method: 'POST',
          body: formData
        })
        
        if (response.ok) {
            const videoData = await response.json()
            console.log('Video upload response:', videoData)
            videoId = videoData.id
        } else {
            const errorData = await response.text()
            console.error('Video upload failed:', errorData)
            throw new Error(`上传视频失败: ${errorData}`)
          }
        } catch (error: any) {
          console.error('Video upload error:', error)
          throw new Error('上传视频失败：' + error.message)
        }
      }
    }

    // 创建游记
    const apiUrl = imageIds.length > 0 ? 'http://localhost:8050/api/diary/savediary_withimage' : 'http://localhost:8050/api/diary/savediary_withoutimage'
    const params = new URLSearchParams({
      title: diaryForm.value.title,
      description: diaryForm.value.description,
      content: diaryForm.value.content,
      userID: userId,
      destination: diaryForm.value.destination,
      tags: diaryForm.value.tags.join(',')
    })

    if (imageIds.length > 0) {
      params.append('image_num', imageIds.length.toString())
      params.append('image', imageIds.join(','))
    }

    if (videoId) {
      console.log('Adding video ID to diary:', videoId)
      params.append('video_id', videoId.toString())
    }

    console.log('Saving diary with params:', Object.fromEntries(params.entries()))
    console.log('API URL:', apiUrl)
    const response = await fetch(apiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: params
    })

    console.log('Save diary response:', response)
    if (response.ok) {
      ElMessage.success('游记发布成功！')
      showWriteDialog.value = false
      resetForm()
      await fetchPublishedDiaries()
    } else {
      const errorData = await response.text()
      console.error('Save diary failed:', errorData)
      throw new Error('发布失败: ' + errorData)
    }
  } catch (error: any) {
    console.error('发布失败:', error)
    ElMessage.error('发布失败：' + error.message)
  } finally {
    submitting.value = false
  }
}

const handleExceed: UploadProps['onExceed'] = () => {
  ElMessage.warning(
    '最多只能上传 5 张图片'
  )
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  const isVideo = file.type.startsWith('video/')
  if (!isImage && !isVideo) {
    ElMessage.error('只能上传图片或视频文件！')
    return false
  }
  const isLt300M = file.size / 1024 / 1024 < 300
  if (!isLt300M) {
    ElMessage.error('文件大小不能超过 300MB！')
    return false
  }
  return (isImage || isVideo) && isLt300M
}

// 处理视频加载错误
const handleVideoError = (e: Event) => {
  const videoElement = e.target as HTMLVideoElement
  console.error('Video error:', videoElement.error)
  videoError.value = '视频加载失败，请稍后重试'
  videoLoadStatus.value = '加载失败'
}

const handleVideoLoaded = () => {
  videoError.value = null
  videoLoadStatus.value = '元数据已加载'
}

const handleVideoLoadStart = () => {
  console.log('Video load started')
  videoLoadStatus.value = '开始加载'
}

const handleVideoCanPlay = () => {
  console.log('Video can play')
  videoLoadStatus.value = '可以播放'
}

// 处理图片点击
const handleImageClick = (imageId: string, index: number) => {
  currentImageIndex.value = index
  showImageViewer.value = true
  resetImageState()
}

// 重置图片状态
const resetImageState = () => {
  scale.value = 1
  translateX.value = 0
  translateY.value = 0
}

// 处理缩放
const handleZoom = (delta: number) => {
  const newScale = scale.value + delta
  if (newScale >= 0.5 && newScale <= 3) {
    scale.value = newScale
  }
}

// 处理鼠标滚轮缩放
const handleWheel = (e: WheelEvent) => {
  e.preventDefault()
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  handleZoom(delta)
}

// 处理鼠标按下
const handleMouseDown = (e: MouseEvent) => {
  // 切换拖动状态
  isDragging.value = !isDragging.value
  
  if (isDragging.value) {
    // 开始拖动时记录起始位置
    startX.value = e.clientX - translateX.value
    startY.value = e.clientY - translateY.value
  }
}

// 处理鼠标移动
const handleMouseMove = (e: MouseEvent) => {
  if (isDragging.value) {
    translateX.value = e.clientX - startX.value
    translateY.value = e.clientY - startY.value
  }
}

// 处理鼠标松开
const handleMouseUp = () => {
  // 不在这里重置拖动状态，保持点击切换的逻辑
}

// 处理鼠标离开
const handleMouseLeave = () => {
  // 鼠标离开时自动结束拖动
  isDragging.value = false
}

// 修改关闭图片预览函数
const closeImageViewer = () => {
  showImageViewer.value = false
  resetImageState()
  isDragging.value = false
}

// 获取用户AI视频URL
const fetchUserVideoURL = async () => {
  try {
    const userId = localStorage.getItem('userId')
    console.log('=== 获取用户视频URL ===')
    console.log('用户id:', userId)
    
    if (!userId) {
      console.log('用户未登录')
      return
    }
    
    const response = await axios.get(`http://localhost:8050/api/user/${userId}/video-url`)
    console.log('API响应:', response.data)
    
    if (response.data && response.data.aiVideoUrl) {
      AIvideoURL.value = response.data.aiVideoUrl
      console.log('=== 视频URL信息 ===')
      console.log('用户id:', userId)
      console.log('ai_video_url:', AIvideoURL.value)
      console.log('==================')
    } else {
      console.log('暂无视频URL')
    }
  } catch (error) {
    console.error('获取视频URL失败:', error)
  }
}

// 添加视频生成相关的方法
const handleVideoGen = async () => {
  if (!videoGenFiles.value.length) {
    ElMessage.warning('请先上传照片或视频')
    return
  }

  // 添加描述长度验证
  if (videoGenForm.value.description.length < 20) {
    ElMessage.warning('描述长度不能少于20个字符')
    return
  }

  videoGenLoading.value = true
  videoGenProgress.value = 0

  try {
    const userId = localStorage.getItem('userId')
    if (!userId) {
      ElMessage.error('用户未登录')
      return
    }

    const formData = new FormData()
    formData.append('title', videoGenForm.value.title)
    formData.append('description', videoGenForm.value.description)
    formData.append('style', videoGenForm.value.style)
    formData.append('userId', userId)
    
    // 修改文件上传方式
    videoGenFiles.value.forEach((file) => {
      formData.append('files', file.raw!)
    })

    console.log('Sending request with data:', {
      title: videoGenForm.value.title,
      description: videoGenForm.value.description,
      style: videoGenForm.value.style,
      userId: userId,
      filesCount: videoGenFiles.value.length
    })

    const response = await axios.post('/api/videos/generate', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          videoGenProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        }
      }
    })

    if (response.data.ok) {
      ElMessage.success('视频生成成功！')
      AIvideoURL.value = response.data.videoUrl
      showVideoGenDialog.value = false
      // 重置表单
      videoGenForm.value = {
        title: '',
        description: '',
        style: 'travel'
      }
      videoGenFiles.value = []
    }
  } catch (error: any) {
    console.error('视频生成失败:', error)
    ElMessage.error('视频生成失败：' + (error.response?.data?.error || error.message))
  } finally {
    videoGenLoading.value = false
    videoGenProgress.value = 0
  }
}

// 添加显示视频URL的方法
const showVideoURL = () => {
  console.log('Current AIvideoURL:', AIvideoURL.value)
  if (!AIvideoURL.value) {
    ElMessage.warning('暂无生成的视频')
    return
  }
  showVideoURLDialog.value = true
}

// 复制链接到剪贴板
const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('链接已复制到剪贴板')
  } catch (err) {
    ElMessage.warning('复制失败，请手动复制链接')
  }
}

// 在组件挂载时获取已发布日记和用户视频URL
onMounted(() => {
  console.log('=== 组件挂载 ===')
  fetchPublishedDiaries()
  fetchUserVideoURL()
})
</script>

<template>
  <div class="share-container">
    <div class="header">
      <div class="header-left">
        <h2>我的游记</h2>
        <el-button 
          type="success" 
          size="small" 
          @click="showVideoURL"
          style="margin-left: 10px;"
        >
          <el-icon><VideoCamera /></el-icon>
          {{ AIvideoURL ? '查看视频' : '暂无视频' }}
        </el-button>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showWriteDialog = true">
          <el-icon><Plus /></el-icon>
          写游记
        </el-button>
        <el-button type="success" @click="showVideoGenDialog = true">
          <el-icon><VideoCamera /></el-icon>
          视频生成
        </el-button>
        <div class="sort-container">
          <el-select v-model="sortField" placeholder="排序方式" @change="fetchPublishedDiaries">
            <el-option label="评分" :value="1" />
            <el-option label="热度" :value="2" />
            <el-option label="时间" :value="3" />
          </el-select>
          <el-select v-model="sortOrder" placeholder="排序顺序" @change="fetchPublishedDiaries">
            <el-option label="从大到小" value="desc" />
            <el-option label="从小到大" value="asc" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 修改视频URL对话框 -->
    <el-dialog
      v-model="showVideoURLDialog"
      title="AI视频链接"
      width="500px"
    >
      <div style="text-align: center; padding: 20px;">
        <div v-if="AIvideoURL" style="background: #f5f7fa; padding: 15px; border-radius: 8px; margin-bottom: 20px;">
          <p style="margin-bottom: 10px; color: #606266;">视频链接：</p>
          <a :href="AIvideoURL" target="_blank" style="color: #409EFF; word-break: break-all; display: inline-block; max-width: 100%;">{{ AIvideoURL }}</a>
        </div>
        <div v-else style="color: #909399; font-size: 14px;">
          暂无生成的视频，请先使用"视频生成"功能生成视频。
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showVideoURLDialog = false">关闭</el-button>
          <el-button v-if="AIvideoURL" type="primary" @click="copyToClipboard(AIvideoURL)">
            复制链接
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 写游记对话框 -->
    <transition name="fade-dialog">
      <el-dialog
        v-model="showWriteDialog"
        title="写游记"
        width="80%"
        :before-close="handleCloseDialog"
      >
        <el-form :model="diaryForm" label-width="80px">
          <el-form-item label="标题" required>
            <el-input v-model="diaryForm.title" placeholder="请输入游记标题" />
          </el-form-item>
          
          <el-form-item label="目的地" required>
            <el-input v-model="diaryForm.destination" placeholder="请输入旅行目的地" />
          </el-form-item>
          
          <el-form-item label="简介" required>
            <el-input
              v-model="diaryForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入游记简介"
            />
          </el-form-item>
          
          <el-form-item label="正文" required>
            <el-input
              v-model="diaryForm.content"
              type="textarea"
              :rows="6"
              placeholder="请输入游记正文"
            />
          </el-form-item>
          
          <el-form-item label="标签" required>
            <el-select
              v-model="diaryForm.tags"
              multiple
              placeholder="请选择1~3个标签"
              :max-collapse-tags="3"
              :collapse-tags="true"
              :multiple-limit="3"
              style="width: 100%;"
            >
              <el-option
                v-for="tag in tagOptions"
                :key="tag"
                :label="tag"
                :value="tag"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="图片">
            <el-upload
              v-model:file-list="fileList"
              action="#"
              list-type="picture-card"
              :auto-upload="false"
              :limit="5"
              :on-exceed="handleExceed"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
              <template #tip>
                <div class="el-upload__tip">
                  支持 jpg/png 文件，单个文件不超过 50MB，最多上传 5 张图片
                </div>
              </template>
            </el-upload>
          </el-form-item>

          <el-form-item label="视频">
            <el-upload
              v-model:file-list="videoList"
              action="#"
              list-type="picture-card"
              :auto-upload="false"
              :limit="1"
              :on-exceed="handleExceed"
              :before-upload="beforeUpload"
              accept="video/*"
            >
              <el-icon><Plus /></el-icon>
              <template #tip>
                <div class="el-upload__tip">
                  支持 mp4/webm 文件，单个文件不超过 300MB，最多上传 1 个视频
                </div>
              </template>
            </el-upload>
          </el-form-item>
        </el-form>
        
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="showWriteDialog = false">取消</el-button>
            <el-button type="primary" @click="submitDiary" :loading="submitting">
              发布
            </el-button>
          </span>
        </template>
      </el-dialog>
    </transition>

    <!-- 查看日记对话框 -->
    <transition name="fade-dialog">
      <el-dialog
        v-model="showViewDialog"
        title="游记详情"
        width="80%"
        class="diary-view-dialog"
      >
        <div v-if="currentDiary" class="diary-detail">
          <div class="diary-detail-header">
            <h2>{{ currentDiary.title }}</h2>
            <div class="diary-detail-meta">
              <span class="destination">
                <el-icon><Location /></el-icon>
                {{ currentDiary.destination }}
              </span>
              <span class="stats">
                <span class="stat-item">
                  <el-icon><Star /></el-icon>
                  {{ currentDiary.rating?.toFixed(1) || '0.0' }}
                </span>
                <span class="stat-item">
                  <el-icon><View /></el-icon>
                  {{ currentDiary.hot || 0 }}
                </span>
              </span>
            </div>
          </div>

          <div class="diary-detail-content">
            <!-- 视频播放器 -->
            <div v-if="videoUrl" class="diary-video">
              <el-carousel :interval="4000" type="card" height="300px">
                <el-carousel-item>
                  <video 
                    :src="videoUrl" 
                    controls 
                    class="video-player"
                    preload="metadata"
                    @error="handleVideoError"
                    @loadedmetadata="handleVideoLoaded"
                    @loadstart="handleVideoLoadStart"
                    @canplay="handleVideoCanPlay"
                  >
                    您的浏览器不支持视频播放
                  </video>
                </el-carousel-item>
              </el-carousel>
              <div v-if="videoError" class="video-error">
                {{ videoError }}
              </div>
              <div v-if="videoDebug" class="video-debug">
                <p>视频URL: {{ videoUrl }}</p>
                <p>加载状态: {{ videoLoadStatus }}</p>
              </div>
            </div>

            <!-- 图片轮播 -->
            <div class="diary-images" v-if="currentDiary.image && currentDiary.image.split(',').filter((id: string) => id.trim() !== '').length > 0">
              <el-carousel :interval="4000" type="card" height="400px">
                <el-carousel-item v-for="(imageId, index) in currentDiary.image.split(',').filter((id: string) => id.trim() !== '')" :key="imageId">
                  <div class="image-container">
                    <img 
                      :src="getImageUrl(imageId)" 
                      :alt="currentDiary.title" 
                      class="carousel-image"
                      @error="handleImageError"
                      @click="handleImageClick(imageId, index)"
                    >
                  </div>
                </el-carousel-item>
              </el-carousel>
            </div>
            <div v-else-if="!videoUrl" class="no-images">
              <el-empty description="暂无图片" />
            </div>

            <div class="diary-description">
              <h3>简介</h3>
              <p style="white-space: pre-wrap;">{{ currentDiary.description }}</p>
            </div>

            <div class="diary-main-content">
              <h3>正文</h3>
              <p v-if="currentDiary.content" style="white-space: pre-wrap;">{{ currentDiary.content }}</p>
              <p v-else class="no-content">暂无正文内容</p>
            </div>
          </div>
        </div>
      </el-dialog>
    </transition>

    <!-- 图片预览对话框 -->
    <transition name="fade-dialog">
      <el-dialog
        v-model="showImageViewer"
        :show-close="false"
        :close-on-click-modal="true"
        :close-on-press-escape="true"
        class="image-viewer-dialog"
        @close="closeImageViewer"
      >
        <div class="image-viewer-container" 
          @wheel="handleWheel"
          @mousedown="handleMouseDown"
          @mousemove="handleMouseMove"
          @mouseup="handleMouseUp"
          @mouseleave="handleMouseLeave"
        >
          <img 
            v-if="currentDiary.image"
            :src="getImageUrl(currentDiary.image.split(',').filter((id: string) => id.trim() !== '')[currentImageIndex])"
            :alt="currentDiary.title"
            class="preview-image"
            :style="{
              transform: `scale(${scale}) translate(${translateX}px, ${translateY}px)`,
              cursor: isDragging ? 'grabbing' : 'grab'
            }"
          >
        </div>
        
        <!-- 控制面板 -->
        <div class="image-controls">
          <el-button-group>
            <el-button @click="handleZoom(-0.1)" :disabled="scale <= 0.5">
              <el-icon><ZoomOut /></el-icon>
            </el-button>
            <el-button @click="resetImageState">
              <el-icon><Refresh /></el-icon>
            </el-button>
            <el-button @click="handleZoom(0.1)" :disabled="scale >= 3">
              <el-icon><ZoomIn /></el-icon>
            </el-button>
          </el-button-group>
          <span class="zoom-level">{{ Math.round(scale * 100) }}%</span>
          <el-button @click="closeImageViewer">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </el-dialog>
    </transition>

    <!-- 添加视频生成对话框 -->
    <el-dialog
      v-model="showVideoGenDialog"
      title="AI视频生成"
      width="50%"
      :close-on-click-modal="false"
    >
      <el-form :model="videoGenForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="videoGenForm.title" placeholder="请输入视频标题" />
        </el-form-item>
        
        <el-form-item label="描述" required>
          <el-input
            v-model="videoGenForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入视频描述（至少20个字符）"
            :minlength="20"
            :maxlength="800"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="风格">
          <el-select v-model="videoGenForm.style" placeholder="请选择视频风格">
            <el-option label="旅行" value="travel" />
            <el-option label="风景" value="landscape" />
            <el-option label="人文" value="cultural" />
            <el-option label="美食" value="food" />
          </el-select>
        </el-form-item>

        <el-form-item label="素材">
          <el-upload
            v-model:file-list="videoGenFiles"
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :limit="10"
            :on-exceed="handleExceed"
            :before-upload="beforeUpload"
            accept="image/*,video/*"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">
                支持图片和视频文件，单个文件不超过 300MB，最多上传 10 个文件
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item v-if="videoGenLoading">
          <el-progress :percentage="videoGenProgress" :format="(percentage: number) => `生成中 ${percentage}%`" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showVideoGenDialog = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleVideoGen" 
            :loading="videoGenLoading"
            :disabled="!videoGenFiles.length"
          >
            开始生成
          </el-button>
        </span>
      </template>
    </el-dialog>

    <div v-if="loading" class="diary-grid">
      <el-skeleton :rows="3" animated v-for="n in 6" :key="n" />
    </div>

    <div v-else-if="!diaries || diaries.length === 0" class="empty-state">
      <el-icon :size="48"><Document /></el-icon>
      <p>暂无游记，快去发布你的第一篇游记吧！</p>
    </div>

    <div v-else class="diary-grid">
      <div v-for="diary in diaries" :key="diary.id" class="diary-card">
        <div class="diary-image-container">
          <template v-if="diary.imageUrl">
            <img 
              :src="diary.imageUrl" 
              :alt="diary.title" 
              class="diary-image"
              @error="handleImageError"
              @load="handleImageLoad(diary.imageUrl)"
            >
          </template>
          <div v-else class="diary-image-placeholder">
            <el-icon :size="32"><Picture /></el-icon>
            <span>暂无图片</span>
          </div>
        </div>
        <div class="diary-content">
          <h3 class="diary-title">{{ diary.title || '无标题' }}</h3>
          <div class="diary-destination">
            <el-icon><Location /></el-icon>
            <span>{{ diary.destination || '未知地点' }}</span>
          </div>
          <p class="diary-description">{{ diary.description || '暂无描述' }}</p>
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
          <div class="diary-actions">
            <el-button type="primary" size="small" @click="viewDiary(diary.id)">
              查看
            </el-button>
            <el-button type="danger" size="small" @click="confirmDelete(diary.id)">
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.share-container {
  padding: clamp(8px, 2vw, 20px);
  max-width: min(1400px, 95vw);
  margin: 0 auto;
  min-height: calc(100vh - 60px);
  background-color: #f5f7fa;
  margin-top: 60px;
  margin-left: clamp(0px, 15vw, 200px);
  width: calc(100% - clamp(0px, 15vw, 200px));
  transition: all 0.3s ease;
  position: relative;
  z-index: 1;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: clamp(20px, 3vw, 30px);
  padding: clamp(12px, 2vw, 20px);
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  flex-wrap: wrap;
  gap: clamp(8px, 2vw, 16px);
}

.header h2 {
  margin: 0;
  font-size: clamp(18px, 2.5vw, 24px);
  color: #303133;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.sort-container {
  display: flex;
  gap: clamp(8px, 2vw, 16px);
  align-items: center;
  flex-wrap: wrap;
}

.sort-container .el-select {
  width: clamp(100px, 15vw, 120px);
}

.diary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  padding: 10px;
  column-gap: 20px;
  column-fill: balance;
}

.diary-card {
  break-inside: avoid;
  margin-bottom: 20px;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  height: fit-content;
}

.diary-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.diary-image-container {
  width: 100%;
  height: 200px;
  overflow: hidden;
  position: relative;
  background-color: #f5f7fa;
}

.diary-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
  background-color: #f5f7fa;
}

.diary-card:hover .diary-image {
  transform: scale(1.05);
}

.diary-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  background-color: #f5f7fa;
}

.diary-image-placeholder span {
  margin-top: 8px;
  font-size: 14px;
}

.diary-content {
  padding: 16px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.diary-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  line-height: 1.4;
}

.diary-destination {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
  font-size: 14px;
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
}

.diary-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-top: 1px solid #ebeef5;
  flex-wrap: wrap;
  gap: 8px;
}

.diary-stats {
  display: flex;
  gap: 16px;
  color: #909399;
  font-size: 13px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.diary-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.diary-actions .el-button {
  padding: 8px 12px;
  font-size: 13px;
}

.diary-actions .el-button--danger {
  background-color: #fef0f0;
  border-color: #fef0f0;
  color: #f56c6c;
}

.diary-actions .el-button--danger:hover {
  background-color: #fde2e2;
  border-color: #fde2e2;
  color: #f56c6c;
}

.diary-actions .el-button--primary {
  background-color: #ecf5ff;
  border-color: #ecf5ff;
  color: #409eff;
}

.diary-actions .el-button--primary:hover {
  background-color: #d9ecff;
  border-color: #d9ecff;
  color: #409eff;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.empty-state i {
  font-size: 48px;
  color: #909399;
  margin-bottom: 16px;
}

.empty-state p {
  color: #909399;
  font-size: 16px;
  margin: 0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .share-container {
    margin-left: 0;
    width: 100%;
    margin-top: 60px;
    padding: 10px;
    box-sizing: border-box;
  }

  .header {
    flex-direction: column;
    align-items: stretch;
    margin-top: 10px;
  }

  .header-actions {
    flex-direction: column;
    width: 100%;
  }

  .sort-container {
    width: 100%;
    justify-content: space-between;
  }

  .diary-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
}

/* 超小屏幕适配 */
@media (max-width: 360px) {
  .share-container {
    margin-top: 50px;
    padding: 8px;
  }

  .diary-stats {
    width: 100%;
    justify-content: space-between;
  }
  
  .diary-actions {
    width: 100%;
    justify-content: space-between;
  }
}

/* 确保内容在导航栏下方 */
@media (max-width: 992px) {
  .share-container {
    margin-left: 0;
    width: 100%;
    box-sizing: border-box;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.el-upload__tip {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
  line-height: 1.4;
}

/* 日记详情对话框样式 */
.diary-view-dialog :deep(.el-dialog) {
  background: rgba(255,255,255,0.75) !important;
  backdrop-filter: blur(14px) !important;
  -webkit-backdrop-filter: blur(14px) !important;
  border-radius: 20px !important;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18), 0 2px 8px rgba(0,0,0,0.10) !important;
}
.diary-view-dialog :deep(.el-dialog__body) {
  background: transparent !important;
}

.diary-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.diary-detail-header {
  text-align: center;
  margin-bottom: 20px;
}

.diary-detail-header h2 {
  font-size: 24px;
  color: #303133;
  margin: 0 0 16px 0;
}

.diary-detail-meta {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  color: #606266;
}

.diary-detail-meta .destination {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 16px;
}

.diary-detail-meta .stats {
  display: flex;
  gap: 16px;
}

.diary-detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.diary-images {
  width: 100%;
  margin-bottom: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
}

.image-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f8f9fa;
  border-radius: 8px;
  overflow: hidden;
}

.carousel-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.no-images {
  width: 100%;
  padding: 40px 0;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.diary-description,
.diary-main-content {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
}

.diary-description h3,
.diary-main-content h3 {
  font-size: 18px;
  color: #303133;
  margin: 0 0 12px 0;
}

.diary-description p,
.diary-main-content p {
  color: #606266;
  font-size: 16px;
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}

.diary-video {
  width: 100%;
  margin-bottom: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
}

.video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

.video-error {
  color: #f56c6c;
  text-align: center;
  padding: 10px;
  background: #fef0f0;
  border-radius: 4px;
  margin-top: 10px;
}

.video-debug {
  margin-top: 10px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
}

.video-debug p {
  margin: 5px 0;
}

.no-content {
  color: #909399;
  text-align: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .diary-view-dialog :deep(.el-dialog) {
    width: 95% !important;
    margin: 10px auto;
  }

  .diary-detail-header h2 {
    font-size: 20px;
  }

  .diary-detail-meta {
    flex-direction: column;
    gap: 8px;
  }

  .diary-description,
  .diary-main-content {
    padding: 16px;
  }

  .diary-description h3,
  .diary-main-content h3 {
    font-size: 16px;
  }

  .diary-description p,
  .diary-main-content p {
    font-size: 14px;
  }
}

.image-viewer-dialog :deep(.el-dialog) {
  margin: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  max-width: none !important;
  background: rgba(0, 0, 0, 0.95);
  border-radius: 0;
}

.image-viewer-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.image-viewer-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  position: relative;
  padding: 20px;
}

.preview-image {
  max-width: 95%;
  max-height: 95%;
  object-fit: contain;
  transition: transform 0.1s ease;
  user-select: none;
  -webkit-user-drag: none;
}

.image-controls {
  position: fixed;
  top: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(0, 0, 0, 0.5);
  padding: 8px 16px;
  border-radius: 8px;
  z-index: 1000;
}

.zoom-level {
  color: white;
  font-size: 14px;
  min-width: 60px;
  text-align: center;
}

.image-controls .el-button {
  background: transparent;
  border: none;
  color: white;
  padding: 8px;
}

.image-controls .el-button:hover {
  background: rgba(255, 255, 255, 0.1);
}

.image-controls .el-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.carousel-image {
  cursor: zoom-in;
  transition: transform 0.3s ease;
}

.carousel-image:hover {
  transform: scale(1.02);
}

/* 写游记弹窗美化 */
:deep(.el-dialog) {
  max-width: 600px !important;
  width: 95vw !important;
  border-radius: 20px !important;
  background: rgba(255,255,255,0.82) !important;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18), 0 2px 8px rgba(0,0,0,0.10) !important;
  backdrop-filter: blur(14px) !important;
  -webkit-backdrop-filter: blur(14px) !important;
  padding: 0 !important;
}
:deep(.el-dialog__header) {
  padding: 18px 28px 0 28px !important;
  font-size: 20px;
  font-weight: 700;
  color: #222;
  border-radius: 20px 20px 0 0;
}
:deep(.el-dialog__body) {
  padding: 18px 28px 10px 28px !important;
  background: transparent !important;
}
:deep(.el-form-item) {
  margin-bottom: 14px !important;
}
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #222;
  font-size: 15px;
}
:deep(.el-input), :deep(.el-textarea) {
  border-radius: 10px !important;
  box-shadow: 0 2px 8px rgba(64,158,255,0.06);
  background: #f8f9fa;
}
:deep(.el-input__inner), :deep(.el-textarea__inner) {
  border-radius: 10px !important;
  background: #f8f9fa !important;
}
:deep(.el-upload--picture-card) {
  border-radius: 12px;
  width: 110px;
  height: 110px;
  font-size: 28px;
  background: #f6f8fa;
  box-shadow: 0 2px 8px rgba(64,158,255,0.06);
}
:deep(.el-upload-list__item) {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(64,158,255,0.08);
}
:deep(.el-upload__tip) {
  color: #909399;
  font-size: 13px;
  margin-top: 6px;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  padding: 10px 28px 18px 28px;
  background: transparent;
}
.dialog-footer .el-button--primary {
  background: linear-gradient(135deg, #409EFF 0%, #36cfc9 100%) !important;
  color: #fff !important;
  border: none !important;
  font-weight: 600;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(64,158,255,0.12);
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
}
.dialog-footer .el-button--primary:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #5cdbd3 100%) !important;
  box-shadow: 0 4px 16px rgba(64,158,255,0.18);
  transform: translateY(-2px) scale(1.04);
}
.dialog-footer .el-button {
  border-radius: 10px;
}

@media (max-width: 600px) {
  :deep(.el-dialog) {
    max-width: 100vw !important;
    width: 100vw !important;
    border-radius: 10px !important;
    padding: 0 !important;
  }
  :deep(.el-dialog__header), :deep(.el-dialog__body) {
    padding-left: 10px !important;
    padding-right: 10px !important;
  }
  .dialog-footer {
    padding-left: 10px;
    padding-right: 10px;
  }
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

.video-gen-dialog :deep(.el-dialog) {
  background: rgba(255,255,255,0.95) !important;
  backdrop-filter: blur(14px) !important;
  -webkit-backdrop-filter: blur(14px) !important;
  border-radius: 20px !important;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18) !important;
}

.video-gen-dialog :deep(.el-progress) {
  margin-top: 20px;
}

.video-gen-dialog :deep(.el-upload--picture-card) {
  width: 120px;
  height: 120px;
  line-height: 120px;
}

.video-gen-dialog :deep(.el-upload-list__item) {
  width: 120px;
  height: 120px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
