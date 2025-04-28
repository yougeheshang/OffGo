<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, View } from '@element-plus/icons-vue'
import type { UploadProps, UploadUserFile } from 'element-plus'
import axios from 'axios'

const title = ref('')
const content = ref('')
const description = ref('')
const destination = ref('')
const fileList = ref<UploadUserFile[]>([])

// 添加已发布日记列表
const publishedDiaries = ref<any[]>([])
const showPublishedDiaries = ref(false)

// 获取已发布的日记
const fetchPublishedDiaries = async () => {
  try {
    const response = await fetch(`http://localhost:8050/get/diaries?sortField=1&sortOrder=desc&keyword=&searchType=name`)
    if (response.ok) {
      publishedDiaries.value = await response.json()
    } else {
      throw new Error('获取已发布日记失败')
    }
  } catch (error: any) {
    ElMessage.error('获取已发布日记失败：' + error.message)
  }
}

// 删除日记
const deleteDiary = async (diaryId: number) => {
  try {
    // 先检查日记是否存在
    const checkResponse = await axios.get(`http://localhost:8050/get/diary/${diaryId}`)
    if (!checkResponse.data) {
      ElMessage.error('日记不存在')
      return
    }

    const confirmed = await ElMessageBox.confirm(
      '确定要删除这篇日记吗？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    if (confirmed) {
      const response = await axios.delete(`http://localhost:8050/api/diary/${diaryId}`)
      if (response.status === 200) {
        ElMessage.success('删除成功')
        // 刷新日记列表
        await fetchPublishedDiaries()
      }
    }
  } catch (error: any) {
    if (error.response?.status === 404) {
      ElMessage.error('日记不存在')
    } else {
      ElMessage.error('删除失败：' + (error.response?.data || error.message))
    }
  }
}

// 查看已发布日记
const viewPublishedDiaries = () => {
  showPublishedDiaries.value = true
  fetchPublishedDiaries()
}

// 关闭已发布日记列表
const closePublishedDiaries = () => {
  showPublishedDiaries.value = false
}

// 在组件挂载时获取已发布日记
onMounted(() => {
  fetchPublishedDiaries()
})

const handleSubmit = async () => {
  try {
    // 首先上传所有图片
    const imageIds = []
    for (const file of fileList.value) {
      // 将文件转换为Base64
      const reader = new FileReader()
      const base64Data = await new Promise<string>((resolve) => {
        reader.onload = () => {
          const base64String = reader.result as string
          // 移除Base64 URL的前缀（如 "data:image/jpeg;base64,"）
          const base64Data = base64String.split(',')[1]
          resolve(base64Data)
        }
        reader.readAsDataURL(file.raw!)
      })
      
      // 创建URLSearchParams对象
      const params = new URLSearchParams()
      params.append('imageData', base64Data)
      
      const response = await fetch('http://localhost:8050/saveImage', {
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
        const errorText = await response.text()
        throw new Error('上传图片失败: ' + errorText)
      }
    }

    // 创建日记
    const apiUrl = imageIds.length > 0 ? 'http://localhost:8050/savediary_withimage' : 'http://localhost:8050/savediary_withoutimage'
    const params = new URLSearchParams({
      title: title.value,
      description: description.value,
      content: content.value,
      userID: '1', // 默认用户 ID
      destination: destination.value
    })

    if (imageIds.length > 0) {
      params.append('image_num', imageIds.length.toString())
    }

    const response = await fetch(`${apiUrl}?${params.toString()}`, {
      method: 'POST'
    })

    if (response.ok) {
      ElMessage.success('日记发布成功！')
      // 清空表单
      title.value = ''
      content.value = ''
      description.value = ''
      destination.value = ''
      fileList.value = []
    } else {
      throw new Error('发布失败')
    }
  } catch (error: any) {
    ElMessage.error('发布失败：' + error.message)
  }
}

const handleExceed: UploadProps['onExceed'] = (files) => {
  ElMessage.warning(
    '最多只能上传 5 张图片'
  )
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB！')
  }
  return isImage && isLt5M
}
</script>

<template>
  <div class="share-container">
    <h1>分享你的旅行日记</h1>
    
    <div class="action-buttons">
      <el-button type="primary" @click="viewPublishedDiaries">
        <el-icon><View /></el-icon>
        查看已发布日记
      </el-button>
    </div>
    
    <el-form @submit.prevent="handleSubmit" class="share-form">
      <el-form-item label="标题" required>
        <el-input v-model="title" placeholder="请输入日记标题" />
      </el-form-item>

      <el-form-item label="简介" required>
        <el-input v-model="description" type="textarea" :rows="2" placeholder="请输入日记简介" />
      </el-form-item>

      <el-form-item label="目的地" required>
        <el-input v-model="destination" placeholder="请输入旅行目的地" />
      </el-form-item>

      <el-form-item label="正文" required>
        <el-input v-model="content" type="textarea" :rows="6" placeholder="请输入日记正文" />
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
        >
          <el-icon><Plus /></el-icon>
          <template #tip>
            <div class="el-upload__tip">
              支持 jpg/png 文件，单个文件不超过 5MB，最多上传 5 张图片
            </div>
          </template>
        </el-upload>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" native-type="submit">发布日记</el-button>
      </el-form-item>
    </el-form>
    
    <!-- 已发布日记列表对话框 -->
    <el-dialog
      v-model="showPublishedDiaries"
      title="已发布的日记"
      width="80%"
      :before-close="closePublishedDiaries"
    >
      <div v-if="publishedDiaries.length === 0" class="no-diaries">
        您还没有发布任何日记
      </div>
      <div v-else class="diaries-list">
        <el-card v-for="diary in publishedDiaries" :key="diary.id" class="diary-card">
          <template #header>
            <div class="diary-header">
              <h3>{{ diary.title }}</h3>
              <el-button type="danger" size="small" @click="deleteDiary(diary.id)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
          <div class="diary-content">
            <p><strong>简介：</strong>{{ diary.description }}</p>
            <p><strong>目的地：</strong>{{ diary.destination }}</p>
            <p><strong>发布时间：</strong>{{ new Date(diary.createTime).toLocaleString() }}</p>
          </div>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.share-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.share-container h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #409EFF;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}

.share-form {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.el-upload__tip {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}

.diaries-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.diary-card {
  margin-bottom: 15px;
}

.diary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.diary-header h3 {
  margin: 0;
}

.diary-content {
  font-size: 14px;
}

.no-diaries {
  text-align: center;
  padding: 30px;
  color: #909399;
  font-size: 16px;
}
</style>
