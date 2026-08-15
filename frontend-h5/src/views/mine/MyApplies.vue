<template>
  <div class="page my-applies-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="我的预约" left-arrow @click-left="router.back()" />

    <!-- 两个 tab -->
    <van-tabs v-model:active="activeTab" @change="onTabChange">
      <van-tab title="我报名的" name="my" />
      <van-tab title="收到的邀请" name="received" />
    </van-tabs>

    <!-- 我报名的 -->
    <div v-show="activeTab === 'my'" class="content">
      <van-pull-refresh v-model="myRefreshing" @refresh="onMyRefresh">
        <van-list
          v-model:loading="myLoading"
          :finished="myFinished"
          finished-text="没有更多了"
          @load="loadMyApplies"
        >
          <div
            v-for="item in myApplyList"
            :key="item.id"
            class="apply-item card"
            @click="goDetail(item.postId)"
          >
            <div class="apply-header">
              <span class="post-title text-ellipsis">{{ item.postTitle }}</span>
              <van-tag :type="applyStatusType(item.status)" size="medium">
                {{ applyStatusText(item.status) }}
              </van-tag>
            </div>
            <div class="apply-time">
              报名时间：{{ formatTime(item.applyTime) }}
            </div>
          </div>
          <EmptyState
            v-if="!myLoading && !myRefreshing && myApplyList.length === 0"
            icon="orders-o"
            text="暂无报名记录"
          />
        </van-list>
      </van-pull-refresh>
    </div>

    <!-- 收到的邀请 -->
    <div v-show="activeTab === 'received'" class="content">
      <van-pull-refresh v-model="receivedRefreshing" @refresh="onReceivedRefresh">
        <van-list
          v-model:loading="receivedLoading"
          :finished="receivedFinished"
          finished-text="没有更多了"
          @load="loadReceivedApplies"
        >
          <div
            v-for="item in receivedList"
            :key="item.id"
            class="apply-item card"
          >
            <div class="apply-header">
              <div class="applicant-info">
                <UserAvatar :name="item.applicantNickname" :avatar="item.applicantAvatar" :size="32" />
                <span class="applicant-name">{{ item.applicantNickname || '用户' }}</span>
              </div>
              <van-tag type="primary" size="medium" v-if="item.status === 'PENDING'">待审核</van-tag>
              <van-tag type="success" size="medium" v-else-if="item.status === 'APPROVED'">已同意</van-tag>
              <van-tag type="danger" size="medium" v-else-if="item.status === 'REJECTED'">已拒绝</van-tag>
            </div>
            <div class="apply-post-title">
              组局：{{ item.postTitle }}
            </div>
            <div class="apply-time">
              申请时间：{{ formatTime(item.applyTime) }}
            </div>
            <!-- 待审核状态显示操作按钮 -->
            <div class="apply-actions" v-if="item.status === 'PENDING'">
              <van-button
                size="small"
                type="primary"
                round
                @click.stop="handleApprove(item.id)"
              >
                同意
              </van-button>
              <van-button
                size="small"
                plain
                type="danger"
                round
                @click.stop="handleReject(item.id)"
              >
                拒绝
              </van-button>
            </div>
          </div>
          <EmptyState
            v-if="!receivedLoading && !receivedRefreshing && receivedList.length === 0"
            icon="orders-o"
            text="暂无收到邀请"
          />
        </van-list>
      </van-pull-refresh>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getMyApplies } from '@/api/user'
import { getReceivedApplies, approveApply, rejectApply } from '@/api/apply'
import EmptyState from '@/components/EmptyState.vue'
import UserAvatar from '@/components/UserAvatar.vue'

const router = useRouter()
const activeTab = ref('my')

// 我报名的
const myApplyList = ref([])
const myPage = ref(1)
const mySize = 10
const myLoading = ref(false)
const myFinished = ref(false)
const myRefreshing = ref(false)

// 收到的邀请
const receivedList = ref([])
const receivedPage = ref(1)
const receivedSize = 10
const receivedLoading = ref(false)
const receivedFinished = ref(false)
const receivedRefreshing = ref(false)

// 报名状态映射
const applyStatusText = (status) => {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    CANCELLED: '已取消',
    WAITLIST: '候补中',
    ATTENDED: '已出勤',
    NOSHOW: '未到场'
  }
  return map[status] || '未知'
}

const applyStatusType = (status) => {
  const map = {
    PENDING: 'primary',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'default',
    WAITLIST: 'warning',
    ATTENDED: 'success',
    NOSHOW: 'danger'
  }
  return map[status] || 'default'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 加载我报名的
const loadMyApplies = async () => {
  try {
    const res = await getMyApplies({ page: myPage.value, size: mySize })
    const list = res.data?.records || res.data || []
    if (myPage.value === 1) {
      myApplyList.value = list
    } else {
      myApplyList.value.push(...list)
    }
    if (list.length < mySize) {
      myFinished.value = true
    } else {
      myPage.value++
    }
  } catch (err) {
    myFinished.value = true
  } finally {
    myLoading.value = false
    myRefreshing.value = false
  }
}

// 加载收到的邀请
const loadReceivedApplies = async () => {
  try {
    const res = await getReceivedApplies({ page: receivedPage.value, size: receivedSize })
    const list = res.data?.records || res.data || []
    if (receivedPage.value === 1) {
      receivedList.value = list
    } else {
      receivedList.value.push(...list)
    }
    if (list.length < receivedSize) {
      receivedFinished.value = true
    } else {
      receivedPage.value++
    }
  } catch (err) {
    receivedFinished.value = true
  } finally {
    receivedLoading.value = false
    receivedRefreshing.value = false
  }
}

// 下拉刷新
const onMyRefresh = () => {
  myPage.value = 1
  myFinished.value = false
  myLoading.value = true
  loadMyApplies()
}

const onReceivedRefresh = () => {
  receivedPage.value = 1
  receivedFinished.value = false
  receivedLoading.value = true
  loadReceivedApplies()
}

// 切换 tab
const onTabChange = () => {
  // 数据已在各自列表中维护，切换时无需重新加载
}

// 同意申请
const handleApprove = async (id) => {
  try {
    await approveApply(id)
    showToast('已同意')
    onReceivedRefresh()
  } catch (err) {
    // 接口层已处理
  }
}

// 拒绝申请
const handleReject = async (id) => {
  try {
    await showConfirmDialog({ title: '提示', message: '确定要拒绝该申请吗？' })
    await rejectApply(id)
    showToast('已拒绝')
    onReceivedRefresh()
  } catch (err) {
    // 用户取消或接口错误
  }
}

// 跳转帖子详情
const goDetail = (postId) => {
  if (postId) {
    router.push(`/post/${postId}`)
  }
}
</script>

<style scoped lang="scss">
.my-applies-page {
  background: var(--bg-page);
}

.apply-item {
  cursor: pointer;
  transition: transform 0.3s var(--ease), box-shadow 0.3s var(--ease);

  &:active {
    transform: scale(0.985);
    box-shadow: var(--shadow-card-hover);
  }

  .apply-header {
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
    }

    .applicant-info {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 1;

      .applicant-name {
        font-size: 15px;
        font-weight: 500;
        color: var(--text-primary);
      }
    }
  }

  .apply-post-title {
    font-size: 14px;
    color: var(--text-secondary);
    margin-bottom: 4px;
  }

  .apply-time {
    font-size: 12px;
    color: var(--text-hint);
  }

  .apply-actions {
    display: flex;
    gap: 8px;
    margin-top: 10px;
    justify-content: flex-end;

    .van-button--primary {
      border: none;
    }
  }
}
</style>
