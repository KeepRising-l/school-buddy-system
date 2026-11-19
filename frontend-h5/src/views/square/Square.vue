<template>
  <TabbarLayout>
    <div class="square-page page-with-tabbar">
      <!-- 顶部极简刊头 Masthead -->
      <header class="masthead">
        <div class="masthead-left">
          <h1 class="masthead-title">校园搭子日程</h1>
        </div>
        <div class="masthead-right">
          <button
            type="button"
            class="search-toggle-btn"
            :class="{ active: showSearch || keyword }"
            @click="showSearch = !showSearch"
          >
            <van-icon name="search" size="15" />
            <span>{{ keyword ? keyword : '检索' }}</span>
          </button>
        </div>
      </header>

      <!-- 折叠式纸面检索框 -->
      <div v-if="showSearch || keyword" class="search-drawer">
        <van-search
          v-model="keyword"
          placeholder="搜索活动主题、球场或地点..."
          background="transparent"
          show-action
          @search="onSearch"
          @clear="onSearch"
          @cancel="clearAndCloseSearch"
        />
      </div>

      <!-- 克制单行公告条 -->
      <div class="notice-strip" v-if="activeNotice && !noticeClosed" @click="showNoticeDetail(activeNotice)">
        <span class="notice-kicker">公告</span>
        <span class="notice-text text-ellipsis">{{ activeNotice.title }} — {{ activeNotice.content }}</span>
        <button type="button" class="notice-close" @click.stop="noticeClosed = true" aria-label="关闭公告">×</button>
      </div>

      <!-- 为你挑选的同频组局 · 暖砂底块编辑推荐带 -->
      <section class="editorial-picks" v-if="featuredList.length > 0 && !keyword && !currentCategory">
        <div class="picks-caption">
          <span>为你挑选的同频组局</span>
          <span class="picks-sub">AI 兴趣推荐</span>
        </div>
        <div class="picks-scroll">
          <div
            v-for="item in featuredList"
            :key="'feat-' + item.id"
            class="pick-item"
            @click="goDetail(item.id)"
          >
            <div class="pick-score">
              <template v-if="item.matchScore">{{ item.matchScore }}% 匹配</template>
              <template v-else>{{ item.category }}</template>
              <span class="dot-sep">·</span>
              <span>{{ formatSpotShort(item) }}</span>
            </div>
            <div class="pick-title text-ellipsis">{{ item.title }}</div>
            <div class="pick-note text-ellipsis">
              {{ item.publisherNickname || '同学' }} · {{ item.location }}
            </div>
          </div>
        </div>
      </section>

      <!-- 分类文字下划线导航 -->
      <nav class="text-tabs-wrap">
        <div class="text-tabs">
          <span
            v-for="cat in categories"
            :key="cat.value"
            :class="['t-tab', { active: currentCategory === cat.value }]"
            @click="switchCategory(cat.value)"
          >
            {{ cat.value === '' ? '全部日程' : cat.label }}
          </span>
        </div>
      </nav>

      <!-- 次级时间与排序微栏 -->
      <div class="sub-filter-bar">
        <div class="time-links">
          <span
            v-for="item in timeOptions"
            :key="item.value"
            :class="['time-link', { active: currentTime === item.value }]"
            @click="switchTime(item.value)"
          >
            {{ item.label }}
          </span>
        </div>
        <div class="sort-links">
          <span
            v-for="item in sortOptions"
            :key="item.value"
            :class="['sort-link', { active: currentSort === item.value }]"
            @click="switchSort(item.value)"
          >
            {{ item.label }}
          </span>
        </div>
      </div>

      <!-- 编辑级流式组局列表 -->
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="— 已浏览全部日程 —"
          @load="loadMore"
        >
          <main class="stream-container">
            <PostCard
              v-for="post in postList"
              :key="post.id"
              :post="post"
              class="stagger-item"
              @click="goDetail(post.id)"
            />
            <EmptyState
              v-if="!loading && !refreshing && postList.length === 0"
              icon="search"
              text="暂无符合条件的组局日程"
              action-text="写新组局"
              @action="router.push('/post/create')"
            />
          </main>
        </van-list>
      </van-pull-refresh>
    </div>
  </TabbarLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog } from 'vant'
import { getSquareList, getFeaturedPosts } from '@/api/post'
import { getAllCategories } from '@/api/tag'
import { getLatestPopup, getActiveAnnouncements } from '@/api/announcement'
import TabbarLayout from '@/components/TabbarLayout.vue'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

// 公告、检索展开态与横向精选推荐位
const activeNotice = ref(null)
const noticeClosed = ref(false)
const showSearch = ref(false)
const featuredList = ref([])

const formatSpotShort = (item) => {
  const left = Math.max(0, Number(item.maxPeople || 0) - Number(item.currentPeople || 0))
  if (item.status === 'FULL' || left === 0) return '可排候补'
  return `余 ${left} 席`
}

const fetchAnnouncements = async () => {
  try {
    const [popupRes, listRes] = await Promise.allSettled([
      getLatestPopup(),
      getActiveAnnouncements()
    ])
    if (listRes.status === 'fulfilled' && Array.isArray(listRes.value?.data) && listRes.value.data.length > 0) {
      activeNotice.value = listRes.value.data[0]
    }
    if (popupRes.status === 'fulfilled' && popupRes.value?.data?.id) {
      const popup = popupRes.value.data
      const readKey = `buddy_popup_read_${popup.id}`
      if (!localStorage.getItem(readKey)) {
        localStorage.setItem(readKey, '1')
        showDialog({
          title: popup.title || '系统公告',
          message: popup.content || '',
          confirmButtonText: '我知道了'
        }).catch(() => {})
      }
    }
  } catch (e) {
    // 静默忽略
  }
}

const showNoticeDetail = (notice) => {
  if (!notice) return
  showDialog({
    title: notice.title || '校园公告',
    message: notice.content || '',
    confirmButtonText: '知道了'
  }).catch(() => {})
}

const fetchFeatured = async () => {
  try {
    const res = await getFeaturedPosts(5)
    featuredList.value = res.data || []
  } catch (e) {
    featuredList.value = []
  }
}

const defaultCategories = [
  { label: '全部', value: '' },
  { label: '球类', value: '球类' },
  { label: '桌游', value: '桌游' },
  { label: '自习', value: '学习' },
  { label: '跑步', value: '跑步' },
  { label: '电竞', value: '电竞' },
  { label: '健身', value: '健身' }
]
const categories = ref([{ label: '全部', value: '' }])

const fetchCategories = async () => {
  try {
    const res = await getAllCategories()
    const list = res.data || []
    if (list.length) {
      categories.value = [{ label: '全部', value: '' }, ...list.map(c => ({ label: c, value: c }))]
    }
    if (currentCategory.value && !categories.value.some(c => c.value === currentCategory.value)) {
      currentCategory.value = ''
    }
  } catch (e) {
    categories.value = defaultCategories
  }
}

const sortOptions = [
  { label: '推荐', value: 'recommend' },
  { label: '最新', value: 'newest' },
  { label: '最热', value: 'hottest' }
]

const timeOptions = [
  { label: '全部时段', value: '' },
  { label: '今日', value: 'today' },
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
    // 失败不置 finished，允许下拉重试
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

const clearAndCloseSearch = () => {
  const hadKeyword = Boolean(keyword.value)
  keyword.value = ''
  showSearch.value = false
  if (hadKeyword) {
    onSearch()
  }
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

onMounted(() => {
  fetchCategories()
  fetchAnnouncements()
  fetchFeatured()
})
</script>

<style scoped lang="scss">
.square-page {
  background: var(--bg-page);
}

/* 顶部极简刊头 */
.masthead {
  padding: 16px 20px 14px;
  background: var(--bg-page);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;

  .masthead-title {
    font-size: 19px;
    font-weight: 600;
    letter-spacing: -0.02em;
    color: var(--text-primary);
  }

  .search-toggle-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    background: transparent;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-md);
    padding: 5px 10px;
    font-size: 12px;
    color: var(--text-secondary);
    cursor: pointer;
    max-width: 140px;
    transition: border-color 0.18s var(--ease), color 0.18s var(--ease);

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &.active {
      border-color: var(--text-primary);
      color: var(--text-primary);
    }
  }
}

.search-drawer {
  padding: 6px 12px 4px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-page);

  :deep(.van-search) {
    padding: 4px 8px;
  }

  :deep(.van-search__content) {
    background: var(--bg-grey);
    border-radius: var(--radius-md);
  }
}

/* 克制单行公告条 */
.notice-strip {
  padding: 9px 20px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-card);
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;

  .notice-kicker {
    flex-shrink: 0;
    font-family: var(--font-mono);
    font-size: 11px;
    font-weight: 600;
    color: var(--primary);
  }

  .notice-text {
    flex: 1;
    font-size: 12px;
    color: var(--text-secondary);
  }

  .notice-close {
    flex-shrink: 0;
    border: none;
    background: transparent;
    color: var(--text-hint);
    font-size: 15px;
    line-height: 1;
    padding: 0 2px;
    cursor: pointer;
  }
}

/* 暖砂底块编辑推荐带 */
.editorial-picks {
  padding: 14px 20px;
  background: var(--bg-grey);
  border-bottom: 1px solid var(--border-color);

  .picks-caption {
    font-size: 12px;
    font-weight: 500;
    color: var(--text-secondary);
    margin-bottom: 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .picks-sub {
      color: var(--text-hint);
      font-size: 11px;
    }
  }

  .picks-scroll {
    display: flex;
    overflow-x: auto;
    gap: 16px;
    -webkit-overflow-scrolling: touch;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .pick-item {
    flex: 0 0 176px;
    border-left: 2px solid var(--primary);
    padding-left: 10px;
    cursor: pointer;
    transition: opacity 0.18s var(--ease);

    &:active {
      opacity: 0.72;
    }

    .pick-score {
      font-family: var(--font-mono);
      font-size: 11px;
      font-weight: 600;
      color: var(--primary);
      margin-bottom: 3px;

      .dot-sep {
        margin: 0 4px;
        opacity: 0.6;
      }
    }

    .pick-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary);
      line-height: 1.35;
      margin-bottom: 2px;
    }

    .pick-note {
      font-size: 12px;
      color: var(--text-secondary);
    }
  }
}

/* 分类文字下划线导航 */
.text-tabs-wrap {
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-page);
}

.text-tabs {
  display: flex;
  overflow-x: auto;
  gap: 20px;
  padding: 12px 20px 0;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar {
    display: none;
  }

  .t-tab {
    flex-shrink: 0;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-hint);
    padding-bottom: 10px;
    border-bottom: 2px solid transparent;
    cursor: pointer;
    white-space: nowrap;
    transition: color 0.18s var(--ease), border-color 0.2s var(--ease);

    &.active {
      color: var(--text-primary);
      font-weight: 600;
      border-bottom-color: var(--text-primary);
    }
  }
}

/* 次级时间与排序微栏 */
.sub-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px 6px;
  border-bottom: 1px solid var(--border-light);

  .time-links,
  .sort-links {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .time-link,
  .sort-link {
    font-size: 12px;
    color: var(--text-hint);
    cursor: pointer;
    transition: color 0.18s var(--ease);

    &.active {
      color: var(--primary);
      font-weight: 600;
    }
  }

  .sort-link.active {
    color: var(--text-primary);
  }
}

/* 主流式列表容器 */
.stream-container {
  padding: 0 20px 24px;
  background: var(--bg-page);
}
</style>
