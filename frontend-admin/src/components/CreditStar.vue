<template>
  <el-rate :model-value="level" disabled :colors="['#F7BA2A','#F7BA2A','#F7BA2A']" />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ score: { type: Number, default: 100 } })

// 根据信誉分计算星级
const level = computed(() => {
  if (props.score >= 90) return 5
  if (props.score >= 75) return 4
  if (props.score >= 60) return 3
  if (props.score >= 40) return 2
  return 1
})
</script>
