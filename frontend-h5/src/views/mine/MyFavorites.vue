<template>
  <div class="page my-favorites-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="我的收藏" left-arrow @click-left="router.back()" />

    <div class="content">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="loadMore"
        >
          <div v-for="post in favoriteList" :key="post.id" class="fav-card card" @click="goDetail(post.id)">
            <div class="card-header">
              <span class="card-title">{{ post.title }}</span>
              <van-tag :type="statusType(post.status)" size="medium" round>{{ statusText(post.status) }}</van-tag>
            </div>
            <div class="card-info">
              <span class="info-item"><van-icon name="clock-o" /> {{ formatTime(post.startTime) }}</span>
              <span class="info-item"><van-icon name="location-o" /> {{ post.location }}</span>
              <span class="info-item"><van-icon name="friends-o" /> {{ post.currentPeople }}/{{ post.maxPeople }}</span>
            </div>
            <div class="card-footer">
              <span class="publisher">{{ post.publisherNickname || '匿名' }}</span>
              <van-button size="mini" type="danger" plain round @click.stop="handleCancel(post)">取消收藏</van-button>
            </div>
          </div>
          <EmptyState
            v-if="!loading && !refreshing && favoriteList.length === 0"
            icon="star-o"
            text="暂无收藏"
            action-text="去广场看看"
            @action="router.push('/square')"
          />
        </van-list>
      </van-pull-refresh>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { getMyFavorites, cancelFavorite } from '@/api/favorite'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const favoriteList = ref([])
const page = ref(1)
const size = 10
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const statusMap = {
  RECRUITING: { text: '招募中', type: 'primary' },
  FULL: { text: '已满员', type: 'warning' },
  ONGOING: { text: '进行中', type: 'success' },
  FINISHED: { text: '已结束', type: 'default' },
  CANCELLED: { text: '已取消', type: 'danger' }
}

const statusText = (s) => statusMap[s]?.text || s
const statusType = (s) => statusMap[s]?.type || 'default'

const loadMore = async () => {
  try {
    const res = await getMyFavorites({ page: page.value, size })
    const list = res.data?.records || []
    favoriteList.value = page.value === 1 ? list : [...favoriteList.value, ...list]
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
  favoriteList.value = []
  finished.value = false
  loading.value = true
  await loadMore()
  refreshing.value = false
}

const goDetail = (id) => router.push(`/post/${id}`)

const handleCancel = async (post) => {
  try {
    await showConfirmDialog({ title: '提示', message: `取消收藏「${post.title}」？` })
    await cancelFavorite(post.id)
    favoriteList.value = favoriteList.value.filter(f => f.id !== post.id)
    showToast('已取消收藏')
  } catch (e) {
    // 用户取消或失败
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth()+1}/${d.getDate()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
</script>

<style scoped lang="scss">
.my-favorites-page {
  .content {
    padding: 12px 16px;
  }

  .fav-card {
    padding: 14px;
    margin-bottom: 12px;
    border-radius: 16px;
    background: var(--bg-card);
    box-shadow: var(--shadow-card);
    cursor: pointer;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 8px;
      margin-bottom: 8px;

      .card-title {
        font-size: 15px;
        font-weight: 600;
        color: var(--text-primary);
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .card-info {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      margin-bottom: 10px;

      .info-item {
        font-size: 12px;
        color: var(--text-hint);
        display: flex;
        align-items: center;
        gap: 3px;
      }
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 10px;
      border-top: 1px solid var(--border-light);

      .publisher {
        font-size: 13px;
        color: var(--text-secondary);
      }
    }
  }
}
</style>
