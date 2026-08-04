<template>
  <el-avatar
    :size="size"
    :src="showImage ? avatar : undefined"
    :shape="round ? 'circle' : 'square'"
    :style="!showImage ? { background: bgColor, border: 'none' } : undefined"
    v-bind="$attrs"
  >
    <span v-if="!showImage" :style="{ fontSize: Math.max(size * 0.4, 14) + 'px' }">{{ firstChar }}</span>
  </el-avatar>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  name: { type: String, default: '' },
  avatar: { type: String, default: '' },
  size: { type: Number, default: 40 },
  round: { type: Boolean, default: true }
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
</script>
