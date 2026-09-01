<template>
  <div class="post-list-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
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
          <el-button type="success" @click="handleExportPosts">导出</el-button>
        </el-form-item>
      </el-form>
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">详情</el-button>
            <!-- 未取消的帖子可下架 -->
            <el-button
              v-if="row.status !== 'CANCELLED'"
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
import { getPostList, takedownPost, deletePost, getTagList, exportPosts } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { downloadFromBlob } from '@/utils/download'

// 搜索表单
const searchForm = reactive({
  status: '',
  category: ''
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
      category: searchForm.category || undefined
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

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.status = ''
  searchForm.category = ''
  pagination.page = 1
  loadData()
}

// 查看详情（H5 用户端地址，管理端无此路由；支持 VITE_H5_BASE 环境变量覆盖）
const handleView = (row) => {
  const h5Base = import.meta.env.VITE_H5_BASE || 'http://localhost:3000'
  window.open(`${h5Base}/post/${row.id}`, '_blank')
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

// 导出帖子列表
const handleExportPosts = async () => {
  try {
    const res = await exportPosts()
    downloadFromBlob(res, 'posts.csv')
    ElMessage.success('导出成功')
  } catch (e) {
    // 错误已在拦截器中处理
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
