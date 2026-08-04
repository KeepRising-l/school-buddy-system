<template>
  <div class="page profile-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="个人资料" left-arrow @click-left="router.back()" />

    <div class="content">
      <!-- 头像 -->
      <div class="avatar-section">
        <van-uploader :after-read="onAvatarRead" accept="image/*" class="avatar-uploader">
          <UserAvatar :name="form.nickname" :avatar="form.avatar" :size="80" />
          <div class="avatar-edit-hint">点击更换</div>
        </van-uploader>
      </div>

      <!-- 昵称 -->
      <van-field
        v-model="form.nickname"
        label="昵称"
        placeholder="请输入昵称"
        maxlength="20"
        class="form-field"
      />

      <!-- 个性签名 -->
      <van-field
        v-model="form.signature"
        label="个性签名"
        placeholder="介绍一下自己吧"
        type="textarea"
        rows="2"
        autosize
        maxlength="100"
        show-word-limit
        class="form-field"
      />

      <!-- 学院 -->
      <van-field
        v-model="form.college"
        is-link
        readonly
        label="学院"
        placeholder="请选择学院"
        @click="showCollegePicker = true"
        class="form-field"
      />
      <van-popup v-model:show="showCollegePicker" round position="bottom">
        <van-picker
          :columns="collegeColumns"
          @confirm="onCollegeConfirm"
          @cancel="showCollegePicker = false"
        />
      </van-popup>

      <!-- 性别 -->
      <van-field label="性别" class="form-field">
        <template #input>
          <van-radio-group v-model="form.gender" direction="horizontal">
            <van-radio :name="0">保密</van-radio>
            <van-radio :name="1">男</van-radio>
            <van-radio :name="2">女</van-radio>
          </van-radio-group>
        </template>
      </van-field>

      <!-- 保存按钮 -->
      <van-button
        type="primary"
        block
        round
        class="save-btn"
        :loading="saving"
        loading-text="保存中..."
        @click="handleSave"
      >
        保存
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getProfile, updateProfile, uploadAvatar } from '@/api/user'
import { useUserStore } from '@/store/user'
import UserAvatar from '@/components/UserAvatar.vue'

const router = useRouter()
const userStore = useUserStore()
const saving = ref(false)
const showCollegePicker = ref(false)

const form = reactive({
  avatar: '',
  nickname: '',
  signature: '',
  college: '',
  gender: 0
})

// 学院选项
const collegeColumns = [
  { text: '计算机学院', value: '计算机学院' },
  { text: '软件学院', value: '软件学院' },
  { text: '信息工程学院', value: '信息工程学院' },
  { text: '电子工程学院', value: '电子工程学院' },
  { text: '机械工程学院', value: '机械工程学院' },
  { text: '经济管理学院', value: '经济管理学院' },
  { text: '外国语学院', value: '外国语学院' },
  { text: '数学学院', value: '数学学院' },
  { text: '物理学院', value: '物理学院' },
  { text: '文学院', value: '文学院' },
  { text: '法学院', value: '法学院' },
  { text: '艺术设计学院', value: '艺术设计学院' }
]

// 学院确认
const onCollegeConfirm = ({ selectedOptions }) => {
  form.college = selectedOptions[0]?.value || ''
  showCollegePicker.value = false
}

// 头像上传
const onAvatarRead = async (file) => {
  // 校验文件大小（2MB）
  if (file.file && file.file.size > 2 * 1024 * 1024) {
    showToast('图片大小不能超过2MB')
    return
  }
  try {
    const formData = new FormData()
    formData.append('file', file.file)
    const res = await uploadAvatar(formData)
    form.avatar = res.data
    showToast('头像上传成功')
  } catch (err) {
    showToast('头像上传失败')
  }
}

// 加载个人资料
const fetchProfile = async () => {
  try {
    const res = await getProfile()
    const data = res.data
    form.avatar = data.avatar || ''
    form.nickname = data.nickname || ''
    form.signature = data.signature || ''
    form.college = data.college || ''
    form.gender = data.gender != null ? data.gender : 0
  } catch (err) {
    // 接口层已处理
  }
}

// 保存个人资料
const handleSave = async () => {
  if (!form.nickname) {
    showToast('请输入昵称')
    return
  }

  saving.value = true
  try {
    await updateProfile({
      avatar: form.avatar,
      nickname: form.nickname,
      signature: form.signature,
      college: form.college,
      gender: form.gender
    })
    // 更新本地用户信息
    userStore.setUserInfo({
      ...userStore.userInfo,
      avatar: form.avatar,
      nickname: form.nickname,
      signature: form.signature,
      college: form.college,
      gender: form.gender
    })
    showToast('保存成功')
    router.back()
  } catch (err) {
    // 接口层已处理
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped lang="scss">
.profile-page {
  background: var(--bg-page);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 0 24px;

  .avatar-uploader {
    position: relative;
    cursor: pointer;
  }

  .avatar-edit-hint {
    text-align: center;
    margin-top: 8px;
    font-size: 12px;
    color: var(--text-hint);
  }
}

.form-field {
  margin-bottom: 1px;
  border-radius: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);

  :deep(.van-field__label) {
    color: var(--text-primary);
    font-weight: 500;
  }
}

.save-btn {
  margin-top: 32px;
  background: var(--primary);
  border: none;
  height: 46px;
  font-size: 16px;
  font-weight: 500;
  box-shadow: 0 16px 32px -12px rgba(16, 163, 127, 0.35);
}
</style>
