<template>
  <div class="page my-reports-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="我的举报" left-arrow @click-left="router.back()" />

    <div class="content">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="loadMore"
        >
          <div v-for="item in reportList" :key="item.id" class="report-card card">
            <div class="card-header">
              <span class="card-title">{{ item.targetTitle || statusText(item.targetType) }}</span>
              <van-tag :type="statusType(item.status)" size="medium" round>{{ statusText(item.status) }}</van-tag>
            </div>
            <div class="card-info">
              <span class="info-item"><van-icon name="warning-o" /> {{ item.reasonType }}</span>
              <span class="info-item"><van-icon name="clock-o" /> {{ formatTime(item.createdTime) }}</span>
            </div>
            <div class="card-remark" v-if="item.handleRemark">
              <span class="remark-label">处理结果：</span>{{ item.handleRemark }}
            </div>
          </div>
          <EmptyState
            v-if="!loading && !refreshing && reportList.length === 0"
            icon="warning-o"
            text="暂无举报记录"
          />
        </van-list>
      </van-pull-refresh>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyReports } from '@/api/report'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const reportList = ref([])
const page = ref(1)
const size = 10
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

// 处理状态：0待处理 1有效（已受理） 2无效（不成立）
const statusMap = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '已受理', type: 'success' },
  2: { text: '不成立', type: 'default' }
}

const statusText = (status) => statusMap[status]?.text || '未知'
const statusType = (status) => statusMap[status]?.type || 'default'

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const loadMore = async () => {
  try {
    const res = await getMyReports({ page: page.value, size })
    const data = res.data || {}
    const list = data.records || []
    reportList.value = page.value === 1 ? list : [...reportList.value, ...list]
    finished.value = page.value >= (data.pages || 1)
    page.value += 1
  } catch (err) {
    // 接口层已处理
  } finally {
    loading.value = false
  }
}

const onRefresh = async () => {
  page.value = 1
  await loadMore()
  refreshing.value = false
}
</script>

<style scoped lang="scss">
.my-reports-page {
  background: var(--bg-page);
}

.content {
  padding: 12px 16px;
}

.report-card {
  margin-bottom: 12px;
  padding: 14px 16px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;

    .card-title {
      font-size: 15px;
      font-weight: 600;
      color: var(--text-primary);
      flex: 1;
      margin-right: 8px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .card-info {
    display: flex;
    flex-wrap: wrap;
    gap: 6px 14px;
    margin-bottom: 8px;

    .info-item {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: var(--text-secondary);

      .van-icon {
        color: var(--primary);
        font-size: 13px;
      }
    }
  }

  .card-remark {
    font-size: 12px;
    color: var(--text-secondary);
    background: var(--bg-grey);
    border-radius: 8px;
    padding: 8px 10px;
    line-height: 1.6;

    .remark-label {
      color: var(--text-primary);
      font-weight: 500;
    }
  }
}
</style>
