<template>
  <div class="page-with-tabbar">
    <slot />
    <nav class="editorial-tabbar">
      <router-link to="/square" class="e-tab-item" active-class="is-active">
        <span class="e-tab-label">日程</span>
      </router-link>

      <router-link to="/post/create" class="e-tab-item e-tab-create" active-class="is-active">
        <span class="e-tab-label">+ 写新组局</span>
      </router-link>

      <router-link to="/mine" class="e-tab-item" active-class="is-active">
        <span class="e-tab-label">我的</span>
        <span v-if="unreadCount > 0" class="e-unread-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/api/request'

const userStore = useUserStore()
const unreadCount = ref(0)

const fetchUnreadBadge = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await request.get('/api/notification/unread-count', { silent: true })
    unreadCount.value = res.data?.count || 0
  } catch (e) {
    // 静默忽略
  }
}

onMounted(() => {
  fetchUnreadBadge()
})
</script>

<style scoped lang="scss">
.editorial-tabbar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: min(100%, var(--app-max-width));
  height: calc(54px + var(--safe-bottom));
  padding: 0 16px var(--safe-bottom);
  background: rgba(247, 245, 240, 0.96);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1px solid var(--border-color);
  display: grid;
  grid-template-columns: 1fr 1.2fr 1fr;
  align-items: center;
  z-index: 100;

  .e-tab-item {
    position: relative;
    height: 100%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    text-decoration: none;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-hint);
    transition: color 0.18s var(--ease), transform 0.15s var(--ease);

    &:active {
      transform: scale(0.96);
    }

    &.is-active {
      color: var(--text-primary);
      font-weight: 600;

      &::after {
        content: '';
        position: absolute;
        top: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 24px;
        height: 2px;
        background: var(--text-primary);
      }
    }
  }

  .e-tab-create {
    color: var(--primary);
    font-weight: 600;

    &.is-active {
      color: var(--primary-dark);

      &::after {
        background: var(--primary);
      }
    }
  }

  .e-unread-badge {
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 999px;
    background: var(--primary);
    color: #ffffff;
    font-family: var(--font-mono);
    font-size: 10px;
    font-weight: 600;
    line-height: 16px;
    text-align: center;
  }
}
</style>
