<template>
  <span class="tag-badge" :style="{ background: bgColor, color: textColor }">
    {{ name }}
  </span>
</template>

<script setup>
const props = defineProps({
  name: { type: String, required: true },
  category: { type: String, default: '' }
})

const colorMap = {
  '球类': { bg: '#eaf5ee', text: '#2e7d4f' },
  '电竞': { bg: '#eaf0fb', text: '#3b5b8c' },
  '桌游': { bg: '#fdf3e6', text: '#a05a1f' },
  '跑步': { bg: '#fbedf0', text: '#b03a48' },
  '健身': { bg: '#f3edf8', text: '#7a4d96' },
  '学习': { bg: '#e8f4f6', text: '#2f6a73' }
}

const bgColor = colorMap[props.category]?.bg || '#f3f4f6'
const textColor = colorMap[props.category]?.text || '#6b7280'
</script>

<style scoped lang="scss">
.tag-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.6;
  white-space: nowrap;
}
</style>
