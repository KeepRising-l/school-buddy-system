<template>
  <div class="page login-page">
    <!-- 顶部编辑级刊头 -->
    <div class="logo-area">
      <div class="logo-kicker">SCHOOL BUDDY // 2.0</div>
      <h1 class="app-title">校园搭子日程</h1>
      <p class="app-subtitle">高校实名兴趣组局与守信陪伴</p>
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
  min-height: calc(100vh - var(--safe-top));
  min-height: calc(100dvh - var(--safe-top));
  background: var(--bg-page);
  position: relative;
  overflow: hidden;
  padding: 0 24px;
}

.logo-area {
  width: 100%;
  margin-top: 84px;
  margin-bottom: 36px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
  text-align: left;

  .logo-kicker {
    font-family: var(--font-mono);
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.08em;
    color: var(--primary);
    margin-bottom: 8px;
  }

  .app-title {
    font-size: 26px;
    font-weight: 600;
    letter-spacing: -0.02em;
    color: var(--text-primary);
  }

  .app-subtitle {
    margin-top: 6px;
    font-size: 13px;
    color: var(--text-secondary);
  }
}

.form-area {
  width: 100%;
  padding: 20px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--bg-card);

  .van-field {
    margin-bottom: 12px;
    border-radius: var(--radius-md);
    background: var(--bg-grey);
    padding: 10px 14px;
    border: 1px solid transparent;
    transition: border-color 0.2s var(--ease), background 0.2s var(--ease);

    &:focus-within {
      background: var(--bg-card);
      border-color: var(--primary);
    }
  }

  .login-btn {
    margin-top: 20px;
    background: var(--primary);
    border: none;
    border-radius: var(--radius-md);
    height: 44px;
    font-size: 15px;
    font-weight: 600;
    transition: transform 0.15s var(--ease), opacity 0.2s var(--ease);

    &:active {
      transform: scale(0.97);
    }
  }
}

.bottom-link {
  margin-top: 24px;
  font-size: 13px;
  color: var(--text-hint);

  .link {
    color: var(--primary);
    text-decoration: none;
    font-weight: 600;
  }
}
</style>
