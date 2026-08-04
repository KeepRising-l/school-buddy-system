<template>
  <div class="stats-container">
    <el-row :gutter="20">
      <!-- 发帖量趋势 -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>发帖量趋势（近30天）</span>
          </template>
          <div ref="postTrendRef" class="chart-box" />
        </el-card>
      </el-col>

      <!-- 成功组局数趋势 -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>成功组局数趋势（近30天）</span>
          </template>
          <div ref="successTrendRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 热门分类 Top5 -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>热门分类 Top5</span>
          </template>
          <div ref="categoryTop5Ref" class="chart-box" />
        </el-card>
      </el-col>

      <!-- 信誉分分布 -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>信誉分分布</span>
          </template>
          <div ref="creditDistRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 用户增长趋势 -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>用户增长趋势（近30天）</span>
          </template>
          <div ref="userGrowthRef" class="chart-box" />
        </el-card>
      </el-col>

      <!-- 活跃用户趋势 -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>活跃用户趋势（近30天）</span>
          </template>
          <div ref="activeTrendRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { getPostTrend, getSuccessTrend, getCategoryTop5, getCreditDistribution, getUserGrowthTrend, getActiveTrend } from '@/api/admin'

// 图表DOM引用
const postTrendRef = ref()
const successTrendRef = ref()
const categoryTop5Ref = ref()
const creditDistRef = ref()
const userGrowthRef = ref()
const activeTrendRef = ref()

// 图表实例
let postTrendChart = null
let successTrendChart = null
let categoryTop5Chart = null
let creditDistChart = null
let userGrowthChart = null
let activeTrendChart = null

// 通用折线图配置
const createLineOption = (title, data) => ({
  tooltip: {
    trigger: 'axis'
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: data.map(item => item.date)
  },
  yAxis: {
    type: 'value',
    minInterval: 1
  },
  series: [
    {
      name: title,
      type: 'line',
      smooth: true,
      areaStyle: {
        opacity: 0.15
      },
      data: data.map(item => item.count || item.value),
      itemStyle: {
        color: '#10a37f'
      }
    }
  ]
})

// 加载发帖量趋势
const loadPostTrend = async () => {
  try {
    const res = await getPostTrend({ days: 30 })
    const data = res.data || []
    postTrendChart = echarts.init(postTrendRef.value)
    postTrendChart.setOption(createLineOption('发帖量', data))
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

// 加载成功组局数趋势
const loadSuccessTrend = async () => {
  try {
    const res = await getSuccessTrend({ days: 30 })
    const data = res.data || []
    successTrendChart = echarts.init(successTrendRef.value)
    successTrendChart.setOption({
      ...createLineOption('成功组局数', data),
      series: [{
        ...createLineOption('成功组局数', data).series[0],
        itemStyle: { color: '#0ea5e9' },
        areaStyle: { opacity: 0.15 }
      }]
    })
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

// 加载热门分类饼图
const loadCategoryTop5 = async () => {
  try {
    const res = await getCategoryTop5()
    const data = res.data || []
    categoryTop5Chart = echarts.init(categoryTop5Ref.value)
    categoryTop5Chart.setOption({
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

// 加载信誉分分布柱状图
const loadCreditDistribution = async () => {
  try {
    const res = await getCreditDistribution()
    const data = res.data || []
    creditDistChart = echarts.init(creditDistRef.value)
    creditDistChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: data.map(item => item.range || item.label)
      },
      yAxis: {
        type: 'value',
        minInterval: 1
      },
      series: [
        {
          name: '用户数',
          type: 'bar',
          barWidth: '50%',
          data: data.map(item => item.count || item.value),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#e6a23c' },
              { offset: 1, color: '#f5d89a' }
            ]),
            borderRadius: [4, 4, 0, 0]
          }
        }
      ]
    })
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

// 加载用户增长趋势
const loadUserGrowth = async () => {
  try {
    const res = await getUserGrowthTrend({ days: 30 })
    const data = res.data || []
    userGrowthChart = echarts.init(userGrowthRef.value)
    userGrowthChart.setOption({
      ...createLineOption('累计用户数', data),
      series: [{
        ...createLineOption('累计用户数', data).series[0],
        name: '累计用户数',
        itemStyle: { color: '#9333ea' },
        areaStyle: { opacity: 0.15 }
      }]
    })
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

// 加载活跃用户趋势
const loadActiveTrend = async () => {
  try {
    const res = await getActiveTrend({ days: 30 })
    const data = res.data || []
    activeTrendChart = echarts.init(activeTrendRef.value)
    activeTrendChart.setOption({
      ...createLineOption('活跃用户数', data),
      series: [{
        ...createLineOption('活跃用户数', data).series[0],
        name: '活跃用户数',
        itemStyle: { color: '#f5a623' },
        areaStyle: { opacity: 0.15 }
      }]
    })
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

// 窗口大小变化时重新调整图表
const handleResize = () => {
  postTrendChart && postTrendChart.resize()
  successTrendChart && successTrendChart.resize()
  categoryTop5Chart && categoryTop5Chart.resize()
  creditDistChart && creditDistChart.resize()
  userGrowthChart && userGrowthChart.resize()
  activeTrendChart && activeTrendChart.resize()
}

onMounted(() => {
  loadPostTrend()
  loadSuccessTrend()
  loadCategoryTop5()
  loadCreditDistribution()
  loadUserGrowth()
  loadActiveTrend()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  // 销毁所有图表实例
  postTrendChart && postTrendChart.dispose()
  successTrendChart && successTrendChart.dispose()
  categoryTop5Chart && categoryTop5Chart.dispose()
  creditDistChart && creditDistChart.dispose()
  userGrowthChart && userGrowthChart.dispose()
  activeTrendChart && activeTrendChart.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.stats-container {
  .chart-card {
    .chart-box {
      height: 350px;
    }
  }
}
</style>
