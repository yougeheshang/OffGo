<script setup lang="ts">
import { ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps<{
  locations: any[]
}>()

const emit = defineEmits(['select'])

const searchQuery = ref('')
const showResults = ref(false)
const searchResults = ref<any[]>([])

// 监听搜索输入
watch(searchQuery, (newQuery) => {
  if (newQuery.trim()) {
    searchResults.value = props.locations.filter(location => 
      location.name && location.name.toLowerCase().includes(newQuery.toLowerCase())
    )
    showResults.value = true
  } else {
    showResults.value = false
  }
})

// 处理选择景点
const handleSelect = (location: any) => {
  emit('select', location)
  searchQuery.value = ''
  showResults.value = false
}

// 点击外部关闭搜索结果
const handleClickOutside = () => {
  showResults.value = false
}
</script>

<template>
  <div class="location-search">
    <div class="search-input-wrapper">
      <el-input
        v-model="searchQuery"
        placeholder="搜索景点..."
        class="search-input"
        :prefix-icon="Search"
        clearable
      />
    </div>
    
    <!-- 搜索结果下拉框 -->
    <div v-if="showResults && searchResults.length > 0" class="search-results">
      <div
        v-for="location in searchResults"
        :key="location.id"
        class="search-result-item"
        @click="handleSelect(location)"
      >
        <img 
          :src="`/public/images/markers/marker-icon-2x-${location.crowdLevel === 0 ? 'green' : location.crowdLevel === 1 ? 'yellow' : 'red'}.png`"
          class="location-icon"
          alt="location icon"
        />
        <span class="location-name">{{ location.name }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.location-search {
  position: relative;
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
  z-index: 1000;
}

.search-input-wrapper {
  padding: 10px;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 8px;
  backdrop-filter: blur(4px);
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.1);
  box-shadow: none;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.search-input :deep(.el-input__inner) {
  color: white;
}

.search-input :deep(.el-input__prefix-inner) {
  color: rgba(255, 255, 255, 0.7);
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.9);
  border-radius: 8px;
  margin-top: 5px;
  max-height: 300px;
  overflow-y: auto;
  backdrop-filter: blur(4px);
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.search-result-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.location-icon {
  width: 20px;
  height: 32px;
  margin-right: 10px;
}

.location-name {
  color: white;
  font-size: 14px;
}

/* 自定义滚动条样式 */
.search-results::-webkit-scrollbar {
  width: 6px;
}

.search-results::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.search-results::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

.search-results::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.4);
}
</style> 