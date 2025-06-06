import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Masonry from 'vue3-masonry'

import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)
app.use(ElementPlus)
app.use(createPinia())
app.use(router)
app.use(Masonry)

app.mount('#app')
