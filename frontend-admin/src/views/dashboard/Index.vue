<template>
  <div class="dashboard-container">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #10a37f">
              <el-icon :size="28"><User /></el-icon>
            </div>
            <div class="stat-info">
              <el-statistic title="总用户数" :value="overview.totalUsers" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #0ea5e9">
              <el-icon :size="28"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <el-statistic title="总帖子数" :value="overview.totalPosts" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f5a623">
              <el-icon :size="28"><EditPen /></el-icon>
            </div>
            <div class="stat-info">
              <el-statistic title="今日发帖" :value="overview.todayPosts" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #9333ea">
              <el-icon :size="28"><Trophy /></el-icon>
            </div>
            <div class="stat-info">
              <el-statistic title="成功组局数" :value="overview.successMatches" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门分类饼图 -->
    <el-card shadow="hover" class="chart-card">
      <template #header>
        <span>热门分类 Top5</span>
      </template>
      <div ref="pieChartRef" class="chart-container" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { getStatsOverview, getCategoryTop5 } from '@/api/admin'

// 统计概览数据（字段名与后端 StatsVO 对齐：successMatches）
const overview = reactive({
  totalUsers: 0,
  totalPosts: 0,
  todayPosts: 0,
  successMatches: 0
})

// 饼图DOM引用
const pieChartRef = ref()
let pieChart = null

// 加载统计概览
const loadOverview = async () => {
  try {
    const res = await getStatsOverview()
    Object.assign(overview, res.data || {})
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

// 加载热门分类数据并初始化饼图
const loadCategoryChart = async () => {
  try {
    const res = await getCategoryTop5()
    const data = res.data || []
    // 复用已存在的图表实例，避免重复 init 造成内存泄漏
    pieChart = echarts.getInstanceByDom(pieChartRef.value) || echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        right: '5%',
        top: 'center'
      },
      series: [
        {
          name: '热门分类',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['40%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 8,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            formatter: '{b}\n{d}%'
          },
          data: data.map(item => ({
            name: item.name || item.category,
            value: item.count || item.value
          }))
        }
      ]
    })
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

onMounted(() => {
  loadOverview()
  loadCategoryChart()
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  // 销毁图表实例
  if (pieChart) {
    pieChart.dispose()
    pieChart = null
  }
  window.removeEventListener('resize', handleResize)
})

// 窗口大小变化时重新调整图表
const handleResize = () => {
  pieChart && pieChart.resize()
}
</script>

<style scoped lang="scss">
.dashboard-container {
  .stat-cards {
    margin-bottom: 20px;
  }

  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      flex-shrink: 0;
    }

    .stat-info {
      flex: 1;
    }
  }

  .chart-card {
    .chart-container {
      height: 400px;
    }
  }
}
</style>
