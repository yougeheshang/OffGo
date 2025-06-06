<template>
  <div class="diary-search-bar">
    <div class="search-input-wrapper">
      <input
        type="text"
        placeholder="搜索日记..."
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
    <!-- 搜索类型选择 -->
    <el-select
      v-model="searchType"
      @change="handleSearch"
      placeholder="请选择搜索类型"
      style="width: 150px"
    >
      <el-option
        v-for="type in searchTypes"
        :key="type.value"
        :label="type.label"
        :value="type.value"
      />
    </el-select>
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
      v-model="sortOrder"
      @change="handleSortChange"
      placeholder="排序顺序"
      style="width: 120px;"
      v-if="sortField !== 4"
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

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Search } from '@element-plus/icons-vue';

const emit = defineEmits(['search', 'sort-change']);
const inputValue = ref('');
// 设置默认搜索类型
const defaultSearchType = 'name';
const searchType = ref(defaultSearchType);

// 监听搜索类型变化，确保不会为空
watch(searchType, (newValue) => {
  if (!newValue) {
    searchType.value = defaultSearchType;
  }
});

const sortField = ref(1); // 1: 热度, 2: 评分, 3: 随机, 4: 智能
const sortOrder = ref('desc');

const searchTypes = [
  { value: 'name', label: '按日记名称搜索' },
  { value: 'destination', label: '按目的地搜索' },
  { value: 'full_text', label: '全文搜索' }
];

const sortFields = [
  { value: 1, label: '按热度排序' },
  { value: 2, label: '按评分排序' },
  { value: 4, label: '智能排序' }
];

const sortOrders = [
  { value: 'desc', label: '从大到小' },
  { value: 'asc', label: '从小到大' }
];

// 监听排序字段变化
watch(sortField, (newValue) => {
  if (!sortOrder.value) {
    sortOrder.value = 'desc';
  }
  handleSortChange();
});

// 监听排序顺序变化
watch(sortOrder, () => {
  handleSortChange();
});

const handleSearch = () => {
  emit('search', {
    keyword: inputValue.value,
    type: searchType.value
  });
};

const handleSortChange = () => {
  emit('sort-change', {
    field: sortField.value,
    order: sortOrder.value
  });
};
</script>

<style scoped>
.diary-search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
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

.el-radio-group {
  display: flex;
  gap: 8px;
}

.el-radio-button {
  margin: 0;
}
</style>
