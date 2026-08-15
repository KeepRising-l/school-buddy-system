<template>
  <div class="page user-profile-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="个人主页" left-arrow @click-left="router.back()" />

    <div v-if="profile" class="content">
      <!-- 用户信息卡片 -->
      <div class="profile-card card">
        <div class="user-row">
          <UserAvatar :name="profile.nickname" :avatar="profile.avatar" :size="64" />
          <div class="user-info">
            <div class="name-row">
              <span class="nickname">{{ profile.nickname || '用户' }}</span>
              <CreditStar :score="profile.creditScore ?? 100" size="small" />
            </div>
            <div class="credit-row">
              <span class="credit-score">信誉分 {{ profile.creditScore ?? 100 }}</span>
              <span class="credit-badge" :class="creditBadgeClass">{{ profile.creditLabel }}</span>
            </div>
            <div class="college" v-if="profile.college">{{ profile.college }}</div>
            <p class="signature" v-if="profile.signature">{{ profile.signature }}</p>
          </div>
        </div>

        <!-- 兴趣标签 -->
        <div class="tag-section" v-if="profile.tags && profile.tags.length">
          <h3 class="section-title">兴趣标签</h3>
          <div class="tag-list">
            <van-tag
              v-for="tag in profile.tags"
              :key="tag.id"
              type="primary"
              plain
              size="medium"
              round
            >
              {{ tag.name }}
              <span v-if="tag.selfLevel" class="tag-level">{{ levelText(tag.selfLevel) }}</span>
            </van-tag>
          </div>
        </div>

        <!-- 评价标签云 -->
        <div class="review-tag-section" v-if="profile.reviewTagStats && profile.reviewTagStats.length">
          <h3 class="section-title">大家眼中的TA</h3>
          <div class="review-tag-list">
            <span
              v-for="stat in profile.reviewTagStats"
              :key="stat.tag"
              class="review-tag"
              :class="tagClass(stat.tag)"
            >
              {{ stat.tag }} ×{{ stat.count }}
            </span>
          </div>
        </div>
      </div>

      <!-- 历史组局 -->
      <div class="history-card card" v-if="profile.historyPosts && profile.historyPosts.length">
        <h3 class="section-title">历史组局</h3>
        <div
          v-for="post in profile.historyPosts"
          :key="post.id"
          class="history-item"
          @click="router.push(`/post/${post.id}`)"
        >
          <span class="post-title">{{ post.title }}</span>
          <van-tag :type="statusType(post.status)" size="small" round>{{ statusText(post.status) }}</van-tag>
        </div>
      </div>
      <div class="privacy-tip" v-else-if="profile.profilePrivacy === 1">
        <van-icon name="lock" /> 该用户设置了隐私，历史组局不可见
      </div>

      <!-- 收到的评价 -->
      <div class="reviews-card card" v-if="profile.recentReviews && profile.recentReviews.length">
        <h3 class="section-title">收到的评价</h3>
        <div v-for="review in profile.recentReviews" :key="review.id" class="review-item">
          <div class="review-header">
            <span class="reviewer">{{ review.reviewerNickname || '匿名' }}</span>
            <van-rate :model-value="review.rating" readonly :size="14" color="#f59e0b" void-color="#e5e7eb" />
          </div>
          <p class="review-content">{{ review.content || '（无文字评价）' }}</p>
          <span class="review-time">{{ formatTime(review.createdTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 加载失败态（V1.2：与 PostDetail 一致，不再无限转圈） -->
    <div class="loading-area" v-else-if="loadError">
      <van-empty description="加载失败，请检查网络后重试">
        <van-button type="primary" round size="small" @click="reload">重新加载</van-button>
      </van-empty>
    </div>

    <!-- 加载中 -->
    <div class="loading-area" v-else>
      <van-loading size="24px">加载中...</van-loading>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserProfile } from '@/api/user'
import CreditStar from '@/components/CreditStar.vue'
import UserAvatar from '@/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()

// 用户ID（可变：主页之间跳转时组件复用，需跟随路由参数更新）
let userId = route.params.userId
const profile = ref(null)

const creditBadgeClass = computed(() => {
  const score = profile.value?.creditScore ?? 100
  if (score >= 90) return 'badge-excellent'
  if (score >= 75) return 'badge-good'
  if (score >= 60) return 'badge-normal'
  return 'badge-warning'
})

const statusMap = {
  RECRUITING: { text: '招募中', type: 'primary' },
  FULL: { text: '已满员', type: 'warning' },
  ONGOING: { text: '进行中', type: 'success' },
  FINISHED: { text: '已结束', type: 'default' },
  CANCELLED: { text: '已取消', type: 'danger' }
}

const statusText = (s) => statusMap[s]?.text || s
const statusType = (s) => statusMap[s]?.type || 'default'

const levelText = (level) => {
  const map = { 1: '青铜', 2: '白银', 3: '黄金', 4: '钻石', 5: '王者' }
  return map[level] || ''
}

// 评价标签配色
const tagClass = (tag) => {
  const negative = ['爽约', '迟到', '态度差']
  const positive = ['靠谱', '友好', '技术好', '准时']
  if (negative.includes(tag)) return 'tag-negative'
  if (positive.includes(tag)) return 'tag-positive'
  return 'tag-neutral'
}

// 加载失败态（V1.2）
const loadError = ref(false)

const fetchProfile = async () => {
  loadError.value = false
  try {
    const res = await getUserProfile(userId)
    profile.value = res.data
  } catch (err) {
    // 修复：失败显示错误态+重试，而不是永远"加载中"
    loadError.value = true
  }
}

const reload = () => {
  profile.value = null
  fetchProfile()
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

// 路由参数变化（/user/1 -> /user/2）时重新加载，避免组件复用导致数据错乱（V1.2 修复）
watch(
  () => route.params.userId,
  (newId) => {
    if (!newId || String(newId) === String(userId)) return
    userId = newId
    profile.value = null
    fetchProfile()
  }
)

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped lang="scss">
.user-profile-page {
  .content {
    padding: 12px 16px;
  }

  .card {
    padding: 16px;
    margin-bottom: 12px;
    border-radius: 16px;
    background: var(--bg-card);
    box-shadow: var(--shadow-card);
  }

  .user-row {
    display: flex;
    gap: 14px;

    .user-info {
      flex: 1;
      min-width: 0;

      .name-row {
        display: flex;
        align-items: center;
        gap: 8px;

        .nickname {
          font-size: 18px;
          font-weight: 700;
          color: var(--text-primary);
        }
      }

      .credit-row {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-top: 4px;

        .credit-score {
          font-size: 13px;
          color: var(--text-secondary);
        }

        .credit-badge {
          font-size: 11px;
          padding: 2px 8px;
          border-radius: 999px;
          font-weight: 500;

          &.badge-excellent { background: rgba(16,163,127,0.12); color: #10a37f; }
          &.badge-good { background: rgba(59,130,246,0.12); color: #3b82f6; }
          &.badge-normal { background: rgba(245,158,11,0.12); color: #d97706; }
          &.badge-warning { background: rgba(239,68,68,0.12); color: #ef4444; }
        }
      }

      .college {
        font-size: 12px;
        color: var(--text-hint);
        margin-top: 4px;
      }

      .signature {
        font-size: 13px;
        color: var(--text-secondary);
        margin-top: 6px;
      }
    }
  }

  .section-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 12px 0 10px;
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;

    .tag-level {
      font-size: 11px;
      opacity: 0.8;
    }
  }

  .review-tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .review-tag {
      font-size: 13px;
      padding: 4px 12px;
      border-radius: 999px;

      &.tag-positive { background: rgba(16,163,127,0.1); color: #10a37f; }
      &.tag-negative { background: rgba(239,68,68,0.1); color: #ef4444; }
      &.tag-neutral { background: rgba(107,114,128,0.1); color: #6b7280; }
    }
  }

  .history-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    padding: 10px 0;
    border-bottom: 1px solid var(--border-light);
    cursor: pointer;

    &:last-child {
      border-bottom: none;
    }

    .post-title {
      font-size: 14px;
      color: var(--text-primary);
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .privacy-tip {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 24px 0;
    font-size: 13px;
    color: var(--text-hint);
  }

  .review-item {
    padding: 10px 0;
    border-bottom: 1px solid var(--border-light);

    &:last-child {
      border-bottom: none;
    }

    .review-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .reviewer {
        font-size: 13px;
        font-weight: 500;
        color: var(--text-primary);
      }
    }

    .review-content {
      font-size: 13px;
      color: var(--text-secondary);
      margin: 4px 0;
    }

    .review-time {
      font-size: 11px;
      color: var(--text-hint);
    }
  }

  .loading-area {
    padding: 80px 0;
    text-align: center;
  }
}
</style>
