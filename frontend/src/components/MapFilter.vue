<template>
  <div class="map-filter" :class="{ 'map-filter--open': isOpen }">
    <div class="map-filter__header">
      <button class="map-filter__nav-btn" @click="prevFilter">
        <span class="arrow">&#60;</span>
      </button>
      <span class="map-filter__title">筛选器: {{ currentFilter }}</span>
      <button class="map-filter__nav-btn" @click="nextFilter">
        <span class="arrow">&#62;</span>
      </button>
    </div>
    
    <div class="map-filter__content">
      <template v-if="currentFilter === '类型'">
        <div v-for="(count, type) in typeCounts" :key="type" 
             class="filter-item" 
             @click="toggleType(type)">
          <div class="filter-item__radio" :class="{ 'filter-item__radio--selected': selectedTypes.includes(type) }">
            <div class="filter-item__radio-inner"></div>
          </div>
          <span class="filter-item__label">{{ type }}</span>
          <span class="filter-item__count">({{ count }})</span>
        </div>
      </template>
      
      <template v-else>
        <div v-for="(count, level) in crowdLevelCounts" :key="level" 
             class="filter-item" 
             @click="toggleCrowdLevel(level)">
          <div class="filter-item__radio" :class="{ 'filter-item__radio--selected': selectedCrowdLevels.includes(level) }">
            <div class="filter-item__radio-inner"></div>
          </div>
          <span class="filter-item__label">{{ level }}</span>
          <span class="filter-item__count">({{ count }})</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  locations: any[]
}>()

const emit = defineEmits<{
  (e: 'filterChange', selectedTypes: string[], selectedCrowdLevels: string[]): void
}>()

const isOpen = ref(true)
const currentFilter = ref('类型')
const selectedTypes = ref<string[]>([])
const selectedCrowdLevels = ref<string[]>([])

// 计算每种类型的数量
const typeCounts = computed(() => {
  const counts: Record<string, number> = {}
  props.locations.forEach(location => {
    if (location.type && location.type !== 'start_point') {
      counts[location.type] = (counts[location.type] || 0) + 1
    }
  })
  return counts
})

// 计算每种拥挤度的数量
const crowdLevelCounts = computed(() => {
  const counts: Record<string, number> = {
    '一般': 0,
    '拥挤': 0,
    '十分拥挤': 0
  }
  props.locations.forEach(location => {
    if (location.crowdLevel !== null) {
      const level = location.crowdLevel === 0 ? '一般' : 
                   location.crowdLevel === 1 ? '拥挤' : '十分拥挤'
      counts[level]++
    }
  })
  return counts
})

// 切换筛选器类型
const nextFilter = () => {
  currentFilter.value = currentFilter.value === '类型' ? '拥挤度' : '类型'
}

const prevFilter = () => {
  currentFilter.value = currentFilter.value === '类型' ? '拥挤度' : '类型'
}

// 切换类型选择
const toggleType = (type: string) => {
  const index = selectedTypes.value.indexOf(type)
  if (index === -1) {
    selectedTypes.value.push(type)
  } else {
    selectedTypes.value.splice(index, 1)
  }
  emitFilterChange()
}

// 切换拥挤度选择
const toggleCrowdLevel = (level: string) => {
  const index = selectedCrowdLevels.value.indexOf(level)
  if (index === -1) {
    selectedCrowdLevels.value.push(level)
  } else {
    selectedCrowdLevels.value.splice(index, 1)
  }
  emitFilterChange()
}

// 发送筛选变化事件
const emitFilterChange = () => {
  emit('filterChange', selectedTypes.value, selectedCrowdLevels.value)
}
</script>

<style scoped>
.map-filter {
  position: absolute;
  left: 30px;
  top: 120px;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  width: 250px;
  transition: all 0.3s ease;
  z-index: 1000;
  color: white;
}

.map-filter__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}

.map-filter__title {
  font-size: 14px;
  font-weight: 500;
  color: white;
}

.map-filter__nav-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  color: #ccc;
  transition: color 0.3s ease;
}

.map-filter__nav-btn:hover {
  color: #1890ff;
}

.arrow {
  font-size: 16px;
  transition: transform 0.3s ease;
}

.map-filter__nav-btn:hover .arrow {
  transform: scale(1.2);
}

.map-filter__content {
  padding: 8px 0;
  max-height: 400px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.5) rgba(0, 0, 0, 0.2);
}

.map-filter__content::-webkit-scrollbar {
  width: 8px;
}

.map-filter__content::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
}

.map-filter__content::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.5);
  border-radius: 4px;
}

.filter-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.filter-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.filter-item__radio {
  width: 16px;
  height: 16px;
  border: 2px solid #d9d9d9;
  border-radius: 50%;
  margin-right: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.filter-item__radio--selected {
  border-color: #1890ff;
  background-color: #1890ff;
}

.filter-item__radio-inner {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: white;
  transform: scale(0);
  transition: transform 0.3s ease;
}

.filter-item__radio--selected .filter-item__radio-inner {
  transform: scale(1);
}

.filter-item__label {
  flex: 1;
  font-size: 14px;
  color: white;
}

.filter-item__count {
  font-size: 12px;
  color: #ccc;
  margin-left: 4px;
}
</style> 