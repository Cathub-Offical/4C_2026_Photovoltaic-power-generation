<template>
  <div class="bigscreen">
    <!-- Header -->
    <div class="bs-header">
      <div class="bs-header-left">
        <svg-icon icon-class="logo" class="bs-logo" />
        <span class="bs-title">光能智界 - 数据总览</span>
      </div>
      <div class="bs-header-center">
        <span class="bs-time">{{ currentTime }}</span>
      </div>
      <div class="bs-header-right">
        <div class="bs-btn" @click="goHome">
          <el-icon><Monitor /></el-icon>
          <span>管理后台</span>
        </div>
        <div class="bs-btn bs-btn-danger" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出登录</span>
        </div>
      </div>
    </div>

    <!-- Body -->
    <div class="bs-body">
      <!-- Left Column -->
      <div class="bs-col bs-col-left">
        <!-- Stats Cards -->
        <div class="bs-panel">
          <div class="bs-panel-title">电站概览</div>
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-icon" style="color:#4facfe">
                <el-icon :size="20"><OfficeBuilding /></el-icon>
              </div>
              <div class="stat-value">{{ stationInfo.totalStations || 0 }}</div>
              <div class="stat-label">电站总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="color:#f093fb">
                <el-icon :size="20"><Cpu /></el-icon>
              </div>
              <div class="stat-value">{{ stationInfo.installedCapacity || '0' }}</div>
              <div class="stat-label">装机容量(MW)</div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="color:#67c23a">
                <el-icon :size="20"><CircleCheck /></el-icon>
              </div>
              <div class="stat-value">{{ stationInfo.onlineDevices || 0 }}</div>
              <div class="stat-label">在线设备</div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="color:#f56c6c">
                <el-icon :size="20"><WarningFilled /></el-icon>
              </div>
              <div class="stat-value warn">{{ stationInfo.offlineDevices || 0 }}</div>
              <div class="stat-label">离线设备</div>
            </div>
          </div>
        </div>

        <!-- Ranking -->
        <div class="bs-panel flex-1">
          <div class="bs-panel-title">电站发电效率排行</div>
          <div ref="rankChartRef" class="chart-container"></div>
        </div>

        <!-- Alarm Stats + Scrolling List -->
        <div class="bs-panel">
          <div class="bs-panel-title">
            报警统计
            <span class="alarm-badge" v-if="alarmInfo.todayAlarmCount > 0">{{ alarmInfo.todayAlarmCount }}</span>
          </div>
          <div class="alarm-row">
            <div class="alarm-item">
              <div class="alarm-val" style="color: #f56c6c">{{ alarmInfo.todayLevel1 ?? 0 }}</div>
              <div class="alarm-lbl">事故</div>
            </div>
            <div class="alarm-item">
              <div class="alarm-val" style="color: #e6a23c">{{ alarmInfo.todayLevel2 ?? 0 }}</div>
              <div class="alarm-lbl">严重</div>
            </div>
            <div class="alarm-item">
              <div class="alarm-val" style="color: #67c23a">{{ alarmInfo.todayLevel3 ?? 0 }}</div>
              <div class="alarm-lbl">普通</div>
            </div>
            <div class="alarm-item">
              <div class="alarm-val" style="color: #409eff">{{ alarmInfo.todayProcessed ?? 0 }}</div>
              <div class="alarm-lbl">已处理</div>
            </div>
          </div>
        </div>

        <!-- Recent Alarms Scroll -->
        <div class="bs-panel" style="flex:0.6; min-height:0;">
          <div class="bs-panel-title">最新报警</div>
          <div class="alarm-scroll">
            <div v-if="recentAlarms.length === 0" class="alarm-empty">暂无报警</div>
            <div v-for="(a, i) in recentAlarms" :key="i" class="alarm-scroll-item">
              <span class="alarm-dot" :style="{background: a.level===1?'#f56c6c':a.level===2?'#e6a23c':'#67c23a'}"></span>
              <span class="alarm-device">{{ a.deviceName || '--' }}</span>
              <span class="alarm-content">{{ a.alarmContent || '--' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Center Column -->
      <div class="bs-col bs-col-center">
        <!-- Top Stats Banner -->
        <div class="bs-banner">
          <div class="banner-item">
            <div class="banner-icon" style="background: transparent">
              <el-icon :size="28"><Sunny /></el-icon>
            </div>
            <div class="banner-info">
              <div class="banner-val">{{ stationInfo.cumulativeDay || '0' }}</div>
              <div class="banner-lbl">今日发电量</div>
            </div>
          </div>
          <div class="banner-item">
            <div class="banner-icon" style="background: transparent">
              <el-icon :size="28"><TrendCharts /></el-icon>
            </div>
            <div class="banner-info">
              <div class="banner-val">{{ stationInfo.cumulativeYear || '0' }}</div>
              <div class="banner-lbl">本年累计发电</div>
            </div>
          </div>
          <div class="banner-item">
            <div class="banner-icon" style="background: transparent">
              <el-icon :size="28"><Money /></el-icon>
            </div>
            <div class="banner-info">
              <div class="banner-val">{{ stationInfo.earningsDay || '0' }}</div>
              <div class="banner-lbl">今日收益</div>
            </div>
          </div>
          <div class="banner-item">
            <div class="banner-icon" style="background: transparent">
              <el-icon :size="28"><Aim /></el-icon>
            </div>
            <div class="banner-info">
              <div class="banner-val">{{ stationInfo.carbonEmissions || '0' }}</div>
              <div class="banner-lbl">碳减排(kg)</div>
            </div>
          </div>
        </div>

        <!-- Today's Power Curve -->
        <div class="bs-panel" style="flex: 0.6">
          <div class="bs-panel-title">今日发电功率曲线</div>
          <div ref="todayCurveRef" class="chart-container"></div>
        </div>

        <!-- Year Generation Trend -->
        <div class="bs-panel flex-1">
          <div class="bs-panel-title">年发电量趋势</div>
          <div ref="yearChartRef" class="chart-container"></div>
        </div>

        <!-- Loop Compare -->
        <div class="bs-panel" style="flex: 0.6">
          <div class="bs-panel-title">发电量环比分析</div>
          <div ref="loopChartRef" class="chart-container"></div>
        </div>
      </div>

      <!-- Right Column -->
      <div class="bs-col bs-col-right">
        <!-- Device Status Gauge -->
        <div class="bs-panel">
          <div class="bs-panel-title">设备运行状态</div>
          <div ref="deviceGaugeRef" style="height: 150px;"></div>
          <div class="device-legend">
            <span><i class="dot" style="background:#67c23a"></i>在线 {{ stationInfo.onlineDevices || 0 }}</span>
            <span><i class="dot" style="background:#f56c6c"></i>离线 {{ stationInfo.offlineDevices || 0 }}</span>
            <span><i class="dot" style="background:#e6a23c"></i>告警 {{ alarmInfo.todayAlarmCount || 0 }}</span>
          </div>
        </div>

        <!-- Pie -->
        <div class="bs-panel">
          <div class="bs-panel-title">时段发电占比</div>
          <div ref="pieChartRef" class="chart-container" style="height: 200px"></div>
        </div>

        <!-- Environmental Benefits -->
        <div class="bs-panel">
          <div class="bs-panel-title">环保效益</div>
          <div class="env-grid">
            <div class="env-item">
              <div class="env-icon"><svg-icon icon-class="tree" /></div>
              <div class="env-text">
                <div class="env-val">{{ treesPlanted }}</div>
                <div class="env-lbl">等效植树(棵)</div>
              </div>
            </div>
            <div class="env-item">
              <div class="env-icon"><svg-icon icon-class="lightning" /></div>
              <div class="env-text">
                <div class="env-val">{{ coalSaved }}</div>
                <div class="env-lbl">节约标煤(kg)</div>
              </div>
            </div>
            <div class="env-item">
              <div class="env-icon"><svg-icon icon-class="water-drop" /></div>
              <div class="env-text">
                <div class="env-val">{{ waterSaved }}</div>
                <div class="env-lbl">节约用水(L)</div>
              </div>
            </div>
          </div>
        </div>

        <!-- System Info -->
        <div class="bs-panel">
          <div class="bs-panel-title">系统信息</div>
          <div class="sys-info">
            <div class="sys-row">
              <span class="sys-lbl">数据更新</span>
              <span class="sys-val">{{ currentTime }}</span>
            </div>
            <div class="sys-row">
              <span class="sys-lbl">运行状态</span>
              <span class="sys-val" style="color: #67c23a">● 正常运行</span>
            </div>
            <div class="sys-row">
              <span class="sys-lbl">设备在线率</span>
              <span class="sys-val">{{ onlineRate }}%</span>
            </div>
            <div class="sys-row">
              <span class="sys-lbl">电站总数</span>
              <span class="sys-val">{{ stationInfo.totalStations || 0 }} 座</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Monitor, SwitchButton, Sunny, TrendCharts, Money, Aim, OfficeBuilding, Cpu, CircleCheck, WarningFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getSiteRank,
  getHomePowerStationInfo,
  getLoopCompareList,
  getPeriodGenerationPercentage,
  listHomeAlarm,
  homepageGeneration,
} from '@/api/analysis/home'
import { formatDateObj, formatChartTime } from '@/utils/index'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

// Time
const currentTime = ref('')
let timeTimer = null
function updateTime() {
  const now = new Date()
  const y = now.getFullYear()
  const M = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${y}-${M}-${d} ${h}:${m}:${s}`
}

// Data
const stationInfo = ref({})
const alarmInfo = ref({})
const recentAlarms = ref([])
const onlineRate = computed(() => {
  const total = (stationInfo.value.onlineDevices || 0) + (stationInfo.value.offlineDevices || 0)
  if (total === 0) return '0.0'
  return ((stationInfo.value.onlineDevices || 0) / total * 100).toFixed(1)
})

// Environmental benefits (derived from carbon emissions)
const treesPlanted = computed(() => {
  const co2 = parseFloat(stationInfo.value.carbonEmissions) || 0
  return Math.round(co2 / 18.3)
})
const coalSaved = computed(() => {
  const co2 = parseFloat(stationInfo.value.carbonEmissions) || 0
  return Math.round(co2 * 0.4)
})
const waterSaved = computed(() => {
  const co2 = parseFloat(stationInfo.value.carbonEmissions) || 0
  return Math.round(co2 * 3.7)
})

// Chart refs
const rankChartRef = ref(null)
const yearChartRef = ref(null)
const loopChartRef = ref(null)
const pieChartRef = ref(null)
const stationBarRef = ref(null)
const todayCurveRef = ref(null)
const deviceGaugeRef = ref(null)

let rankChart = null
let yearChart = null
let loopChart = null
let pieChart = null
let stationBarChart = null
let todayCurveChart = null
let deviceGaugeChart = null

// Dark chart theme
const darkText = '#c0c6d0'
const gridColor = 'rgba(255,255,255,0.06)'

// Navigation
function goHome() {
  router.push('/index')
}
function handleLogout() {
  userStore.logOut().then(() => {
    router.push('/login')
  })
}

// Fetch data
async function fetchAll() {
  try {
    const [infoRes, alarmRes] = await Promise.all([
      getHomePowerStationInfo(),
      listHomeAlarm(),
    ])
    stationInfo.value = infoRes.data || {}
    alarmInfo.value = alarmRes.data || {}
    recentAlarms.value = alarmRes.data?.recentAlarms || []
  } catch (e) {
    console.error('Failed to fetch overview data', e)
  }

  drawRankChart()
  drawYearChart()
  drawLoopChart()
  drawPieChart()
  drawStationBar()
  drawTodayCurve()
  drawDeviceGauge()
}

// Rank chart
async function drawRankChart() {
  try {
    const res = await getSiteRank()
    const data = (res.rows || res.data || []).slice(0, 10)
    const names = data.map(d => d.powerStationName || d.name)
    const values = data.map(d => parseFloat(d.sumValue) || 0)

    nextTick(() => {
      if (!rankChartRef.value) return
      rankChart = echarts.init(rankChartRef.value)
      rankChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: params => params[0].name + ': ' + params[0].value + '%' },
        grid: { top: 10, left: 10, right: 30, bottom: 10, containLabel: true },
        xAxis: { type: 'value', axisLabel: { color: darkText, fontSize: 10 }, splitLine: { lineStyle: { color: gridColor } } },
        yAxis: { type: 'category', data: names.reverse(), axisLabel: { color: darkText, fontSize: 10, width: 60, overflow: 'truncate' }, axisLine: { lineStyle: { color: gridColor } } },
        series: [{
          type: 'bar',
          data: values.reverse(),
          barMaxWidth: 16,
          itemStyle: {
            borderRadius: [0, 4, 4, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#1a5fb4' },
              { offset: 1, color: '#4facfe' },
            ]),
          },
        }],
      })
    })
  } catch (e) {
    console.error('rank chart error', e)
  }
}

// Year generation line
async function drawYearChart() {
  try {
    const res = await homepageGeneration()
    const data = res.data || []
    const times = data.map(d => d.time || d.currentTime || '')
    const vals = data.map(d => d.value || d.currentValue || 0)

    nextTick(() => {
      if (!yearChartRef.value) return
      yearChart = echarts.init(yearChartRef.value)
      yearChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { top: 30, left: 10, right: 20, bottom: 10, containLabel: true },
        xAxis: { type: 'category', data: times, axisLabel: { color: darkText, fontSize: 10 }, axisLine: { lineStyle: { color: gridColor } } },
        yAxis: { type: 'value', axisLabel: { color: darkText, fontSize: 10 }, splitLine: { lineStyle: { color: gridColor } } },
        series: [{
          name: '发电量(kWh)',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { width: 3, color: '#4facfe' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(79,172,254,0.35)' },
              { offset: 1, color: 'rgba(79,172,254,0.02)' },
            ]),
          },
          itemStyle: { color: '#4facfe' },
          data: vals,
        }],
      })
    })
  } catch (e) {
    console.error('year chart error', e)
  }
}

// Loop compare
async function drawLoopChart() {
  try {
    const dateObj = formatDateObj(new Date())
    const queryTime = dateObj.year + '-' + dateObj.month + '-' + dateObj.day
    const res = await getLoopCompareList({ queryTime, timeType: 'MONTH' })
    const data = res.data || []

    const times = data.map(d => formatChartTime('month', d.currentTime))
    const curVals = data.map(d => d.currentValue || 0)
    const oldVals = data.map(d => d.contrastValues || 0)

    nextTick(() => {
      if (!loopChartRef.value) return
      loopChart = echarts.init(loopChartRef.value)
      loopChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['本期', '同期'], textStyle: { color: darkText, fontSize: 11 }, top: 0 },
        grid: { top: 30, left: 10, right: 10, bottom: 10, containLabel: true },
        xAxis: { type: 'category', data: times, axisLabel: { color: darkText, fontSize: 10 }, axisLine: { lineStyle: { color: gridColor } } },
        yAxis: { type: 'value', axisLabel: { color: darkText, fontSize: 10 }, splitLine: { lineStyle: { color: gridColor } } },
        series: [
          { name: '本期', type: 'bar', barMaxWidth: 14, itemStyle: { color: '#4facfe', borderRadius: [3, 3, 0, 0] }, data: curVals },
          { name: '同期', type: 'bar', barMaxWidth: 14, itemStyle: { color: 'rgba(79,172,254,0.3)', borderRadius: [3, 3, 0, 0] }, data: oldVals },
        ],
      })
    })
  } catch (e) {
    console.error('loop chart error', e)
  }
}

// Pie chart
async function drawPieChart() {
  try {
    const dateObj = formatDateObj(new Date())
    const queryTime = dateObj.year + '-' + dateObj.month
    const res = await getPeriodGenerationPercentage({ queryTime, timeType: 'month' })
    const data = res.data || []

    nextTick(() => {
      if (!pieChartRef.value) return
      pieChart = echarts.init(pieChartRef.value)
      pieChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, textStyle: { color: darkText, fontSize: 10 }, itemWidth: 12, itemHeight: 8 },
        color: ['#f56c6c', '#e6a23c', '#67c23a', '#409eff', '#9b59b6'],
        series: [{
          type: 'pie',
          radius: ['35%', '60%'],
          center: ['50%', '42%'],
          itemStyle: { borderRadius: 4, borderColor: '#0d1b2a', borderWidth: 2 },
          label: { color: darkText, fontSize: 10, formatter: '{b}\n{d}%' },
          data: data.map(d => ({ name: d.name || d.period, value: d.value || d.generation || 0 })),
        }],
      })
    })
  } catch (e) {
    console.error('pie chart error', e)
  }
}

// Station generation bar
async function drawStationBar() {
  try {
    const res = await getSiteRank()
    const data = (res.rows || res.data || []).slice(0, 8)
    const names = data.map(d => d.powerStationName || d.name || '')
    const values = data.map(d => d.totalGeneration || 0)

    nextTick(() => {
      if (!stationBarRef.value) return
      stationBarChart = echarts.init(stationBarRef.value)
      stationBarChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { top: 10, left: 10, right: 20, bottom: 10, containLabel: true },
        xAxis: { type: 'category', data: names, axisLabel: { color: darkText, fontSize: 9, rotate: 30, width: 50, overflow: 'truncate' }, axisLine: { lineStyle: { color: gridColor } } },
        yAxis: { type: 'value', axisLabel: { color: darkText, fontSize: 10 }, splitLine: { lineStyle: { color: gridColor } } },
        series: [{
          type: 'bar',
          barMaxWidth: 20,
          itemStyle: {
            borderRadius: [4, 4, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#43e97b' },
              { offset: 1, color: '#38f9d7' },
            ]),
          },
          data: values,
        }],
      })
    })
  } catch (e) {
    console.error('station bar error', e)
  }
}

// Today's power curve (simulated hourly data for today)
function drawTodayCurve() {
  nextTick(() => {
    if (!todayCurveRef.value) return
    todayCurveChart = echarts.init(todayCurveRef.value)
    const hours = []
    const power = []
    const irradiance = []
    const nowHour = new Date().getHours()
    for (let h = 0; h < 24; h++) {
      hours.push(h + ':00')
      if (h <= nowHour) {
        let p = 0, ir = 0
        if (h >= 6 && h <= 18) {
          const x = (h - 12) / 6
          const base = Math.max(0, 1 - x * x)
          p = base * (850 + Math.random() * 150)
          ir = base * (950 + Math.random() * 100)
        }
        power.push(Math.round(p))
        irradiance.push(Math.round(ir))
      } else {
        power.push(null)
        irradiance.push(null)
      }
    }
    todayCurveChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['功率(kW)', '辐照度(W/m²)'], textStyle: { color: darkText, fontSize: 10 }, top: 0, right: 10 },
      grid: { top: 28, left: 10, right: 10, bottom: 10, containLabel: true },
      xAxis: { type: 'category', data: hours, axisLabel: { color: darkText, fontSize: 9 }, axisLine: { lineStyle: { color: gridColor } } },
      yAxis: [
        { type: 'value', axisLabel: { color: darkText, fontSize: 9 }, splitLine: { lineStyle: { color: gridColor } } },
        { type: 'value', axisLabel: { color: darkText, fontSize: 9 }, splitLine: { show: false } },
      ],
      series: [
        {
          name: '功率(kW)', type: 'line', smooth: true, symbol: 'none',
          lineStyle: { width: 2, color: '#f5a623' },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(245,166,35,0.3)' }, { offset: 1, color: 'rgba(245,166,35,0.02)' }]) },
          itemStyle: { color: '#f5a623' }, data: power,
        },
        {
          name: '辐照度(W/m²)', type: 'line', smooth: true, symbol: 'none', yAxisIndex: 1,
          lineStyle: { width: 2, color: '#67c23a', type: 'dashed' },
          itemStyle: { color: '#67c23a' }, data: irradiance,
        },
      ],
    })
  })
}

// Device status gauge
function drawDeviceGauge() {
  nextTick(() => {
    if (!deviceGaugeRef.value) return
    deviceGaugeChart = echarts.init(deviceGaugeRef.value)
    const online = stationInfo.value.onlineDevices || 0
    const offline = stationInfo.value.offlineDevices || 0
    const total = online + offline || 1
    const rate = (online / total * 100).toFixed(1)
    deviceGaugeChart.setOption({
      series: [{
        type: 'gauge',
        center: ['50%', '65%'],
        radius: '95%',
        startAngle: 200,
        endAngle: -20,
        min: 0,
        max: 100,
        splitNumber: 10,
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#f56c6c' }, { offset: 0.5, color: '#e6a23c' }, { offset: 1, color: '#67c23a' }]) },
        progress: { show: true, width: 12, roundCap: true },
        pointer: { show: false },
        axisLine: { lineStyle: { width: 12, color: [[1, 'rgba(255,255,255,0.08)']] }, roundCap: true },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        title: { show: false },
        detail: {
          fontSize: 22, fontWeight: 'bold', fontFamily: 'DIN, Consolas, monospace',
          color: '#67c23a', offsetCenter: [0, '0%'],
          formatter: val => val + '%',
        },
        data: [{ value: parseFloat(rate) }],
      }],
    })
  })
}

// Resize
function handleResize() {
  rankChart?.resize()
  yearChart?.resize()
  loopChart?.resize()
  pieChart?.resize()
  stationBarChart?.resize()
  todayCurveChart?.resize()
  deviceGaugeChart?.resize()
}

onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  fetchAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  clearInterval(timeTimer)
  window.removeEventListener('resize', handleResize)
  rankChart?.dispose()
  yearChart?.dispose()
  loopChart?.dispose()
  pieChart?.dispose()
  stationBarChart?.dispose()
  todayCurveChart?.dispose()
  deviceGaugeChart?.dispose()
})
</script>

<style scoped>
.bigscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #0a1628;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Microsoft YaHei', sans-serif;
  color: #c0c6d0;
  z-index: 9999;
}

/* Header */
.bs-header {
  height: 56px;
  background: linear-gradient(90deg, rgba(10,22,40,0.95), rgba(13,27,42,0.9));
  border-bottom: 1px solid rgba(79,172,254,0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}
.bs-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bs-logo {
  width: 32px;
  height: 32px;
}
.bs-title {
  font-size: 20px;
  font-weight: 600;
  background: linear-gradient(90deg, #4facfe, #00f2fe);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 2px;
}
.bs-header-center {
  font-size: 16px;
  color: rgba(255,255,255,0.6);
  letter-spacing: 1px;
}
.bs-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bs-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 6px;
  background: rgba(79,172,254,0.15);
  border: 1px solid rgba(79,172,254,0.3);
  color: #4facfe;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}
.bs-btn:hover {
  background: rgba(79,172,254,0.3);
}
.bs-btn-danger {
  background: rgba(245,108,108,0.15);
  border-color: rgba(245,108,108,0.3);
  color: #f56c6c;
}
.bs-btn-danger:hover {
  background: rgba(245,108,108,0.3);
}

/* Body */
.bs-body {
  flex: 1;
  display: flex;
  gap: 12px;
  padding: 12px;
  min-height: 0;
}
.bs-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}
.bs-col-left {
  width: 22%;
}
.bs-col-center {
  flex: 1;
}
.bs-col-right {
  width: 22%;
}

/* Panel */
.bs-panel {
  background: rgba(13,27,42,0.8);
  border: 1px solid rgba(79,172,254,0.12);
  border-radius: 8px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.bs-panel.flex-1 {
  flex: 1;
}
.bs-panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
  margin-bottom: 10px;
  padding-left: 10px;
  border-left: 3px solid #4facfe;
  display: flex;
  align-items: center;
  gap: 8px;
}
.chart-container {
  flex: 1;
  min-height: 0;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.stat-card {
  background: rgba(79,172,254,0.06);
  border: 1px solid rgba(79,172,254,0.1);
  border-radius: 8px;
  padding: 12px 10px;
  text-align: center;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #4facfe;
  font-family: 'DIN', 'Consolas', monospace;
}
.stat-value.warn {
  color: #f56c6c;
}
.stat-label {
  font-size: 11px;
  color: #8892a4;
  margin-top: 4px;
}

/* Alarm */
.alarm-badge {
  background: #f56c6c;
  color: #fff;
  border-radius: 10px;
  padding: 1px 8px;
  font-size: 11px;
  font-weight: 600;
}
.alarm-row {
  display: flex;
  justify-content: space-around;
  padding: 8px 0;
}
.alarm-item {
  text-align: center;
}
.alarm-val {
  font-size: 24px;
  font-weight: 700;
  font-family: 'DIN', 'Consolas', monospace;
}
.alarm-lbl {
  font-size: 11px;
  color: #8892a4;
  margin-top: 4px;
}

/* Banner */
.bs-banner {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}
.banner-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(13,27,42,0.8);
  border: 1px solid rgba(79,172,254,0.12);
  border-radius: 8px;
  padding: 14px 16px;
}
.banner-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.banner-info {
  min-width: 0;
}
.banner-val {
  font-size: 18px;
  font-weight: 700;
  color: #e2e8f0;
  font-family: 'DIN', 'Consolas', monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.banner-lbl {
  font-size: 11px;
  color: #8892a4;
  margin-top: 2px;
}

/* System Info */
.sys-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 4px 0;
}
.sys-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  border-radius: 4px;
  background: rgba(79,172,254,0.04);
}
.sys-lbl {
  font-size: 12px;
  color: #8892a4;
}
.sys-val {
  font-size: 12px;
  color: #e2e8f0;
  font-family: 'Consolas', monospace;
}

/* Stat Icon */
.stat-icon {
  margin-bottom: 4px;
}

/* Alarm Scroll */
.alarm-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
.alarm-scroll::-webkit-scrollbar {
  width: 3px;
}
.alarm-scroll::-webkit-scrollbar-thumb {
  background: rgba(79,172,254,0.2);
  border-radius: 2px;
}
.alarm-scroll-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 4px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  font-size: 11px;
}
.alarm-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.alarm-device {
  color: #e2e8f0;
  white-space: nowrap;
  flex-shrink: 0;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.alarm-content {
  color: #8892a4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.alarm-empty {
  text-align: center;
  color: #8892a4;
  padding: 16px;
  font-size: 12px;
}

/* Device Legend */
.device-legend {
  display: flex;
  justify-content: center;
  gap: 16px;
  font-size: 11px;
  color: #8892a4;
  margin-top: 4px;
}
.device-legend .dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}

/* Environmental Benefits */
.env-grid {
  display: flex;
  justify-content: space-between;
  padding: 8px 4px;
}
.env-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 8px;
}
.env-item + .env-item {
  border-left: 1px solid rgba(255,255,255,0.06);
}
.env-icon {
  width: 22px;
  height: 22px;
  color: #43e97b;
  flex-shrink: 0;
}
.env-icon .svg-icon {
  width: 22px;
  height: 22px;
}
.env-text {
  min-width: 0;
}
.env-val {
  font-size: 16px;
  font-weight: 700;
  color: #43e97b;
  font-family: 'DIN', 'Consolas', monospace;
  line-height: 1.3;
}
.env-lbl {
  font-size: 10px;
  color: #8892a4;
  white-space: nowrap;
}

/* Time decoration */
.bs-time {
  font-family: 'DIN', 'Consolas', monospace;
  font-size: 18px;
  color: rgba(79,172,254,0.8);
  letter-spacing: 2px;
}
</style>
