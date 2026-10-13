<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-shell">
          <el-icon :size="26" color="#10a37f"><Flag /></el-icon>
        </div>
        <h2>校园搭子预约系统</h2>
        <p>管理后台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="account">
          <el-input v-model="form.account" placeholder="请输入用户名或学号或手机号" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <!-- 演示账号提示仅本地开发构建显示，生产构建不包含 -->
      <div v-if="isDev" class="login-tip">
        开发环境演示账号：admin001 / 123456（仅本地测试使用）
      </div>
      <!-- 窄屏（手机）提示：管理端是电脑端后台，手机屏幕下表格与图表会显示不全 -->
      <div v-if="isNarrow" class="login-tip narrow">
        管理端为电脑端后台，建议使用电脑浏览器访问（学生端请用手机打开 3000 端口）
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/admin'
import { adminLogin } from '@/api/admin'
import { ElMessage } from 'element-plus'

const router = useRouter()
const adminStore = useAdminStore()
const formRef = ref()
const loading = ref(false)
// 是否开发环境（生产构建不显示演示账号提示）
const isDev = import.meta.env.DEV

// 窄屏（手机）判定，跟随窗口尺寸变化
const isNarrow = ref(window.innerWidth < 768)
const onResize = () => { isNarrow.value = window.innerWidth < 768 }
onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => window.removeEventListener('resize', onResize))

const form = reactive({
  account: '',
  password: ''
})

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  if (loading.value) return
  loading.value = true
  try {
    const res = await adminLogin(form)
    // 防御后端返回字段缺失
    if (!res.data || !res.data.token) {
      ElMessage.error('登录响应异常，缺少 token')
      return
    }
    adminStore.setToken(res.data.token)
    adminStore.setUserInfo(res.data.user || {})
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100vh;
  // dvh：移动端地址栏收起/展开时 100vh 不跟随，登录卡会偏出屏幕
  height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #10a37f 0%, #0d8a6b 45%, #14352c 100%);

  .login-card {
    // 窄屏（手机）下不再硬撑 400px，避免横向溢出
    width: min(400px, calc(100vw - 32px));
    padding: 40px;
    background: #fff;
    border-radius: 20px;
    box-shadow: 0 24px 64px -24px rgba(0, 0, 0, 0.25);

    .login-header {
      text-align: center;
      margin-bottom: 32px;

      .logo-shell {
        width: 56px;
        height: 56px;
        border-radius: 18px;
        background: #e9f7f2;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto 16px;
      }

      h2 { margin: 12px 0 4px; color: #14161a; font-size: 20px; }
      p { color: #9ca3af; font-size: 14px; }
    }

    .login-tip {
      text-align: center;
      color: #9ca3af;
      font-size: 12px;
      margin-top: 16px;

      &.narrow {
        color: #b88230;
        background: #fdf6ec;
        border: 1px solid #f5dab1;
        border-radius: 8px;
        padding: 8px 12px;
        line-height: 1.5;
      }
    }
  }
}
</style>
