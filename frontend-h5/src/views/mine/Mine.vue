<template>
  <TabbarLayout>
    <div class="mine-page page-with-tabbar">
      <!-- 已登录状态 -->
      <template v-if="userStore.isLoggedIn">
        <!-- 渐变头部 -->
        <div class="user-header">
          <div class="user-card">
            <UserAvatar
              :name="userStore.userInfo.nickname"
              :avatar="userStore.userInfo.avatar"
              :size="64"
              class="user-avatar"
            />
            <div class="user-info">
              <div class="name-row">
                <span class="nickname">{{ userStore.userInfo.nickname || userStore.userInfo.name || '用户' }}</span>
                <CreditStar :score="userStore.userInfo.creditScore || 100" size="small" />
              </div>
              <div class="credit-row">
                <span class="credit-score">信誉分 {{ userStore.userInfo.creditScore || 100 }}</span>
                <span class="credit-badge" :class="creditBadgeClass">{{ userStore.creditLabel }}</span>
              </div>
              <div class="college" v-if="userStore.userInfo.college">{{ userStore.userInfo.college }}</div>
            </div>
          </div>

          <!-- 数据统计 -->
          <div class="stats-row">
            <div class="stat-item" @click="router.push('/mine/posts')">
              <span class="stat-num">{{ userStore.userInfo.postCount || 0 }}</span>
              <span class="stat-label">发布</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item" @click="router.push('/mine/applies')">
              <span class="stat-num">{{ userStore.userInfo.applyCount || 0 }}</span>
              <span class="stat-label">报名</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-num">{{ userStore.userInfo.creditScore || 100 }}</span>
              <span class="stat-label">信誉</span>
            </div>
          </div>
        </div>

        <!-- 功能列表 -->
        <van-cell-group inset class="func-group">
          <van-cell title="个人资料" is-link to="/mine/profile" icon="user-o" />
          <van-cell title="兴趣标签" is-link to="/mine/tags" icon="label-o" />
          <van-cell title="我的发布" is-link to="/mine/posts" icon="notes-o" />
          <van-cell title="我的预约" is-link to="/mine/applies" icon="orders-o" />
        </van-cell-group>

        <!-- 退出登录 -->
        <van-cell-group inset class="func-group">
          <van-cell
            title="退出登录"
            class="logout-cell"
            @click="handleLogout"
          />
        </van-cell-group>
      </template>

      <!-- 未登录状态 -->
      <template v-else>
        <div class="not-logged">
          <div class="not-logged-icon">
            <van-icon name="user-o" size="48" color="#10a37f" />
          </div>
          <p class="not-logged-text">登录后查看更多内容</p>
          <van-button type="primary" round to="/login" class="login-btn">去登录</van-button>
        </div>
      </template>
    </div>
  </TabbarLayout>
</template>

<script setup>
import { computed } from 'vue'
import { showConfirmDialog } from 'vant'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import TabbarLayout from '@/components/TabbarLayout.vue'
import CreditStar from '@/components/CreditStar.vue'
import UserAvatar from '@/components/UserAvatar.vue'

const router = useRouter()
const userStore = useUserStore()

const creditBadgeClass = computed(() => {
  const score = userStore.userInfo.creditScore || 100
  if (score >= 90) return 'badge-excellent'
  if (score >= 75) return 'badge-good'
  if (score >= 60) return 'badge-normal'
  return 'badge-warning'
})

const handleLogout = async () => {
  try {
    await showConfirmDialog({ title: '提示', message: '确定要退出登录吗？' })
    userStore.logout()
    router.replace('/login')
  } catch (err) {
    // 用户取消
  }
}
</script>

<style scoped lang="scss">
.mine-page {
  background: var(--bg-page);
}

.user-header {
  background: var(--bg-card);
  padding: 24px 20px 20px;
  border-radius: 0 0 28px 28px;
  box-shadow: 0 20px 40px -28px rgba(0, 0, 0, 0.1);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;

  .user-avatar {
    border: 3px solid var(--primary-soft);
    flex-shrink: 0;
  }

  .user-info {
    flex: 1;

    .name-row {
      display: flex;
      align-items: center;
      gap: 8px;

      .nickname {
        font-size: 20px;
        font-weight: 700;
        letter-spacing: -0.01em;
        color: var(--text-primary);
      }
    }

    .credit-row {
      margin-top: 6px;
      display: flex;
      align-items: center;
      gap: 8px;

      .credit-score {
        font-size: 13px;
        color: var(--text-secondary);
      }

      .credit-badge {
        padding: 2px 8px;
        border-radius: 999px;
        font-size: 11px;
        font-weight: 500;

        &.badge-excellent { background: var(--primary-soft); color: var(--primary-dark); }
        &.badge-good { background: var(--primary-soft); color: var(--primary-dark); }
        &.badge-normal { background: rgba(245, 166, 35, 0.15); color: #b45309; }
        &.badge-warning { background: rgba(229, 72, 77, 0.12); color: #c53030; }
      }
    }

    .college {
      margin-top: 4px;
      font-size: 12px;
      color: var(--text-hint);
    }
  }
}

.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin-top: 20px;
  padding: 14px 0;
  background: var(--bg-grey);
  border-radius: 16px;

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    cursor: pointer;

    .stat-num {
      font-size: 18px;
      font-weight: 700;
      color: var(--text-primary);
    }

    .stat-label {
      font-size: 11px;
      color: var(--text-hint);
    }
  }

  .stat-divider {
    width: 1px;
    height: 24px;
    background: var(--border-light);
  }
}

.func-group {
  margin: 16px 12px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: var(--shadow-card);

  :deep(.van-cell) {
    border-radius: 0;
    margin-bottom: 0;

    &:not(:last-child)::after {
      left: 16px;
      right: 16px;
      border-color: var(--border-light);
    }
  }

  .logout-cell {
    text-align: center;

    :deep(.van-cell__title) {
      color: var(--danger);
      text-align: center;
    }
  }
}

.not-logged {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120px 0 0;

  .not-logged-icon {
    width: 80px;
    height: 80px;
    border-radius: 24px;
    background: var(--primary-soft);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .not-logged-text {
    margin: 20px 0 28px;
    font-size: 16px;
    color: var(--text-hint);
  }

  .login-btn {
    width: 160px;
    background: var(--primary);
    border: none;
    height: 44px;
    font-size: 16px;
    box-shadow: 0 16px 32px -12px rgba(16, 163, 127, 0.35);
  }
}
</style>
