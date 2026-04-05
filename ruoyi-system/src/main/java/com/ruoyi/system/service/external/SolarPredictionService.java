package com.ruoyi.system.service.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 光伏发电预测服务
 * 基于Open-Meteo天气数据和自定义算法进行发电量预测
 */
@Service
public class SolarPredictionService {

    private static final Logger log = LoggerFactory.getLogger(SolarPredictionService.class);
    
    @Autowired
    private OpenMeteoService openMeteoService;
    
    @Value("${solar.default-latitude:23.1291}")
    private double defaultLatitude;
    
    @Value("${solar.default-longitude:113.2644}")
    private double defaultLongitude;
    
    /**
     * 预测指定电站的小时发电量
     * @param installedCapacity 装机容量 kWp
     * @param latitude 纬度
     * @param longitude 经度
     * @param tiltAngle 倾斜角度 (度)
     * @param azimuthAngle 方位角 (度, 南=180)
     * @param days 预测天数
     * @return 小时级发电预测列表
     */
    public List<Map<String, Object>> predictHourlyGeneration(
            double installedCapacity, double latitude, double longitude,
            double tiltAngle, double azimuthAngle, int days) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取小时级天气预报
        List<Map<String, Object>> hourlyForecast = openMeteoService.getHourlyForecast(latitude, longitude, days);
        
        for (Map<String, Object> hourData : hourlyForecast) {
            Map<String, Object> prediction = new HashMap<>();
            
            String timeStr = (String) hourData.get("time");
            prediction.put("time", timeStr);
            
            // 获取辐射数据
            double ghi = toDouble(hourData.get("shortwaveRadiation"));      // W/m²
            double dni = toDouble(hourData.get("directNormalIrradiance")); // W/m²
            double dhi = toDouble(hourData.get("diffuseRadiation"));       // W/m²
            
            // 获取环境参数
            double temperature = toDouble(hourData.get("temperature"));
            double cloudCover = toDouble(hourData.get("cloudCover"));
            int weatherCode = toInt(hourData.get("weatherCode"));
            
            // 计算倾斜面辐射 (POA - Plane of Array)
            double poaIrradiance = calculatePOAIrradiance(ghi, dni, dhi, latitude, tiltAngle, azimuthAngle, timeStr);
            prediction.put("poaIrradiance", Math.round(poaIrradiance * 10) / 10.0);
            
            // 计算温度损失系数
            double tempLoss = calculateTemperatureLoss(temperature, poaIrradiance);
            prediction.put("temperatureLoss", Math.round(tempLoss * 1000) / 10.0); // 百分比
            
            // 计算其他损失
            double otherLosses = calculateOtherLosses(cloudCover, weatherCode);
            prediction.put("otherLosses", Math.round(otherLosses * 1000) / 10.0);
            
            // 计算预测发电量 (kWh)
            double efficiency = (1 - tempLoss) * (1 - otherLosses);
            double predictedPower = installedCapacity * (poaIrradiance / 1000.0) * efficiency;
            predictedPower = Math.max(0, predictedPower);
            prediction.put("predictedPower", Math.round(predictedPower * 100) / 100.0);
            
            // 置信区间
            double uncertainty = calculateUncertainty(cloudCover, weatherCode);
            prediction.put("lowerBound", Math.round(predictedPower * (1 - uncertainty) * 100) / 100.0);
            prediction.put("upperBound", Math.round(predictedPower * (1 + uncertainty) * 100) / 100.0);
            prediction.put("confidence", Math.round((1 - uncertainty) * 100));
            
            prediction.put("weatherCode", weatherCode);
            prediction.put("weatherDescription", openMeteoService.getWeatherDescription(weatherCode));
            prediction.put("temperature", temperature);
            prediction.put("cloudCover", cloudCover);
            
            result.add(prediction);
        }
        
        return result;
    }
    
    /**
     * 预测指定电站的每日发电量
     * @param installedCapacity 装机容量 kWp
     * @param latitude 纬度
     * @param longitude 经度
     * @param tiltAngle 倾斜角度
     * @param azimuthAngle 方位角
     * @param days 预测天数
     * @return 每日发电预测列表
     */
    public List<Map<String, Object>> predictDailyGeneration(
            double installedCapacity, double latitude, double longitude,
            double tiltAngle, double azimuthAngle, int days) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取每日天气预报
        List<Map<String, Object>> dailyForecast = openMeteoService.getDailyForecast(latitude, longitude, days);
        
        for (Map<String, Object> dayData : dailyForecast) {
            Map<String, Object> prediction = new HashMap<>();
            
            String date = (String) dayData.get("date");
            prediction.put("date", date);
            
            // 获取日辐射总量 (MJ/m² -> kWh/m²)
            double shortwaveSum = toDouble(dayData.get("shortwaveRadiationSum"));
            double dailyGHI = shortwaveSum / 3.6; // MJ -> kWh
            prediction.put("dailyGHI", Math.round(dailyGHI * 100) / 100.0);
            
            // 获取日照时长
            double sunshineDuration = toDouble(dayData.get("sunshineDuration")) / 3600; // 秒 -> 小时
            prediction.put("sunshineDuration", Math.round(sunshineDuration * 10) / 10.0);
            
            // 获取温度
            double tempMax = toDouble(dayData.get("temperatureMax"));
            double tempMin = toDouble(dayData.get("temperatureMin"));
            double avgTemp = (tempMax + tempMin) / 2;
            prediction.put("temperatureMax", tempMax);
            prediction.put("temperatureMin", tempMin);
            prediction.put("temperatureAvg", Math.round(avgTemp * 10) / 10.0);
            
            int weatherCode = toInt(dayData.get("weatherCode"));
            prediction.put("weatherCode", weatherCode);
            prediction.put("weatherDescription", openMeteoService.getWeatherDescription(weatherCode));
            
            // 估算倾斜面日辐射量
            double poaDaily = estimateDailyPOA(dailyGHI, latitude, tiltAngle, azimuthAngle, date);
            prediction.put("poaDaily", Math.round(poaDaily * 100) / 100.0);
            
            // 计算系统效率
            double systemEfficiency = calculateDailySystemEfficiency(avgTemp, weatherCode, poaDaily);
            prediction.put("systemEfficiency", Math.round(systemEfficiency * 1000) / 10.0);
            
            // 计算预测发电量 (kWh)
            double predictedGeneration = installedCapacity * poaDaily * systemEfficiency;
            predictedGeneration = Math.max(0, predictedGeneration);
            prediction.put("predictedGeneration", Math.round(predictedGeneration * 10) / 10.0);
            
            // 置信区间
            double uncertainty = calculateDailyUncertainty(weatherCode);
            prediction.put("lowerBound", Math.round(predictedGeneration * (1 - uncertainty) * 10) / 10.0);
            prediction.put("upperBound", Math.round(predictedGeneration * (1 + uncertainty) * 10) / 10.0);
            prediction.put("confidence", Math.round((1 - uncertainty) * 100));
            
            result.add(prediction);
        }
        
        return result;
    }
    
    /**
     * 使用默认参数预测发电量
     */
    public List<Map<String, Object>> predictDailyGeneration(double installedCapacity, int days) {
        return predictDailyGeneration(installedCapacity, defaultLatitude, defaultLongitude, 25, 180, days);
    }
    
    /**
     * 获取实时发电功率预测
     * @param installedCapacity 装机容量 kWp
     * @param latitude 纬度
     * @param longitude 经度
     * @return 当前预测功率
     */
    public Map<String, Object> predictCurrentPower(double installedCapacity, double latitude, double longitude) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取当前天气
        Map<String, Object> currentWeather = openMeteoService.getCurrentWeather(latitude, longitude);
        
        double ghi = toDouble(currentWeather.get("shortwaveRadiation"));
        double directRadiation = toDouble(currentWeather.get("directRadiation"));
        double diffuseRadiation = toDouble(currentWeather.get("diffuseRadiation"));
        double temperature = toDouble(currentWeather.get("temperature"));
        double cloudCover = toDouble(currentWeather.get("cloudCover"));
        int weatherCode = toInt(currentWeather.get("weatherCode"));
        
        result.put("ghi", ghi);
        result.put("directRadiation", directRadiation);
        result.put("diffuseRadiation", diffuseRadiation);
        result.put("temperature", temperature);
        result.put("cloudCover", cloudCover);
        result.put("weatherCode", weatherCode);
        result.put("weatherDescription", openMeteoService.getWeatherDescription(weatherCode));
        
        // Open-Meteo返回的shortwave_radiation已经是实际测量值，已包含云量影响
        // 因此不需要再乘以cloudLoss系数，只需考虑温度损失和其他固定损失
        double tempLoss = calculateTemperatureLoss(temperature, ghi);
        double otherLoss = 0.05; // 其他固定损失（灰尘、线损等）
        
        // 逆变器效率（根据负载率变化，这里简化为固定值）
        double inverterEfficiency = 0.96;
        
        double efficiency = (1 - tempLoss) * (1 - otherLoss) * inverterEfficiency;
        double predictedPower = installedCapacity * (ghi / 1000.0) * efficiency;
        predictedPower = Math.max(0, predictedPower);
        
        result.put("predictedPower", Math.round(predictedPower * 100) / 100.0);
        result.put("efficiency", Math.round(efficiency * 1000) / 10.0);
        result.put("time", currentWeather.get("time"));
        
        return result;
    }
    
    /**
     * 使用默认坐标预测当前功率
     */
    public Map<String, Object> predictCurrentPower(double installedCapacity) {
        return predictCurrentPower(installedCapacity, defaultLatitude, defaultLongitude);
    }
    
    /**
     * 计算倾斜面辐射 (简化模型)
     */
    private double calculatePOAIrradiance(double ghi, double dni, double dhi, 
                                          double latitude, double tiltAngle, 
                                          double azimuthAngle, String timeStr) {
        if (ghi <= 0) return 0;
        
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            int dayOfYear = dateTime.getDayOfYear();
            int hour = dateTime.getHour();
            
            // 太阳赤纬角
            double declination = 23.45 * Math.sin(Math.toRadians(360.0 / 365 * (dayOfYear - 81)));
            
            // 时角
            double hourAngle = 15 * (hour - 12);
            
            // 太阳高度角
            double latRad = Math.toRadians(latitude);
            double decRad = Math.toRadians(declination);
            double hourRad = Math.toRadians(hourAngle);
            
            double sinAltitude = Math.sin(latRad) * Math.sin(decRad) + 
                                 Math.cos(latRad) * Math.cos(decRad) * Math.cos(hourRad);
            double altitude = Math.toDegrees(Math.asin(sinAltitude));
            
            if (altitude <= 0) return 0; // 太阳在地平线以下
            
            // 太阳方位角
            double cosAzimuth = (Math.sin(decRad) - Math.sin(latRad) * sinAltitude) / 
                               (Math.cos(latRad) * Math.cos(Math.asin(sinAltitude)));
            cosAzimuth = Math.max(-1, Math.min(1, cosAzimuth));
            double solarAzimuth = Math.toDegrees(Math.acos(cosAzimuth));
            if (hour > 12) solarAzimuth = 360 - solarAzimuth;
            
            // 入射角
            double tiltRad = Math.toRadians(tiltAngle);
            double altRad = Math.toRadians(altitude);
            double azDiff = Math.toRadians(solarAzimuth - azimuthAngle);
            
            double cosIncidence = Math.sin(altRad) * Math.cos(tiltRad) + 
                                  Math.cos(altRad) * Math.sin(tiltRad) * Math.cos(azDiff);
            cosIncidence = Math.max(0, cosIncidence);
            
            // 计算POA
            double directPOA = dni * cosIncidence;
            double diffusePOA = dhi * (1 + Math.cos(tiltRad)) / 2;
            double groundReflected = ghi * 0.2 * (1 - Math.cos(tiltRad)) / 2; // 地面反射
            
            return directPOA + diffusePOA + groundReflected;
            
        } catch (Exception e) {
            // 简化计算
            double tiltFactor = 1 + 0.1 * Math.cos(Math.toRadians(tiltAngle));
            return ghi * tiltFactor;
        }
    }
    
    /**
     * 估算每日倾斜面辐射量
     */
    private double estimateDailyPOA(double dailyGHI, double latitude, 
                                    double tiltAngle, double azimuthAngle, String date) {
        // 简化模型：基于纬度和倾角的转换因子
        double optimalTilt = Math.abs(latitude) * 0.9; // 最优倾角约为纬度的0.9倍
        double tiltDiff = Math.abs(tiltAngle - optimalTilt);
        double tiltFactor = 1 - tiltDiff * 0.005; // 每偏离1度损失0.5%
        tiltFactor = Math.max(0.8, Math.min(1.1, tiltFactor));
        
        // 方位角影响（南向最优）
        double azimuthDiff = Math.abs(azimuthAngle - 180);
        double azimuthFactor = 1 - azimuthDiff * 0.002; // 每偏离1度损失0.2%
        azimuthFactor = Math.max(0.85, Math.min(1.0, azimuthFactor));
        
        return dailyGHI * tiltFactor * azimuthFactor;
    }
    
    /**
     * 计算温度损失系数
     * @param temperature 环境温度 °C
     * @param irradiance 辐射强度 W/m²
     * @return 损失系数 (0-1)
     */
    private double calculateTemperatureLoss(double temperature, double irradiance) {
        // 电池温度 = 环境温度 + 辐射 * NOCT系数
        double cellTemp = temperature + irradiance * 0.03; // 简化NOCT模型
        
        // 温度系数：每升高1°C，效率下降约0.4%
        double stcTemp = 25; // 标准测试条件温度
        double tempCoeff = -0.004; // 功率温度系数
        
        double loss = (cellTemp - stcTemp) * Math.abs(tempCoeff);
        return Math.max(0, Math.min(0.3, loss)); // 限制在0-30%
    }
    
    /**
     * 计算其他损失
     */
    private double calculateOtherLosses(double cloudCover, int weatherCode) {
        double loss = 0.02; // 基础损失（灰尘、线损等）
        
        // 云量影响
        loss += cloudCover * 0.001; // 每1%云量增加0.1%损失
        
        // 天气影响
        if (weatherCode >= 51 && weatherCode <= 67) { // 雨
            loss += 0.05;
        } else if (weatherCode >= 71 && weatherCode <= 77) { // 雪
            loss += 0.1;
        } else if (weatherCode >= 95) { // 雷暴
            loss += 0.15;
        }
        
        return Math.min(0.5, loss);
    }
    
    /**
     * 计算每日系统效率
     */
    private double calculateDailySystemEfficiency(double avgTemp, int weatherCode, double poaDaily) {
        // 基础效率
        double baseEfficiency = 0.85;
        
        // 温度影响
        double tempLoss = (avgTemp - 25) * 0.004;
        tempLoss = Math.max(-0.05, Math.min(0.15, tempLoss));
        
        // 天气影响
        double weatherLoss = 0;
        if (weatherCode >= 45 && weatherCode <= 48) weatherLoss = 0.05; // 雾
        else if (weatherCode >= 51 && weatherCode <= 67) weatherLoss = 0.08; // 雨
        else if (weatherCode >= 71 && weatherCode <= 77) weatherLoss = 0.15; // 雪
        else if (weatherCode >= 80 && weatherCode <= 82) weatherLoss = 0.1; // 阵雨
        else if (weatherCode >= 95) weatherLoss = 0.2; // 雷暴
        
        // 低辐射损失
        double lowIrradianceLoss = poaDaily < 2 ? 0.1 : (poaDaily < 4 ? 0.05 : 0);
        
        return baseEfficiency * (1 - tempLoss) * (1 - weatherLoss) * (1 - lowIrradianceLoss);
    }
    
    /**
     * 计算预测不确定性
     */
    private double calculateUncertainty(double cloudCover, int weatherCode) {
        double uncertainty = 0.1; // 基础不确定性10%
        
        // 云量增加不确定性
        uncertainty += cloudCover * 0.002;
        
        // 恶劣天气增加不确定性
        if (weatherCode >= 51) uncertainty += 0.1;
        if (weatherCode >= 80) uncertainty += 0.1;
        
        return Math.min(0.5, uncertainty);
    }
    
    /**
     * 计算每日预测不确定性
     */
    private double calculateDailyUncertainty(int weatherCode) {
        double uncertainty = 0.15; // 每日预测基础不确定性
        
        if (weatherCode >= 3) uncertainty += 0.05;  // 多云
        if (weatherCode >= 45) uncertainty += 0.1;  // 雾/雨/雪
        if (weatherCode >= 80) uncertainty += 0.1;  // 阵雨/雷暴
        
        return Math.min(0.4, uncertainty);
    }
    
    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (Exception e) { return 0.0; }
    }
    
    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (Exception e) { return 0; }
    }
}
