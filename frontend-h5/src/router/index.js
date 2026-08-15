import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    redirect: '/square'
  },
  {
    path: '/square',
    name: 'Square',
    component: () => import('@/views/square/Square.vue'),
    meta: { title: '广场', showTabbar: true }
  },
  {
    path: '/post/create',
    name: 'PostCreate',
    component: () => import('@/views/post/PostCreate.vue'),
    meta: { title: '发布组局', requireAuth: true }
  },
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: () => import('@/views/post/PostDetail.vue'),
    meta: { title: '组局详情' }
  },
  {
    path: '/mine',
    name: 'Mine',
    component: () => import('@/views/mine/Mine.vue'),
    meta: { title: '我的', showTabbar: true, requireAuth: true }
  },
  {
    path: '/mine/profile',
    name: 'Profile',
    component: () => import('@/views/mine/Profile.vue'),
    meta: { title: '个人资料', requireAuth: true }
  },
  {
    path: '/mine/tags',
    name: 'TagSetting',
    component: () => import('@/views/mine/TagSetting.vue'),
    meta: { title: '兴趣标签', requireAuth: true }
  },
  {
    path: '/mine/posts',
    name: 'MyPosts',
    component: () => import('@/views/mine/MyPosts.vue'),
    meta: { title: '我的发布', requireAuth: true }
  },
  {
    path: '/mine/applies',
    name: 'MyApplies',
    component: () => import('@/views/mine/MyApplies.vue'),
    meta: { title: '我的预约', requireAuth: true }
  },
  {
    path: '/mine/favorites',
    name: 'MyFavorites',
    component: () => import('@/views/mine/MyFavorites.vue'),
    meta: { title: '我的收藏', requireAuth: true }
  },
  {
    path: '/mine/reports',
    name: 'MyReports',
    component: () => import('@/views/mine/MyReports.vue'),
    meta: { title: '我的举报', requireAuth: true }
  },
  {
    path: '/message',
    name: 'NotificationList',
    component: () => import('@/views/message/NotificationList.vue'),
    meta: { title: '消息中心', requireAuth: true }
  },
  {
    path: '/user/:userId',
    name: 'UserProfile',
    component: () => import('@/views/user/UserProfile.vue'),
    meta: { title: '个人主页' }
  },
  {
    path: '/review/:postId',
    name: 'ReviewPage',
    component: () => import('@/views/review/ReviewPage.vue'),
    meta: { title: '评价', requireAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 校园搭子` : '校园搭子'
  const token = localStorage.getItem('h5_token')
  if (to.meta.requireAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
