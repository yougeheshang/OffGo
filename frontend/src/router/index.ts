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
      path: '/plan',
      name: 'plan',
      component: () => import('@/views/PlanView.vue'),
      props: true
    },
    {
      path: '/share',
      name: 'share',
      component: () => import('@/views/ShareView.vue')
    },
    {
      path: '/diary',
      name: 'diary',
      component: () => import('@/views/DiaryView.vue')
    },
    {
      path: '/diary/:id',
      name: 'diary-detail',
      component: () => import('@/views/DiaryView.vue')
    }
  ],
})

export default router
