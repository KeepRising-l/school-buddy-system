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
      <div class="login-tip">
        默认管理员账号：admin001 / 123456
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/admin'
import { adminLogin } from '@/api/admin'
import { ElMessage } from 'element-plus'

const router = useRouter()
const adminStore = useAdminStore()
const formRef = ref()
const loading = ref(false)

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
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #10a37f 0%, #0d8a6b 45%, #14352c 100%);

  .login-card {
    width: 400px;
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
    }
  }
}
</style>
