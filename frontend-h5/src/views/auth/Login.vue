<template>
  <div class="page login-page">
    <!-- 顶部 Logo 区域 -->
    <div class="logo-area">
      <div class="logo-icon">
        <van-icon name="friends-o" size="40" color="#10a37f" />
      </div>
      <h1 class="app-title">校园搭子</h1>
      <p class="app-subtitle">找到你的兴趣伙伴</p>
    </div>

    <!-- 登录表单 -->
    <div class="form-area card">
      <van-field
        v-model="form.account"
        placeholder="学号/手机号"
        left-icon="user-o"
        clearable
      />
      <van-field
        v-model="form.password"
        type="password"
        placeholder="请输入密码"
        left-icon="lock"
        show-password
        @keyup.enter="handleLogin"
      />
      <van-button
        type="primary"
        block
        round
        class="login-btn"
        :loading="loading"
        loading-text="登录中..."
        @click="handleLogin"
      >
        登录
      </van-button>
    </div>

    <!-- 底部注册链接 -->
    <div class="bottom-link">
      没有账号？<router-link to="/register" class="link">去注册</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({
  account: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.account) {
    showToast('请输入用户名/学号/手机号')
    return
  }
  if (!form.password) {
    showToast('请输入密码')
    return
  }
  if (loading.value) return

  loading.value = true
  try {
    const res = await login({
      account: form.account,
      password: form.password
    })
    if (!res.data || !res.data.token) {
      showToast('登录响应异常，缺少 token')
      return
    }
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.user || {})
    showToast('登录成功')
    // 开放重定向防护：只允许站内相对路径
    const redirect = route.query.redirect
    if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
      router.replace(redirect)
    } else {
      router.replace('/square')
    }
  } catch (err) {
    // 接口层已处理错误提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  background: var(--bg-page);
  position: relative;
}

.logo-area {
  margin-top: 72px;
  margin-bottom: 40px;
  text-align: center;

  .logo-icon {
    width: 76px;
    height: 76px;
    border-radius: 24px;
    background: var(--bg-card);
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 18px;
    box-shadow: 0 24px 48px -24px rgba(16, 163, 127, 0.35);
    border: 1px solid var(--border-light);
  }

  .app-title {
    font-size: 28px;
    font-weight: 700;
    letter-spacing: -0.01em;
    color: var(--text-primary);
  }

  .app-subtitle {
    margin-top: 8px;
    font-size: 14px;
    color: var(--text-hint);
  }
}

.form-area {
  width: calc(100% - 48px);
  padding: 24px 20px;

  .van-field {
    margin-bottom: 14px;
    border-radius: 14px;
    background: var(--bg-grey);
    padding: 10px 14px;
    border: 1px solid transparent;
    transition: border-color 0.3s var(--ease), background 0.3s var(--ease);

    &:focus-within {
      background: var(--bg-card);
      border-color: var(--primary);
    }
  }

  .login-btn {
    margin-top: 24px;
    background: var(--primary);
    border: none;
    height: 46px;
    font-size: 16px;
    font-weight: 500;
    box-shadow: 0 16px 32px -12px rgba(16, 163, 127, 0.35);
  }
}

.bottom-link {
  margin-top: 28px;
  font-size: 14px;
  color: var(--text-hint);

  .link {
    color: var(--primary);
    text-decoration: none;
    font-weight: 500;
  }
}
</style>
