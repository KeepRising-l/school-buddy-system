<template>
  <div class="post-card" @click="$emit('click')">
    <div class="post-header">
      <span class="post-title text-ellipsis-2">{{ post.title }}</span>
      <van-tag :type="statusType" size="medium" round class="status-tag">{{ statusText }}</van-tag>
    </div>

    <!-- 描述预览 -->
    <p class="post-desc text-ellipsis-2" v-if="post.description">{{ post.description }}</p>

    <!-- 标签 -->
    <div class="post-tags" v-if="post.category">
      <van-tag type="primary" plain size="medium" round>{{ post.category }}</van-tag>
      <van-tag v-if="post.tagName" plain size="medium" round>{{ post.tagName }}</van-tag>
    </div>

    <!-- 信息行 -->
    <div class="post-info">
      <div class="info-item">
        <van-icon name="clock-o" />
        <span>{{ formatTime(post.startTime) }}</span>
      </div>
      <div class="info-item">
        <van-icon name="location-o" />
        <span class="text-ellipsis">{{ post.location }}</span>
      </div>
    </div>

    <!-- 底部 -->
    <div class="post-footer">
      <div class="publisher">
        <UserAvatar :name="post.publisherNickname" :avatar="post.publisherAvatar" :size="24" />
        <span class="publisher-name">{{ post.publisherNickname || '匿名' }}</span>
        <CreditStar :score="(post.publisherCreditLevel || 5) * 20" size="small" />
      </div>
      <div class="people">
        <div class="people-bar">
          <div class="people-fill" :style="{ width: peoplePercent + '%' }"></div>
        </div>
        <span class="people-text">{{ post.currentPeople }}/{{ post.maxPeople }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import CreditStar from './CreditStar.vue'
import UserAvatar from './UserAvatar.vue'

const props = defineProps({
  post: { type: Object, required: true }
})

defineEmits(['click'])

const statusMap = {
  RECRUITING: { text: '招募中', type: 'primary' },
  FULL: { text: '已满员', type: 'warning' },
  ONGOING: { text: '进行中', type: 'success' },
  FINISHED: { text: '已结束', type: 'default' },
  CANCELLED: { text: '已取消', type: 'danger' }
}

const statusText = computed(() => statusMap[props.post.status]?.text || '未知')
const statusType = computed(() => statusMap[props.post.status]?.type || 'default')

const peoplePercent = computed(() => {
  if (!props.post.maxPeople) return 0
  return Math.min(100, Math.round((props.post.currentPeople / props.post.maxPeople) * 100))
})

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth()+1}/${d.getDate()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
</script>

<style scoped lang="scss">
.post-card {
  background: var(--bg-card);
  border-radius: 20px;
  padding: 18px;
  margin-bottom: 14px;
  box-shadow: var(--shadow-card), var(--shadow-inset-highlight);
  border: 1px solid rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: transform 0.3s var(--ease), box-shadow 0.3s var(--ease);

  &:active {
    background: #fafbfc;
    transform: scale(0.985);
    box-shadow: var(--shadow-card-hover);
  }

  .post-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 8px;

    .post-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
      flex: 1;
      line-height: 1.45;
    }

    .status-tag {
      flex-shrink: 0;
    }
  }

  .post-desc {
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.5;
    margin-bottom: 8px;
  }

  .post-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 10px;
  }

  .post-info {
    display: flex;
    gap: 16px;
    margin-bottom: 10px;

    .info-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: var(--text-hint);

      .van-icon { color: var(--text-disabled); font-size: 13px; }

      span {
        max-width: 120px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .post-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid var(--border-light);

    .publisher {
      display: flex;
      align-items: center;
      gap: 6px;

      .publisher-name {
        font-size: 13px;
        color: var(--text-secondary);
      }
    }

    .people {
      display: flex;
      align-items: center;
      gap: 8px;

      .people-bar {
        width: 48px;
        height: 6px;
        background: var(--bg-grey);
        border-radius: 3px;
        overflow: hidden;

        .people-fill {
          height: 100%;
          background: var(--primary);
          border-radius: 3px;
          transition: width 0.3s var(--ease);
        }
      }

      .people-text {
        font-size: 12px;
        color: var(--primary-dark);
        font-weight: 500;
      }
    }
  }
}
</style>
