<template>
  <div class="page notification-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="消息中心" left-arrow @click-left="router.back()">
      <template #right>
        <span v-if="hasUnread" class="read-all" @click="handleReadAll">全部已读</span>
      </template>
    </van-nav-bar>

    <div class="content">
      <!-- 通知列表 -->
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多通知了"
          @load="loadMore"
        >
          <div v-for="item in notificationList" :key="item.id" class="notification-item card" @click="handleClick(item)">
            <div class="noti-left">
              <div class="noti-icon" :class="iconClass(item.type)">
                <van-icon :name="iconName(item.type)" />
              </div>
            </div>
            <div class="noti-body">
              <div class="noti-title-row">
                <span class="noti-title">{{ item.title }}</span>
                <span v-if="item.isRead === 0" class="unread-dot"></span>
              </div>
              <p class="noti-content">{{ item.content }}</p>
              <span class="noti-time">{{ formatTime(item.createdTime) }}</span>
            </div>
          </div>
          <EmptyState
            v-if="!loading && !refreshing && notificationList.length === 0"
            icon="bell-o"
            text="暂无消息"
          />
        </van-list>
      </van-pull-refresh>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getNotifications, markNotificationRead, markAllNotificationsRead } from '@/api/notification'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const notificationList = ref([])
const page = ref(1)
const size = 20
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const hasUnread = computed(() => notificationList.value.some(n => n.isRead === 0))

// 通知图标映射
const iconMap = {
  APPLY_NEW: { icon: 'friends-o', cls: 'icon-green' },
  APPLY_RESULT: { icon: 'passed', cls: 'icon-blue' },
  WAITLIST_PROMOTED: { icon: 'upgrade', cls: 'icon-orange' },
  POST_AUDIT: { icon: 'records-o', cls: 'icon-purple' },
  POST_TAKEDOWN: { icon: 'warning-o', cls: 'icon-red' },
  POST_CANCEL: { icon: 'close', cls: 'icon-red' },
  ACTIVITY_REMIND: { icon: 'clock-o', cls: 'icon-orange' },
  SYSTEM: { icon: 'info-o', cls: 'icon-grey' }
}

const iconName = (type) => iconMap[type]?.icon || 'bell-o'
const iconClass = (type) => iconMap[type]?.cls || 'icon-grey'

const loadMore = async () => {
  try {
    const res = await getNotifications({ page: page.value, size })
    const list = res.data?.records || []
    notificationList.value = page.value === 1 ? list : [...notificationList.value, ...list]
    finished.value = (res.data?.records?.length || 0) < size
    page.value++
  } catch (e) {
    finished.value = true
  } finally {
    loading.value = false
  }
}

const onRefresh = async () => {
  page.value = 1
  notificationList.value = []
  try {
    await getNotifications({ page: 1, size })
  } finally {
    refreshing.value = false
  }
  // 重新加载第一页
  finished.value = false
  loading.value = true
  await loadMore()
}

// 点击通知：标记已读 + 跳转
const handleClick = async (item) => {
  if (item.isRead === 0) {
    try {
      await markNotificationRead(item.id)
      item.isRead = 1
    } catch (e) {
      // 忽略标记失败
    }
  }
  if (item.link) {
    router.push(item.link)
  }
}

// 全部已读
const handleReadAll = async () => {
  try {
    await markAllNotificationsRead()
    notificationList.value.forEach(n => { n.isRead = 1 })
    showToast('已全部标记为已读')
  } catch (e) {
    // 拦截器已提示
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 24 * 60 * 60 * 1000) return Math.floor(diff / 3600000) + '小时前'
  return `${d.getMonth()+1}/${d.getDate()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

onMounted(() => {
  loadMore()
})
</script>

<style scoped lang="scss">
.notification-page {
  .read-all {
    font-size: 13px;
    color: var(--primary);
  }

  .content {
    padding: 12px 16px;
  }

  .notification-item {
    display: flex;
    gap: 12px;
    padding: 14px;
    margin-bottom: 12px;
    border-radius: 16px;
    background: var(--bg-card);
    box-shadow: var(--shadow-card);
    cursor: pointer;
    transition: transform 0.2s var(--ease);

    &:active {
      transform: scale(0.985);
    }

    .noti-left {
      .noti-icon {
        width: 40px;
        height: 40px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;

        .van-icon {
          font-size: 20px;
          color: #fff;
        }
      }

      .icon-green { background: #10a37f; }
      .icon-blue { background: #3b82f6; }
      .icon-orange { background: #f59e0b; }
      .icon-purple { background: #8b5cf6; }
      .icon-red { background: #ef4444; }
      .icon-grey { background: #6b7280; }
    }

    .noti-body {
      flex: 1;
      min-width: 0;

      .noti-title-row {
        display: flex;
        align-items: center;
        gap: 6px;

        .noti-title {
          font-size: 15px;
          font-weight: 600;
          color: var(--text-primary);
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .unread-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #ef4444;
          flex-shrink: 0;
        }
      }

      .noti-content {
        font-size: 13px;
        color: var(--text-secondary);
        line-height: 1.5;
        margin: 4px 0;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .noti-time {
        font-size: 12px;
        color: var(--text-hint);
      }
    }
  }
}
</style>
