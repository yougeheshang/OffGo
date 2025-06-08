<template>
  <div class="interest-bg" @click.self="close">
    <div class="interest-card">
      <h2 class="interest-title">你的兴趣你做主</h2>
      <div class="interest-tip">选择越多，推荐越丰富</div>
      <div class="interest-list">
        <span
          v-for="tag in tags"
          :key="tag"
          :class="['interest-item', { selected: selected.includes(tag) }]"
          @click="toggle(tag)"
        >{{ tag }}</span>
      </div>
      <button class="interest-btn" :disabled="selected.length === 0" @click="submit">
        选好啦，开启旅程
      </button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, defineEmits, defineProps, watch } from 'vue'
const tags = [
  '自然风光', '历史人文', '休闲度假', '网红打卡', '城市地标',
  '校园探索', '校园生活', '学术资源', '科研风光', '户外探险'
]
const props = defineProps<{ visible: boolean, userInterests?: string[] }>()
const emit = defineEmits(['close', 'submit'])
const selected = ref<string[]>(props.userInterests ? [...props.userInterests] : [])

watch(() => props.userInterests, (val) => {
  selected.value = val ? [...val] : []
})

const toggle = (tag: string) => {
  if (selected.value.includes(tag)) {
    selected.value = selected.value.filter(t => t !== tag)
  } else if (selected.value.length < 5) {
    selected.value.push(tag)
  }
}
const close = () => emit('close')
const submit = () => emit('submit', selected.value)
</script>
<style scoped>
.interest-bg {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  position: fixed;
  left: 0; top: 0; z-index: 9999;
}
.interest-bg::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(255,255,255,0.1);
  backdrop-filter: blur(8px);
  z-index: -1;
}
.interest-card {
  background: rgba(35, 39, 47, 0.85);
  border-radius: 18px;
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.25);
  padding: 38px 32px 28px 32px;
  width: 370px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid rgba(255,255,255,0.1);
  position: relative;
}
.interest-title {
  color: #fff;
  font-size: 1.7rem;
  font-weight: 700;
  margin-bottom: 16px;
  text-align: center;
}
.interest-tip {
  color: #bfc9d1;
  font-size: 15px;
  margin-bottom: 18px;
  text-align: center;
}
.interest-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  margin-bottom: 24px;
}
.interest-item {
  padding: 8px 18px;
  border-radius: 18px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #6cb4ff 0%, #36a3ff 100%);
  cursor: pointer;
  transition: background 0.2s, color 0.2s, box-shadow 0.2s;
  user-select: none;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.10);
}
.interest-item.selected {
  background: linear-gradient(135deg, #ffb347 0%, #ffcc80 100%);
  color: #333;
  box-shadow: 0 4px 16px rgba(255, 179, 71, 0.18);
}
.interest-btn {
  width: 100%;
  padding: 12px 0;
  background: #6cb4ff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}
.interest-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}
.interest-btn:hover:not(:disabled) {
  background: #409eff;
}
</style> 