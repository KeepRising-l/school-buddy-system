<template>
  <div class="user-avatar-wrapper" :style="{ width: size + 'px', height: size + 'px' }">
    <van-image
      v-if="showImage"
      round
      :width="size"
      :height="size"
      :src="avatar"
      :fit="fit"
      @error="onImageError"
    />
    <div
      v-else
      class="avatar-placeholder"
      :style="{
        width: size + 'px',
        height: size + 'px',
        fontSize: Math.max(size * 0.42, 12) + 'px',
        background: bgColor,
        borderRadius: round ? '50%' : radius || '50%'
      }"
    >
      {{ firstChar }}
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  name: { type: String, default: '' },
  avatar: { type: String, default: '' },
  size: { type: Number, default: 40 },
  round: { type: Boolean, default: true },
  radius: { type: String, default: '' },
  fit: { type: String, default: 'cover' }
})

const imageFailed = ref(false)

const showImage = computed(() => !!props.avatar && !imageFailed.value)

const firstChar = computed(() => {
  const n = props.name || ''
  return n.charAt(0).toUpperCase() || '?'
})

const bgColor = computed(() => {
  const palette = [
    '#3b82f6', '#10a37f', '#d97706', '#e5484d', '#6b7280',
    '#65a30d', '#9333ea', '#0ea5e9', '#db2777', '#ca8a04',
    '#14b8a6', '#7c3aed', '#f97316', '#2563eb', '#84cc16'
  ]
  const n = props.name || '?'
  let hash = 0
  for (let i = 0; i < n.length; i++) {
    hash = n.charCodeAt(i) + ((hash << 5) - hash)
  }
  return palette[Math.abs(hash) % palette.length]
})

const onImageError = () => {
  imageFailed.value = true
}
</script>

<style scoped>
.user-avatar-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 0;
}
.avatar-placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  user-select: none;
  line-height: 1;
}
</style>
