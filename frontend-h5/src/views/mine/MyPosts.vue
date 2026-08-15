<template>
  <div class="page my-posts-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="我的发布" left-arrow @click-left="router.back()" />

    <!-- 状态 tab 切换 -->
    <van-tabs v-model:active="activeTab" @change="onTabChange">
      <van-tab title="全部" name="ALL" />
      <van-tab title="招募中" name="RECRUITING" />
      <van-tab title="进行中" name="ONGOING" />
      <van-tab title="已结束" name="FINISHED" />
    </van-tabs>

    <!-- 帖子列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadMore"
      >
        <div class="post-list content">
          <div v-for="post in postList" :key="post.id" class="my-post-card" @click="goDetail(post.id)">
            <div class="post-header">
              <span class="post-title">{{ post.title }}</span>
              <div class="post-tags">
                <!-- 审核状态徽章（V1.1 先审后发） -->
                <van-tag v-if="post.isAuditPass === 0" type="warning" size="medium">待审核</van-tag>
                <van-tag v-else-if="post.isAuditPass === 2" type="danger" size="medium">未通过</van-tag>
                <van-tag :type="statusTagType(post.status)" size="medium">{{ statusText(post.status) }}</van-tag>
              </div>
            </div>
            <div class="post-info">
              <span>{{ post.category }}</span>
              <span>{{ post.currentPeople }}/{{ post.maxPeople }}人</span>
              <span>{{ formatTime(post.startTime) }}</span>
            </div>
            <div class="post-actions" v-if="post.status === 'RECRUITING' || post.status === 'FULL'">
              <van-button size="small" type="primary" plain @click.stop="goManageApply(post.id)">管理报名</van-button>
            </div>
          </div>
          <EmptyState
            v-if="!loading && !refreshing && postList.length === 0"
            icon="notes-o"
            text="暂无发布记录"
            action-text="去发布"
            @action="router.push('/post/create')"
          />
        </div>
      </van-list>
    </van-pull-refresh>

    <!-- 报名管理弹出层 -->
    <van-popup v-model:show="showApplySheet" position="bottom" round :style="{ maxHeight: '70%' }">
      <div class="apply-sheet">
        <div class="apply-sheet-header">
          <span class="title">报名管理</span>
          <van-icon name="cross" @click="showApplySheet = false" />
        </div>
        <div class="apply-list" v-if="applyList.length">
          <div v-for="item in applyList" :key="item.id" class="apply-item">
            <div class="apply-user">
              <span class="apply-nickname">{{ item.applicantNickname || '用户' + item.applicantId }}</span>
              <van-tag :type="applyStatusType(item.status)" size="medium">{{ applyStatusText(item.status) }}</van-tag>
            </div>
            <div class="apply-actions" v-if="item.status === 'PENDING'">
              <van-button size="small" type="primary" @click="handleApprove(item.id)">通过</van-button>
              <van-button size="small" type="danger" plain @click="handleReject(item.id)">拒绝</van-button>
            </div>
          </div>
        </div>
        <EmptyState v-else icon="friends-o" text="暂无报名" />
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMyPosts } from '@/api/user'
import { getApplyList, approveApply, rejectApply } from '@/api/apply'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const activeTab = ref('ALL')
const postList = ref([])
const page = ref(1)
const size = 10
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

// 报名管理
const showApplySheet = ref(false)
const applyList = ref([])
const currentPostId = ref(null)

// 状态映射
const statusTextMap = {
  RECRUITING: '招募中',
  FULL: '已满员',
  ONGOING: '进行中',
  FINISHED: '已结束',
  CANCELLED: '已取消'
}

const statusText = (s) => statusTextMap[s] || s
const statusTagType = (s) => {
  const map = { RECRUITING: 'primary', FULL: 'warning', ONGOING: 'success', FINISHED: 'default', CANCELLED: 'danger' }
  return map[s] || 'default'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 加载更多
const loadMore = async () => {
  try {
    const params = {
      page: page.value,
      size
    }
    if (activeTab.value !== 'ALL') {
      params.status = activeTab.value
    }
    const res = await getMyPosts(params)
    const list = res.data?.records || res.data || []
    if (page.value === 1) {
      postList.value = list
    } else {
      postList.value.push(...list)
    }
    if (list.length < size) {
      finished.value = true
    } else {
      page.value++
    }
  } catch (err) {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

// 下拉刷新
const onRefresh = () => {
  page.value = 1
  finished.value = false
  loading.value = true
  loadMore()
}

// 切换 tab
const onTabChange = () => {
  page.value = 1
  finished.value = false
  postList.value = []
  loading.value = true
  loadMore()
}

// 跳转详情
const goDetail = (id) => {
  router.push(`/post/${id}`)
}

// 管理报名
const goManageApply = async (postId) => {
  currentPostId.value = postId
  showApplySheet.value = true
  try {
    const res = await getApplyList(postId)
    applyList.value = res.data || []
  } catch (err) {
    // 接口层已处理
  }
}

// 报名状态文本
const applyStatusText = (status) => {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消', ATTENDED: '已出勤', NOSHOW: '未到场' }
  return map[status] || status
}

// 报名状态标签类型
const applyStatusType = (status) => {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'default', ATTENDED: 'success', NOSHOW: 'danger' }
  return map[status] || 'default'
}

// 通过报名
const handleApprove = async (applyId) => {
  try {
    await approveApply(applyId)
    showToast('已通过')
    // 刷新报名列表和帖子列表
    await goManageApply(currentPostId.value)
    onRefresh()
  } catch (err) {
    // 接口层已处理
  }
}

// 拒绝报名
const handleReject = async (applyId) => {
  try {
    await rejectApply(applyId)
    showToast('已拒绝')
    await goManageApply(currentPostId.value)
    onRefresh()
  } catch (err) {
    // 接口层已处理
  }
}
</script>

<style scoped lang="scss">
.my-posts-page {
  background: var(--bg-page);
}

.post-list {
  .my-post-card {
    background: var(--bg-card);
    border-radius: 20px;
    padding: 16px 18px;
    margin-bottom: 14px;
    box-shadow: var(--shadow-card);
    cursor: pointer;
    transition: transform 0.3s var(--ease), box-shadow 0.3s var(--ease);

    &:active {
      transform: scale(0.985);
      box-shadow: var(--shadow-card-hover);
    }

    .post-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .post-title {
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

    .post-info {
      display: flex;
      gap: 12px;
      font-size: 12px;
      color: var(--text-hint);
    }

    .post-actions {
      margin-top: 10px;
      display: flex;
      justify-content: flex-end;
    }
  }
}

.apply-sheet {
  padding: 16px;

  .apply-sheet-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
    }

    .van-icon {
      font-size: 20px;
      color: var(--text-hint);
      cursor: pointer;
    }
  }

  .apply-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid var(--border-light);

    &:last-child { border-bottom: none; }

    .apply-user {
      display: flex;
      align-items: center;
      gap: 8px;

      .apply-nickname {
        font-size: 14px;
        color: var(--text-primary);
      }
    }

    .apply-actions {
      display: flex;
      gap: 8px;
    }
  }
}
</style>
