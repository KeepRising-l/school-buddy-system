<template>
  <div class="page post-create-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="发布组局" left-arrow @click-left="router.back()" />

    <div class="content">
      <!-- 标题 -->
      <van-field
        v-model="form.title"
        label="标题"
        placeholder="请输入标题（5-30字）"
        maxlength="30"
        show-word-limit
        class="form-field"
      />

      <!-- 兴趣分类 -->
      <van-field
        v-model="categoryText"
        is-link
        readonly
        label="兴趣分类"
        placeholder="请选择分类"
        @click="showCategoryPicker = true"
        class="form-field"
      />
      <van-popup v-model:show="showCategoryPicker" round position="bottom">
        <van-picker
          :columns="categoryColumns"
          @confirm="onCategoryConfirm"
          @cancel="showCategoryPicker = false"
        />
      </van-popup>

      <!-- 具体项目（联动） -->
      <van-field
        v-model="tagNamesText"
        is-link
        readonly
        label="具体项目"
        placeholder="请选择项目"
        @click="handleTagClick"
        class="form-field"
      />
      <van-popup v-model:show="showTagPicker" round position="bottom">
        <van-picker
          :columns="tagColumns"
          @confirm="onTagConfirm"
          @cancel="showTagPicker = false"
        />
      </van-popup>

      <!-- 开始时间 -->
      <van-field
        v-model="startTimeText"
        is-link
        readonly
        label="开始时间"
        placeholder="请选择开始时间"
        @click="showStartPicker = true"
        class="form-field"
      />
      <van-popup v-model:show="showStartPicker" round position="bottom">
        <van-date-picker
          v-model="startDatePicker"
          title="选择开始日期"
          :min-date="minDate"
          @confirm="onStartDateConfirm"
          @cancel="showStartPicker = false"
        />
      </van-popup>
      <van-popup v-model:show="showStartTimePicker" round position="bottom">
        <van-time-picker
          v-model="startTimePicker"
          title="选择开始时间"
          @confirm="onStartTimeConfirm"
          @cancel="showStartTimePicker = false"
        />
      </van-popup>

      <!-- 结束时间 -->
      <van-field
        v-model="endTimeText"
        is-link
        readonly
        label="结束时间"
        placeholder="请选择结束时间"
        @click="showEndPicker = true"
        class="form-field"
      />
      <van-popup v-model:show="showEndPicker" round position="bottom">
        <van-date-picker
          v-model="endDatePicker"
          title="选择结束日期"
          :min-date="minDate"
          @confirm="onEndDateConfirm"
          @cancel="showEndPicker = false"
        />
      </van-popup>
      <van-popup v-model:show="showEndTimePicker" round position="bottom">
        <van-time-picker
          v-model="endTimePicker"
          title="选择结束时间"
          @confirm="onEndTimeConfirm"
          @cancel="showEndTimePicker = false"
        />
      </van-popup>

      <!-- 地点 -->
      <van-field
        v-model="form.location"
        label="地点"
        placeholder="请输入活动地点"
        maxlength="50"
        class="form-field"
      />

      <!-- 人数限制 -->
      <van-field label="人数限制" class="form-field">
        <template #input>
          <van-stepper v-model="form.maxPeople" min="2" max="20" />
        </template>
      </van-field>

      <!-- 段位要求 -->
      <van-field
        v-model="rankText"
        is-link
        readonly
        label="段位要求"
        placeholder="请选择段位要求"
        @click="showRankPicker = true"
        class="form-field"
      />
      <van-popup v-model:show="showRankPicker" round position="bottom">
        <van-picker
          :columns="rankColumns"
          @confirm="onRankConfirm"
          @cancel="showRankPicker = false"
        />
      </van-popup>

      <!-- 性别要求 -->
      <van-field label="性别要求" class="form-field">
        <template #input>
          <van-radio-group v-model="form.requireGender" direction="horizontal">
            <van-radio :name="0">不限</van-radio>
            <van-radio :name="1">仅男</van-radio>
            <van-radio :name="2">仅女</van-radio>
          </van-radio-group>
        </template>
      </van-field>

      <!-- 详细描述 -->
      <van-field
        v-model="form.description"
        label="详细描述"
        type="textarea"
        placeholder="请描述活动详情（最多500字）"
        maxlength="500"
        show-word-limit
        rows="4"
        autosize
        class="form-field"
      />

      <!-- 发布按钮 -->
      <van-button
        type="primary"
        block
        round
        class="submit-btn"
        :loading="loading"
        loading-text="发布中..."
        @click="handleSubmit"
      >
        发布
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { createPost } from '@/api/post'
import { getAllTags } from '@/api/tag'

const router = useRouter()
const loading = ref(false)

// 表单数据
const form = reactive({
  title: '',
  category: '',
  tagIds: '',
  startTime: '',
  endTime: '',
  location: '',
  maxPeople: 4,
  requireLevel: null,
  requireGender: 0,
  description: ''
})

// 分类和项目数据
const tagData = ref([])
const showCategoryPicker = ref(false)
const showTagPicker = ref(false)
const showRankPicker = ref(false)
const showStartPicker = ref(false)
const showEndPicker = ref(false)
const showStartTimePicker = ref(false)
const showEndTimePicker = ref(false)

// 当前选中的标签名称
const selectedTagName = ref('')

// 临时存储日期选择结果
const tempStartDate = ref('')
const tempEndDate = ref('')

// 分类显示文本
const categoryText = computed(() => form.category || '')
const tagNamesText = computed(() => selectedTagName.value || '')
const rankText = computed(() => {
  const map = { 1: '青铜以上', 2: '白银以上', 3: '黄金以上', 4: '钻石以上', 5: '王者' }
  return map[form.requireLevel] || '不限'
})
const startTimeText = computed(() => form.startTime ? formatDisplayTime(form.startTime) : '')
const endTimeText = computed(() => form.endTime ? formatDisplayTime(form.endTime) : '')

// 分类选项
const categoryColumns = computed(() => {
  const cats = [...new Set(tagData.value.map(t => t.category))]
  return cats.map(c => ({ text: c, value: c }))
})

// 当前分类下的标签选项（联动）
const tagColumns = computed(() => {
  if (!form.category) return []
  return tagData.value
    .filter(t => t.category === form.category)
    .map(t => ({ text: t.name, value: t.id }))
})

// 段位选项
const rankColumns = [
  { text: '不限', value: null },
  { text: '青铜以上', value: 1 },
  { text: '白银以上', value: 2 },
  { text: '黄金以上', value: 3 },
  { text: '钻石以上', value: 4 },
  { text: '王者', value: 5 }
]

// 时间选择器
const minDate = new Date()
const startDatePicker = ref(['2026', '01', '01'])
const endDatePicker = ref(['2026', '01', '01'])
const startTimePicker = ref(['09', '00'])
const endTimePicker = ref(['18', '00'])

// 分类确认
const onCategoryConfirm = ({ selectedOptions }) => {
  form.category = selectedOptions[0]?.value || ''
  // 切换分类时重置项目选择
  form.tagIds = ''
  selectedTagName.value = ''
  showCategoryPicker.value = false
}

// 点击项目选择器（需先选分类）
const handleTagClick = () => {
  if (!form.category) {
    showToast('请先选择兴趣分类')
    return
  }
  showTagPicker.value = true
}

// 项目确认
const onTagConfirm = ({ selectedOptions }) => {
  const selected = selectedOptions[0]
  if (selected) {
    form.tagIds = String(selected.value)
    selectedTagName.value = selected.text
  }
  showTagPicker.value = false
}

// 段位确认
const onRankConfirm = ({ selectedOptions }) => {
  form.requireLevel = selectedOptions[0]?.value ?? null
  showRankPicker.value = false
}

// 开始日期确认 → 继续选时间
const onStartDateConfirm = ({ selectedValues }) => {
  tempStartDate.value = `${selectedValues[0]}-${selectedValues[1]}-${selectedValues[2]}`
  showStartPicker.value = false
  showStartTimePicker.value = true
}

// 开始时间确认
const onStartTimeConfirm = ({ selectedValues }) => {
  form.startTime = `${tempStartDate.value} ${selectedValues[0]}:${selectedValues[1]}:00`
  showStartTimePicker.value = false
}

// 结束日期确认 → 继续选时间
const onEndDateConfirm = ({ selectedValues }) => {
  tempEndDate.value = `${selectedValues[0]}-${selectedValues[1]}-${selectedValues[2]}`
  showEndPicker.value = false
  showEndTimePicker.value = true
}

// 结束时间确认
const onEndTimeConfirm = ({ selectedValues }) => {
  form.endTime = `${tempEndDate.value} ${selectedValues[0]}:${selectedValues[1]}:00`
  showEndTimePicker.value = false
}

// 格式化显示时间
const formatDisplayTime = (time) => {
  if (!time) return ''
  return time.replace(/-/g, '/')
}

// 加载标签数据
const fetchTags = async () => {
  try {
    const res = await getAllTags()
    tagData.value = res.data || []
  } catch (err) {
    // 静默处理
  }
}

// 表单校验
const validate = () => {
  if (!form.title || form.title.length < 5) {
    showToast('标题至少5个字')
    return false
  }
  if (!form.category) {
    showToast('请选择兴趣分类')
    return false
  }
  if (!form.startTime) {
    showToast('请选择开始时间')
    return false
  }
  if (!form.endTime) {
    showToast('请选择结束时间')
    return false
  }
  // 时间校验（PRD：开始时间须晚于当前1小时，结束须晚于开始）
  const start = new Date(form.startTime.replace(/-/g, '/'))
  const end = new Date(form.endTime.replace(/-/g, '/'))
  if (start <= Date.now() + 60 * 60 * 1000) {
    showToast('开始时间需晚于当前时间1小时')
    return false
  }
  if (end <= start) {
    showToast('结束时间必须晚于开始时间')
    return false
  }
  if (!form.location) {
    showToast('请输入活动地点')
    return false
  }
  return true
}

// 提交发布
const handleSubmit = async () => {
  if (!validate()) return
  if (loading.value) return

  loading.value = true
  try {
    await createPost({
      title: form.title,
      category: form.category,
      tagIds: form.tagIds,
      startTime: form.startTime,
      endTime: form.endTime,
      location: form.location,
      maxPeople: form.maxPeople,
      requireLevel: form.requireLevel,
      requireGender: form.requireGender,
      description: form.description
    })
    showToast('发布成功')
    router.back()
  } catch (err) {
    // 接口层已处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchTags()
})
</script>

<style scoped lang="scss">
.post-create-page {
  background: var(--bg-page);
}

.content {
  padding: 12px 16px 32px;
}

.form-field {
  margin-bottom: 1px;
  border-radius: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);

  :deep(.van-field__label) {
    color: var(--text-primary);
    font-weight: 500;
  }
}

.submit-btn {
  margin-top: 32px;
  background: var(--primary);
  border: none;
  height: 46px;
  font-size: 16px;
  font-weight: 500;
  box-shadow: 0 16px 32px -12px rgba(16, 163, 127, 0.35);
}
</style>
