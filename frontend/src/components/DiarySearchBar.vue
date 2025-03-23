<template>
  <div class="diary-search-bar">
    <input
      type="text"
      placeholder="搜索日记..."
      v-model="inputValue"
      @keyup.enter="handleSearch"
    >
    <!-- 搜索类型选择 -->
    <el-select
      v-model="searchType"
      @change="handleSearch"
      placeholder="请选择搜索类型"
      clearable
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
      placeholder="排序顺序"
      style="width: 80px;"
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
import { ref } from 'vue';
const emit = defineEmits(['search', 'sort-change']);
const inputValue = ref('');
const searchType = ref('name');
const sortField = ref('hot');
const sortOrder = ref('desc');

const searchTypes = [
  { value: 'name', label: '按日记名称搜索' },
  { value: 'destination', label: '按日记目的地搜索' }
];

const sortFields = [
  { value: 'hot', label: '按热度排序' },
  { value: 'rating', label: '按评分排序' }
];

const sortOrders = [
  { value: 'desc', label: '降序' },
  { value: 'asc', label: '升序' }
];
//后端要替代
const handleSearch = () => {
  emit('search', {
    keyword: inputValue.value,
    type: searchType.value
  });
};
//后端要替代
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

  input {
    flex: 1;
    padding: 12px 20px;
    border: 1px solid #DCDCDC;
    border-radius: 8px;
    min-width: 700px;
  }
}
</style>
