<template>
  <div class="user-list-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="学号/昵称/手机号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="college" label="学院" min-width="140" />
        <el-table-column prop="creditScore" label="信誉分" width="100" align="center" />
        <el-table-column label="信誉星级" width="160" align="center">
          <template #default="{ row }">
            <CreditStar :score="row.creditScore" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 1"
              type="danger"
              size="small"
              @click="handleBan(row)"
            >封禁</el-button>
            <el-button
              v-else
              type="success"
              size="small"
              @click="handleUnban(row)"
            >解封</el-button>
            <el-button type="warning" size="small" @click="handleResetCredit(row)">重置信誉分</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 重置信誉分弹窗 -->
    <el-dialog v-model="creditDialogVisible" title="重置信誉分" width="400px" @close="resetCreditForm">
      <el-form ref="creditFormRef" :model="creditForm" :rules="creditRules" label-width="80px">
        <el-form-item label="新分数" prop="score">
          <el-input-number v-model="creditForm.score" :min="0" :max="100" :step="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="creditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creditLoading" @click="submitResetCredit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="用户详情" width="800px" @close="detailDialogVisible = false">
      <div v-if="detailUser" v-loading="detailLoading">
        <!-- 基本信息 -->
        <el-descriptions :column="3" border size="small" style="margin-bottom: 16px">
          <el-descriptions-item label="学号">{{ detailUser.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ detailUser.nickname }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detailUser.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学院">{{ detailUser.college || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderLabel(detailUser.gender) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailUser.status === 1 ? 'success' : 'danger'" size="small">
              {{ detailUser.status === 1 ? '正常' : '封禁' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="信誉分">{{ detailUser.creditScore }}</el-descriptions-item>
          <el-descriptions-item label="信誉等级">{{ detailUser.creditLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ detailUser.createdTime }}</el-descriptions-item>
          <el-descriptions-item label="个性签名" :span="3">{{ detailUser.signature || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Tab切换：发布历史/报名历史/评价记录 -->
        <el-tabs v-model="detailTab" @tab-change="handleDetailTabChange">
          <!-- 发布历史 -->
          <el-tab-pane label="发布历史" name="posts">
            <el-table :data="detailPosts" border size="small" max-height="300">
              <el-table-column prop="id" label="ID" width="60" />
              <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
              <el-table-column prop="category" label="分类" width="90" />
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdTime" label="发布时间" width="160" />
            </el-table>
            <div style="display: flex; justify-content: flex-end; margin-top: 8px">
              <el-pagination
                v-model:current-page="detailPostPage.page"
                :page-size="5"
                :total="detailPostPage.total"
                layout="total, prev, next"
                @current-change="loadDetailPosts"
              />
            </div>
          </el-tab-pane>

          <!-- 报名历史 -->
          <el-tab-pane label="报名历史" name="applies">
            <el-table :data="detailApplies" border size="small" max-height="300">
              <el-table-column prop="id" label="ID" width="60" />
              <el-table-column prop="postTitle" label="帖子标题" min-width="160" show-overflow-tooltip />
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="applyStatusType(row.status)">{{ applyStatusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="applyTime" label="报名时间" width="160" />
            </el-table>
            <div style="display: flex; justify-content: flex-end; margin-top: 8px">
              <el-pagination
                v-model:current-page="detailApplyPage.page"
                :page-size="5"
                :total="detailApplyPage.total"
                layout="total, prev, next"
                @current-change="loadDetailApplies"
              />
            </div>
          </el-tab-pane>

          <!-- 评价记录 -->
          <el-tab-pane label="收到的评价" name="reviews">
            <el-table :data="detailReviews" border size="small" max-height="300">
              <el-table-column prop="id" label="ID" width="60" />
              <el-table-column prop="reviewerNickname" label="评价人" width="100" />
              <el-table-column prop="postTitle" label="帖子标题" min-width="140" show-overflow-tooltip />
              <el-table-column label="评分" width="80" align="center">
                <template #default="{ row }">
                  <el-rate v-model="row.rating" disabled size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="tag" label="标签" width="80" />
              <el-table-column prop="content" label="内容" min-width="120" show-overflow-tooltip />
            </el-table>
            <div style="display: flex; justify-content: flex-end; margin-top: 8px">
              <el-pagination
                v-model:current-page="detailReviewPage.page"
                :page-size="5"
                :total="detailReviewPage.total"
                layout="total, prev, next"
                @current-change="loadDetailReviews"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUserList, banUser, unbanUser, resetCredit, getUserDetail, getUserPosts, getUserApplies, getUserReviews } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import CreditStar from '@/components/CreditStar.vue'

// 搜索表单
const searchForm = reactive({ keyword: '' })

// 表格数据
const loading = ref(false)
const tableData = ref([])

// 分页参数
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 重置信誉分弹窗
const creditDialogVisible = ref(false)
const creditLoading = ref(false)
const creditFormRef = ref()
const currentUserId = ref(null)
const creditForm = reactive({ score: 100 })
const creditRules = {
  score: [{ required: true, message: '请输入信誉分', trigger: 'blur' }]
}

// 用户详情弹窗
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailUser = ref(null)
const detailTab = ref('posts')

// 详情数据
const detailPosts = ref([])
const detailApplies = ref([])
const detailReviews = ref([])

const detailPostPage = reactive({ page: 1, total: 0 })
const detailApplyPage = reactive({ page: 1, total: 0 })
const detailReviewPage = reactive({ page: 1, total: 0 })

// 状态映射
const statusMap = {
  RECRUITING: '招募中',
  FULL: '已满员',
  ONGOING: '进行中',
  FINISHED: '已结束',
  CANCELLED: '已取消'
}
const statusLabel = (s) => statusMap[s] || s
const statusTagType = (s) => {
  const map = { RECRUITING: '', FULL: 'warning', ONGOING: 'success', FINISHED: 'info', CANCELLED: 'danger' }
  return map[s] || ''
}

const applyStatusLabel = (s) => {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消', ATTENDED: '已出勤', NOSHOW: '未到场' }
  return map[s] || s
}
const applyStatusType = (s) => {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info', ATTENDED: 'success', NOSHOW: 'danger' }
  return map[s] || ''
}

const genderLabel = (g) => {
  if (g === 1) return '男'
  if (g === 2) return '女'
  return '未知'
}

// 加载用户列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword || undefined
    })
    const data = res.data || {}
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadData()
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  pagination.page = 1
  loadData()
}

// 封禁用户
const handleBan = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要封禁用户「${row.nickname}」吗？`, '提示', {
      type: 'warning'
    })
    await banUser(row.id)
    ElMessage.success('封禁成功')
    loadData()
  } catch (e) {
    // 用户取消或请求失败
  }
}

// 解封用户
const handleUnban = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要解封用户「${row.nickname}」吗？`, '提示', {
      type: 'warning'
    })
    await unbanUser(row.id)
    ElMessage.success('解封成功')
    loadData()
  } catch (e) {
    // 用户取消或请求失败
  }
}

// 打开重置信誉分弹窗
const handleResetCredit = (row) => {
  currentUserId.value = row.id
  creditForm.score = row.creditScore
  creditDialogVisible.value = true
}

// 提交重置信誉分
const submitResetCredit = async () => {
  if (!creditFormRef.value) return
  await creditFormRef.value.validate()
  creditLoading.value = true
  try {
    await resetCredit({ userId: currentUserId.value, creditScore: creditForm.score })
    ElMessage.success('重置信誉分成功')
    creditDialogVisible.value = false
    loadData()
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    creditLoading.value = false
  }
}

// 重置弹窗表单
const resetCreditForm = () => {
  creditForm.score = 100
  currentUserId.value = null
}

// 打开用户详情
const handleDetail = async (row) => {
  currentUserId.value = row.id
  detailTab.value = 'posts'
  detailDialogVisible.value = true
  detailLoading.value = true
  try {
    const res = await getUserDetail(row.id)
    detailUser.value = res.data
    await loadDetailPosts()
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    detailLoading.value = false
  }
}

// Tab切换
const handleDetailTabChange = (tab) => {
  if (tab === 'posts') loadDetailPosts()
  else if (tab === 'applies') loadDetailApplies()
  else if (tab === 'reviews') loadDetailReviews()
}

// 加载发布历史
const loadDetailPosts = async () => {
  try {
    const res = await getUserPosts(currentUserId.value, { page: detailPostPage.page, size: 5 })
    const data = res.data || {}
    detailPosts.value = data.records || []
    detailPostPage.total = data.total || 0
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

// 加载报名历史
const loadDetailApplies = async () => {
  try {
    const res = await getUserApplies(currentUserId.value, { page: detailApplyPage.page, size: 5 })
    const data = res.data || {}
    detailApplies.value = data.records || []
    detailApplyPage.total = data.total || 0
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

// 加载评价记录
const loadDetailReviews = async () => {
  try {
    const res = await getUserReviews(currentUserId.value, { page: detailReviewPage.page, size: 5 })
    const data = res.data || {}
    detailReviews.value = data.records || []
    detailReviewPage.total = data.total || 0
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.user-list-container {
  .search-card {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
