<template>
  <div class="credit-star" :class="[size]">
    <van-icon v-for="i in 5" :key="i" name="star" :class="{ active: i <= level }" />
    <span v-if="showLabel" class="credit-label">{{ label }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  score: { type: Number, default: 100 },
  size: { type: String, default: 'normal' }, // small / normal
  showLabel: { type: Boolean, default: false }
})

const level = computed(() => {
  if (props.score >= 90) return 5
  if (props.score >= 75) return 4
  if (props.score >= 60) return 3
  if (props.score >= 40) return 2
  return 1
})

const label = computed(() => {
  if (props.score >= 90) return '靠谱队友'
  if (props.score >= 75) return '优质队友'
  if (props.score >= 60) return '普通队友'
  if (props.score >= 40) return '需改善'
  return '失信用户'
})
</script>

<style scoped lang="scss">
.credit-star {
  display: inline-flex;
  align-items: center;
  gap: 1px;

  .van-icon {
    color: var(--text-disabled);
    font-size: 14px;
    &.active { color: #f59e0b; }
  }

  .credit-label {
    margin-left: 4px;
    font-size: 11px;
    color: var(--text-hint);
  }

  &.small .van-icon { font-size: 10px; }
}
</style>
