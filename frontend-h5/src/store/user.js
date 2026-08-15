import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('h5_token') || '')

  // 缓存被污染时兜底，避免应用启动即崩溃
  let parsedUser = {}
  try {
    const raw = localStorage.getItem('h5_user')
    if (raw) {
      parsedUser = JSON.parse(raw)
    }
  } catch (e) {
    console.warn('用户信息缓存损坏，已重置')
    localStorage.removeItem('h5_user')
  }
  const userInfo = ref(parsedUser)

  const isLoggedIn = computed(() => !!token.value)
  const creditLevel = computed(() => {
    // 修复：用 ?? 而不是 ||，0 分（失信用户）不能被兜底成 100
    const score = userInfo.value.creditScore ?? 100
    if (score >= 90) return 5
    if (score >= 75) return 4
    if (score >= 60) return 3
    if (score >= 40) return 2
    return 1
  })
  const creditLabel = computed(() => {
    const score = userInfo.value.creditScore ?? 100
    if (score >= 90) return '靠谱队友'
    if (score >= 75) return '优质队友'
    if (score >= 60) return '普通队友'
    if (score >= 40) return '需改善'
    return '失信用户'
  })

  function setToken(val) {
    token.value = val
    localStorage.setItem('h5_token', val)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('h5_user', JSON.stringify(info))
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('h5_token')
    localStorage.removeItem('h5_user')
  }

  return { token, userInfo, isLoggedIn, creditLevel, creditLabel, setToken, setUserInfo, logout }
})
