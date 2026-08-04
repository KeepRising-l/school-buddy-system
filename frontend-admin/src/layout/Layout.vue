<template>
  <el-container style="height: 100vh">
    <el-aside :width="isCollapse ? '64px' : '220px'" style="transition: width 0.3s">
      <Sidebar :is-collapse="isCollapse" />
    </el-aside>
    <el-container>
      <el-header style="padding: 0; height: 56px; line-height: 56px; border-bottom: 1px solid var(--el-border-color-lighter); display: flex; align-items: center; justify-content: space-between; padding: 0 20px;">
        <Header :is-collapse="isCollapse" @toggle="isCollapse = !isCollapse" />
      </el-header>
      <el-main style="background: var(--el-bg-color-page); padding: 20px; overflow-y: auto;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

const isCollapse = ref(false)
</script>
