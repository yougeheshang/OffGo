import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import axios from 'axios'
import { API_BASE_URL, IMAGE_BASE_URL } from './config'

// 配置axios
axios.defaults.baseURL = API_BASE_URL
axios.defaults.withCredentials = true

// 添加请求拦截器
axios.interceptors.request.use(config => {
  console.log('发送请求:', {
    url: config.url,
    method: config.method,
    baseURL: config.baseURL,
    headers: config.headers
  })
  
  // 如果是FormData，不设置Content-Type，让浏览器自动设置
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  } else if (config.method === 'put' || config.method === 'post') {
    // 对于PUT和POST请求，设置Content-Type为application/json
    config.headers['Content-Type'] = 'application/json'
  }
  return config
}, error => {
  console.error('请求错误:', error)
  return Promise.reject(error)
})

// 添加响应拦截器
axios.interceptors.response.use(
  response => {
    console.log('收到响应:', {
      status: response.status,
      data: response.data,
      headers: response.headers
    })
    return response
  },
  error => {
    console.error('响应错误:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status
    })
    return Promise.reject(error)
  }
)

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus)
app.use(createPinia())
app.use(router)

app.mount('#app')
