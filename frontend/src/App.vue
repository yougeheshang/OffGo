<script setup lang="ts">
  import VerticalNav from '@/components/VerticalNav.vue'
</script>

<template>
  <!-- 应用根容器 -->
  <div class="app-container">
    <!-- 左侧垂直导航 -->
    <VerticalNav />
    <!-- 主内容区域 -->
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.app-container {
  /* 弹性布局容器 */
  display: flex;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.main-content {
  /* 主内容区自适应 */
  flex: 1;
}

/* 响应式处理 */
@media (max-width: 768px) {
  .main-content {
    margin-left: 0;
    padding: 10px;
  }
}
</style>
