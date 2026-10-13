import axios from 'axios'
import { showToast, showLoadingToast, closeToast } from 'vant'
import router from '@/router'
import { useUserStore } from '@/store/user'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

// 全局 loading 计数：只对非静默请求计数，保证 show/close 一一配对。
// 静默请求（如报名弹窗、标签预取）不参与计数，避免提前关闭其他请求的 loading。
let loadingCount = 0
const isSilent = (config) => !!(config && config.silent)

const showGlobalLoading = () => {
  loadingCount += 1
  if (loadingCount === 1) {
    showLoadingToast({ message: '加载中...', duration: 0 })
  }
}
const closeGlobalLoading = () => {
  loadingCount = Math.max(0, loadingCount - 1)
  if (loadingCount === 0) {
    closeToast()
  }
}

// 清空登录态并安全返回登录页：
// 1) 通过 store.logout() 同步清掉内存状态（否则页面仍显示已登录）
// 2) 带上回跳地址（会被登录页做站内路径校验）
// 3) 已在登录页时不重复跳转，避免重复导航
const handleUnauthorized = () => {
  try {
    useUserStore().logout()
  } catch (e) {
    localStorage.removeItem('h5_token')
    localStorage.removeItem('h5_user')
  }
  const current = router.currentRoute.value
  if (current.path !== '/login') {
    router.replace({ path: '/login', query: { redirect: current.fullPath } })
  }
}

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('h5_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (!isSilent(config)) {
      showGlobalLoading()
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 与请求拦截器配对：仅非静默请求参与计数
    if (!isSilent(response.config)) {
      closeGlobalLoading()
    }
    const res = response.data
    if (!res) {
      showToast('服务器响应异常')
      return Promise.reject(new Error('服务器响应为空'))
    }
    if (res.code === 200) {
      return res
    }
    // 401 先清理状态并安全跳转，避免跳转前还弹一次错误提示
    if (res.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    // 业务错误：完整展示后端消息（支持长文本换行，显示3.5秒）
    // silent 请求由调用方自己处理错误提示（如报名弹窗），这里不再重复弹 toast
    if (!isSilent(response.config)) {
      showToast({
        message: res.message || '操作失败',
        duration: 3500,
        wordBreak: 'break-word'
      })
    }
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  error => {
    // 与请求拦截器配对：仅非静默请求参与计数（config 缺失说明请求未发出，未计数）
    if (error.config && !isSilent(error.config)) {
      closeGlobalLoading()
    }
    // HTTP 层面的 401（网关/过滤器返回）同样清理并跳转
    if (error.response && error.response.status === 401) {
      handleUnauthorized()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    // silent 请求由调用方自己处理错误提示，避免双重提示
    if (!isSilent(error.config)) {
      showToast({
        message: error.response?.data?.message || '网络错误，请稍后重试',
        duration: 3500,
        wordBreak: 'break-word'
      })
    }
    // 统一可读错误信息：HTTP 层报错（5xx / CORS 403 等）时 Axios 的 error.message 是
    // "Request failed with status code 500" 这种英文原文，调用方拿它做弹窗提示用户看不懂。
    // 这里把后端 message 归一化到 error.message（保留 error.response 等原字段，向后兼容）。
    if (error.response?.data?.message) {
      error.message = error.response.data.message
    } else if (!error.response) {
      error.message = error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络异常，请检查网络后重试'
    }
    return Promise.reject(error)
  }
)

export default request
