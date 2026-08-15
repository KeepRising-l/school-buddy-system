import axios from 'axios'
import { showToast, showLoadingToast, closeToast } from 'vant'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('h5_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 非静默请求显示 loading
    if (!config.silent) {
      showLoadingToast({ message: '加载中...', forbidClick: true, duration: 0 })
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    closeToast()
    const res = response.data
    if (!res) {
      showToast('服务器响应异常')
      return Promise.reject(new Error('服务器响应为空'))
    }
    if (res.code === 200) {
      return res
    }
    // 401 先清理状态并跳转，避免跳转前还弹一次错误提示
    if (res.code === 401) {
      localStorage.removeItem('h5_token')
      localStorage.removeItem('h5_user')
      router.push('/login')
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    // 业务错误：完整展示后端消息（支持长文本换行，显示3.5秒）
    // silent 请求由调用方自己处理错误提示（如报名弹窗），这里不再重复弹 toast
    if (!response.config.silent) {
      showToast({
        message: res.message || '操作失败',
        duration: 3500,
        wordBreak: 'break-word'
      })
    }
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  error => {
    closeToast()
    // HTTP 层面的 401（网关/过滤器返回）同样清理并跳转
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('h5_token')
      localStorage.removeItem('h5_user')
      router.push('/login')
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    showToast({
      message: error.response?.data?.message || '网络错误，请稍后重试',
      duration: 3500,
      wordBreak: 'break-word'
    })
    return Promise.reject(error)
  }
)

export default request
