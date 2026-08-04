<template>
  <div class="empty-state">
    <van-icon :name="icon" size="60" color="#c7cad1" />
    <p class="empty-text">{{ text }}</p>
    <van-button v-if="actionText" type="primary" size="small" round @click="$emit('action')">
      {{ actionText }}
    </van-button>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: 'search' },
  text: { type: String, default: '暂无数据' },
  actionText: { type: String, default: '' }
})

defineEmits(['action'])
</script>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;

  .van-icon {
    color: var(--text-disabled) !important;
  }

  .empty-text {
    margin: 16px 0;
    color: var(--text-hint);
    font-size: 14px;
  }
}
</style>
