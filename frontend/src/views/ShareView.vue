<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useRouter } from 'vue-router'
import type { UploadFile } from 'element-plus'
import { IMAGE_BASE_URL } from '../config'

interface Diary {
  id?: number
  title: string
  content: string
  images: Array<{
    name: string
    url: string
  }>
  createTime?: string
}

const activeTab = ref('diary')
const diaries = ref<Diary[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('写日记')
const currentDiary = ref<Diary>({
  title: '',
  content: '',
  images: []
})
const fileList = ref<UploadFile[]>([])
const isEdit = ref(false)
const router = useRouter()

const fetchDiaries = async () => {
  try {
    console.log('开始获取日记列表...')
    const response = await axios.get('/api/diaries')
    console.log('获取到的原始数据:', response.data)
    
    if (!response.data) {
      console.error('返回数据为空')
      ElMessage.error('获取日记列表失败：返回数据为空')
      return
    }
    
    diaries.value = response.data.map((diary: any) => ({
      id: diary.id,
      title: diary.title || '无标题',
      content: diary.content || '无内容',
      createTime: diary.createTime,
      images: Array.isArray(diary.images) ? diary.images.map((url: string) => ({
        name: url.split('/').pop() || '',
        url: url.startsWith('http') ? url : `${IMAGE_BASE_URL}${url.replace('/uploads/', '')}`
      })) : []
    }))
    console.log('处理后的数据:', diaries.value)
  } catch (error: any) {
    console.error('获取日记列表失败:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status,
      config: {
        url: error.config?.url,
        method: error.config?.method,
        baseURL: axios.defaults.baseURL
      }
    })
    ElMessage.error(`获取日记列表失败: ${error.response?.data?.message || error.message || '未知错误'}`)
  }
}

const showAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '写日记'
  currentDiary.value = {
    title: '',
    content: '',
    images: []
  }
  fileList.value = []
  dialogVisible.value = true
}

const editDiary = (diary: Diary) => {
  isEdit.value = true
  dialogTitle.value = '编辑日记'
  currentDiary.value = { ...diary }
  fileList.value = diary.images.map(image => ({
    name: image.name,
    url: image.url.startsWith('http') ? image.url : `${IMAGE_BASE_URL}${image.url}`,
    status: 'success',
    uid: Date.now()
  }))
  dialogVisible.value = true
}

const handleImageChange = (file: UploadFile) => {
  fileList.value.push(file)
}

const handleImageSuccess = (response: { url: string }, file: UploadFile) => {
  if (response && response.url) {
    const imageUrl = response.url.replace('/uploads/', '')
    currentDiary.value.images.push({
      name: file.name,
      url: `${IMAGE_BASE_URL}${imageUrl}`
    })
    fileList.value.push({
      name: file.name,
      url: `${IMAGE_BASE_URL}${imageUrl}`,
      status: 'success',
      uid: Date.now()
    })
  }
}

const handleImageRemove = (file: UploadFile) => {
  const index = currentDiary.value.images.findIndex(img => img.url === file.url)
  if (index > -1) {
    currentDiary.value.images.splice(index, 1)
  }
  
  const fileIndex = fileList.value.findIndex(f => f.url === file.url)
  if (fileIndex > -1) {
    fileList.value.splice(fileIndex, 1)
  }
}

const handleSubmit = async () => {
  try {
    if (!currentDiary.value.title || !currentDiary.value.content) {
      ElMessage.warning('请填写标题和内容')
      return
    }

    const diaryData = {
      title: currentDiary.value.title,
      content: currentDiary.value.content,
      images: currentDiary.value.images.map(img => {
        // 从完整URL中提取文件名
        return img.url.split('/').pop() || ''
      })
    }

    console.log('提交的数据:', diaryData)

    if (isEdit.value && currentDiary.value.id) {
      await axios.put(`/api/diaries/${currentDiary.value.id}`, diaryData)
      ElMessage.success('更新成功')
    } else {
      await axios.post('/api/diaries', diaryData)
      ElMessage.success('保存成功')
    }
    
    // 重置表单
    currentDiary.value = {
      title: '',
      content: '',
      images: []
    }
    fileList.value = []
    dialogVisible.value = false
    
    // 重新获取日记列表
    await fetchDiaries()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (id?: number) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要删除这篇日记吗？', '提示', {
      type: 'warning'
    })
    await axios.delete(`/api/diaries/${id}`)
    fetchDiaries()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除日记失败:', error)
    }
  }
}

const formatTime = (time?: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

const viewDiary = (diary: Diary) => {
  if (diary.id) {
    router.push({ name: 'diary-detail', params: { id: diary.id } })
  }
}

onMounted(() => {
  fetchDiaries()
})
</script>

<template>
  <div class="share-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="日记" name="diary">
        <div class="diary-list">
          <el-card class="diary-card" v-for="diary in diaries" :key="diary.id">
            <template #header>
              <div class="card-header">
                <span>{{ diary.title }}</span>
                <div class="card-actions">
                  <el-button type="primary" @click="editDiary(diary)">编辑</el-button>
                  <el-button type="danger" @click="handleDelete(diary.id)">删除</el-button>
                </div>
              </div>
            </template>
            <div class="diary-content">
              <p>{{ diary.content }}</p>
              <div class="image-gallery" v-if="diary.images && diary.images.length > 0">
                <el-image 
                  v-for="(image, index) in diary.images" 
                  :key="index"
                  :src="image.url"
                  :preview-src-list="diary.images.map(i => i.url)"
                  fit="cover"
                  class="diary-image"
                />
              </div>
            </div>
            <div class="diary-footer">
              <span class="time">{{ formatTime(diary.createTime) }}</span>
            </div>
          </el-card>

          <el-button type="primary" class="add-button" @click="showAddDialog">写日记</el-button>

          <!-- 添加/编辑日记对话框 -->
          <el-dialog
            :title="dialogTitle"
            v-model="dialogVisible"
            width="60%"
          >
            <el-form :model="currentDiary" label-width="80px">
              <el-form-item label="标题">
                <el-input v-model="currentDiary.title" />
              </el-form-item>
              <el-form-item label="内容">
                <el-input
                  v-model="currentDiary.content"
                  type="textarea"
                  :rows="4"
                />
              </el-form-item>
              <el-form-item label="图片">
                <el-upload
                  class="upload-demo"
                  action="http://localhost:8070/api/diaries/upload"
                  :on-success="handleImageSuccess"
                  :on-remove="handleImageRemove"
                  :file-list="currentDiary.images"
                  list-type="picture-card"
                  :limit="9"
                >
                  <el-icon><Plus /></el-icon>
                </el-upload>
              </el-form-item>
            </el-form>
            <template #footer>
              <span class="dialog-footer">
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit">保存</el-button>
              </span>
            </template>
          </el-dialog>
        </div>
      </el-tab-pane>
      <!-- 其他标签页内容 -->
    </el-tabs>
  </div>
</template>

<style scoped>
.share-container {
  padding: 20px;
  margin-left: 70px; /* 为左侧导航栏留出空间 */
  min-height: 100vh;
  box-sizing: border-box;
  width: calc(100% - 70px); /* 减去左侧导航栏的宽度 */
}

.diary-list {
  max-width: 800px;
  margin: 0 auto;
  padding-bottom: 80px; /* 为底部按钮留出空间 */
  width: 100%; /* 确保列表宽度正确 */
}

.diary-card {
  margin-bottom: 20px;
  width: 100%; /* 确保卡片宽度正确 */
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.diary-content {
  margin: 10px 0;
}

.image-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.diary-image {
  width: 150px;
  height: 150px;
  object-fit: cover;
  border-radius: 4px;
}

.diary-footer {
  display: flex;
  justify-content: flex-end;
  color: #999;
  font-size: 12px;
}

.add-button {
  position: fixed;
  right: 30px;
  bottom: 30px;
  z-index: 100;
}

.card-actions {
  display: flex;
  gap: 10px;
}
</style>

