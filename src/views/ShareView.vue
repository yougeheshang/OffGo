<template>
  <div class="share-view">
    <el-upload
      class="upload-demo"
      action="#"
      :auto-upload="false"
      :on-change="handleFileChange"
      :on-remove="handleFileRemove"
      :file-list="fileList"
      multiple
      accept="image/*"
      :limit="10"
    >
      <template #trigger>
        <el-button type="primary">选择图片</el-button>
      </template>
      <template #tip>
        <div class="el-upload__tip">
          请选择图片文件，支持jpg/png格式，最多10张图片
        </div>
      </template>
    </el-upload>

    <div class="video-info" v-if="fileList.length > 0">
      <el-form :model="videoForm" label-width="100px">
        <el-form-item label="视频标题">
          <el-input v-model="videoForm.title" placeholder="请输入视频标题"></el-input>
        </el-form-item>
        <el-form-item label="视频描述">
          <el-input
            v-model="videoForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入视频描述"
          ></el-input>
        </el-form-item>
        <el-form-item label="视频风格">
          <el-select v-model="videoForm.style" placeholder="请选择视频风格">
            <el-option label="旅行" value="travel"></el-option>
            <el-option label="生活" value="life"></el-option>
            <el-option label="美食" value="food"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <el-button 
      @click="handleVideoGen" 
      type="primary" 
      :loading="loading"
      :disabled="!fileList.length || !videoForm.title || !videoForm.description || !videoForm.style"
      style="margin-top: 20px;"
    >
      生成视频
    </el-button>

    <el-progress 
      v-if="loading"
      :percentage="uploadProgress"
      :format="progressFormat"
      style="margin-top: 20px;"
    ></el-progress>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const fileList = ref([])
const loading = ref(false)
const uploadProgress = ref(0)

const videoForm = reactive({
  title: '',
  description: '',
  style: ''
})

const handleFileChange = (file) => {
  console.log('文件已选择:', file)
  if (file.raw) {
    fileList.value.push(file)
  }
}

const handleFileRemove = (file) => {
  const index = fileList.value.indexOf(file)
  if (index !== -1) {
    fileList.value.splice(index, 1)
  }
}

const progressFormat = (percentage) => {
  return percentage === 100 ? '处理完成' : `${percentage}%`
}

const handleVideoGen = async () => {
  if (!fileList.value.length) {
    ElMessage.warning('请先选择图片')
    return
  }

  if (!videoForm.title || !videoForm.description || !videoForm.style) {
    ElMessage.warning('请填写完整的视频信息')
    return
  }

  try {
    loading.value = true
    uploadProgress.value = 0
    const formData = new FormData()
    formData.append('title', videoForm.title)
    formData.append('description', videoForm.description)
    formData.append('style', videoForm.style)
    
    // 添加所有选中的图片
    fileList.value.forEach((file, index) => {
      if (file.raw) {
        console.log(`正在添加第 ${index + 1} 个文件:`, file.raw.name)
        formData.append('files', file.raw)
      }
    })

    console.log('准备发送请求，文件数量:', fileList.value.length)

    const response = await axios.post('http://localhost:8050/api/videos/generate', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        uploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
      }
    })

    if (response.data) {
      ElMessage.success('视频生成成功')
      console.log('生成的视频信息:', response.data)
      // 这里可以添加视频预览或下载的逻辑
    }
  } catch (error) {
    console.error('视频生成失败:', error)
    let errorMessage = '视频生成失败'
    
    if (error.response) {
      const errorData = error.response.data
      if (errorData && errorData.error) {
        errorMessage = errorData.error
      } else {
        errorMessage = `服务器错误: ${error.response.status}`
      }
      console.error('服务器响应:', error.response.data)
    } else if (error.request) {
      errorMessage = '无法连接到服务器，请检查网络连接'
    } else {
      errorMessage = error.message || '未知错误'
    }
    
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
    uploadProgress.value = 0
  }
}
</script>

<style scoped>
.share-view {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.upload-demo {
  margin-bottom: 20px;
}

.el-upload__tip {
  margin-top: 8px;
  color: #666;
}

.video-info {
  margin: 20px 0;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #fafafa;
}
</style> 