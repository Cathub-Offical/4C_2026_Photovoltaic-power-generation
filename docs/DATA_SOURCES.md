# 光伏监控系统 - 实时数据源集成指南

## 概述

本项目基于 **Open-Meteo 天气数据 + 自定义光伏发电算法** 计算实时发电量，替代原有的模拟数据（Math.random()），实现真实的光伏发电监控和分析功能。

## 数据源架构

```
┌─────────────────────────────────────────────────────────────┐
│                    光伏监控系统                              │
├─────────────────────────────────────────────────────────────┤
│  实时/预测数据                      │  对标分析             │
│  Open-Meteo API + 自定义算法        │  Solargis API        │
│  (免费，无需API Key)                │  (性能基准，可选)     │
├─────────────────────────────────────────────────────────────┤
│                    历史训练数据                              │
│            Kaggle + UNISOLAR 数据集                         │
└─────────────────────────────────────────────────────────────┘
```

## 1. Open-Meteo API（核心数据源）

### 简介
Open-Meteo 是一个免费的天气预报 API，无需 API Key，提供高精度的太阳辐射数据。

### 特点
- **免费使用**：无需注册，无需 API Key
- **高精度辐射数据**：GHI、DNI、DHI
- **小时级预报**：最多16天预报
- **历史数据**：可查询历史天气

### 提供的数据
- 太阳辐射 (W/m²)
  - GHI (全球水平辐射)
  - DNI (直接法向辐射)
  - DHI (散射辐射)
- 温度、湿度
- 云量
- 日出日落时间
- 日照时长

### 服务类
`com.ruoyi.system.service.external.OpenMeteoService`

---

## 2. 发电预测算法（基于Open-Meteo）

### 简介
基于 Open-Meteo 天气数据的自定义光伏发电预测算法。

### 算法特点
- **倾斜面辐射计算**：考虑组件倾角和方位角
- **温度损失模型**：基于 NOCT 模型计算温度影响
- **云量影响**：非线性云量-效率关系
- **置信区间**：提供预测不确定性估计

### 预测内容
- 小时级功率预测
- 每日发电量预测
- 实时功率估算
- 系统效率预测

### 服务类
`com.ruoyi.system.service.external.SolarPredictionService`

### 使用示例
```java
@Autowired
private SolarPredictionService predictionService;

// 预测未来7天的每日发电量
List<Map<String, Object>> predictions = predictionService.predictDailyGeneration(
    100.0,  // 装机容量 kWp
    23.1291, // 纬度
    113.2644, // 经度
    25,     // 倾斜角度
    180,    // 方位角（南向）
    7       // 预测天数
);
```

---

## 3. Solargis API（对标分析）

### 简介
Solargis 提供高精度的太阳辐射和光伏发电潜力数据，用于性能对标分析。

### 注册与配置
1. 访问 https://solargis.com/ 申请 API 访问权限
2. 获取 API Key

### 环境变量配置
```bash
export SOLARGIS_API_KEY=your_api_key_here
```

### 提供的数据
- 长期平均辐射数据 (LTA)
- 月度辐射统计
- 光伏发电潜力 (PVOUT)
- 性能比计算

### 服务类
`com.ruoyi.system.service.external.SolargisService`

### 对标分析功能
```java
@Autowired
private SolargisService solargisService;

// 进行对标分析
Map<String, Object> benchmark = solargisService.benchmarkAnalysis(
    23.1291,  // 纬度
    113.2644, // 经度
    100.0,    // 装机容量 kWp
    actualMonthlyGeneration // 实际月发电量列表
);
```

**注意**：如果未配置 Solargis API Key，系统会使用基于地理位置的估算数据。

---

## 4. 历史数据服务（Kaggle + UNISOLAR）

### 简介
集成 Kaggle 和 UNISOLAR 数据集，用于历史分析和模型训练。

### 数据集配置

#### Kaggle Solar Power Generation Data
1. 从 Kaggle 下载数据集
2. 放置到 `data/kaggle/` 目录
3. 支持的文件格式：
   - `Plant_X_Generation_Data.csv`
   - `generation_data.csv`

#### UNISOLAR Irradiance Data
1. 下载 UNISOLAR 辐射数据
2. 放置到 `data/unisolar/` 目录
3. 支持的文件格式：
   - `{location}_irradiance.csv`
   - `irradiance_data.csv`

### 配置路径
```yaml
historical:
  data:
    path: data/historical
  kaggle:
    path: data/kaggle
  unisolar:
    path: data/unisolar
```

### 服务类
`com.ruoyi.system.service.external.HistoricalDataService`

### 功能
- 加载历史发电数据
- 加载历史辐射数据
- 数据聚合统计
- 训练数据准备

---

## 5. 统一实时数据服务

### 简介
`RealTimeDataService` 整合所有外部数据源，提供统一的数据访问接口。

### 服务类
`com.ruoyi.system.service.external.RealTimeDataService`

### 主要方法
| 方法 | 描述 |
|------|------|
| `getDeviceRealTimePower()` | 获取设备实时功率 |
| `getDeviceVoltage()` | 获取设备电压 |
| `getDeviceCurrent()` | 获取设备电流 |
| `getDeviceTemperature()` | 获取设备温度 |
| `getACMeasurements()` | 获取交流测量数据 |
| `getDCMeasurements()` | 获取直流测量数据 |
| `getStationRealTimeStatus()` | 获取电站实时状态 |
| `getGenerationTrend()` | 获取发电趋势 |
| `getPeakValleyData()` | 获取峰平谷数据 |
| `getBenchmarkAnalysis()` | 获取对标分析 |

### 数据优先级
1. **Open-Meteo 天气数据 + 预测算法**（实时计算）
2. **InfluxDB 历史数据**（如果存在）
3. **Solargis 对标数据**（可选）

---

## 6. 配置文件完整示例

```yaml
# application.yml

# InfluxDB配置（存储历史数据）
influxdb:
  url: http://localhost:8086
  token: your_influxdb_token
  org: your_org
  bucket: power_generation

# Open-Meteo API (免费，无需配置)
# 文档: https://open-meteo.com/

# Solargis API配置 (可选)
solargis:
  api-key: ${SOLARGIS_API_KEY:}

# 光伏系统默认参数
solar:
  default-latitude: 23.1291
  default-longitude: 113.2644
  default-tilt-angle: 25
  default-azimuth-angle: 180

# 历史数据路径
historical:
  data:
    path: data/historical
  kaggle:
    path: data/kaggle
  unisolar:
    path: data/unisolar
```

---

## 7. 已更新的 Controller

以下 Controller 已更新为使用实时数据服务：

| Controller | 更新内容 |
|------------|----------|
| `RealTimeController` | 设备实时数据、电站状态、功率因数、三相不平衡分析 |
| `DeviceController` | 交流测量、实时功率、发电趋势、发电信息 |
| `PeakValleyController` | 峰平谷占比、报表数据 |
| `StatisticsAnalysisController` | 环比分析、同比分析、首页统计 |

---

## 8. 注意事项

### API 限制
- **Open-Meteo**: 免费无限制（建议合理使用，每分钟不超过100次）
- **Solargis**: 根据订阅计划（可选）

### 数据缓存
- `RealTimeDataService` 内置 1 分钟缓存
- 减少 API 调用频率
- 提高响应速度

### 降级策略
当外部 API 不可用时：
1. 使用 InfluxDB 历史数据
2. 使用预测算法估算
3. 使用基于地理位置的估算值

### 安全建议
- 不要在代码中硬编码 API Key
- 使用环境变量或配置中心管理敏感信息
- 定期轮换 API Key

---

## 9. 快速开始

### 零配置启动
系统**无需任何 API Key** 即可运行，自动使用：
- **Open-Meteo 免费天气 API**（太阳辐射、温度、云量）
- **自定义光伏发电算法**（基于天气数据实时计算）
- **基于地理位置的辐射估算**

### 推荐配置
1. 设置正确的电站地理坐标（纬度、经度）
2. 配置 InfluxDB 存储历史数据
3. 设置电站容量、组件倾角、方位角

### 完整配置
1. 配置 Solargis API Key（可选，用于对标分析）
2. 下载并放置 Kaggle/UNISOLAR 数据集（可选，用于历史分析）
3. 配置 InfluxDB
4. 设置电站详细参数（容量、倾角、方位角）
