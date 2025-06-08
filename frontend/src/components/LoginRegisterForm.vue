<template>
  <div class="login-bg" @click.self="handleOutsideClick">
    <div class="login-card">
      <h2 class="login-title">登录 <span class="brand">OffGo</span></h2>
      <form class="login-form" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="username"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
            required
          />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            required
          />
        </div>
        <button class="login-btn" :disabled="loading" type="submit">
          {{ isLogin ? '登录' : '注册' }}
        </button>
        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
      </form>
      <div class="switch-mode">
        <span>{{ isLogin ? '还没有账号？' : '已有账号？' }}</span>
        <a href="#" @click.prevent="switchMode">{{ isLogin ? '立即注册' : '去登录' }}</a>
      </div>
    </div>
    <InterestSelectDialog
      v-if="showInterestDialog"
      :visible="showInterestDialog"
      :userInterests="userInterests"
      @close="showInterestDialog = false"
      @submit="handleInterestSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, defineEmits } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE_URL } from '../config'
import { useUserStore } from '@/stores/user'
import InterestSelectDialog from './InterestSelectDialog.vue'

const emit = defineEmits(['login-success', 'close'])
const isLogin = ref(true)
const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')
const userStore = useUserStore()
const showInterestDialog = ref(false)
const userInterests = ref<string[]>([])

const usernamePattern = /^[A-Za-z0-9]{6,10}$/
const passwordPattern = /^[A-Za-z0-9]{6,15}$/

onMounted(() => {
  const storedUsername = localStorage.getItem('username')
  if (storedUsername) {
    username.value = storedUsername
  }
})

const handleSubmit = async () => {
  errorMsg.value = ''
  if (!username.value || !password.value) {
    errorMsg.value = '请输入账号和密码'
    return
  }
  if (!usernamePattern.test(username.value)) {
    errorMsg.value = '账号必须6-10位，只能用数字或英文字母'
    return
  }
  if (!passwordPattern.test(password.value)) {
    errorMsg.value = '密码必须6-15位，只能用数字或英文字母'
    return
  }
  loading.value = true
  try {
    if (isLogin.value) {
      const response = await axios.get(`${API_BASE_URL}/sign_in_name/${username.value}/${password.value}`)
      if (response.data) {
        const userIdResponse = await axios.get(`${API_BASE_URL}/get_user_id/${username.value}`)
        const userId = userIdResponse.data
        const userInfoResponse = await axios.get(`${API_BASE_URL}/get_user_info/${username.value}`)
        const userRole = userInfoResponse.data.role
        localStorage.setItem('username', username.value)
        localStorage.setItem('userId', userId.toString())
        localStorage.setItem('userRole', userRole)
        userStore.login(username.value, userId)
        ElMessage.success('登录成功')
        handleLoginSuccess()
      } else {
        errorMsg.value = '账号或密码错误'
      }
    } else {
      const response = await axios.get(`${API_BASE_URL}/sign_up/${username.value}/${password.value}`)
      if (response.data > 0) {
        const userInfoResponse = await axios.get(`${API_BASE_URL}/get_user_info/${username.value}`)
        const userRole = userInfoResponse.data.role
        localStorage.setItem('username', username.value)
        localStorage.setItem('userId', response.data.toString())
        localStorage.setItem('userRole', userRole)
        userStore.login(username.value, response.data)
        ElMessage.success('注册成功')
        handleLoginSuccess()
      } else if (response.data === 0) {
        errorMsg.value = '账号已被注册'
      } else {
        errorMsg.value = '注册失败，请检查账号和密码格式'
      }
    }
  } catch (error) {
    errorMsg.value = '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

const switchMode = () => {
  isLogin.value = !isLogin.value
  username.value = ''
  password.value = ''
  errorMsg.value = ''
}

const handleOutsideClick = () => {
  emit('close')
}

const handleInterestSubmit = (interests: string[]) => {
  userInterests.value = interests
  showInterestDialog.value = false
  // 这里可以调用后端保存兴趣接口
  emit('login-success')
}

const handleLoginSuccess = () => {
  // 登录/注册成功后弹出兴趣选择
  showInterestDialog.value = true
}
</script>

<style scoped>
.login-bg {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  position: relative;
}

.login-bg::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: -1;
}

.login-card {
  position: relative;
  z-index: 1;
  background: rgba(35, 39, 47, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 18px;
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.25);
  padding: 40px 36px 28px 36px;
  width: 370px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.login-title {
  color: #fff;
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 28px;
  letter-spacing: 1px;
  text-align: center;
}
.brand {
  color: #6cb4ff;
  font-weight: 800;
}
.login-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.form-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
}
.form-group label {
  color: #bfc9d1;
  font-size: 15px;
  margin-bottom: 6px;
  font-weight: 500;
}
.form-group input {
  width: 100%;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(24, 26, 32, 0.6);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  color: #fff;
  font-size: 16px;
  outline: none;
  margin-bottom: 2px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
  transition: all 0.2s;
}
.form-group input:focus {
  border: 1.5px solid #6cb4ff;
}
.login-btn {
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
.login-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}
.login-btn:hover:not(:disabled) {
  background: #409eff;
}
.switch-mode {
  margin-top: 18px;
  color: #bfc9d1;
  font-size: 15px;
  text-align: center;
}
.switch-mode a {
  color: #6cb4ff;
  margin-left: 4px;
  text-decoration: none;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s;
}
.switch-mode a:hover {
  color: #409eff;
}
.error-msg {
  color: #ff4d4f;
  text-align: center;
  margin-top: 6px;
  font-size: 15px;
  font-weight: 500;
}
</style> 