<template>
  <div class="app-container">
    <div class="home-page">
      <div class="home-left" :style="{height: (containerHeight - 24) + 'px'}">
        <!-- 尖峰平谷 -->
        <div style="margin-top: 0px">
          <div class="header-default">
            时段发电占比
            <span>单位:kWh</span>
          </div>
          <div class="date-wrap" style="width: 120px">
            <div
              :class="peaksTimeType == 'month' ? 'switch-date-active' : 'switch-date'"
              @click="switchPeaksDate('month')"
            >
              本月
            </div>
            <div :class="peaksTimeType == 'day' ? 'switch-date-active' : 'switch-date'" @click="switchPeaksDate('day')">
              本日
            </div>
          </div>
          <div ref="ringChartRef" style="height: 280px; margin-top: 32px"></div>
        </div>
        <!--  -->
        <div style="margin-top: 16px; flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: hidden;">
          <div class="header-default">
            <span>当日发电效率排行</span>
          </div>
          <div class="site-rank-list">
            <div class="flex-start rank-item rank-item-zero">
              <div style="width: 20%">排名</div>
              <div style="width: 40%">电站名称</div>
              <div style="width: 40%">发电效率</div>
            </div>
            <div v-for="(item, index) in rankList" :key="index" class="flex-start rank-item">
              <div style="width: 20%">
                <img v-if="index < 3" :src="getImageUrl(`no-${index + 1}`)" alt="" />
                <div v-else>{{ index + 1 }}</div>
              </div>
              <div @click="toSite(item)" title="查看详情" style="width: 40%; cursor: pointer" class="site">
                {{ item.powerStationName }}
              </div>
              <div style="width: 40%">{{ item.sumValue }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="home-center">
        <!-- 统计 6 个 -->
        <div class="sum-wrapper">
          <div
            v-for="(item, index) in statisticList"
            class="sum-item"
            :class="{ bg1: index === 1 || index === 3, bg2: index === 2 }"
          >
            <div class="img">
              <img :src="getImageUrl(`icon-${index + 1}`)" alt="" />
            </div>
            <div class="">
              <div class="value">{{ item.value }}</div>
              <div v-show="!sidebarOpened" class="label">{{ item.label }}</div>
            </div>
          </div>
        </div>
        <!-- map -->
        <div class="map-wrap">
          <aMap ref="mapRef" :markers="mapMarkerList" :height="containerHeight - 390" />
        </div>
        <!-- 全厂发电量环比 -->
        <div style="margin-top: 12px">
          <div class="header-default">
            全厂发电量环比
            <span>单位:kWh</span>
          </div>
          <div class="date-wrap" style="width: 180px">
            <div :class="loopTimeType == 'year' ? 'switch-date-active' : 'switch-date'" @click="switchLoopDate('year')">
              本年
            </div>
            <div
              :class="loopTimeType == 'month' ? 'switch-date-active' : 'switch-date'"
              @click="switchLoopDate('month')"
            >
              本月
            </div>
            <div :class="loopTimeType == 'day' ? 'switch-date-active' : 'switch-date'" @click="switchLoopDate('day')">
              本日
            </div>
          </div>
          <div ref="loopChartRef" style="height: 380px; width: 100%; margin-top: 24px"></div>
        </div>
      </div>
      <div class="home-right" :style="{height: (containerHeight - 24) + 'px'}">
        <div>
          <div class="header-default">
            年发电量
            <span>单位:kWh</span>
          </div>
          <div ref="lineChartRef" style="height: 280px"></div>
        </div>

        <!-- 报警 -->
        <div style="margin-top: 16px">
          <div class="header-default">今日报警概览</div>
          <div class="alarm-wrapper">
            <div class="flex-start" style="height: 70px; cursor: pointer" @click="toAlarm">
              <img class="total-img" src="@/assets/images/icon-alarm.png" alt="" />
              <div class="total-view">
                <div class="value">{{ alarmInfo.todayAlarmCount ?? 0 }}</div>
                <div class="label">全部报警</div>
              </div>
            </div>
            <div class="alarm-list flex-start">
              <div class="item">
                <div class="value">{{ alarmInfo.todayUnprocessed ?? 0 }}</div>
                <div class="label">未处理</div>
              </div>
              <div class="item">
                <div class="value" style="color: #2085d2">{{ alarmInfo.todayProcessed ?? 0 }}</div>
                <div class="label">已处理</div>
              </div>
            </div>
            <div class="alarm-list flex-start">
              <div class="item">
                <div class="value" style="color: #d22041">{{ alarmInfo.todayLevel1 ?? 0 }}</div>
                <div class="label">事故</div>
              </div>
              <div class="item">
                <div class="value" style="color: #deb650">{{ alarmInfo.todayLevel2 ?? 0 }}</div>
                <div class="label">严重</div>
              </div>
              <div class="item">
                <div class="value" style="color: #20d2a3">{{ alarmInfo.todayLevel3 ?? 0 }}</div>
                <div class="label">普通</div>
              </div>
            </div>
          </div>
        </div>
        <!-- 最新报警列表 -->
        <div style="margin-top: 16px; flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: hidden;">
          <div class="header-default">最新报警记录</div>
          <div class="recent-alarm-list">
            <div v-if="recentAlarms.length === 0" class="recent-alarm-empty">暂无报警记录</div>
            <div
              v-for="(item, index) in recentAlarms"
              :key="index"
              class="recent-alarm-item"
              @click="toAlarm"
            >
              <div class="recent-alarm-top">
                <span
                  class="recent-alarm-level"
                  :style="{
                    background: item.level === 1 ? '#d22041' : item.level === 2 ? '#deb650' : '#20d2a3'
                  }"
                >
                  {{ item.level === 1 ? '事故' : item.level === 2 ? '严重' : '普通' }}
                </span>
                <span class="recent-alarm-device">{{ item.deviceName || '--' }}</span>
                <span
                  class="recent-alarm-status"
                  :style="{ color: item.status === '2' ? '#20d2a3' : '#f55957' }"
                >
                  {{ item.status === '2' ? '已处理' : '未处理' }}
                </span>
              </div>
              <div class="recent-alarm-content">{{ item.alarmContent || '--' }}</div>
              <div class="recent-alarm-time">{{ item.alarmTime }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="Index">
import { formatDate, formatDateObj, formatChartTime } from "@/utils/index"
import { getImageUrl } from "@/utils/index"
import * as echarts from "echarts"
import aMap from "../components/Map/amap-index"
import { ref, computed, nextTick, onMounted, onBeforeUnmount, onUnmounted, onActivated, onDeactivated, watch } from "vue"
import useAppStore from "@/store/modules/app"
// import { useRouter } from "vue-router"
import { resize, destroyListener } from "@/utils/resize"
import {
  getSiteRank,
  getPeriodGenerationPercentage,
  getLoopCompareList,
  getHomePowerStationInfo,
  listHomeAlarm,
  homepageGeneration,
} from "@/api/analysis/home"

const router = useRouter()
const { proxy } = getCurrentInstance()
const containerHeight = ref(window.innerHeight - 90)
const appStore = useAppStore()
const sidebarOpened = computed(() => appStore.sidebar.opened)

// echarts-ring
const ringChartRef = ref(null)
const ringChartData = ref([])
let echartsInstance = null
function drawRingChart() {
  nextTick(() => {
    echartsInstance = echarts.init(ringChartRef.value, "macarons")
    echartsInstance.setOption({
      tooltip: {
        trigger: "item",
        formatter: "{b}: {c} / {d}%",
      },
      legend: {
        top: 16,
        bottom: 16,
        left: "center",
      },
      series: [
        {
          name: "",
          type: "pie",
          top: 52,
          radius: ["30%", "60%"],
          itemStyle: {
            borderRadius: 4,
            borderColor: "#fff",
            borderWidth: 2,
          },
          label: {
            // show: true,
            // position: "center",
            formatter: "{b}\n{c}",
          },
          color: ["#e7534f", "#eec048", "#56C6C8", "#6590f2", "#3A71A8"],
          data: ringChartData.value,
        },
      ],
    })
  })
}

// echarts-line -年发电量
const lineChartRef = ref(null)
const lineTime = ref([])
const lineYData = ref([])
const loopTimeType = ref("month")
let lineInstance = null
function drawLineChart() {
  nextTick(() => {
    lineInstance = echarts.init(lineChartRef.value, "macarons")
    lineInstance.setOption({
      title: {
        text: "",
      },
      tooltip: {
        trigger: "axis",
      },
      legend: false,
      grid: {
        top: 20,
        left: "1%",
        right: 16,
        bottom: 8,
        containLabel: true,
      },
      // toolbox: {
      //   feature: {
      //     saveAsImage: {},
      //     dataView: {},
      //     restore: {},
      //     magicType: {
      //       type: ["line", "bar", "stack"],
      //     },
      //   },
      // },
      xAxis: {
        type: "category",
        // boundaryGap: false,
        data: lineTime.value,
      },
      yAxis: {
        type: "value",
      },
      series: [
        {
          name: "发电量",
          type: "line",
          smooth: true,
          label: {
            show: true,
            // position: "top",
          },
          // barWidth: 20,
          // stack: "Total",
          color: "#2085d2",
          data: lineYData.value,
        },
      ],
    })
  })
}

// echart-环比
function switchLoopDate(type) {
  loopTimeType.value = type
  getLoopData()
}
function getLoopData() {
  let dateObj = formatDateObj(new Date())
  let queryTime = ""
  queryTime = dateObj.year + "-" + dateObj.month + "-" + dateObj.day
  // if (peaksTimeType.value == "day") {
  //   queryTime = dateObj.year + "-" + dateObj.month + "-" + dateObj.day
  // } else if (peaksTimeType.value == "month") {
  //   queryTime = dateObj.year + "-" + dateObj.month
  // } else {
  //   queryTime = dateObj.year
  // }
  getLoopCompareList({
    queryTime,
    timeType: loopTimeType.value.toUpperCase(),
  }).then((res) => {
    loopData.value = res.data.map((item) => {
      return {
        time: formatChartTime(loopTimeType.value, item.currentTime),
        value: item.currentValue,
        oldValue: item.contrastValues,
        ratio: item.ratio,
      }
    })
    drawLoopChart()
  })
}
const loopChartRef = ref(null)
let loopChartInstance = null
const loopData = ref([])
function drawLoopChart() {
  loopChartInstance = echarts.init(loopChartRef.value, "macarons")
  // 监听 echarts 的 finished 事件
  let resizeObserverAdded = false
  loopChartInstance.on("finished", function () {
    if (!resizeObserverAdded) {
      resize(loopChartInstance, loopChartRef.value)
    }
    resizeObserverAdded = true
  })
  let option = {
    title: {},
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "shadow",
        label: {
          show: true,
        },
      },
    },
    calculable: true,
    legend: {
      data: ["本期值", "同期值", `环比值`],
      itemGap: 5,
      // align: "center",
      top: 4,
    },
    grid: {
      top: 36,
      left: "1%",
      right: "1%",
      bottom: 16,
      containLabel: true,
    },
    xAxis: [
      {
        name: "",
        type: "category",
        data: loopData.value.map((item) => {
          return item.time
        }),
      },
    ],
    yAxis: [
      {
        type: "value",
        name: "",
        axisLabel: {
          formatter: function (a) {
            return +a
          },
          // rotate: 25, // 倾斜
        },
      },
      {
        type: "value",
        name: "",
        position: "right",
        min: function (value) {
          const absMax = Math.min(Math.max(Math.abs(value.min), Math.abs(value.max), 20) * 1.2, 150)
          return -Math.ceil(absMax)
        },
        max: function (value) {
          const absMax = Math.min(Math.max(Math.abs(value.min), Math.abs(value.max), 20) * 1.2, 150)
          return Math.ceil(absMax)
        },
        axisLabel: {
          formatter: function (val) {
            return val.toFixed(0) + "%"
          },
        },
      },
    ],
    series: [
      {
        name: "本期值",
        type: "bar",
        smooth: true,
        color: "#56C6C8",
        data: loopData.value.map((item) => {
          return item.value
        }),
      },
      {
        name: "同期值",
        type: "bar",
        smooth: true,
        color: "#eaeaea",
        data: loopData.value.map((item) => {
          return item.oldValue
        }),
      },
      {
        name: `环比值`,
        type: "line",
        smooth: true,
        yAxisIndex: 1,
        color: "#5171ef",
        tooltip: {
          valueFormatter: function (value) {
            return value + "%"
          },
        },
        data: loopData.value.map((item) => {
          return item.ratio
        }),
      },
    ],
  }
  loopChartInstance.setOption(option)
  loopChartInstance.resize()
}

// data
const rankList = ref([])
const mapRef = ref(null)
const mapMarkerList = ref([])
const statisticList = ref([
  { label: "当年累计发电量", value: "0" },
  { label: "当日发电量", value: "0" },
  { label: "当日发电收益", value: "0" },
  { label: "总装机容量", value: "0" },
  { label: "累计CO2减排量", value: "0" },
])
const alarmInfo = ref({})
const recentAlarms = ref([])
function getData() {
  // 排名-map
  getSiteRank().then((res) => {
    rankList.value = res.data
    mapMarkerList.value = res.data
      .filter((f) => {
        return f.lon && f.lat
      })
      .map((item) => {
        return {
          name: item.powerStationName,
          position: [item.lon, item.lat],
          echartsPosition: [item.lon, item.lat],
          value: item.sumValue,
          id: item.powerStationId,
        }
      })
  })
  // 统计5项
  getHomePowerStationInfo().then((res) => {
    statisticList.value[1].value = res.data.cumulativeDay
    statisticList.value[2].value = res.data.earningsDay
    statisticList.value[3].value = res.data.installedCapacity
    statisticList.value[0].value = res.data.cumulativeYear
    statisticList.value[4].value = res.data.carbonEmissions
  })
  // alarm
  listHomeAlarm({}).then((res) => {
    alarmInfo.value = res.data
    recentAlarms.value = res.data.recentAlarms || []
  })
  // 峰平谷
  getPeaksData()
  // 年发电量
  homepageGeneration({ queryTime: formatDate(new Date()).substring(0, 10), timeType: "YEAR" }).then((res) => {
    lineYData.value = res.data.map((item) => {
      return item.value
    })
    lineTime.value = res.data.map((item) => {
      return formatChartTime("year", item.time)
    })
    drawLineChart()
  })
}

// 峰平谷
const peaksTimeType = ref("month")
function switchPeaksDate(type) {
  peaksTimeType.value = type
  getPeaksData()
}
function getPeaksData() {
  let dateObj = formatDateObj(new Date())
  let queryTime = ""
  if (peaksTimeType.value == "day") {
    queryTime = dateObj.year + "-" + dateObj.month + "-" + dateObj.day
  } else if (peaksTimeType.value == "month") {
    queryTime = dateObj.year + "-" + dateObj.month
  }
  let params = {
    queryTime,
    timeType: peaksTimeType.value.toUpperCase(),
  }
  getPeriodGenerationPercentage(params).then((res) => {
    ringChartData.value = res.data
    drawRingChart()
  })
}
function toAlarm() {
  router.push("/alarm")
}

function toSite(item) {
  router.push("/realTime/site?site=" + item.powerStationId)
}

// 自动刷新定时器
let refreshTimer = null
const REFRESH_INTERVAL = 10000 // 10秒刷新一次

function startAutoRefresh() {
  refreshTimer = setInterval(() => {
    getData()
    getLoopData()
  }, REFRESH_INTERVAL)
}

function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  getData()
  getLoopData()
  startAutoRefresh()
})

onActivated(() => {
  resize(loopChartInstance, loopChartRef.value)
  startAutoRefresh()
})

onDeactivated(() => {
  destroyListener(loopChartInstance, loopChartRef.value)
  stopAutoRefresh()
})

onBeforeUnmount(() => {
  stopAutoRefresh()
  if (!loopChartInstance) {
    return
  }
  loopChartInstance.dispose() // 销毁图表
  loopChartInstance = null
})
</script>

<style scoped lang="scss">
.home-page {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  .home-left {
    background: rgba(0, 20, 200, 0.03);
    width: 320px;
    display: flex;
    flex-direction: column;
  }
  .home-right {
    background: rgba(0, 20, 200, 0.023);
    width: 360px;
    display: flex;
    flex-direction: column;
  }
  .home-center {
    flex: 1;
    margin: 0 12px;
  }
  .header-default {
    height: 40px;
    width: 100%;
    color: #fff;
    padding: 0 12px;
    line-height: 40px;
    background: #3a71a8;
    border-radius: 2px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .date-wrap {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    background: #eaeaea;
    border-radius: 12px;
    height: 26px;
    float: right;
    margin-top: 12px;
    cursor: pointer;
    .switch-date,
    .switch-date-active {
      // font-size: 30px;
      color: #333;
      width: 60px;
      height: 26px;
      line-height: 26px;
      text-align: center;
    }
    .switch-date-active {
      // background: center center #4a54ff;
      // background-image: linear-gradient(315deg, #6772ff 0, #00f9e5 100%);
      // background-size: 104% 104%;
      background-color: #5aa8f6;
      color: #fff;
      font-weight: 500;
      border-radius: 24px;
    }
  }
}
.sum-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  .sum-item {
    width: 19.5%;
    // margin-right: 12px;
    background: #c9ddf1;
    display: flex;
    justify-content: flex-start;
    align-items: center;
    padding: 24px 12px;
    background: center center #2085d2;
    background-image: linear-gradient(135deg, #3a71a8 0%, #5a8fbc 100%);
    // background-size: 104% 104%;
    border-radius: 6px;
    color: #fff;
    height: 80px;
    margin-bottom: 10px;
    .img {
      img {
        width: 44px;
        margin-right: 8px;
      }
    }
    .value {
      font-size: 22px;
      font-weight: 500;
      margin-bottom: 4px;
    }
  }
  .bg1 {
    background: center center #3d9ce4;
    background-image: linear-gradient(135deg, #3a71a8 0%, #5a8fbc 100%);
  }
  .bg2 {
    background: center center #237cc0;
    background-image: linear-gradient(135deg, #3a71a8 0%, #5a8fbc 100%);
  }
}

.alarm-wrapper {
  .total-img {
    width: 70px;
    margin-left: 16px;
    margin-right: 8px;
  }
  .total-view {
    .value {
      color: #f55957;
      font-size: 24px;
      font-weight: 600;
      letter-spacing: 1px;
      margin-bottom: 4px;
    }
    .label {
      color: #333;
    }
  }

  .alarm-list {
    margin: 6px 0 10px;
    .item {
      width: 100px;
      text-align: center;
    }
    .value {
      color: #f67d7b;
      // color: #f67d7b;
      font-size: 22px;
      font-weight: 600;
      letter-spacing: 1px;
      margin-bottom: 2px;
    }
    .label {
      color: #666;
      font-size: 14px;
    }
  }
}

.recent-alarm-list {
  padding: 4px 0;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(32,133,210,0.3); border-radius: 2px; }
  .recent-alarm-empty {
    text-align: center;
    color: #999;
    padding: 20px 0;
    font-size: 13px;
  }
  .recent-alarm-item {
    padding: 10px 12px;
    border-bottom: 1px solid #eef0f5;
    cursor: pointer;
    transition: background 0.2s;
    &:last-child { border-bottom: none; }
    &:hover { background: rgba(32, 133, 210, 0.05); }
    .recent-alarm-top {
      display: flex;
      align-items: center;
      margin-bottom: 4px;
    }
    .recent-alarm-level {
      font-size: 11px;
      color: #fff;
      padding: 1px 6px;
      border-radius: 3px;
      margin-right: 6px;
      flex-shrink: 0;
    }
    .recent-alarm-device {
      font-size: 13px;
      color: #333;
      font-weight: 500;
      flex: 1;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
    .recent-alarm-status {
      font-size: 12px;
      flex-shrink: 0;
      margin-left: 6px;
    }
    .recent-alarm-content {
      font-size: 12px;
      color: #666;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
    .recent-alarm-time {
      font-size: 11px;
      color: #aaa;
      margin-top: 3px;
    }
  }
}

.site-rank-list {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(32,133,210,0.3); border-radius: 2px; }
  .rank-item-zero {
    color: #000;
    font-weight: 500;
  }
  .rank-item {
    text-align: center;
    // line-height: 34px;
    height: 40px;
    color: #666;
    img {
      width: 22px;
    }
    .site {
      color: #2085d2;
    }
  }
}
</style>
