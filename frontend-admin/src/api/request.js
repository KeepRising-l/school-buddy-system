import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (!res) {
      ElMessage.error('服务器响应异常')
      return Promise.reject(new Error('服务器响应为空'))
    }
    if (res.code === 200) {
      return res
    }
    // 401 先清理状态并跳转，避免跳转前还弹一次错误提示
    if (res.code === 401) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_user')
      router.push('/login')
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    // 其他业务错误
    if (res.message) {
      ElMessage.error(res.message)
    }
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  error => {
    // HTTP 层面的 401（网关/过滤器返回）同样清理并跳转
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_user')
      router.push('/login')
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
