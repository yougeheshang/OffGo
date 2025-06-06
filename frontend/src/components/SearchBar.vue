<script setup lang="ts">
  import { ref } from 'vue'
  import { Search, VideoCamera } from '@element-plus/icons-vue'
  import { ElMessage } from 'element-plus'

  const props = defineProps<{
    videoUrl?: string
  }>()
  
  const emit = defineEmits(['search', 'sort-change', 'show-video'])
  const inputValue = ref('')
  const sortField = ref('smart')
  const sortOrder = ref('desc')
  const sortFields = [
    { value: 'hot', label: '按热度排序' },
    { value: 'rating', label: '按评分排序' },
    { value: 'smart', label: '智能排序' }
  ]
  const sortOrders = [
    { value: 'desc', label: '从大到小' },
    { value: 'asc', label: '从小到大' }
  ]

  const handleSearch = () => {
    emit('search', inputValue.value)
  }

  const handleSortChange = () => {
    emit('sort-change', {
      field: sortField.value,
      order: sortOrder.value
    })
  }

  const handleShowVideo = () => {
    if (!props.videoUrl) {
      ElMessage.warning('暂无生成的视频')
      return
    }
    emit('show-video')
  }
</script>

<template>
  <div class="search-bar">
    <div class="search-input-wrapper">
      <input
        type="text"
        placeholder="搜索旅游目的地..."
        v-model="inputValue"
        @keyup.enter="handleSearch"
      >
      <el-button 
        type="primary" 
        class="search-button"
        @click="handleSearch"
      >
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
    </div>
    <!-- 排序控件 -->
    <el-select
      v-model="sortField"
      @change="handleSortChange"
      placeholder="请选择排序方式"
      clearable
      style="width: 150px"
    >
      <el-option
        v-for="item in sortFields"
        :key="item.value"
        :label="item.label"
        :value="item.value"
      />
    </el-select>
    <!-- 排序顺序选择 -->
    <el-select
      v-if="sortField !== 'smart'"
      v-model="sortOrder"
      @change="handleSortChange"
      placeholder="排序顺序"
      style="width: 120px;"
    >
      <el-option
        v-for="order in sortOrders"
        :key="order.value"
        :label="order.label"
        :value="order.value"
      />
    </el-select>
  </div>
</template>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 200px;
}

.title-section h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.video-button {
  display: flex;
  align-items: center;
  gap: 4px;
}

.search-input-wrapper {
  flex: 1;
  display: flex;
  gap: 8px;
  min-width: 700px;
}

.search-input-wrapper input {
  flex: 1;
  padding: 12px 20px;
  border: 1px solid #DCDCDC;
  border-radius: 8px;
  font-size: 14px;
}

.search-button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 20px;
  height: 100%;
  border-radius: 8px;
}

.search-button .el-icon {
  margin-right: 4px;
}
</style>
