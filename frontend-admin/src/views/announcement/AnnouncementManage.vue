<template>
  <div class="announcement-container">
    <!-- 顶部操作栏 -->
    <el-card shadow="never" class="action-card">
      <el-button type="primary" @click="handleAdd">新增公告</el-button>
    </el-card>

    <!-- 公告表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 2 ? 'warning' : ''">
              {{ row.type === 2 ? '弹窗' : '公告' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已发布' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column prop="expireTime" label="过期时间" width="170">
          <template #default="{ row }">
            {{ row.expireTime || '永不过期' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="success"
              size="small"
              @click="handlePublish(row)"
            >发布</el-button>
            <el-button
              v-if="row.status === 1"
              type="warning"
              size="small"
              @click="handleClose(row)"
            >关闭</el-button>
            <el-button
              v-if="row.status === 0"
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 添加/编辑公告弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '新增公告'" width="600px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option :value="1" label="普通公告" />
            <el-option :value="2" label="弹窗公告" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="留空表示永不过期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  getAnnouncementList, addAnnouncement, updateAnnouncement,
  publishAnnouncement, closeAnnouncement, deleteAnnouncement
} from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

// 表格数据
const loading = ref(false)
const tableData = ref([])

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 弹窗相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const editId = ref(null)

// 表单数据
const form = reactive({
  title: '',
  type: 1,
  content: '',
  expireTime: null
})

// 表单校验规则
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

// 加载公告列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await getAnnouncementList({
      page: pagination.page,
      size: pagination.size
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

// 新增公告
const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  dialogVisible.value = true
}

// 编辑公告
const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.title = row.title
  form.type = row.type
  form.content = row.content
  form.expireTime = row.expireTime
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    const payload = {
      title: form.title,
      type: form.type,
      content: form.content,
      expireTime: form.expireTime || null
    }
    if (isEdit.value) {
      await updateAnnouncement({ id: editId.value, ...payload })
      ElMessage.success('编辑成功')
    } else {
      await addAnnouncement(payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    submitLoading.value = false
  }
}

// 发布公告
const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要发布公告「${row.title}」吗？`, '提示', {
      type: 'warning'
    })
    await publishAnnouncement(row.id)
    ElMessage.success('发布成功')
    loadData()
  } catch (e) {
    // 用户取消或请求失败
  }
}

// 关闭公告
const handleClose = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要关闭公告「${row.title}」吗？`, '提示', {
      type: 'warning'
    })
    await closeAnnouncement(row.id)
    ElMessage.success('关闭成功')
    loadData()
  } catch (e) {
    // 用户取消或请求失败
  }
}

// 删除公告
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除公告「${row.title}」吗？删除后不可恢复。`, '提示', {
      type: 'warning'
    })
    await deleteAnnouncement(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // 用户取消或请求失败
  }
}

// 重置表单
const resetForm = () => {
  form.title = ''
  form.type = 1
  form.content = ''
  form.expireTime = null
  editId.value = null
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.announcement-container {
  .action-card {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
