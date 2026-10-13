import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAdminStore } from '@/store/admin'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

// 清空登录态并回到登录页（已在登录页时不重复跳转）
const handleUnauthorized = () => {
  try {
    useAdminStore().logout()
  } catch (e) {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
  }
  if (router.currentRoute.value.path !== '/login') {
    router.replace('/login')
  }
}

// 解析 Blob 响应体里的 JSON 错误（导出接口出错时后端返回的是 JSON 而不是 CSV）
const parseBlobError = async (blob) => {
  try {
    const text = await blob.text()
    const data = JSON.parse(text)
    return data && typeof data === 'object' ? data : null
  } catch (e) {
    return null
  }
}

// 判断 Blob 是否其实是 JSON 错误体（而不是 CSV）
const isJsonBlob = (blob) =>
  blob && blob.type && blob.type.toLowerCase().includes('application/json')

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
  async response => {
    // Blob 响应（文件下载）绕过 R<T> 包装；
    // 若响应体实际是 JSON（401/业务错误），必须先识别出来，避免把错误内容当成 CSV 保存
    if (response.config.responseType === 'blob') {
      if (isJsonBlob(response.data)) {
        const err = await parseBlobError(response.data)
        if (err && err.code === 401) {
          handleUnauthorized()
          return Promise.reject(new Error('登录已过期，请重新登录'))
        }
        const message = (err && err.message) || '导出失败，请稍后重试'
        ElMessage.error(message)
        return Promise.reject(new Error(message))
      }
      return response
    }
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
      handleUnauthorized()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    // 其他业务错误
    if (res.message) {
      ElMessage.error(res.message)
    }
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  async error => {
    // HTTP 层面的 401（网关/过滤器返回）同样清理并跳转
    if (error.response && error.response.status === 401) {
      handleUnauthorized()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    // HTTP 层错误：响应体可能是 JSON Blob（导出接口的 4xx/5xx），尝试解析出后端消息
    const data = error.response && error.response.data
    if (data instanceof Blob) {
      const err = await parseBlobError(data)
      const message = (err && err.message) || error.message || '网络错误'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
