<template>
  <div class="page tag-setting-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="兴趣标签" left-arrow @click-left="router.back()" />

    <div class="content">
      <!-- 按分类展示标签 -->
      <div v-for="cat in groupedTags" :key="cat.category" class="tag-category">
        <h3 class="category-title">{{ cat.category }}</h3>
        <div class="tag-list">
          <van-tag
            v-for="tag in cat.tags"
            :key="tag.id"
            :type="isSelected(tag.id) ? 'primary' : 'default'"
            size="medium"
            round
            class="tag-item"
            @click="onTagClick(tag)"
          >
            {{ tag.name }}
            <span v-if="getSelectedRank(tag.id)" class="tag-rank">·{{ getSelectedRank(tag.id) }}</span>
          </van-tag>
        </div>
      </div>

      <!-- 段位选择弹窗 -->
      <van-popup v-model:show="showRankPicker" round position="bottom">
        <van-picker
          :columns="rankColumns"
          title="选择段位"
          @confirm="onRankConfirm"
          @cancel="showRankPicker = false"
        />
      </van-popup>

      <!-- 保存按钮 -->
      <van-button
        type="primary"
        block
        round
        class="save-btn"
        :loading="saving"
        loading-text="保存中..."
        @click="handleSave"
      >
        保存
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getAllTags } from '@/api/tag'
import { getMyTags, setMyTags } from '@/api/user'

const router = useRouter()
const saving = ref(false)
const showRankPicker = ref(false)

// 所有标签数据
const allTags = ref([])
// 已选标签：{ tagId: { id, name, category, rank } }
const selectedTags = ref({})
// 当前正在选择段位的标签
const currentTag = ref(null)

// 段位选项
const rankColumns = [
  { text: '青铜', value: 1 },
  { text: '白银', value: 2 },
  { text: '黄金', value: 3 },
  { text: '钻石', value: 4 },
  { text: '王者', value: 5 }
]

// 按分类分组
const groupedTags = computed(() => {
  const map = {}
  allTags.value.forEach(tag => {
    const cat = tag.category || '其他'
    if (!map[cat]) {
      map[cat] = { category: cat, tags: [] }
    }
    map[cat].tags.push(tag)
  })
  return Object.values(map)
})

// 判断标签是否选中
const isSelected = (tagId) => {
  return !!selectedTags.value[tagId]
}

// 获取已选标签的段位
const getSelectedRank = (tagId) => {
  const level = selectedTags.value[tagId]?.level
  if (!level) return ''
  const map = { 1: '青铜', 2: '白银', 3: '黄金', 4: '钻石', 5: '王者' }
  return map[level] || ''
}

// 点击标签
const onTagClick = (tag) => {
  if (selectedTags.value[tag.id]) {
    // 已选中，取消选中
    const copy = { ...selectedTags.value }
    delete copy[tag.id]
    selectedTags.value = copy
  } else {
    // 未选中，弹出段位选择
    currentTag.value = tag
    showRankPicker.value = true
  }
}

// 段位确认
const onRankConfirm = ({ selectedOptions }) => {
  if (currentTag.value) {
    selectedTags.value = {
      ...selectedTags.value,
      [currentTag.value.id]: {
        id: currentTag.value.id,
        name: currentTag.value.name,
        category: currentTag.value.category,
        level: selectedOptions[0]?.value || 3
      }
    }
  }
  showRankPicker.value = false
  currentTag.value = null
}

// 加载所有标签
const fetchAllTags = async () => {
  try {
    const res = await getAllTags()
    allTags.value = res.data || []
  } catch (err) {
    // 静默处理
  }
}

// 加载我的标签
const fetchMyTags = async () => {
  try {
    const res = await getMyTags()
    const list = res.data || []
    const map = {}
    list.forEach(t => {
      map[t.id] = {
        id: t.id,
        name: t.name,
        category: t.category,
        level: t.selfLevel || t.level || 3
      }
    })
    selectedTags.value = map
  } catch (err) {
    // 静默处理
  }
}

// 保存标签
const handleSave = async () => {
  saving.value = true
  try {
    const selected = Object.values(selectedTags.value)
    const tagIds = selected.map(t => t.id)
    const levels = selected.map(t => t.level)
    await setMyTags({ tagIds, levels })
    showToast('保存成功')
    router.back()
  } catch (err) {
    // 接口层已处理
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchAllTags()
  fetchMyTags()
})
</script>

<style scoped lang="scss">
.tag-setting-page {
  background: var(--bg-page);
}

.tag-category {
  margin-bottom: 16px;

  .category-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 10px;
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .tag-item {
      cursor: pointer;

      .tag-rank {
        font-size: 11px;
        opacity: 0.8;
      }
    }
  }
}

.save-btn {
  margin-top: 32px;
  background: var(--primary);
  border: none;
  height: 46px;
  font-size: 16px;
  font-weight: 500;
  box-shadow: 0 16px 32px -12px rgba(16, 163, 127, 0.35);
}
</style>
