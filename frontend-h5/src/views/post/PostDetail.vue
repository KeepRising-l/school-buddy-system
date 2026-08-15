<template>
  <div class="page post-detail-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="组局详情" left-arrow @click-left="router.back()">
      <template #right>
        <van-icon name="warning-o" size="20" color="#9ca3af" class="report-icon" @click="handleReport" />
      </template>
    </van-nav-bar>

    <div class="content" v-if="post">
      <!-- 标题 + 状态标签 -->
      <div class="title-row">
        <h2 class="post-title">{{ post.title }}</h2>
        <van-tag :type="statusType" size="medium">{{ statusText }}</van-tag>
      </div>

      <!-- 活动信息卡片 -->
      <div class="info-card card">
        <div class="info-item">
          <van-icon name="clock-o" />
          <span>{{ formatTime(post.startTime) }} ~ {{ formatTime(post.endTime) }}</span>
        </div>
        <div class="info-item">
          <van-icon name="location-o" />
          <span>{{ post.location }}</span>
        </div>
        <div class="info-item">
          <van-icon name="friends-o" />
          <span>{{ post.currentPeople }}/{{ post.maxPeople }} 人</span>
        </div>
        <div class="info-item" v-if="post.requireLevel && post.requireLevel !== 0">
          <van-icon name="medal-o" />
          <span>段位要求：{{ requireLevelText }}</span>
        </div>
        <div class="info-item" v-if="post.requireGender && post.requireGender !== 0">
          <van-icon name="user-o" />
          <span>性别要求：{{ requireGenderText }}</span>
        </div>
      </div>

      <!-- 详细描述 -->
      <div class="desc-card card">
        <h3 class="section-title">详细描述</h3>
        <p class="desc-text">{{ post.description }}</p>
      </div>

      <!-- 发布者信息卡片 -->
      <div class="publisher-card card" @click="goPublisherProfile">
        <UserAvatar :name="post.publisherNickname" :avatar="post.publisherAvatar" :size="44" />
        <div class="publisher-info">
          <div class="publisher-name-row">
            <span class="name">{{ post.publisherNickname || '匿名' }}</span>
            <CreditStar :score="(post.publisherCreditLevel || 5) * 20" size="small" />
          </div>
          <div class="publisher-detail">
            <span v-if="post.publisherCollege">{{ post.publisherCollege }}</span>
            <span v-if="post.publisherSignature" class="signature">{{ post.publisherSignature }}</span>
          </div>
          <!-- 发布者兴趣标签 -->
          <div class="publisher-tags" v-if="post.publisherTags && post.publisherTags.length">
            <van-tag
              v-for="tag in post.publisherTags"
              :key="tag.tagId"
              type="primary"
              plain
              size="medium"
              round
              class="publisher-tag"
            >
              {{ tag.tagName }}
              <span v-if="tag.selfLevel" class="tag-level">{{ levelText(tag.selfLevel) }}</span>
            </van-tag>
          </div>
        </div>
      </div>

      <!-- 已报名用户列表 -->
      <div class="joined-card card" v-if="post.applyUsers && post.applyUsers.length">
        <h3 class="section-title">已报名 ({{ post.currentPeople }})</h3>
        <div class="joined-scroll">
          <div v-for="u in post.applyUsers" :key="u.userId" class="joined-user">
            <UserAvatar :name="u.nickname" :avatar="u.avatar" :size="36" />
            <span class="joined-name">{{ u.nickname }}</span>
          </div>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="comment-card card">
        <h3 class="section-title">评论 ({{ commentList.length }})</h3>
        <div class="comment-list" v-if="commentList.length">
          <div v-for="c in commentList" :key="c.id" class="comment-item">
            <UserAvatar :name="c.nickname" :avatar="c.avatar" :size="32" />
            <div class="comment-body"><span class="comment-name">{{ c.nickname }}</span>
              <p class="comment-content">{{ c.content }}</p>
              <span class="comment-time">{{ formatTime(c.createTime) }}</span>
            </div>
          </div>
        </div>
        <EmptyState v-else icon="chat-o" text="暂无评论" />
      </div>
    </div>

    <!-- 加载失败（修复：原来失败后永远"加载中..."） -->
    <div class="loading-area" v-else-if="loadError">
      <van-empty image="error" description="加载失败">
        <van-button size="small" type="primary" round @click="reload">重试</van-button>
      </van-empty>
    </div>

    <!-- 加载中占位 -->
    <div class="loading-area" v-else>
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <!-- 底部操作区域 -->
    <div class="bottom-action safe-area-bottom" v-if="post">
      <!-- 收藏按钮（V1.1） -->
      <div class="fav-btn" @click="handleToggleFavorite">
        <van-icon :name="favorited ? 'star' : 'star-o'" :color="favorited ? '#f59e0b' : '#9ca3af'" size="22" />
        <span class="fav-text" :class="{ active: favorited }">{{ favorited ? '已收藏' : '收藏' }}</span>
        <span v-if="favoriteCount > 0" class="fav-count">{{ favoriteCount }}</span>
      </div>

      <!-- 评论输入 -->
      <van-field
        v-model="commentText"
        placeholder="写评论..."
        class="comment-input"
        @keyup.enter="handleAddComment"
      >
        <template #button>
          <van-button size="small" type="primary" :loading="commentSubmitting" @click="handleAddComment">发送</van-button>
        </template>
      </van-field>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <!-- 报名资格预检提示：提前告知不达标原因，避免报名失败挫败感 -->
        <div v-if="applyBlockReason" class="apply-block-tip" @click="goFixBlock">
          <van-icon name="warning-o" class="tip-icon" />
          <span class="tip-text">{{ applyBlockReason }}</span>
          <span v-if="blockAction" class="tip-action">{{ blockAction }} ›</span>
        </div>

        <!-- 自己发布的帖子 -->
        <template v-if="isOwner">
          <van-button
            v-if="post.status === 'RECRUITING' || post.status === 'FULL' || post.status === 'ONGOING'"
            type="primary"
            round
            block
            @click="openApplySheet"
          >
            管理报名
          </van-button>
          <van-button
            v-if="post.status === 'FINISHED'"
            type="primary"
            round
            block
            @click="goReview"
          >
            去评价
          </van-button>
          <!-- 取消组局（V1.2）：后端接口已有，补上前端入口 -->
          <van-button
            v-if="post.status === 'RECRUITING' || post.status === 'FULL' || post.status === 'ONGOING'"
            plain
            type="danger"
            round
            block
            @click="handleCancelPost"
          >
            取消组局
          </van-button>
        </template>

        <!-- 他人视角（后端 applyStatus 为 PENDING/APPROVED/REJECTED/ATTENDED/NOSHOW/CANCELLED） -->
        <template v-else>
          <!-- 已结束且本人已出勤：可去评价（修复：未参与者不再显示"去评价"按钮） -->
          <van-button
            v-if="post.status === 'FINISHED' && applyStatus === 'ATTENDED'"
            type="primary"
            round
            block
            @click="goReview"
          >
            去评价
          </van-button>
          <!-- 已报名待审核 -->
          <van-button
            v-else-if="applyStatus === 'PENDING'"
            disabled
            round
            block
          >
            等待审核中
          </van-button>
          <!-- 已加入/已出勤 -->
          <template v-else-if="applyStatus === 'APPROVED' || applyStatus === 'ATTENDED'">
            <van-button disabled round block>{{ applyStatus === 'ATTENDED' ? '已出勤' : '已加入' }}</van-button>
            <van-button v-if="applyStatus === 'APPROVED'" plain type="danger" round block @click="handleCancelApply">取消报名</van-button>
          </template>
          <!-- 候补中（V1.1） -->
          <template v-else-if="applyStatus === 'WAITLIST'">
            <van-button disabled round block>候补中</van-button>
            <van-button plain type="danger" round block @click="handleCancelApply">取消候补</van-button>
          </template>
          <!-- 已满员（未报名）→ 可候补（V1.1） -->
          <template v-else-if="post.status === 'FULL'">
            <van-button
              type="warning"
              round
              block
              :loading="applyLoading"
              :disabled="!!applyBlockReason"
              @click="handleWaitlistJoin"
            >
              {{ applyBlockReason ? '暂不符合候补条件' : '候补报名' }}
            </van-button>
          </template>
          <!-- 进行中/已取消（不可报名） -->
          <van-button
            v-else-if="post.status === 'ONGOING' || post.status === 'CANCELLED'"
            disabled
            round
            block
          >
            {{ post.status === 'CANCELLED' ? '该组局已取消' : '活动进行中' }}
          </van-button>
          <!-- 被拒绝/未到场（修复：明确展示状态，而不是显示"报名参加"点了报错） -->
          <van-button
            v-else-if="applyStatus === 'REJECTED'"
            disabled
            round
            block
          >
            报名未通过
          </van-button>
          <van-button
            v-else-if="applyStatus === 'NOSHOW'"
            disabled
            round
            block
          >
            已标记未到场
          </van-button>
          <!-- 未报名 -->
          <van-button
            v-else
            type="primary"
            round
            block
            :loading="applyLoading"
            :disabled="!!applyBlockReason"
            @click="handleApplyJoin"
          >
            {{ applyBlockReason ? '暂不符合报名条件' : '报名参加' }}
          </van-button>
        </template>
      </div>
    </div>

    <!-- 报名管理弹出层 -->
    <van-popup v-model:show="showApplySheet" position="bottom" round :style="{ maxHeight: '70%' }">
      <div class="apply-sheet">
        <div class="apply-sheet-header">
          <span class="title">报名管理</span>
          <van-icon name="cross" @click="showApplySheet = false" />
        </div>
        <div class="apply-list" v-if="applyList.length">
          <div v-for="item in applyList" :key="item.id" class="apply-item">
            <div class="apply-user">
              <UserAvatar :name="item.applicantNickname" :avatar="item.applicantAvatar" :size="28" />
              <span class="apply-nickname">{{ item.applicantNickname || '用户' }}</span>
              <van-tag :type="applyStatusType(item.status)" size="medium">{{ applyStatusText(item.status) }}</van-tag>
            </div>
            <div class="apply-actions" v-if="item.status === 'PENDING'">
              <van-button size="small" type="primary" @click="handleApprove(item.id)">通过</van-button>
              <van-button size="small" type="danger" plain @click="handleReject(item.id)">拒绝</van-button>
            </div>
            <div class="apply-actions" v-else-if="item.status === 'APPROVED' && canMarkAttendance">
              <van-button size="small" type="success" @click="handleMarkAttendance(item.id, true)">已出勤</van-button>
              <van-button size="small" type="danger" plain @click="handleMarkAttendance(item.id, false)">未到场</van-button>
            </div>
          </div>
        </div>
        <EmptyState v-else icon="friends-o" text="暂无报名" />
      </div>
    </van-popup>

    <!-- 举报类型选择（V1.1） -->
    <van-action-sheet
      v-model:show="showReportSheet"
      title="举报该组局"
      :actions="reportActions"
      cancel-text="取消"
      @select="onReportSelect"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog, showDialog } from 'vant'
import { getPostDetail, addComment, getComments, cancelPost } from '@/api/post'
import { applyJoin, waitlistJoin, cancelApply, getApplyList, approveApply, rejectApply, markAttendance } from '@/api/apply'
import { getFavoriteStatus, addFavorite, cancelFavorite } from '@/api/favorite'
import { submitReport } from '@/api/report'
import { getMyTags } from '@/api/user'
import { useUserStore } from '@/store/user'
import CreditStar from '@/components/CreditStar.vue'
import EmptyState from '@/components/EmptyState.vue'
import UserAvatar from '@/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 帖子ID（可变：详情页之间跳转时组件复用，需跟随路由参数更新）
let postId = route.params.id
const post = ref(null)
const commentList = ref([])
const commentText = ref('')
const applyLoading = ref(false)
const showApplySheet = ref(false)
const applyList = ref([])
// 加载失败态（V1.1 修复：失败不再无限转圈）
const loadError = ref(false)
// 收藏状态（V1.1）
const favorited = ref(false)
const favoriteCount = ref(0)
// 举报状态（V1.1）
const showReportSheet = ref(false)
const reportActions = [
  { name: '违规内容（广告/色情/违法）' },
  { name: '虚假信息（信息与实际不符）' },
  { name: '恶意行为（恶意报名/骚扰）' },
  { name: '其他问题' }
]

// 状态映射
const statusMap = {
  RECRUITING: { text: '招募中', type: 'primary' },
  FULL: { text: '已满员', type: 'warning' },
  ONGOING: { text: '进行中', type: 'success' },
  FINISHED: { text: '已结束', type: 'default' },
  CANCELLED: { text: '已取消', type: 'danger' }
}

const statusText = computed(() => statusMap[post.value?.status]?.text || '未知')
const statusType = computed(() => statusMap[post.value?.status]?.type || 'default')

// 段位要求文本
const requireLevelText = computed(() => {
  const map = { 1: '青铜以上', 2: '白银以上', 3: '黄金以上', 4: '钻石以上', 5: '王者' }
  return map[post.value?.requireLevel] || '不限'
})

// 性别要求文本
const requireGenderText = computed(() => {
  const map = { 1: '仅男', 2: '仅女' }
  return map[post.value?.requireGender] || '不限'
})

// 段位文本
const levelText = (level) => {
  const map = { 1: '青铜', 2: '白银', 3: '黄金', 4: '钻石', 5: '王者' }
  return map[level] || ''
}

// 是否为帖子发布者
const isOwner = computed(() => {
  return post.value?.publisherId === userStore.userInfo?.id
})

// 是否可以标记出勤（ONGOING/FINISHED状态下发布者可操作）
const canMarkAttendance = computed(() => {
  return post.value?.status === 'ONGOING' || post.value?.status === 'FINISHED'
})

// 当前用户的报名状态
const applyStatus = computed(() => {
  return post.value?.applyStatus || ''
})

// 我的兴趣标签（含段位），用于报名资格预检
const myTags = ref([])

/**
 * 报名资格预检（V1.2）：
 * 提前计算当前用户是否满足段位/性别/信誉要求，不满足时禁用报名按钮并给出原因，
 * 避免用户点击报名后才知道失败。与后端 ApplyServiceImpl.validateQualification 规则保持一致。
 */
const applyBlockReason = computed(() => {
  const p = post.value
  if (!p) return ''
  if (isOwner.value) return ''
  if (applyStatus.value) return ''
  // 仅招募中/已满员（可候补）时预检
  if (p.status !== 'RECRUITING' && p.status !== 'FULL') return ''
  if (!p.requireLevel || p.requireLevel === 0) return ''
  if (!userStore.isLoggedIn) return ''
  // 信誉分
  const credit = userStore.userInfo?.creditScore ?? 100
  if (credit < 60) return '你的信誉分不足60，无法报名该组局'
  // 性别
  if (p.requireGender && p.requireGender !== 0 && userStore.userInfo?.gender !== p.requireGender) {
    return `性别不满足要求（${requireGenderText.value}）`
  }
  // 段位：帖子任一相关标签上的用户最高段位需达到要求（与后端一致）
  const tagIds = String(p.tagIds || '').split(',').map(Number).filter(Boolean)
  if (!tagIds.length) return '该组局未设置项目标签，无法校验段位'
  const myLevel = myTags.value
    .filter(t => tagIds.includes(Number(t.id)))
    .reduce((max, t) => Math.max(max, Number(t.selfLevel) || 0), 0)
  if (myLevel < p.requireLevel) {
    return `段位不满足：该组局要求${requireLevelText.value}，你当前${myLevel ? '最高' + levelText(myLevel) : '未设置相关项目段位'}`
  }
  return ''
})

// 预检提示条的操作按钮文案（按原因给出直达入口）
const blockAction = computed(() => {
  const reason = applyBlockReason.value
  if (!reason) return ''
  if (reason.includes('段位')) return '去设置段位'
  if (reason.includes('性别')) return '去修改资料'
  return ''
})

// 跳转解决预检问题的页面
const goFixBlock = () => {
  const reason = applyBlockReason.value
  if (!reason) return
  if (reason.includes('段位')) {
    router.push({ path: '/mine/tags', query: { redirect: `/post/${postId}` } })
  } else if (reason.includes('性别')) {
    router.push({ path: '/mine/profile', query: { redirect: `/post/${postId}` } })
  }
}

// 加载我的标签（预检用）
const fetchMyTags = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getMyTags()
    myTags.value = res.data || []
  } catch (err) {
    // 静默处理，预检降级为不拦截
  }
}

// 格式化时间
// 举报帖子（V1.1）
const handleReport = async () => {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: `/post/${postId}` } })
    return
  }
  showReportSheet.value = true
}

// 选择举报类型
const onReportSelect = async (action) => {
  showReportSheet.value = false
  if (!action || !action.name) return
  try {
    await showDialog({
      title: '确认举报',
      message: `举报类型：${action.name}。请确认举报内容真实有效，恶意举报将影响你的信誉。`,
      showCancelButton: true,
      confirmButtonText: '提交举报',
      cancelButtonText: '再想想'
    })
    await submitReport({
      targetType: 'POST',
      targetId: Number(postId),
      reasonType: action.name.replace(/（.*）/, ''),
      description: action.name
    })
    showToast('举报已提交，等待处理')
  } catch (e) {
    // 用户取消或接口错误（拦截器已提示）
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 加载帖子详情
const fetchDetail = async () => {
  loadError.value = false
  try {
    const res = await getPostDetail(postId)
    post.value = res.data
  } catch (err) {
    // 修复：失败显示错误态+重试按钮，而不是永远"加载中"
    loadError.value = true
  }
}

// 重试加载（V1.1）
const reload = () => {
  fetchDetail()
  fetchComments()
  fetchFavoriteStatus()
}

// 加载收藏状态（V1.1）
const fetchFavoriteStatus = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getFavoriteStatus(postId)
    favorited.value = res.data?.favorited || false
    favoriteCount.value = res.data?.count || 0
  } catch (err) {
    // 静默处理
  }
}

// 切换收藏（V1.1）
const handleToggleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: `/post/${postId}` } })
    return
  }
  try {
    if (favorited.value) {
      await cancelFavorite(postId)
      favorited.value = false
      favoriteCount.value = Math.max(0, favoriteCount.value - 1)
      showToast('已取消收藏')
    } else {
      await addFavorite(postId)
      favorited.value = true
      favoriteCount.value += 1
      showToast('收藏成功')
    }
  } catch (err) {
    // 接口层已处理
  }
}

// 加载评论列表
const fetchComments = async () => {
  try {
    const res = await getComments(postId, { page: 1, size: 100 })
    commentList.value = res.data?.records || res.data || []
  } catch (err) {
    // 接口层已处理
  }
}

// 报名参加
const handleApplyJoin = async () => {
  // V1.2 游客点击报名：引导登录并回跳原帖（与收藏/举报行为一致）
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: `/post/${postId}` } })
    return
  }
  applyLoading.value = true
  try {
    await applyJoin(postId)
    showToast('报名成功，等待审核')
    await fetchDetail()
  } catch (err) {
    // 关键操作：用弹窗展示后端返回的具体原因（如段位不满足），避免用户不知道失败原因
    showDialog({
      title: '报名失败',
      message: err.message || '请稍后重试',
      confirmButtonText: '知道了'
    })
  } finally {
    applyLoading.value = false
  }
}

// 候补报名（V1.1）
const handleWaitlistJoin = async () => {
  // V1.2 游客点击候补：引导登录并回跳原帖
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: `/post/${postId}` } })
    return
  }
  applyLoading.value = true
  try {
    await waitlistJoin(postId)
    showToast('已进入候补队列')
    await fetchDetail()
  } catch (err) {
    showDialog({
      title: '候补失败',
      message: err.message || '请稍后重试',
      confirmButtonText: '知道了'
    })
  } finally {
    applyLoading.value = false
  }
}

// 取消组局（V1.2 新增前端入口）
const handleCancelPost = async () => {
  try {
    await showConfirmDialog({
      title: '取消组局',
      message: '确定要取消该组局吗？所有已报名用户将收到取消通知，此操作不可恢复。',
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想'
    })
    await cancelPost(postId)
    showToast('已取消组局')
    await fetchDetail()
  } catch (err) {
    // 用户取消或接口错误
  }
}

// 取消报名
const handleCancelApply = async () => {
  try {
    // 提示信誉影响：活动开始前1小时内取消将扣5信誉分
    await showConfirmDialog({
      title: '取消报名',
      message: '确定要取消报名吗？活动开始前1小时内取消将扣除5信誉分。',
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想'
    })
    const applyId = post.value?.myApplyId
    if (applyId) {
      await cancelApply(applyId)
      showToast('已取消报名')
      await fetchDetail()
    }
  } catch (err) {
    // 用户取消或接口错误
  }
}

// 发送评论
// 评论发送（修复：防重复点击，连点会发多条相同评论）
const commentSubmitting = ref(false)
const handleAddComment = async () => {
  // V1.2 游客点击发送：引导登录并回跳原帖（与报名/收藏/举报行为一致）
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: `/post/${postId}` } })
    return
  }
  if (commentSubmitting.value) return
  if (!commentText.value.trim()) {
    showToast('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await addComment({ postId, content: commentText.value.trim() })
    commentText.value = ''
    showToast('评论成功')
    await fetchComments()
  } catch (err) {
    // 接口层已处理
  } finally {
    commentSubmitting.value = false
  }
}

// 打开报名管理弹窗
const openApplySheet = async () => {
  showApplySheet.value = true
  try {
    const res = await getApplyList(postId)
    applyList.value = res.data || []
  } catch (err) {
    // 接口层已处理
  }
}

// 报名状态文本
const applyStatusText = (status) => {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    CANCELLED: '已取消',
    ATTENDED: '已出勤',
    NOSHOW: '未到场'
  }
  return map[status] || status
}

// 报名状态标签类型
const applyStatusType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'default',
    ATTENDED: 'success',
    NOSHOW: 'danger'
  }
  return map[status] || 'default'
}

// 通过报名
const handleApprove = async (applyId) => {
  try {
    await approveApply(applyId)
    showToast('已通过')
    await openApplySheet()
    await fetchDetail()
  } catch (err) {
    // 接口层已处理
  }
}

// 拒绝报名
const handleReject = async (applyId) => {
  try {
    await rejectApply(applyId)
    showToast('已拒绝')
    await openApplySheet()
    await fetchDetail()
  } catch (err) {
    // 接口层已处理
  }
}

// 标记出勤
const handleMarkAttendance = async (applyId, attended) => {
  try {
    const msg = attended ? '确定标记为已出勤？' : '确定标记为未到场？（将扣除信誉分）'
    await showConfirmDialog({ title: '提示', message: msg })
    await markAttendance(applyId, attended)
    showToast(attended ? '已标记出勤' : '已标记未到场')
    await openApplySheet()
    await fetchDetail()
  } catch (err) {
    // 用户取消或接口错误
  }
}

// 跳转评价页面
const goReview = () => {
  router.push(`/review/${postId}`)
}

// 跳转发布者个人主页（V1.1）
const goPublisherProfile = () => {
  if (post.value?.publisherId) {
    router.push(`/user/${post.value.publisherId}`)
  }
}

// 加载全部数据（详情/评论/收藏/标签）
const loadAll = () => {
  fetchDetail()
  fetchComments()
  fetchFavoriteStatus()
  fetchMyTags()
}

// 路由参数变化（/post/1 -> /post/2）时重置状态并重新加载，
// 否则组件复用导致页面停留在上一个帖子的数据（V1.2 修复）
watch(
  () => route.params.id,
  (newId) => {
    if (!newId || String(newId) === String(postId)) return
    postId = newId
    post.value = null
    loadError.value = false
    commentList.value = []
    commentText.value = ''
    favorited.value = false
    favoriteCount.value = 0
    applyList.value = []
    showApplySheet.value = false
    loadAll()
  }
)

onMounted(loadAll)
</script>

<style scoped lang="scss">
.post-detail-page {
  padding-bottom: 160px;
  background: var(--bg-page);
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 16px;
  background: var(--bg-card);
  margin-bottom: 14px;

  .post-title {
    font-size: 19px;
    font-weight: 700;
    letter-spacing: -0.01em;
    color: var(--text-primary);
    flex: 1;
    margin-right: 8px;
    line-height: 1.45;
  }
}

.info-card {
  .info-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: var(--text-secondary);
    margin-bottom: 10px;

    &:last-child { margin-bottom: 0; }

    .van-icon { color: var(--primary); font-size: 16px; }
  }
}

.desc-card {
  .desc-text {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.7;
    white-space: pre-wrap;
  }
}

.publisher-card {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;

  &:active { background: #fafbfc; }

  .publisher-info {
    flex: 1;

    .publisher-name-row {
      display: flex;
      align-items: center;
      gap: 6px;

      .name {
        font-size: 15px;
        font-weight: 600;
        color: var(--text-primary);
      }
    }

    .publisher-detail {
      margin-top: 4px;
      font-size: 12px;
      color: var(--text-hint);

      .signature {
        margin-left: 8px;
      }
    }

    .publisher-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 8px;

      .publisher-tag {
        font-size: 11px;

        .tag-level {
          margin-left: 2px;
          opacity: 0.8;
        }
      }
    }
  }
}

.joined-card {
  .joined-scroll {
    display: flex;
    overflow-x: auto;
    gap: 16px;
    padding-top: 8px;
    -webkit-overflow-scrolling: touch;

    &::-webkit-scrollbar { display: none; }

    .joined-user {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      flex-shrink: 0;

      .joined-name {
        font-size: 11px;
        color: var(--text-secondary);
        max-width: 48px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

.comment-card {
  .comment-item {
    display: flex;
    gap: 8px;
    padding: 12px 0;
    border-bottom: 1px solid var(--border-light);

    &:last-child { border-bottom: none; }

    .comment-body {
      flex: 1;

      .comment-name {
        font-size: 13px;
        color: var(--text-primary);
        font-weight: 500;
      }

      .comment-content {
        font-size: 14px;
        color: var(--text-secondary);
        margin-top: 4px;
        line-height: 1.5;
      }

      .comment-time {
        font-size: 11px;
        color: var(--text-disabled);
        margin-top: 4px;
        display: block;
      }
    }
  }
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.loading-area {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.bottom-action {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 10px 16px;
  border-top: 1px solid var(--border-light);

  .fav-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 12px;
    margin-bottom: 10px;
    border-radius: 999px;
    background: var(--bg-grey);
    cursor: pointer;

    .fav-text {
      font-size: 12px;
      color: var(--text-hint);

      &.active {
        color: #f59e0b;
      }
    }

    .fav-count {
      font-size: 11px;
      color: var(--text-hint);
    }
  }

  .comment-input {
    margin-bottom: 10px;
    border-radius: 999px;
    background: var(--bg-grey);
    padding: 4px 12px;
  }

  .action-buttons {
    display: flex;
    gap: 8px;

    .van-button {
      flex: 1;
    }

    .van-button--primary {
      border: none;
      box-shadow: 0 12px 24px -12px rgba(16, 163, 127, 0.4);
    }
  }

  // 报名资格预检提示条（V1.2）
  .apply-block-tip {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 12px;
    margin-bottom: 10px;
    border-radius: 10px;
    background: #fff7e6;
    border: 1px solid #ffe1a8;
    cursor: pointer;

    .tip-icon {
      color: #f59e0b;
      font-size: 15px;
      flex-shrink: 0;
    }

    .tip-text {
      flex: 1;
      font-size: 12px;
      color: #8a6d3b;
      line-height: 1.5;
    }

    .tip-action {
      font-size: 12px;
      font-weight: 600;
      color: var(--primary);
      flex-shrink: 0;
    }
  }
}

.apply-sheet {
  padding: 16px;

  .apply-sheet-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
    }

    .van-icon {
      font-size: 20px;
      color: var(--text-hint);
      cursor: pointer;
    }
  }

  .apply-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid var(--border-light);

    &:last-child { border-bottom: none; }

    .apply-user {
      display: flex;
      align-items: center;
      gap: 8px;

      .apply-nickname {
        font-size: 14px;
        color: var(--text-primary);
      }
    }

    .apply-actions {
      display: flex;
      gap: 8px;
    }
  }
}
</style>
