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
          :default-index="rankPickerIndex"
          title="选择段位"
          @confirm="onRankConfirm"
          @cancel="showRankPicker = false"
        />
      </van-popup>

      <!-- 已选标签操作菜单（修改段位/取消选择） -->
      <van-action-sheet
        v-model:show="showTagActions"
        :actions="tagActions"
        cancel-text="取消"
        @select="onTagActionSelect"
      />

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
// 已选标签操作菜单
const showTagActions = ref(false)
const tagActions = [
  { name: '修改段位', value: 'edit' },
  { name: '取消选择', value: 'remove', color: '#ee0a24' }
]

// 所有标签数据
const allTags = ref([])
// 已选标签：{ tagId: { id, name, category, level, hasLevel } }
const selectedTags = ref({})
// 当前正在操作（选段位/改段位）的标签
const currentTag = ref(null)
// 段位选择器默认定位的列索引（改段位时定位到当前段位）
const rankPickerIndex = ref(0)

// 段位选项
const rankColumns = [
  { text: '青铜', value: 1 },
  { text: '白银', value: 2 },
  { text: '黄金', value: 3 },
  { text: '钻石', value: 4 },
  { text: '王者', value: 5 }
]

// 该标签是否有段位体系（后端按分类返回：球类/电竞有，桌游/跑步等没有）
const hasLevelSystem = (tag) => {
  return tag?.hasLevel !== false
}

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
  const selected = selectedTags.value[tag.id]
  if (selected) {
    if (!hasLevelSystem(tag)) {
      // 无段位概念的标签：点击即取消选中
      removeTag(tag.id)
      return
    }
    // 已选中的有段位标签：弹出操作菜单（修改段位/取消选择）
    currentTag.value = tag
    showTagActions.value = true
    return
  }
  if (!hasLevelSystem(tag)) {
    // 无段位概念的标签：直接选中，不弹段位选择（晨跑/剧本杀等不需要段位）
    selectTag(tag, 0)
    return
  }
  // 未选中的有段位标签：弹出段位选择
  currentTag.value = tag
  rankPickerIndex.value = 2 // 默认定位黄金
  showRankPicker.value = true
}

// 选中标签（含段位）
const selectTag = (tag, level) => {
  selectedTags.value = {
    ...selectedTags.value,
    [tag.id]: {
      id: tag.id,
      name: tag.name,
      category: tag.category,
      level,
      hasLevel: hasLevelSystem(tag)
    }
  }
}

// 取消选中标签
const removeTag = (tagId) => {
  const copy = { ...selectedTags.value }
  delete copy[tagId]
  selectedTags.value = copy
}

// 已选标签操作菜单选择
const onTagActionSelect = (action) => {
  showTagActions.value = false
  if (!action || !currentTag.value) return
  if (action.value === 'remove') {
    removeTag(currentTag.value.id)
  } else if (action.value === 'edit') {
    // 修改段位：picker 定位到当前段位
    const curLevel = selectedTags.value[currentTag.value.id]?.level || 3
    rankPickerIndex.value = Math.min(4, Math.max(0, curLevel - 1))
    showRankPicker.value = true
  }
  currentTag.value = null
}

// 段位确认
const onRankConfirm = ({ selectedOptions }) => {
  if (currentTag.value) {
    selectTag(currentTag.value, selectedOptions[0]?.value || 3)
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
      // 无段位概念的项目（hasLevel=false）selfLevel 为 0，不显示段位
      const hasLevel = t.hasLevel !== false
      map[t.id] = {
        id: t.id,
        name: t.name,
        category: t.category,
        level: hasLevel ? (t.selfLevel || t.level || 3) : 0,
        hasLevel
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
    const levels = selected.map(t => t.level || 0)
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
