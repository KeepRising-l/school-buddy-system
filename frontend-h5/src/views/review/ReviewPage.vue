<template>
  <div class="page review-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="评价搭子" left-arrow @click-left="router.back()" />

    <div class="content">
      <!-- 评价窗口提示（V1.2）：活动结束后7天内可评价，避免用户过时才想起来 -->
      <div class="window-tip">
        <van-icon name="info-o" />
        <span>活动结束后 7 天内可评价，逾期将无法提交</span>
      </div>

      <!-- 被评价人信息 -->
      <div class="review-target card">
        <UserAvatar :name="targetUser.nickname" :avatar="targetUser.avatar" :size="50" />
        <span class="target-name">{{ targetUser.nickname || '用户' }}</span>
      </div>

      <!-- 选择被评价人（发布者视角，多个已出勤参与者时；V1.1） -->
      <div class="target-select card" v-if="reviewTargets.length > 1">
        <h3 class="section-title">选择要评价的搭子</h3>
        <div class="target-list">
          <div
            v-for="u in reviewTargets"
            :key="u.userId"
            :class="['target-item', { active: String(u.userId) === String(targetUser.id) }]"
            @click="selectTarget(u)"
          >
            <UserAvatar :name="u.nickname" :avatar="u.avatar" :size="32" />
            <span class="target-item-name">{{ u.nickname }}</span>
          </div>
        </div>
      </div>

      <!-- 星级评分 -->
      <div class="rating-section card">
        <h3 class="section-title">评分</h3>
        <van-rate v-model="form.score" :size="28" color="#f59e0b" void-color="#e5e7eb" />
      </div>

      <!-- 评价标签 -->
      <div class="tag-section card">
        <h3 class="section-title">评价标签</h3>
        <div class="tag-list">
          <van-tag
            v-for="tag in tagOptions"
            :key="tag"
            :type="form.tags.includes(tag) ? 'primary' : 'default'"
            size="medium"
            round
            class="review-tag"
            @click="toggleTag(tag)"
          >
            {{ tag }}
          </van-tag>
        </div>
      </div>

      <!-- 文字评价 -->
      <div class="comment-section card">
        <h3 class="section-title">文字评价</h3>
        <van-field
          v-model="form.content"
          type="textarea"
          placeholder="说说你对这位搭子的评价吧"
          maxlength="200"
          show-word-limit
          rows="3"
          autosize
        />
      </div>

      <!-- 提交按钮 -->
      <van-button
        type="primary"
        block
        round
        class="submit-btn"
        :loading="submitting"
        loading-text="提交中..."
        @click="handleSubmit"
      >
        提交评价
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { submitReview } from '@/api/review'
import { getPostDetail } from '@/api/post'
import { useUserStore } from '@/store/user'
import UserAvatar from '@/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const postId = route.params.postId
const submitting = ref(false)

// 被评价人信息
const targetUser = ref({
  id: '',
  nickname: '',
  avatar: ''
})

// 可评价的参与者列表（发布者视角，已出勤者；V1.1）
const reviewTargets = ref([])

// 评价标签选项
const tagOptions = ['靠谱', '准时', '友好', '技术好', '迟到', '爽约', '态度差']

// 表单数据
const form = reactive({
  score: 5,
  tags: [],
  content: ''
})

// 切换标签选中
const toggleTag = (tag) => {
  const index = form.tags.indexOf(tag)
  if (index > -1) {
    form.tags.splice(index, 1)
  } else {
    form.tags.push(tag)
  }
}

// 加载帖子信息，根据当前用户身份确定被评价人（V1.1 修复：发布者只能评价已出勤的参与者）
const fetchPostInfo = async () => {
  try {
    const res = await getPostDetail(postId)
    const post = res.data
    const myId = userStore.userInfo?.id
    if (!post || !myId) return

    if (String(post.publisherId) === String(myId)) {
      // 当前用户是发布者 → 从可评价列表（已出勤参与者）中选择
      reviewTargets.value = post.reviewTargets || []
      if (reviewTargets.value.length > 0) {
        targetUser.value = {
          id: reviewTargets.value[0].userId,
          nickname: reviewTargets.value[0].nickname,
          avatar: reviewTargets.value[0].avatar
        }
      } else {
        targetUser.value = { id: '', nickname: '', avatar: '' }
        showToast('暂无已出勤的参与者可评价')
      }
    } else {
      // 当前用户是参与者 → 评价发布者
      targetUser.value = {
        id: post.publisherId,
        nickname: post.publisherNickname,
        avatar: post.publisherAvatar
      }
    }
  } catch (err) {
    // 接口层已处理
  }
}

// 切换被评价人（发布者多参与者场景）
const selectTarget = (user) => {
  targetUser.value = { id: user.userId, nickname: user.nickname, avatar: user.avatar }
}

// 提交评价
const handleSubmit = async () => {
  if (form.score === 0) {
    showToast('请选择评分')
    return
  }

  submitting.value = true
  try {
    await submitReview({
      postId,
      targetUserId: targetUser.value.id,
      rating: form.score,
      tag: form.tags.length > 0 ? form.tags.join(',') : '',
      content: form.content
    })
    showToast('评价成功')
    router.back()
  } catch (err) {
    // 接口层已处理
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchPostInfo()
})
</script>

<style scoped lang="scss">
.review-page {
  background: var(--bg-page);
}

.review-target {
  display: flex;
  align-items: center;
  gap: 12px;

  .target-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
  }
}

.target-select {
  .target-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .target-item {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      border-radius: 999px;
      background: var(--bg-grey);
      cursor: pointer;
      border: 1px solid transparent;
      transition: all 0.3s var(--ease);

      &.active {
        background: var(--primary-soft);
        border-color: var(--primary);
      }

      .target-item-name {
        font-size: 13px;
        color: var(--text-primary);
      }
    }
  }
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

// 评价窗口提示（V1.2）
.window-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  margin-bottom: 12px;
  border-radius: 10px;
  background: #e8f6f1;
  border: 1px solid #b8e5d8;
  font-size: 12px;
  color: var(--primary);

  .van-icon {
    flex-shrink: 0;
  }
}

.rating-section {
  text-align: center;
  padding: 20px 16px;
}

.tag-section {
  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .review-tag {
      cursor: pointer;
    }
  }
}

.comment-section {
  :deep(.van-field) {
    background: #f7f8fa;
    border-radius: 8px;
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
