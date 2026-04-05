<template>
  <div class="amap-container">
    <div id="container" :style="`height: ${height}px; width: 100%`"></div>

    <div class="marker-card" :style="'top:' + top + 'px;left:' + left + 'px;'" v-if="showMarkerCard">
      <!-- <div>查看详情</div> -->
      <div class="card-item">
        <div class="card-left">电站：</div>
        <div class="card-right">{{ markerInfo.name }}</div>
      </div>
      <!-- <div class="card-item">
        <div class="card-left">装机容量：</div>
        <div class="card-right">{{ markerInfo.value || "" }}</div>
      </div> -->
      <div class="card-item">
        <div class="card-left">当年发电效率：</div>
        <div class="card-right">{{ markerInfo.value || "" }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { getImageUrl } from "@/utils/index"
import { nextTick, onMounted, reactive } from "vue"
import { useRouter } from "vue-router"

const router = useRouter()
const props = defineProps({
  markers: {
    type: Array,
    default() {
      return []
    },
  },
  height: {
    type: Number,
    default: 400,
  },
})
watch(
  () => props.markers,
  (value) => markerSet()
)
const emit = defineEmits()
let echartsInstance = null
let markersInitialized = false
function initMap() {
  let centerPosition = [120.47, 36.14]
  var map = new AMap.Map("container", {
    resizeEnable: true,
    center: centerPosition,
    zoom: 11,
    features: ["bg", "road", "building", "point"],
    // mapStyle: "amap://styles/whitesmoke", // blue darkblue grey dark light whitesmoke
  })
  echartsInstance = map
  echartsInstance.clearMap() // 清除地图覆盖物
  markerSet()
}
function markerSet() {
  // 检查地图实例是否存在
  if (!echartsInstance) {
    return
  }
  echartsInstance.clearMap()
  // 添加一些分布不均的点到地图上,地图上添加三个点标记，作为参照
  props.markers.forEach(function (marker) {
    if (!marker.position || !marker.position[0] || !marker.position[1]) {
      return
    }
    let newMarker = new AMap.Marker({
      // map: echartsInstance,
      label: {
        content: marker.name,
        // direction: "top",
        offset: [6, 0],
      }, // 鼠标滑过显示
      icon: getImageUrl("greenPoint"),
      position: [marker.position[0], marker.position[1]],
      offset: new AMap.Pixel(-13, -30),
      clickable: true, // 点标记是否可点击
      extData: marker, // 用户自定义属性，支持JavaScript API任意数据类型，如Marker的id等
      // animation: 'AMAP_ANIMATION_DROP'
    })
    newMarker.on("mouseover", mouseover)
    newMarker.on("mouseout", mouseout)
    newMarker.on("click", mousetap)
    newMarker.setMap(echartsInstance)
    echartsInstance.add(newMarker)
  })
  // 首次加载时自适应视野，之后保持用户手动拖动的位置
  if (!markersInitialized && echartsInstance && props.markers.length > 0) {
    markersInitialized = true
    echartsInstance.setFitView(null, false, [150, 50, 100, 60])
  }
}

const showMarkerCard = ref(false)
const top = ref(0)
const left = ref(0)
const markerInfo = ref({})
function mouseover(e) {
  console.log(e)
  showMarkerCard.value = true
  top.value = e.pixel.y + 5
  left.value = e.pixel.x + 15
  // 兼容不同版本的高德地图API
  markerInfo.value = e.target.getExtData ? e.target.getExtData() : (e.target._opts?.extData || {})
}

function mouseout(e) {
  showMarkerCard.value = false
}

function mousetap(e) {
  console.log(e)
  // 兼容不同版本的高德地图API
  markerInfo.value = e.target.getExtData ? e.target.getExtData() : (e.target._opts?.extData || {})
  router.push("/realTime/site?site=" + markerInfo.value.id)
}

onMounted(() => {
  nextTick(() => {
    if (typeof AMap !== 'undefined') {
      initMap()
    } else {
      let elapsed = 0
      const timer = setInterval(() => {
        elapsed += 200
        if (typeof AMap !== 'undefined') {
          clearInterval(timer)
          initMap()
        } else if (elapsed >= 10000) {
          clearInterval(timer)
          console.warn('高德地图SDK未加载，请检查网络或API Key')
        }
      }, 200)
    }
  })
})
</script>

<style scoped lang="scss">
.amap-container {
  // height: 360px;
  width: 100%;
  position: relative;
}
:deep(.amap-container img) {
  width: 26px;
  height: 30px;
}
:deep(.amap-marker-label) {
  border: 1px solid #eaeaea;
  padding: 4px 8px;
  color: #409eff;
  font-weight: 600;
  // font-size: 14px;
}

.pagination-container.hidden {
  display: none;
}
.marker-card {
  position: absolute;
  background-color: rgba(0, 0, 0, 0.7);
  color: #fff;
  // width: 120px;
  // height: 100px;
  border-radius: 4px;
  padding: 4px;
  .card-item {
    display: flex;
    height: 28px;
    line-height: 28px;
    font-size: 12px;

    .card-right {
      max-width: 120px;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
      word-break: break-all;
    }
  }
}
</style>
