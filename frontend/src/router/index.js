import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { createRouter, createWebHistory } from 'vue-router'

// 路由配置
const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Signup.vue'),
    meta: { requiresAuth: false, title: '注册' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/HomeSimple.vue'),
    meta: { requiresAuth: true, title: '首页' }
  },
  {
    path: '/timeline',
    name: 'Timeline',
    component: () => import('@/views/TimelineFixed.vue'),
    meta: { requiresAuth: true, title: '时间线' }
  },
  {
    path: '/ranking',
    name: 'Ranking',
    component: () => import('@/views/RankingFixed.vue'),
    meta: { requiresAuth: true, title: '排行榜' }
  },
  {
    path: '/profile/:userId?',
    name: 'Profile',
    component: () => import('@/views/ProfileSimple.vue'),
    meta: { requiresAuth: true, title: '个人中心' }
  },
  {
    path: '/create-post',
    name: 'CreatePost',
    component: () => import('@/views/CreatePostSimple.vue'),
    meta: { requiresAuth: true, title: '创作新帖' }
  },
  {
    path: '/post/:postId',
    name: 'PostDetail',
    component: () => import('@/views/PostDetail.vue'),
    meta: { requiresAuth: true, title: '帖子详情' }
  },
  {
    path: '/users',
    name: 'AllUsers',
    component: () => import('@/views/AllUsers.vue'),
    meta: { requiresAuth: true, title: '所有用户' }
  },
  {
    path: '/user/:userId',
    name: 'UserProfile',
    component: () => import('@/views/UserProfile.vue'),
    meta: { requiresAuth: true, title: '用户主页' }
  },
  {
    path: '/following',
    name: 'Following',
    component: () => import('@/views/SimplePages.vue'),
    meta: { requiresAuth: true, title: '关注列表' }
  },
  {
    path: '/followers',
    name: 'Followers',
    component: () => import('@/views/SimplePages.vue'),
    meta: { requiresAuth: true, title: '粉丝列表' }
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('@/views/SimplePages.vue'),
    meta: { requiresAuth: true, title: '我的收藏' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/SimplePages.vue'),
    meta: { requiresAuth: true, title: '设置' }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/SimplePages.vue'),
    meta: { requiresAuth: true, title: '搜索' }
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('@/views/StatisticsView.vue'),
    meta: { requiresAuth: true, title: '流量走势图' }
  },
  {
    path: '/discover',
    name: 'Discover',
    component: () => import('@/views/DiscoverView.vue'),
    meta: { requiresAuth: true, title: '发现' }
  },
  {
    path: '/browse-history',
    name: 'BrowseHistory',
    component: () => import('@/views/BrowseHistoryView.vue'),
    meta: { requiresAuth: true, title: '浏览记录' }
  },
  {
    path: '/uv-statistics',
    name: 'UVStatistics',
    component: () => import('@/views/UVStatisticsView.vue'),
    meta: { requiresAuth: true, title: 'UV统计' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { requiresAuth: false, title: '页面未找到' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - Redis社交平台` : 'Redis社交平台'
  
  // 检查是否需要认证
  if (to.meta.requiresAuth) {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      next('/login')
      return
    }
    
    // 验证会话是否有效（从登录页跳转过来时，跳过验证，因为刚登录成功）
    if (from.name !== 'Login' && from.name !== 'Register') {
      const isValid = await userStore.checkLoginStatus()
      if (!isValid) {
        ElMessage.error('登录已过期，请重新登录')
        next('/login')
        return
      }
    }
    
    // 更新当前页面
    userStore.updateCurrentPage(to.path)
  }
  
  // 如果已登录用户访问登录页，重定向到首页
  if ((to.name === 'Login' || to.name === 'Register') && userStore.isLoggedIn) {
    next('/home')
    return
  }
  
  next()
})

// 路由后置守卫
router.afterEach((to, from) => {
  const userStore = useUserStore()
  
  // 记录用户活动
  if (userStore.isLoggedIn) {
    userStore.recordActivity()
  }
})

export default router
