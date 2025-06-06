import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

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
      component: () => import('@/views/DiaryView.vue')
    },
    {
      path: '/plan',
      name: 'plan',
      component: () => import('@/views/PlanView.vue'),
      props: true,
      meta: { activePath: '/plan' }
    },
    {
      path: '/plan/:id',
      name: 'planDetail',
      component: () => import('@/views/PlanView.vue'),
      props: true,
      meta: { activePath: '/plan' },
      beforeEnter: (to, from, next) => {
        const id = Number(to.params.id);
        if (isNaN(id)) {
          next('/plan'); // 如果ID无效，重定向到计划列表页
        } else {
          next();
        }
      }
    },
    {
      path: '/share',
      name: 'share',
      component: () => import('@/views/ShareView.vue'),
      beforeEnter: (to, from, next) => {
        const isLoggedIn = localStorage.getItem('username');
        if (!isLoggedIn) {
          ElMessage.warning('请先登录后再分享日记');
          next('/');
        } else {
          next();
        }
      }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    }
  ],
})

export default router
