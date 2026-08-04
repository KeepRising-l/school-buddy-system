import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '数据看板', icon: 'DataAnalysis' }
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'post',
        name: 'PostList',
        component: () => import('@/views/post/PostList.vue'),
        meta: { title: '帖子管理', icon: 'Document' }
      },
      {
        path: 'tag',
        name: 'TagManage',
        component: () => import('@/views/tag/TagManage.vue'),
        meta: { title: '标签管理', icon: 'PriceTag' }
      },
      {
        path: 'stats',
        name: 'StatsView',
        component: () => import('@/views/stats/StatsView.vue'),
        meta: { title: '数据统计', icon: 'TrendCharts' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 校园搭子管理后台` : '校园搭子管理后台'
  const token = localStorage.getItem('admin_token')
  // 未登录访问受保护页面 → 去登录
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }
  // 已登录访问登录页 → 回首页
  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }
  next()
})

export default router
