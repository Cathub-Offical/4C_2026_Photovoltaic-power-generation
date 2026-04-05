package com.ruoyi.system.service.external;

import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.PowerStation;
import com.ruoyi.system.mapper.DeviceMapper;
import com.ruoyi.system.mapper.PowerStationMapper;
import com.ruoyi.system.service.InfluxDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 统一实时数据服务
 * 基于Open-Meteo天气数据 + 自定义算法计算发电量
 */
@Service
public class RealTimeDataService {

    private static final Logger log = LoggerFactory.getLogger(RealTimeDataService.class);

    @Autowired
    private OpenMeteoService openMeteoService;

    @Autowired
    private SolargisService solargisService;

    @Autowired
    private SolarPredictionService predictionService;

    @Autowired
    private InfluxDBService influxDBService;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private PowerStationMapper powerStationMapper;

    // 数据缓存（减少API调用）
    private Map<String, CachedData> cache = new HashMap<>();
    private static final long CACHE_TTL_MS = 60000; // 1分钟缓存

    /**
     * 获取设备实时功率
     * 基于Open-Meteo天气数据 + 自定义算法计算
     */
    public double getDeviceRealTimePower(String deviceId, double installedCapacity) {
        String cacheKey = "power_" + deviceId;
        CachedData cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (double) cached.data;
        }

        // 获取设备对应的电站位置信息
        double latitude = 23.1291;
        double longitude = 113.2644;
        if (deviceId != null) {
            Device device = deviceMapper.selectDeviceById(deviceId);
            if (device != null && device.getPowerStationId() != null) {
                PowerStation station = powerStationMapper.selectPowerStationById(device.getPowerStationId());
                if (station != null) {
                    if (station.getLat() != null) latitude = station.getLat().doubleValue();
                    if (station.getLon() != null) longitude = station.getLon().doubleValue();
                }
            }
        }

        // 使用Open-Meteo天气数据 + 预测算法计算当前功率
        Map<String, Object> prediction = predictionService.predictCurrentPower(installedCapacity, latitude, longitude);
        double power = ((Number) prediction.getOrDefault("predictedPower", 0.0)).doubleValue();

        cache.put(cacheKey, new CachedData(power));
        return power;
    }

    /**
     * 获取设备实时电压
     */
    public Map<String, Double> getDeviceVoltage(String deviceId) {
        Map<String, Double> result = new HashMap<>();

        // 获取当前天气数据来估算电压波动
        Map<String, Object> weather = openMeteoService.getCurrentWeather();
        double temperature = ((Number) weather.getOrDefault("temperature", 25.0)).doubleValue();

        // 基于温度计算电压（温度升高，电压略降）
        double baseVoltage = 380;
        double tempEffect = (temperature - 25) * 0.1;
        double voltage = baseVoltage - tempEffect;

        // 三相电压（略有差异）
        result.put("voltageA", Math.round((voltage + 0.5) * 10) / 10.0);
        result.put("voltageB", Math.round(voltage * 10) / 10.0);
        result.put("voltageC", Math.round((voltage - 0.3) * 10) / 10.0);
        result.put("averageVoltage", Math.round(voltage * 10) / 10.0);

        return result;
    }

    /**
     * 获取设备实时电流
     */
    public Map<String, Double> getDeviceCurrent(String deviceId, double power) {
        Map<String, Double> result = new HashMap<>();

        // 基于功率和电压计算电流
        double voltage = 380;
        double powerFactor = 0.95;
        double totalCurrent = power * 1000 / (Math.sqrt(3) * voltage * powerFactor);

        // 三相电流（略有不平衡）
        double imbalance = 0.02; // 2%不平衡
        result.put("currentA", Math.round(totalCurrent * (1 + imbalance) * 10) / 10.0);
        result.put("currentB", Math.round(totalCurrent * 10) / 10.0);
        result.put("currentC", Math.round(totalCurrent * (1 - imbalance) * 10) / 10.0);
        result.put("averageCurrent", Math.round(totalCurrent * 10) / 10.0);

        return result;
    }

    /**
     * 获取设备温度
     */
    public double getDeviceTemperature(String deviceId) {
        Map<String, Object> weather = openMeteoService.getCurrentWeather();
        double ambientTemp = ((Number) weather.getOrDefault("temperature", 25.0)).doubleValue();
        double irradiance = ((Number) weather.getOrDefault("shortwaveRadiation", 500.0)).doubleValue();

        // 设备温度 = 环境温度 + 辐射加热效应
        double deviceTemp = ambientTemp + irradiance * 0.02;
        return Math.round(deviceTemp * 10) / 10.0;
    }

    /**
     * 获取逆变器效率
     */
    public double getInverterEfficiency(String deviceId, double power, double installedCapacity) {
        // 效率与负载率相关
        double loadRatio = installedCapacity > 0 ? power / installedCapacity : 0;

        // 典型逆变器效率曲线
        double efficiency;
        if (loadRatio < 0.1) {
            efficiency = 0.90 + loadRatio * 0.5; // 低负载效率较低
        } else if (loadRatio < 0.5) {
            efficiency = 0.95 + loadRatio * 0.06;
        } else {
            efficiency = 0.98 - (loadRatio - 0.5) * 0.02; // 高负载略降
        }

        return Math.round(Math.min(0.99, Math.max(0.85, efficiency)) * 1000) / 10.0;
    }

    /**
     * 获取直流侧数据
     */
    public Map<String, Object> getDCMeasurements(String deviceId, double power) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> weather = openMeteoService.getCurrentWeather();
        double irradiance = ((Number) weather.getOrDefault("shortwaveRadiation", 500.0)).doubleValue();

        // 直流电压与辐射相关
        double dcVoltage = 600 + irradiance * 0.1;
        result.put("dcVoltage", Math.round(dcVoltage * 10) / 10.0);

        // 直流电流 = 功率 / 电压
        double dcCurrent = dcVoltage > 0 ? (power * 1000) / dcVoltage : 0;
        result.put("dcCurrent", Math.round(dcCurrent * 10) / 10.0);

        result.put("dcPower", Math.round(power * 10) / 10.0);

        return result;
    }

    /**
     * 获取交流侧数据
     */
    public Map<String, Object> getACMeasurements(String deviceId, double power) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Double> voltage = getDeviceVoltage(deviceId);
        Map<String, Double> current = getDeviceCurrent(deviceId, power);

        result.put("avoltage", voltage.get("voltageA"));
        result.put("bvoltage", voltage.get("voltageB"));
        result.put("cvoltage", voltage.get("voltageC"));
        result.put("acurrent", current.get("currentA"));
        result.put("bcurrent", current.get("currentB"));
        result.put("ccurrent", current.get("currentC"));
        result.put("totalActivePower", Math.round(power * 10) / 10.0);
        result.put("factor", 0.95 + (power > 10 ? 0.03 : 0.01));
        result.put("frequency", 50.0 + (Math.random() - 0.5) * 0.1);

        return result;
    }

    /**
     * 获取电站实时状态
     */
    public Map<String, Object> getStationRealTimeStatus(String stationId) {
        Map<String, Object> result = new HashMap<>();

        PowerStation station = powerStationMapper.selectPowerStationById(stationId);
        if (station == null) {
            return result;
        }

        double latitude = station.getLat() != null ? station.getLat().doubleValue() : 23.1291;
        double longitude = station.getLon() != null ? station.getLon().doubleValue() : 113.2644;
        double capacity = station.getInstalledCapacity() != null ? station.getInstalledCapacity().doubleValue() : 100;

        // 获取当前天气
        Map<String, Object> weather = openMeteoService.getCurrentWeather(latitude, longitude);
        result.put("weather", weather);

        // 获取预测功率
        Map<String, Object> prediction = predictionService.predictCurrentPower(capacity, latitude, longitude);
        double totalPower = ((Number) prediction.getOrDefault("predictedPower", 0.0)).doubleValue();
        result.put("totalPower", Math.round(totalPower * 10) / 10.0);

        // 从InfluxDB获取今日发电量
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String todayStart = today + "T00:00:00+08:00";
        String todayStop = today + "T23:59:59+08:00";
        double todayGeneration = influxDBService.queryTotalGeneration(stationId, todayStart, todayStop);
        result.put("todayGeneration", Math.round(todayGeneration * 10) / 10.0);

        // 累计发电量
        double totalGeneration = influxDBService.queryAllTimeGeneration(stationId);
        result.put("totalGeneration", Math.round(totalGeneration * 10) / 10.0);

        // 设备状态
        List<Device> inverters = deviceMapper.selectInverterList(stationId);
        List<Device> ammeters = deviceMapper.selectAmmeterList(stationId);
        result.put("onlineDevices", (inverters != null ? inverters.size() : 0) + 
                                    (ammeters != null ? ammeters.size() : 0));
        result.put("offlineDevices", 0);

        result.put("status", 1); // 正常运行
        result.put("updateTime", new Date());

        return result;
    }

    /**
     * 获取发电趋势数据（使用InfluxDB真实数据）
     */
    public List<Map<String, Object>> getGenerationTrend(String stationId, String deviceId,
                                                         String timeType, double installedCapacity) {
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate today = LocalDate.now();
        String year  = String.valueOf(today.getYear());
        String month = String.format("%02d", today.getMonthValue());
        String day   = String.format("%02d", today.getDayOfMonth());

        List<Map<String, Object>> historical;

        if ("DAY".equalsIgnoreCase(timeType)) {
            // 优先查设备级小时数据，无deviceId时降级为电站级
            if (deviceId != null && !deviceId.isEmpty()) {
                historical = influxDBService.queryDeviceHourly(deviceId, year, month, day);
            } else {
                historical = influxDBService.queryStationTimeSeries(stationId, "DAY", year, month, day);
            }
        } else if ("MONTH".equalsIgnoreCase(timeType)) {
            historical = influxDBService.queryStationTimeSeries(stationId, "MONTH", year, month, "01");
        } else {
            historical = influxDBService.queryStationTimeSeries(stationId, "YEAR", year, "01", "01");
        }

        for (Map<String, Object> hist : historical) {
            Map<String, Object> item = new HashMap<>();
            item.put("time",      hist.get("timeBucket"));
            item.put("value",     hist.get("sumValue"));
            item.put("predicted", false);
            result.add(item);
        }

        return result;
    }

    /**
     * 获取功率因数数据
     */
    public Map<String, Object> getPowerFactorData(String stationId, String timeType, String timeCode) {
        Map<String, Object> result = new HashMap<>();

        // 获取天气数据来估算功率因数变化
        Map<String, Object> weather = openMeteoService.getCurrentWeather();
        double cloudCover = ((Number) weather.getOrDefault("cloudCover", 30.0)).doubleValue();

        // 功率因数与负载相关
        double basePF = 0.92;
        double cloudEffect = cloudCover * 0.0005; // 云量影响
        double currentPF = basePF + (1 - cloudCover / 100) * 0.06;

        Map<String, Object> detail = new HashMap<>();
        detail.put("max", String.format("%.2f", Math.min(0.99, currentPF + 0.03)));
        detail.put("min", String.format("%.2f", Math.max(0.80, currentPF - 0.10)));
        detail.put("avg", String.format("%.2f", currentPF));
        result.put("detail", detail);

        // 生成时间序列
        List<Map<String, Object>> itemList = generateTimeSeriesData(timeType, timeCode, 
            () -> 0.85 + Math.random() * 0.12);
        result.put("itemList", itemList);

        return result;
    }

    /**
     * 获取三相不平衡数据
     */
    public List<Map<String, Object>> getThreePhaseUnbalanceData(String stationId, String timeType, 
                                                                  String timeCode, boolean isVoltage) {
        Map<String, Object> weather = openMeteoService.getCurrentWeather();
        double temperature = ((Number) weather.getOrDefault("temperature", 25.0)).doubleValue();

        // 基于温度计算基准值
        double baseValue = isVoltage ? (220 - (temperature - 25) * 0.1) : 15;
        double variation = isVoltage ? 3 : 5;

        return generateThreePhaseData(timeType, timeCode, baseValue, variation);
    }

    /**
     * 获取对标分析数据
     */
    public Map<String, Object> getBenchmarkAnalysis(String stationId, double installedCapacity,
                                                     List<Double> actualMonthlyGeneration) {
        PowerStation station = stationId != null ? 
            powerStationMapper.selectPowerStationById(stationId) : null;
        double latitude = (station != null && station.getLat() != null) ? 
            station.getLat().doubleValue() : 23.1291;
        double longitude = (station != null && station.getLon() != null) ? 
            station.getLon().doubleValue() : 113.2644;

        return solargisService.benchmarkAnalysis(latitude, longitude, installedCapacity, actualMonthlyGeneration);
    }

    /**
     * 获取峰平谷数据
     */
    public Map<String, Object> getPeakValleyData(String stationId, String dateTime) {
        Map<String, Object> result = new HashMap<>();

        // 从InfluxDB获取历史数据
        LocalDate date = dateTime != null && dateTime.length() >= 7 ? 
            LocalDate.parse(dateTime.substring(0, 7) + "-01") : LocalDate.now().withDayOfMonth(1);
        
        String year = String.valueOf(date.getYear());
        String month = String.format("%02d", date.getMonthValue());

        List<Map<String, Object>> timeSeries = influxDBService.queryStationTimeSeries(
            stationId, "MONTH", year, month, "01");

        // 计算峰平谷分布
        double totalConsumption = 0;
        Map<String, Double> periodConsumption = new HashMap<>();
        periodConsumption.put("tip", 0.0);
        periodConsumption.put("peak", 0.0);
        periodConsumption.put("flat", 0.0);
        periodConsumption.put("trough", 0.0);
        periodConsumption.put("deep", 0.0);

        for (Map<String, Object> data : timeSeries) {
            double value = ((Number) data.getOrDefault("sumValue", 0.0)).doubleValue();
            totalConsumption += value;

            // 根据时间分配到不同时段（简化模型）
            periodConsumption.merge("tip", value * 0.05, Double::sum);
            periodConsumption.merge("peak", value * 0.25, Double::sum);
            periodConsumption.merge("flat", value * 0.40, Double::sum);
            periodConsumption.merge("trough", value * 0.20, Double::sum);
            periodConsumption.merge("deep", value * 0.10, Double::sum);
        }

        // 如果没有历史数据，使用预测
        if (totalConsumption == 0) {
            PowerStation station = stationId != null ? 
                powerStationMapper.selectPowerStationById(stationId) : null;
            double capacity = (station != null && station.getInstalledCapacity() != null) ? 
                station.getInstalledCapacity().doubleValue() : 100;

            List<Map<String, Object>> prediction = predictionService.predictDailyGeneration(capacity, 30);
            for (Map<String, Object> pred : prediction) {
                double value = ((Number) pred.getOrDefault("predictedGeneration", 0.0)).doubleValue();
                totalConsumption += value;
            }

            periodConsumption.put("tip", totalConsumption * 0.05);
            periodConsumption.put("peak", totalConsumption * 0.25);
            periodConsumption.put("flat", totalConsumption * 0.40);
            periodConsumption.put("trough", totalConsumption * 0.20);
            periodConsumption.put("deep", totalConsumption * 0.10);
        }

        // 电价系数
        Map<String, Double> priceFactors = new HashMap<>();
        priceFactors.put("tip", 1.2);
        priceFactors.put("peak", 0.8);
        priceFactors.put("flat", 0.5);
        priceFactors.put("trough", 0.3);
        priceFactors.put("deep", 0.2);

        double totalCost = 0;
        for (String period : periodConsumption.keySet()) {
            double consumption = periodConsumption.get(period);
            double cost = consumption * priceFactors.get(period);
            totalCost += cost;

            result.put(period + "PowerConsumption", Math.round(consumption * 100) / 100.0);
            result.put(period + "PowerCost", Math.round(cost * 100) / 100.0);
            result.put(period + "PowerProportion", 
                String.format("%.1f", totalConsumption > 0 ? consumption / totalConsumption * 100 : 0));
        }

        result.put("totalPowerConsumption", Math.round(totalConsumption * 100) / 100.0);
        result.put("totalPowerCost", Math.round(totalCost * 100) / 100.0);

        return result;
    }

    // ==================== 辅助方法 ====================

    private List<Map<String, Object>> generateTimeSeriesData(String timeType, String timeCode,
                                                              java.util.function.Supplier<Double> valueGenerator) {
        List<Map<String, Object>> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        int year = timeCode != null && timeCode.length() >= 4 ? 
            Integer.parseInt(timeCode.substring(0, 4)) : cal.get(Calendar.YEAR);
        int month = timeCode != null && timeCode.length() >= 7 ? 
            Integer.parseInt(timeCode.substring(5, 7)) : cal.get(Calendar.MONTH) + 1;
        int day = timeCode != null && timeCode.length() >= 10 ? 
            Integer.parseInt(timeCode.substring(8, 10)) : cal.get(Calendar.DAY_OF_MONTH);

        if ("DAY".equalsIgnoreCase(timeType)) {
            for (int i = 0; i < 24; i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("timeCode", String.format("%02d", i));
                item.put("value", String.format("%.2f", valueGenerator.get()));
                result.add(item);
            }
        } else if ("MONTH".equalsIgnoreCase(timeType)) {
            cal.set(year, month - 1, 1);
            int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = 1; i <= maxDay; i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("timeCode", String.format("%02d", i));
                item.put("value", String.format("%.2f", valueGenerator.get()));
                result.add(item);
            }
        } else {
            for (int i = 1; i <= 12; i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("timeCode", String.format("%02d", i));
                item.put("value", String.format("%.2f", valueGenerator.get()));
                result.add(item);
            }
        }

        return result;
    }

    private List<Map<String, Object>> generateThreePhaseData(String timeType, String timeCode,
                                                              double baseValue, double variation) {
        List<Map<String, Object>> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        int year = timeCode != null && timeCode.length() >= 4 ? 
            Integer.parseInt(timeCode.substring(0, 4)) : cal.get(Calendar.YEAR);
        int month = timeCode != null && timeCode.length() >= 7 ? 
            Integer.parseInt(timeCode.substring(5, 7)) : cal.get(Calendar.MONTH) + 1;
        int day = timeCode != null && timeCode.length() >= 10 ? 
            Integer.parseInt(timeCode.substring(8, 10)) : cal.get(Calendar.DAY_OF_MONTH);

        int count = "DAY".equalsIgnoreCase(timeType) ? 24 : 
                   "MONTH".equalsIgnoreCase(timeType) ? cal.getActualMaximum(Calendar.DAY_OF_MONTH) : 12;

        for (int i = 0; i < count; i++) {
            Map<String, Object> item = new HashMap<>();
            
            if ("DAY".equalsIgnoreCase(timeType)) {
                item.put("timeCode", String.format("%d时", i));
            } else if ("MONTH".equalsIgnoreCase(timeType)) {
                item.put("timeCode", String.format("%d日", i + 1));
            } else {
                item.put("timeCode", String.format("%d月", i + 1));
            }

            item.put("valueA", String.format("%.1f", baseValue + Math.random() * variation));
            item.put("valueB", String.format("%.1f", baseValue + Math.random() * variation));
            item.put("valueC", String.format("%.1f", baseValue + Math.random() * variation));
            result.add(item);
        }

        return result;
    }

    /**
     * 缓存数据类
     */
    private static class CachedData {
        Object data;
        long timestamp;

        CachedData(Object data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
