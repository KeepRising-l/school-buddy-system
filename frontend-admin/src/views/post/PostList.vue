<template>
  <div class="post-list-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.isAuditPass" placeholder="全部" clearable style="width: 140px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="帖子状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="招募中" value="RECRUITING" />
            <el-option label="已满员" value="FULL" />
            <el-option label="进行中" value="ONGOING" />
            <el-option label="已结束" value="FINISHED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="tag in tagOptions" :key="tag.id" :label="tag.name" :value="tag.name" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 快捷Tab -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-radio-group v-model="activeTab" @change="handleTabChange">
        <el-radio-button label="all">全部帖子</el-radio-button>
        <el-radio-button label="pending">待审核</el-radio-button>
        <el-radio-button label="passed">已通过</el-radio-button>
      </el-radio-group>
    </el-card>

    <!-- 帖子表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="publisherNickname" label="发布者" width="100" />
        <el-table-column prop="createdTime" label="时间" width="170" />
        <el-table-column label="人数" width="90" align="center">
          <template #default="{ row }">
            {{ row.currentPeople || 0 }}/{{ row.maxPeople || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="auditTagType(row.isAuditPass)">{{ auditLabel(row.isAuditPass) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">详情</el-button>
            <!-- 待审核：通过/拒绝 -->
            <template v-if="row.isAuditPass === 0">
              <el-button type="success" size="small" @click="handleAudit(row, true)">通过</el-button>
              <el-button type="warning" size="small" @click="handleAudit(row, false)">拒绝</el-button>
            </template>
            <!-- 已通过且未取消：可下架 -->
            <el-button
              v-if="row.isAuditPass === 1 && row.status !== 'CANCELLED'"
              type="danger"
              size="small"
              @click="handleTakedown(row)"
            >下架</el-button>
            <!-- 物理删除 -->
            <el-button type="info" size="small" @click="handleDelete(row)">删除</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getPostList, takedownPost, auditPost, deletePost, getTagList } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

// 快捷Tab
const activeTab = ref('all')

// 搜索表单
const searchForm = reactive({
  status: '',
  category: '',
  isAuditPass: undefined
})

// 分类选项
const tagOptions = ref([])

// 表格数据
const loading = ref(false)
const tableData = ref([])

// 分页参数
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 帖子状态映射
const statusMap = {
  RECRUITING: { label: '招募中', type: '' },
  FULL: { label: '已满员', type: 'warning' },
  ONGOING: { label: '进行中', type: 'success' },
  FINISHED: { label: '已结束', type: 'info' },
  CANCELLED: { label: '已取消', type: 'danger' }
}

// 审核状态映射
const auditLabel = (isAuditPass) => isAuditPass === 1 ? '已通过' : '待审核'
const auditTagType = (isAuditPass) => isAuditPass === 1 ? 'success' : 'warning'

const statusLabel = (status) => statusMap[status]?.label || status
const statusTagType = (status) => statusMap[status]?.type || ''

// 加载帖子列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await getPostList({
      page: pagination.page,
      size: pagination.size,
      status: searchForm.status || undefined,
      category: searchForm.category || undefined,
      isAuditPass: searchForm.isAuditPass !== undefined && searchForm.isAuditPass !== null ? searchForm.isAuditPass : undefined
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

// 加载分类选项
const loadTagOptions = async () => {
  try {
    const res = await getTagList()
    tagOptions.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

// Tab切换
const handleTabChange = (val) => {
  if (val === 'all') {
    searchForm.isAuditPass = undefined
  } else if (val === 'pending') {
    searchForm.isAuditPass = 0
  } else if (val === 'passed') {
    searchForm.isAuditPass = 1
  }
  pagination.page = 1
  loadData()
}

// 搜索
const handleSearch = () => {
  // 同步Tab状态
  if (searchForm.isAuditPass === undefined) activeTab.value = 'all'
  else if (searchForm.isAuditPass === 0) activeTab.value = 'pending'
  else if (searchForm.isAuditPass === 1) activeTab.value = 'passed'
  pagination.page = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.status = ''
  searchForm.category = ''
  searchForm.isAuditPass = undefined
  activeTab.value = 'all'
  pagination.page = 1
  loadData()
}

// 查看详情
const handleView = (row) => {
  window.open(`/post/${row.id}`, '_blank')
}

// 审核帖子
const handleAudit = async (row, pass) => {
  try {
    await ElMessageBox.confirm(
      `确定要${pass ? '通过' : '拒绝'}帖子「${row.title}」的审核吗？`,
      '审核确认',
      { type: pass ? 'success' : 'warning' }
    )
    await auditPost(row.id, { pass })
    ElMessage.success(pass ? '审核通过' : '已拒绝')
    loadData()
  } catch (e) {
    // 用户取消或请求失败
  }
}

// 下架帖子
const handleTakedown = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要下架帖子「${row.title}」吗？下架后该帖子将不再展示。`, '提示', {
      type: 'warning'
    })
    await takedownPost(row.id)
    ElMessage.success('下架成功')
    loadData()
  } catch (e) {
    // 用户取消或请求失败
  }
}

// 物理删除帖子
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要永久删除帖子「${row.title}」吗？此操作不可恢复，帖子及其所有评论、报名、评价数据将被删除。`,
      '危险操作',
      { type: 'error', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deletePost(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // 用户取消或请求失败
  }
}

onMounted(() => {
  loadData()
  loadTagOptions()
})
</script>

<style scoped lang="scss">
.post-list-container {
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
