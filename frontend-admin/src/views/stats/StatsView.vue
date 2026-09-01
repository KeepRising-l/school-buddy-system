<template>
  <div class="stats-container">
    <el-tabs v-model="activeTab">
      <!-- Tab 1: 趋势图表 -->
      <el-tab-pane label="趋势图表" name="trend">
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
      </el-tab-pane>

      <!-- Tab 2: 留存与漏斗 -->
      <el-tab-pane label="留存与漏斗" name="retention">
        <el-row :gutter="20">
          <!-- 留存折线图 -->
          <el-col :span="14">
            <el-card shadow="hover" class="chart-card">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <span>用户留存分析（近30天）</span>
                  <el-button size="small" @click="handleExportStats('retention')">导出CSV</el-button>
                </div>
              </template>
              <div ref="retentionRef" class="chart-box" />
            </el-card>
          </el-col>

          <!-- 留存明细表 -->
          <el-col :span="10">
            <el-card shadow="hover" class="chart-card">
              <template #header>
                <span>留存率明细</span>
              </template>
              <el-table :data="retentionTableData" border size="small" max-height="350">
                <el-table-column prop="date" label="日期" width="110" />
                <el-table-column prop="newUsers" label="新增" width="70" align="center" />
                <el-table-column prop="d1Rate" label="次日留存" width="90" align="center">
                  <template #default="{ row }">{{ row.d1Rate }}%</template>
                </el-table-column>
                <el-table-column prop="d7Rate" label="7日留存" width="90" align="center">
                  <template #default="{ row }">{{ row.d7Rate }}%</template>
                </el-table-column>
                <el-table-column prop="d30Rate" label="30日留存" width="90" align="center">
                  <template #default="{ row }">{{ row.d30Rate }}%</template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px">
          <!-- 漏斗图 -->
          <el-col :span="12">
            <el-card shadow="hover" class="chart-card">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <span>转化漏斗分析（近30天）</span>
                  <el-button size="small" @click="handleExportStats('funnel')">导出CSV</el-button>
                </div>
              </template>
              <div ref="funnelRef" class="chart-box" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import {
  getPostTrend, getSuccessTrend, getCategoryTop5, getCreditDistribution,
  getUserGrowthTrend, getActiveTrend, getRetention, getFunnel, exportStats
} from '@/api/admin'
import { ElMessage } from 'element-plus'
import { downloadFromBlob } from '@/utils/download'

// ===== Tab 状态 =====
const activeTab = ref('trend')

// ===== 趋势图表 DOM 引用 =====
const postTrendRef = ref()
const successTrendRef = ref()
const categoryTop5Ref = ref()
const creditDistRef = ref()
const userGrowthRef = ref()
const activeTrendRef = ref()

// ===== 留存 & 漏斗 DOM 引用 =====
const retentionRef = ref()
const funnelRef = ref()
const retentionTableData = ref([])

// ===== 图表实例 =====
let postTrendChart = null
let successTrendChart = null
let categoryTop5Chart = null
let creditDistChart = null
let userGrowthChart = null
let activeTrendChart = null
let retentionChart = null
let funnelChart = null

// ===== 通用折线图配置 =====
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

// ===== 趋势图表加载 =====

const loadPostTrend = async () => {
  try {
    const res = await getPostTrend({ days: 30 })
    const data = res.data || []
    postTrendChart = echarts.init(postTrendRef.value)
    postTrendChart.setOption(createLineOption('发帖量', data))
  } catch (e) {}
}

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
  } catch (e) {}
}

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
  } catch (e) {}
}

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
            color: new echarts.graphic.LinearGradient(0, 1, 0, 0, [
              { offset: 0, color: '#e6a23c' },
              { offset: 1, color: '#f5d89a' }
            ]),
            borderRadius: [4, 4, 0, 0]
          }
        }
      ]
    })
  } catch (e) {}
}

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
  } catch (e) {}
}

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
  } catch (e) {}
}

// ===== 留存 & 漏斗图表加载 =====

const loadRetention = async () => {
  try {
    const res = await getRetention({ days: 30 })
    const data = res.data || []
    retentionTableData.value = data

    retentionChart = echarts.init(retentionRef.value)
    retentionChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          let html = params[0].axisValue + '<br/>'
          params.forEach(p => {
            html += `${p.marker} ${p.seriesName}: <b>${p.value}%</b><br/>`
          })
          return html
        }
      },
      legend: {
        data: ['次日留存', '7日留存', '30日留存']
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
        max: 100,
        axisLabel: { formatter: '{value}%' }
      },
      series: [
        {
          name: '次日留存',
          type: 'line',
          smooth: true,
          data: data.map(item => item.d1Rate),
          itemStyle: { color: '#10a37f' }
        },
        {
          name: '7日留存',
          type: 'line',
          smooth: true,
          data: data.map(item => item.d7Rate),
          itemStyle: { color: '#0ea5e9' }
        },
        {
          name: '30日留存',
          type: 'line',
          smooth: true,
          data: data.map(item => item.d30Rate),
          itemStyle: { color: '#9333ea' }
        }
      ]
    })
  } catch (e) {}
}

const loadFunnel = async () => {
  try {
    const res = await getFunnel({ days: 30 })
    const data = res.data || []

    const maxCount = Math.max(...data.map(d => d.count), 1)

    funnelChart = echarts.init(funnelRef.value)
    funnelChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params) => {
          const p = params[0]
          const item = data[p.dataIndex]
          return `${item.stage}<br/>数量: <b>${item.count}</b><br/>转化率: <b>${item.rate}%</b>`
        }
      },
      grid: {
        left: '15%',
        right: '15%',
        top: '5%',
        bottom: '5%',
        containLabel: true
      },
      xAxis: {
        type: 'value',
        max: maxCount
      },
      yAxis: {
        type: 'category',
        data: data.map(d => d.stage),
        inverse: true
      },
      series: [{
        type: 'bar',
        barWidth: '60%',
        data: data.map((d, index) => ({
          value: d.count,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: ['#10a37f', '#0ea5e9', '#9333ea', '#f5a623'][index] || '#10a37f' },
              { offset: 1, color: ['#34d399', '#38bdf8', '#c084fc', '#fbbf24'][index] || '#34d399' }
            ]),
            borderRadius: [0, 4, 4, 0]
          }
        })),
        label: {
          show: true,
          position: 'right',
          formatter: (params) => {
            const item = data[params.dataIndex]
            return `${item.count}  (${item.rate}%)`
          }
        }
      }]
    })
  } catch (e) {}
}

// ===== 统计数据导出 =====
const handleExportStats = async (type) => {
  try {
    const res = await exportStats({ days: 30, type })
    downloadFromBlob(res, `stats_${type}.csv`)
    ElMessage.success('导出成功')
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

// ===== Tab 切换懒加载 =====
watch(activeTab, (val) => {
  if (val === 'retention') {
    nextTick(() => {
      loadRetention()
      loadFunnel()
    })
  }
})

// ===== 窗口大小变化时重新调整图表 =====
const handleResize = () => {
  postTrendChart && postTrendChart.resize()
  successTrendChart && successTrendChart.resize()
  categoryTop5Chart && categoryTop5Chart.resize()
  creditDistChart && creditDistChart.resize()
  userGrowthChart && userGrowthChart.resize()
  activeTrendChart && activeTrendChart.resize()
  retentionChart && retentionChart.resize()
  funnelChart && funnelChart.resize()
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
  retentionChart && retentionChart.dispose()
  funnelChart && funnelChart.dispose()
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
