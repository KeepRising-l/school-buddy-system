<template>
  <article class="editorial-stream-item" @click="$emit('click')">
    <!-- 左侧：等宽数字席位锚点 -->
    <div class="slot-anchor">
      <span class="slot-ratio">{{ formattedCurrent }}/{{ formattedMax }}</span>
      <span :class="['slot-hint', hintClass]">{{ slotHintText }}</span>
    </div>

    <!-- 右侧：编辑级排版正文 -->
    <div class="item-content">
      <!-- 眉标：分类 · 段位门槛 · 推荐理由 -->
      <div class="item-kicker">
        <span>{{ post.category || '组局' }}</span>
        <template v-if="post.tagName && post.tagName !== post.category">
          <span class="dot-sep">·</span>
          <span>{{ post.tagName }}</span>
        </template>
        <template v-if="requireLevelText">
          <span class="dot-sep">·</span>
          <span>{{ requireLevelText }}</span>
        </template>
        <template v-if="primaryReason">
          <span class="dot-sep">·</span>
          <span class="kicker-reason">{{ primaryReason }}</span>
        </template>
      </div>

      <!-- 标题 -->
      <h2 class="item-title text-ellipsis-2">{{ post.title }}</h2>

      <!-- 时间与地点 -->
      <div class="item-schedule text-ellipsis">
        {{ formatTime(post.startTime) }}<template v-if="post.location"> · {{ post.location }}</template>
      </div>

      <!-- 底行：发布者信息 + 克制行动按钮 -->
      <div class="item-bottom">
        <div class="author-meta text-ellipsis">
          <span class="author-name">{{ post.publisherNickname || '匿名' }}</span>
          <template v-if="post.publisherCreditScore != null || post.publisherCreditLevel != null">
            <span class="dot-sep">·</span>
            <span>信誉 {{ post.publisherCreditScore ?? (post.publisherCreditLevel * 20) }}</span>
          </template>
        </div>
        <span :class="['editorial-action', { urgent: isUrgent, muted: isMutedStatus }]">
          {{ actionLabel }}
        </span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  post: { type: Object, required: true }
})

defineEmits(['click'])

const pad2 = (n) => String(n ?? 0).padStart(2, '0')
const formattedCurrent = computed(() => pad2(props.post.currentPeople))
const formattedMax = computed(() => pad2(props.post.maxPeople))

const spotsLeft = computed(() => {
  const max = Number(props.post.maxPeople || 0)
  const cur = Number(props.post.currentPeople || 0)
  return Math.max(0, max - cur)
})

const isUrgent = computed(() => props.post.status === 'RECRUITING' && spotsLeft.value > 0 && spotsLeft.value <= 1)
const isMutedStatus = computed(() => props.post.status === 'FINISHED' || props.post.status === 'CANCELLED')

const slotHintText = computed(() => {
  const st = props.post.status
  if (st === 'RECRUITING') {
    if (spotsLeft.value <= 2 && spotsLeft.value > 0) return `差 ${spotsLeft.value} 人`
    return '招募中'
  }
  const map = {
    FULL: '已满候补',
    ONGOING: '进行中',
    FINISHED: '已结束',
    CANCELLED: '已取消'
  }
  return map[st] || '招募中'
})

const hintClass = computed(() => {
  const st = props.post.status
  if (st === 'RECRUITING' && spotsLeft.value <= 2 && spotsLeft.value > 0) return 'is-urgent'
  if (st === 'ONGOING') return 'is-ongoing'
  if (st === 'FULL') return 'is-warn'
  return 'is-calm'
})

const actionLabel = computed(() => {
  const st = props.post.status
  if (st === 'RECRUITING') return '申请入局'
  if (st === 'FULL') return '排队候补'
  if (st === 'ONGOING') return '去签到'
  return '查看详情'
})

const requireLevelText = computed(() => {
  const map = { 1: '萌新以上', 2: '小白以上', 3: '门槛 Lv3 熟手', 4: '门槛 Lv4 高手', 5: '门槛 Lv5 宗师' }
  return map[props.post.requireLevel] || '不限段位'
})

const primaryReason = computed(() => {
  if (Array.isArray(props.post.recommendReasons) && props.post.recommendReasons.length > 0) {
    return props.post.recommendReasons[0]
  }
  return ''
})

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return time
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const tomorrow = new Date(now)
  tomorrow.setDate(now.getDate() + 1)
  const isTomorrow = d.toDateString() === tomorrow.toDateString()
  const hhmm = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  if (isToday) return `今日 ${hhmm}`
  if (isTomorrow) return `明日 ${hhmm}`
  return `${d.getMonth() + 1}月${d.getDate()}日 ${hhmm}`
}
</script>

<style scoped lang="scss">
.editorial-stream-item {
  padding: 18px 0;
  border-bottom: 1px solid var(--border-color);
  display: grid;
  grid-template-columns: 56px 1fr;
  gap: 14px;
  align-items: start;
  cursor: pointer;
  transition: background-color 0.18s var(--ease);

  &:active {
    background-color: rgba(28, 27, 24, 0.025);
  }

  .slot-anchor {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding-top: 2px;

    .slot-ratio {
      font-family: var(--font-mono);
      font-size: 17px;
      font-weight: 600;
      color: var(--text-primary);
      letter-spacing: -0.5px;
      line-height: 1.15;
    }

    .slot-hint {
      font-size: 11px;
      font-weight: 600;
      line-height: 1.2;

      &.is-urgent {
        color: var(--primary);
      }

      &.is-ongoing {
        color: var(--success);
      }

      &.is-warn {
        color: var(--warning);
      }

      &.is-calm {
        color: var(--text-hint);
        font-weight: 500;
      }
    }
  }

  .item-content {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 5px;
  }

  .item-kicker {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 5px;
    font-size: 12px;
    color: var(--text-secondary);
    line-height: 1.3;

    .dot-sep {
      color: var(--text-disabled);
    }

    .kicker-reason {
      color: var(--primary);
      font-weight: 500;
    }
  }

  .item-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    line-height: 1.42;
    letter-spacing: -0.15px;
  }

  .item-schedule {
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.45;
  }

  .item-bottom {
    margin-top: 4px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }

  .author-meta {
    font-size: 12px;
    color: var(--text-hint);
    min-width: 0;

    .author-name {
      color: var(--text-secondary);
      font-weight: 500;
    }

    .dot-sep {
      margin: 0 5px;
      color: var(--text-disabled);
    }
  }

  .editorial-action {
    flex-shrink: 0;
    font-size: 12px;
    font-weight: 600;
    color: var(--primary);
    background: var(--primary-soft);
    padding: 5px 12px;
    border-radius: var(--radius-md);
    transition: transform 0.15s var(--ease), background-color 0.18s var(--ease);

    &.urgent {
      background: var(--primary);
      color: #ffffff;
    }

    &.muted {
      background: var(--bg-grey);
      color: var(--text-secondary);
      font-weight: 500;
    }
  }
}
</style>
