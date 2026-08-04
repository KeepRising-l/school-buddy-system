<template>
  <div class="header">
    <div class="header-left">
      <el-icon class="collapse-btn" @click="$emit('toggle')">
        <Fold v-if="!isCollapse" />
        <Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="$route.meta.title">{{ $route.meta.title }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="header-right">
      <el-dropdown @command="handleCommand">
        <span class="user-info">
          <UserAvatar :name="adminStore.userInfo.nickname" :avatar="adminStore.userInfo.avatar" :size="32" />
          <span class="username">{{ adminStore.userInfo.nickname || '管理员' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/admin'
import { ElMessageBox } from 'element-plus'
import UserAvatar from '@/components/UserAvatar.vue'

defineProps({
  isCollapse: Boolean
})
defineEmits(['toggle'])

const router = useRouter()
const adminStore = useAdminStore()

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      adminStore.logout()
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped lang="scss">
.header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      &:hover { color: #10a37f; }
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      .username { font-size: 14px; }
    }
  }
}
</style>
