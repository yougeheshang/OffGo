<script setup lang="ts">
  import { ref } from 'vue'
  const emit = defineEmits(['search', 'sort-change'])
  const inputValue = ref('')
  const sortField = ref('hot')
  const sortOrder = ref('desc')
  const sortFields = [
    { value: 'hot', label: '按热度排序' },
    { value: 'rating', label: '按评分排序' }
  ]
  const sortOrders = [
    { value: 'desc', label: '降序' },
    { value: 'asc', label: '升序' }
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
</script>

<template>
  <div class="search-bar">
    <input
      type="text"
      placeholder="搜索旅游目的地..."
      v-model="inputValue"
      @keyup.enter="handleSearch"
    >
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

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;

  input {
    flex: 1;
    padding: 12px 20px;
    border: 1px solid 	#DCDCDC;
    border-radius: 8px;
    min-width: 700px;
  }
}

</style>
