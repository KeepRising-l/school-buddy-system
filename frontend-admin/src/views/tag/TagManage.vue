<template>
  <div class="tag-manage-container">
    <!-- 顶部操作栏 -->
    <el-card shadow="never" class="action-card">
      <el-button type="primary" @click="handleAdd">添加标签</el-button>
    </el-card>

    <!-- 标签表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading" :span-method="categorySpanMethod">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="category" label="分类" width="140" />
        <el-table-column prop="name" label="项目名" width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑标签弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑标签' : '添加标签'" width="480px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类" prop="category">
          <el-select
            v-model="form.category"
            filterable
            allow-create
            default-first-option
            placeholder="选择已有分类或输入新分类"
            style="width: 100%"
          >
            <el-option
              v-for="cat in categoryList"
              :key="cat"
              :label="cat"
              :value="cat"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { getTagList, addTag, updateTag, disableTag, getTagCategories } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

// 表格数据
const loading = ref(false)
const tableData = ref([])

// 分类列表
const categoryList = ref([])

// 按分类分组排序
const sortedData = computed(() => {
  return [...tableData.value].sort((a, b) => {
    if (a.category !== b.category) return a.category.localeCompare(b.category)
    return (a.sortOrder || 0) - (b.sortOrder || 0)
  })
})

// 分类合并行
const categorySpanMethod = ({ row, column, rowIndex, columnIndex }) => {
  if (columnIndex === 1) {
    const data = sortedData.value
    const currentCategory = data[rowIndex].category
    // 找到该分类的第一行
    const firstIndex = data.findIndex(item => item.category === currentCategory)
    const count = data.filter(item => item.category === currentCategory).length
    if (rowIndex === firstIndex) {
      return { rowspan: count, colspan: 1 }
    } else {
      return { rowspan: 0, colspan: 0 }
    }
  }
}

// 弹窗相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const editId = ref(null)

// 表单数据
const form = reactive({
  category: '',
  name: '',
  description: '',
  sortOrder: 0
})

// 表单校验规则
const rules = {
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  name: [{ required: true, message: '请输入项目名', trigger: 'blur' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }]
}

// 加载标签列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await getTagList()
    tableData.value = res.data || []
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const res = await getTagCategories()
    categoryList.value = res.data || []
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

// 添加标签
const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  dialogVisible.value = true
}

// 编辑标签
const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.category = row.category
  form.name = row.name
  form.description = row.description
  form.sortOrder = row.sortOrder
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateTag({ id: editId.value, ...form })
      ElMessage.success('编辑成功')
    } else {
      await addTag({ ...form })
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

// 禁用/启用标签
const handleToggleStatus = async (row) => {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}标签「${row.name}」吗？`, '提示', {
      type: 'warning'
    })
    await disableTag(row.id)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (e) {
    // 用户取消或请求失败
  }
}

// 重置表单
const resetForm = () => {
  form.category = ''
  form.name = ''
  form.description = ''
  form.sortOrder = 0
  editId.value = null
}

onMounted(() => {
  loadData()
  loadCategories()
})
</script>

<style scoped lang="scss">
.tag-manage-container {
  .action-card {
    margin-bottom: 16px;
  }
}
</style>
