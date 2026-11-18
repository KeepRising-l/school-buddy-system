<template>
  <TabbarLayout>
    <div class="square-page page-with-tabbar">
      <!-- 顶部区域 -->
      <div class="square-header">
        <span class="eyebrow">探索校园</span>
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

      <!-- 系统公告滚动横幅（闭合 P1-2 公告未接入缺口） -->
      <div class="notice-wrap" v-if="activeNotice">
        <van-notice-bar
          left-icon="volume-o"
          :text="`${activeNotice.title}：${activeNotice.content}`"
          color="#0d8a6a"
          background="#ecfdf5"
          mode="closeable"
          class="campus-notice-bar"
          @click="showNoticeDetail(activeNotice)"
        />
      </div>

      <!-- 你可能感兴趣 · 横向精选推荐位（闭合 PRD 5.3.2 缺口） -->
      <div class="featured-section" v-if="featuredList.length > 0 && !keyword && !currentCategory">
        <div class="featured-header">
          <span class="featured-title">✨ 你可能感兴趣</span>
          <span class="featured-sub">基于兴趣与信誉智能撮合</span>
        </div>
        <div class="featured-scroll">
          <div
            v-for="item in featuredList"
            :key="'feat-' + item.id"
            class="featured-card"
            @click="goDetail(item.id)"
          >
            <div class="feat-top">
              <span class="feat-cat">{{ item.category }}<template v-if="item.tagName"> · {{ item.tagName }}</template></span>
              <span class="feat-match" v-if="item.matchScore">{{ item.matchScore }}% 契合</span>
            </div>
            <div class="feat-title text-ellipsis">{{ item.title }}</div>
            <div class="feat-meta">
              <span><van-icon name="location-o" /> {{ item.location }}</span>
              <span>{{ item.currentPeople }}/{{ item.maxPeople }}人</span>
            </div>
            <div class="feat-reasons" v-if="item.recommendReasons && item.recommendReasons.length">
              <span v-for="r in item.recommendReasons" :key="r" class="feat-reason-tag">{{ r }}</span>
            </div>
          </div>
        </div>
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
              v-for="(post, index) in postList"
              :key="post.id"
              :post="post"
              class="stagger-item"
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

// 公告与横向精选推荐位
const activeNotice = ref(null)
const featuredList = ref([])

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

// 分类筛选（V1.2 动态加载：跟随管理端分类配置，新增/禁用自动同步；失败回退内置列表）
const defaultCategories = [
  { label: '全部', value: '' },
  { label: '球类', value: '球类' },
  { label: '电竞', value: '电竞' },
  { label: '桌游', value: '桌游' },
  { label: '跑步', value: '跑步' },
  { label: '健身', value: '健身' },
  { label: '学习', value: '学习' }
]
const categories = ref([{ label: '全部', value: '' }])

const fetchCategories = async () => {
  try {
    const res = await getAllCategories()
    const list = res.data || []
    if (list.length) {
      categories.value = [{ label: '全部', value: '' }, ...list.map(c => ({ label: c, value: c }))]
    }
    // 当前选中的分类被禁用/移除时重置为"全部"
    if (currentCategory.value && !categories.value.some(c => c.value === currentCategory.value)) {
      currentCategory.value = ''
    }
  } catch (e) {
    // 加载失败回退内置分类，保证筛选可用
    categories.value = defaultCategories
  }
}

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
    // 修复：失败不置 finished，允许下拉重试（原来伪装成"没有更多了"）
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

.square-header {
  background: var(--bg-card);
  padding: 24px 20px 40px;
  border-radius: 0 0 32px 32px;
  box-shadow: 0 20px 40px -28px rgba(0, 0, 0, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.7);

  .eyebrow {
    margin-bottom: 10px;
  }

  .page-title {
    font-size: 28px;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: var(--text-primary);
  }

  .page-subtitle {
    margin-top: 6px;
    font-size: 13px;
    color: var(--text-hint);
  }
}

.search-wrap {
  margin: -28px 16px 0;
  position: relative;
  z-index: 1;

  :deep(.van-search) {
    padding: 0;
  }

  :deep(.van-search__content) {
    background: var(--bg-card);
    box-shadow: 0 16px 32px -20px rgba(0, 0, 0, 0.14), var(--shadow-inset-highlight);
    border-radius: 999px;
    border: 1px solid rgba(255, 255, 255, 0.8);
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

.notice-wrap {
  margin: 12px 16px 0;

  .campus-notice-bar {
    border-radius: 14px;
    font-size: 12px;
    box-shadow: 0 4px 12px rgba(16, 163, 127, 0.08);
    cursor: pointer;
  }
}

.featured-section {
  margin: 14px 0 2px;

  .featured-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 18px 8px;

    .featured-title {
      font-size: 14px;
      font-weight: 700;
      color: var(--text-primary);
    }

    .featured-sub {
      font-size: 11px;
      color: var(--text-hint);
    }
  }

  .featured-scroll {
    display: flex;
    overflow-x: auto;
    gap: 10px;
    padding: 2px 16px 6px;
    -webkit-overflow-scrolling: touch;

    &::-webkit-scrollbar { display: none; }

    .featured-card {
      flex-shrink: 0;
      width: 210px;
      padding: 12px 14px;
      border-radius: 16px;
      background: linear-gradient(145deg, #ffffff, #f3faf7);
      border: 1px solid rgba(16, 163, 127, 0.16);
      box-shadow: 0 8px 20px -12px rgba(16, 163, 127, 0.22);
      cursor: pointer;

      .feat-top {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 6px;

        .feat-cat {
          font-size: 11px;
          color: var(--primary-dark);
          font-weight: 600;
        }

        .feat-match {
          font-size: 10px;
          font-weight: 600;
          padding: 1px 6px;
          border-radius: 999px;
          background: var(--primary);
          color: #fff;
        }
      }

      .feat-title {
        font-size: 14px;
        font-weight: 600;
        color: var(--text-primary);
        margin-bottom: 6px;
      }

      .feat-meta {
        display: flex;
        justify-content: space-between;
        font-size: 11px;
        color: var(--text-hint);
        margin-bottom: 6px;
      }

      .feat-reasons {
        display: flex;
        flex-wrap: wrap;
        gap: 4px;

        .feat-reason-tag {
          font-size: 10px;
          padding: 1px 6px;
          border-radius: 6px;
          background: rgba(16, 163, 127, 0.1);
          color: #0d8a6a;
        }
      }
    }
  }
}
</style>
