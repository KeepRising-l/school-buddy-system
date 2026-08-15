<template>
  <div class="report-list-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待处理" :value="0" />
            <el-option label="有效" :value="1" />
            <el-option label="无效" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 举报表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="对象" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.targetType === 'POST' ? 'primary' : 'warning'" size="small">
              {{ row.targetType === 'POST' ? '帖子' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetTitle" label="被举报对象" min-width="150" show-overflow-tooltip />
        <el-table-column prop="reasonType" label="举报类型" width="110" />
        <el-table-column prop="reporterNickname" label="举报人" width="100" />
        <el-table-column label="处理状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="举报时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 0"
              type="warning"
              size="small"
              @click="handleProcess(row)"
            >处理</el-button>
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
          @size-change="handleSizeChange"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 举报详情弹窗 -->
    <el-dialog v-model="detailVisible" title="举报详情" width="560px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="举报ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="举报类型">{{ detail.reasonType }}</el-descriptions-item>
        <el-descriptions-item label="对象类型">{{ detail.targetType === 'POST' ? '帖子' : '用户' }}</el-descriptions-item>
        <el-descriptions-item label="被举报对象">{{ detail.targetTitle }}</el-descriptions-item>
        <el-descriptions-item label="举报人">{{ detail.reporterNickname }}</el-descriptions-item>
        <el-descriptions-item label="举报时间">{{ detail.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="举报说明" :span="2">{{ detail.description || '无' }}</el-descriptions-item>
        <el-descriptions-item label="处理状态" :span="2">
          <el-tag :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <template v-if="detail.status !== 0">
          <el-descriptions-item label="处理人">{{ detail.handleByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ detail.handleTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理备注" :span="2">{{ detail.handleRemark || '无' }}</el-descriptions-item>
        </template>
      </el-descriptions>
    </el-dialog>

    <!-- 处理弹窗 -->
    <el-dialog v-model="processVisible" title="处理举报" width="480px">
      <el-form label-width="80px">
        <el-form-item label="判定结果">
          <el-radio-group v-model="processForm.valid">
            <el-radio :value="true">有效（联动处理）</el-radio>
            <el-radio :value="false">无效</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="processForm.remark" type="textarea" :rows="3" placeholder="填写处理说明（可选）" />
        </el-form-item>
        <el-alert
          v-if="processForm.valid"
          type="warning"
          :closable="false"
          show-icon
          title="判定有效后将自动执行：举报帖子→下架，举报用户→封禁，并通知双方"
          style="margin-bottom: 12px"
        />
      </el-form>
      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" :loading="processing" @click="handleSubmitProcess">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getReportList, getReportDetail, handleReport } from '@/api/admin'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const detail = ref(null)
const processVisible = ref(false)
const processForm = reactive({ valid: true, remark: '' })
const processing = ref(false)
const currentRow = ref(null)

const searchForm = reactive({
  status: undefined
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const statusLabel = (status) => {
  const map = { 0: '待处理', 1: '有效', 2: '无效' }
  return map[status] || '未知'
}

const statusTagType = (status) => {
  const map = { 0: 'warning', 1: 'danger', 2: 'info' }
  return map[status] || 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getReportList({
      page: pagination.page,
      size: pagination.size,
      status: searchForm.status !== undefined && searchForm.status !== null ? searchForm.status : undefined
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

const handleSizeChange = () => {
  pagination.page = 1
  loadData()
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.status = undefined
  pagination.page = 1
  loadData()
}

const handleDetail = async (row) => {
  try {
    const res = await getReportDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

const handleProcess = (row) => {
  currentRow.value = row
  processForm.valid = true
  processForm.remark = ''
  processVisible.value = true
}

const handleSubmitProcess = async () => {
  processing.value = true
  try {
    await handleReport(currentRow.value.id, {
      valid: processForm.valid,
      remark: processForm.remark
    })
    ElMessage.success('处理成功')
    processVisible.value = false
    loadData()
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    processing.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
