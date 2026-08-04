import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAdminStore = defineStore('admin', () => {
  const token = ref(localStorage.getItem('admin_token') || '')

  // 缓存被污染时兜底，避免应用启动即崩溃
  let parsedUser = {}
  try {
    const raw = localStorage.getItem('admin_user')
    if (raw) {
      parsedUser = JSON.parse(raw)
    }
  } catch (e) {
    console.warn('管理员信息缓存损坏，已重置')
    localStorage.removeItem('admin_user')
  }
  const userInfo = ref(parsedUser)

  function setToken(val) {
    token.value = val
    localStorage.setItem('admin_token', val)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('admin_user', JSON.stringify(info))
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
  }

  return { token, userInfo, setToken, setUserInfo, logout }
})
