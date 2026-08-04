<template>
  <TabbarLayout>
    <div class="square-page page-with-tabbar">
      <!-- 顶部区域 -->
      <div class="square-header">
        <h1 class="page-title">发现组局</h1>
        <p class="page-subtitle">找到志同道合的伙伴</p>
      </div>

      <!-- 搜索栏 -->
      <div class="search-wrap">
        <van-search
          v-model="keyword"
          placeholder="搜索组局、地点..."
          shape="round"
          background="transparent"
          @search="onSearch"
        />
      </div>

      <!-- 分类筛选标签 -->
      <div class="filter-section">
        <div class="filter-scroll">
          <div
            v-for="cat in categories"
            :key="cat.value"
            :class="['filter-chip', { active: currentCategory === cat.value }]"
            @click="switchCategory(cat.value)"
          >
            {{ cat.label }}
          </div>
        </div>
      </div>

      <!-- 时间筛选 + 排序 -->
      <div class="sort-section">
        <div class="time-chips">
          <div
            v-for="item in timeOptions"
            :key="item.value"
            :class="['time-chip', { active: currentTime === item.value }]"
            @click="switchTime(item.value)"
          >
            {{ item.label }}
          </div>
        </div>
        <div class="sort-tabs">
          <span
            v-for="item in sortOptions"
            :key="item.value"
            :class="['sort-tab', { active: currentSort === item.value }]"
            @click="switchSort(item.value)"
          >
            {{ item.label }}
          </span>
        </div>
      </div>

      <!-- 帖子列表 -->
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="loadMore"
        >
          <div class="post-list content">
            <PostCard
              v-for="post in postList"
              :key="post.id"
              :post="post"
              @click="goDetail(post.id)"
            />
            <EmptyState
              v-if="!loading && !refreshing && postList.length === 0"
              icon="search"
              text="暂无组局信息"
              action-text="去发布"
              @action="router.push('/post/create')"
            />
          </div>
        </van-list>
      </van-pull-refresh>
    </div>
  </TabbarLayout>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getSquareList } from '@/api/post'
import TabbarLayout from '@/components/TabbarLayout.vue'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const categories = [
  { label: '全部', value: '' },
  { label: '球类', value: '球类' },
  { label: '电竞', value: '电竞' },
  { label: '桌游', value: '桌游' },
  { label: '跑步', value: '跑步' },
  { label: '健身', value: '健身' },
  { label: '学习', value: '学习' }
]

const sortOptions = [
  { label: '推荐', value: 'recommend' },
  { label: '最新', value: 'newest' },
  { label: '最热', value: 'hottest' }
]

const timeOptions = [
  { label: '不限', value: '' },
  { label: '今天', value: 'today' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' }
]

const keyword = ref('')
const currentCategory = ref('')
const currentTime = ref('')
const currentSort = ref('recommend')
const postList = ref([])
const page = ref(1)
const size = 10
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

// 请求序列号：快速切换分类/排序时丢弃过期请求，防止数据错乱
let requestId = 0

const loadMore = async () => {
  const currentRequestId = ++requestId
  try {
    const res = await getSquareList({
      page: page.value,
      size,
      category: currentCategory.value || undefined,
      sort: currentSort.value,
      keyword: keyword.value || undefined,
      timeRange: currentTime.value || undefined
    })
    // 不是最新请求，丢弃结果（避免旧数据覆盖新筛选）
    if (currentRequestId !== requestId) return
    const list = res.data?.records || res.data || []
    if (page.value === 1) {
      postList.value = list
    } else {
      postList.value.push(...list)
    }
    if (list.length < size) {
      finished.value = true
    } else {
      page.value++
    }
  } catch (err) {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onRefresh = () => {
  page.value = 1
  finished.value = false
  loading.value = true
  loadMore()
}

const onSearch = () => {
  page.value = 1
  finished.value = false
  postList.value = []
  loading.value = true
  loadMore()
}

const switchCategory = (cat) => {
  currentCategory.value = cat
  page.value = 1
  finished.value = false
  postList.value = []
  loading.value = true
  loadMore()
}

const switchSort = (sort) => {
  currentSort.value = sort
  page.value = 1
  finished.value = false
  postList.value = []
  loading.value = true
  loadMore()
}

const switchTime = (time) => {
  currentTime.value = time
  page.value = 1
  finished.value = false
  postList.value = []
  loading.value = true
  loadMore()
}

const goDetail = (id) => {
  router.push(`/post/${id}`)
}
</script>

<style scoped lang="scss">
.square-page {
  background: var(--bg-page);
}

.square-header {
  background: var(--bg-card);
  padding: 24px 20px 36px;
  border-radius: 0 0 28px 28px;
  box-shadow: 0 20px 40px -28px rgba(0, 0, 0, 0.1);

  .page-title {
    font-size: 26px;
    font-weight: 700;
    letter-spacing: -0.01em;
    color: var(--text-primary);
  }

  .page-subtitle {
    margin-top: 6px;
    font-size: 13px;
    color: var(--text-hint);
  }
}

.search-wrap {
  margin: -24px 16px 0;
  position: relative;
  z-index: 1;

  :deep(.van-search) {
    padding: 0;
  }

  :deep(.van-search__content) {
    background: var(--bg-card);
    box-shadow: 0 16px 32px -20px rgba(0, 0, 0, 0.14);
    border-radius: 999px;
    border: 1px solid var(--border-light);
  }
}

.filter-section {
  padding: 12px 0 0;
}

.filter-scroll {
  display: flex;
  overflow-x: auto;
  padding: 0 16px;
  gap: 8px;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar { display: none; }

  .filter-chip {
    flex-shrink: 0;
    padding: 7px 16px;
    border-radius: 999px;
    font-size: 13px;
    color: var(--text-secondary);
    background: var(--bg-card);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
    cursor: pointer;
    transition: all 0.3s var(--ease);
    white-space: nowrap;
    border: 1px solid var(--border-light);

    &.active {
      background: var(--primary-soft);
      color: var(--primary-dark);
      font-weight: 600;
      border-color: transparent;
      box-shadow: none;
    }
  }
}

.sort-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px 0;
  margin-bottom: 4px;

  .time-chips {
    display: flex;
    gap: 6px;

    .time-chip {
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 11px;
      color: var(--text-hint);
      background: var(--bg-card);
      cursor: pointer;
      transition: all 0.3s var(--ease);

      &.active {
        background: var(--primary-soft);
        color: var(--primary-dark);
        font-weight: 500;
      }
    }
  }

  .sort-tabs {
    display: flex;
    gap: 16px;

    .sort-tab {
      font-size: 13px;
      color: var(--text-hint);
      cursor: pointer;
      position: relative;
      padding-bottom: 4px;

      &.active {
        color: var(--text-primary);
        font-weight: 600;

        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 50%;
          transform: translateX(-50%);
          width: 16px;
          height: 2px;
          background: var(--primary);
          border-radius: 1px;
        }
      }
    }
  }
}

.post-list {
  padding-top: 4px;
}
</style>
