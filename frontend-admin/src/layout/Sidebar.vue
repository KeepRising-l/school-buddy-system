<template>
  <div class="sidebar" :class="{ collapse: isCollapse }">
    <div class="logo">
      <el-icon :size="24"><Flag /></el-icon>
      <span v-show="!isCollapse" class="logo-text">校园搭子后台</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="isCollapse"
      :collapse-transition="false"
      router
      background-color="#14352c"
      text-color="#a7c6bc"
      active-text-color="#10a37f"
    >
      <el-menu-item index="/dashboard">
        <el-icon><DataAnalysis /></el-icon>
        <template #title>数据看板</template>
      </el-menu-item>
      <el-menu-item index="/user">
        <el-icon><User /></el-icon>
        <template #title>用户管理</template>
      </el-menu-item>
      <el-menu-item index="/post">
        <el-icon><Document /></el-icon>
        <template #title>帖子管理</template>
      </el-menu-item>
      <el-menu-item index="/tag">
        <el-icon><PriceTag /></el-icon>
        <template #title>标签管理</template>
      </el-menu-item>
      <el-menu-item index="/report">
        <el-icon><Warning /></el-icon>
        <template #title>举报中心</template>
      </el-menu-item>
      <el-menu-item index="/announcement">
        <el-icon><Bell /></el-icon>
        <template #title>系统公告</template>
      </el-menu-item>
      <el-menu-item index="/stats">
        <el-icon><TrendCharts /></el-icon>
        <template #title>数据统计</template>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

defineProps({
  isCollapse: Boolean
})

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped lang="scss">
.sidebar {
  height: 100vh;
  background: #14352c;
  overflow-y: auto;

  .logo {
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: #fff;
    font-size: 16px;
    font-weight: bold;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .el-icon {
      color: #10a37f;
    }
  }

  :deep(.el-menu) {
    border-right: none;
  }

  // 选中项：柔翡翠底 + 白字
  :deep(.el-menu-item.is-active) {
    background: rgba(16, 163, 127, 0.18);
    color: #10a37f;
    font-weight: 500;
  }

  :deep(.el-menu-item:hover) {
    background: rgba(255, 255, 255, 0.06);
  }
}
</style>
