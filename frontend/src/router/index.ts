import { createRouter, createWebHistory } from 'vue-router'


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/diary',
      name: 'diary',
      component: () => import('@/views/DiaryView.vue') // 待开发
    },
    {
      path: '/diary/:id',
      name: 'diaryDetail',
      component: () => import('@/views/diaryID.vue'),
      props: true,
    },
    {
      path: '/plan',
      name: 'plan',
      component: () => import('@/views/PlanView.vue'), // 待开发
      props: true
    },
    {
      path: '/share',
      name: 'share',
      component: () => import('@/views/ShareView.vue') // 待开发
    }
  ],
})

export default router
