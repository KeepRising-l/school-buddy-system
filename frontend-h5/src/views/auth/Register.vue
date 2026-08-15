<template>
  <div class="page register-page">
    <!-- 顶部标题 -->
    <div class="header">
      <div class="logo-icon">
        <van-icon name="friends-o" size="28" color="#10a37f" />
      </div>
      <h1 class="page-title">注册账号</h1>
      <p class="page-desc">加入校园搭子，找到志同道合的伙伴</p>
    </div>

    <!-- 注册方式切换 -->
    <van-tabs v-model:active="registerType" class="register-tabs">
      <van-tab title="学号注册" name="student" />
      <van-tab title="手机号注册" name="phone" />
    </van-tabs>

    <!-- 注册表单 -->
    <div class="form-area card">
      <template v-if="registerType === 'student'">
        <van-field
          v-model="form.studentId"
          placeholder="请输入学号（8-12位数字）"
          left-icon="idcard"
          clearable
          maxlength="12"
        />
        <van-field
          v-model="form.phone"
          placeholder="请输入手机号（选填）"
          left-icon="phone-o"
          clearable
          maxlength="11"
          type="tel"
        />
      </template>

      <template v-else>
        <van-field
          v-model="form.phone"
          placeholder="请输入手机号"
          left-icon="phone-o"
          clearable
          maxlength="11"
          type="tel"
        />
        <van-field
          v-model="form.studentId"
          placeholder="请输入学号（选填）"
          left-icon="idcard"
          clearable
          maxlength="12"
        />
      </template>

      <van-field
        v-model="form.password"
        type="password"
        placeholder="请输入密码（8-20位，含字母和数字）"
        left-icon="lock"
        show-password
        maxlength="20"
      />
      <van-field
        v-model="form.confirmPassword"
        type="password"
        placeholder="请确认密码"
        left-icon="lock"
        show-password
        maxlength="20"
      />
      <van-field
        v-model="form.name"
        placeholder="请输入姓名"
        left-icon="user-o"
        clearable
        maxlength="20"
      />
      <van-button
        type="primary"
        block
        round
        class="register-btn"
        :loading="loading"
        loading-text="注册中..."
        @click="handleRegister"
      >
        注册
      </van-button>
    </div>

    <!-- 底部登录链接 -->
    <div class="bottom-link">
      已有账号？<router-link to="/login" class="link">去登录</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { register } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const registerType = ref('student')

const form = reactive({
  studentId: '',
  phone: '',
  password: '',
  confirmPassword: '',
  name: ''
})

const validate = () => {
  if (registerType.value === 'student') {
    if (!form.studentId) {
      showToast('请输入学号')
      return false
    }
    if (!/^\d{8,12}$/.test(form.studentId)) {
      showToast('学号需为8-12位数字')
      return false
    }
    if (form.phone && !/^1[3-9]\d{9}$/.test(form.phone)) {
      showToast('请输入正确的11位手机号')
      return false
    }
  } else {
    if (!form.phone) {
      showToast('请输入手机号')
      return false
    }
    if (!/^1[3-9]\d{9}$/.test(form.phone)) {
      showToast('请输入正确的11位手机号')
      return false
    }
    if (form.studentId && !/^\d{8,12}$/.test(form.studentId)) {
      showToast('学号需为8-12位数字')
      return false
    }
  }
  if (!form.password) {
    showToast('请输入密码')
    return false
  }
  if (form.password.length < 8 || form.password.length > 20) {
    showToast('密码长度为8-20位')
    return false
  }
  if (!/[a-zA-Z]/.test(form.password) || !/\d/.test(form.password)) {
    showToast('密码需包含字母和数字')
    return false
  }
  if (form.password !== form.confirmPassword) {
    showToast('两次密码输入不一致')
    return false
  }
  if (!form.name) {
    showToast('请输入姓名')
    return false
  }
  return true
}

const handleRegister = async () => {
  // V1.2 防重复提交：快速连点只发一次请求
  if (loading.value) return
  if (!validate()) return

  loading.value = true
  try {
    await register({
      studentNo: form.studentId || undefined,
      phone: form.phone || undefined,
      password: form.password,
      realName: form.name,
      registerType: registerType.value
    })
    showToast('注册成功，请登录')
    router.push('/login')
  } catch (err) {
    // 接口层已处理错误提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.register-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  background: var(--bg-page);
  position: relative;
}

.header {
  margin-top: 56px;
  margin-bottom: 16px;
  text-align: center;

  .logo-icon {
    width: 56px;
    height: 56px;
    border-radius: 18px;
    background: var(--bg-card);
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 18px;
    box-shadow: 0 20px 40px -20px rgba(16, 163, 127, 0.35);
    border: 1px solid var(--border-light);
  }

  .page-title {
    font-size: 26px;
    font-weight: 700;
    letter-spacing: -0.01em;
    color: var(--text-primary);
  }

  .page-desc {
    margin-top: 8px;
    font-size: 14px;
    color: var(--text-hint);
  }
}

.register-tabs {
  width: calc(100% - 48px);
  margin-bottom: 12px;

  :deep(.van-tabs__nav) {
    background: transparent;
  }

  :deep(.van-tab) {
    color: var(--text-hint);
  }

  :deep(.van-tab--active) {
    color: var(--primary);
    font-weight: 600;
  }

  :deep(.van-tabs__line) {
    background: var(--primary);
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

  .register-btn {
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
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-hint);

  .link {
    color: var(--primary);
    text-decoration: none;
    font-weight: 500;
  }
}
</style>
