package com.ruoyi.system.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Open-Meteo API 服务
 * 用于获取天气预报数据，支持光伏发电预测
 * API文档: https://open-meteo.com/en/docs
 * 免费API，无需API Key
 */
@Service
public class OpenMeteoService {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoService.class);
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String HISTORICAL_URL = "https://archive-api.open-meteo.com/v1/archive";
    
    @Value("${solar.default-latitude:23.1291}")
    private double defaultLatitude;
    
    @Value("${solar.default-longitude:113.2644}")
    private double defaultLongitude;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // 缓存天气数据，减少API调用
    private final Map<String, CachedWeather> weatherCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 300000; // 5分钟缓存
    
    public OpenMeteoService() {
        // 配置超时时间
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 连接超时5秒
        factory.setReadTimeout(10000);    // 读取超时10秒
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }
    
    // 缓存数据结构
    private static class CachedWeather {
        Map<String, Object> data;
        long timestamp;
        
        CachedWeather(Map<String, Object> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    /**
     * 获取当前天气数据
     * @param latitude 纬度
     * @param longitude 经度
     * @return 当前天气数据
     */
    public Map<String, Object> getCurrentWeather(double latitude, double longitude) {
        String cacheKey = String.format("current_%.2f_%.2f", latitude, longitude);
        
        // 检查缓存
        CachedWeather cached = weatherCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String url = String.format(
                "%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,weather_code,cloud_cover,wind_speed_10m,wind_direction_10m,shortwave_radiation,direct_radiation,diffuse_radiation&timezone=Asia%%2FShanghai",
                FORECAST_URL, latitude, longitude);
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode current = root.get("current");
            
            if (current != null) {
                result.put("temperature", getDouble(current, "temperature_2m"));
                result.put("humidity", getDouble(current, "relative_humidity_2m"));
                result.put("apparentTemperature", getDouble(current, "apparent_temperature"));
                result.put("precipitation", getDouble(current, "precipitation"));
                result.put("rain", getDouble(current, "rain"));
                result.put("weatherCode", getInt(current, "weather_code"));
                result.put("cloudCover", getDouble(current, "cloud_cover"));
                result.put("windSpeed", getDouble(current, "wind_speed_10m"));
                result.put("windDirection", getDouble(current, "wind_direction_10m"));
                result.put("shortwaveRadiation", getDouble(current, "shortwave_radiation")); // W/m²
                result.put("directRadiation", getDouble(current, "direct_radiation"));       // W/m²
                result.put("diffuseRadiation", getDouble(current, "diffuse_radiation"));     // W/m²
                result.put("time", getString(current, "time"));
                
                // 缓存结果
                weatherCache.put(cacheKey, new CachedWeather(result));
            }
        } catch (Exception e) {
            log.warn("获取Open-Meteo当前天气失败，使用估算数据: {}", e.getMessage());
            // 返回基于时间的估算数据
            result = getEstimatedCurrentWeather(latitude);
        }
        
        return result;
    }
    
    /**
     * 获取估算的当前天气数据（当API不可用时）
     */
    private Map<String, Object> getEstimatedCurrentWeather(double latitude) {
        Map<String, Object> result = new HashMap<>();
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        
        // 基于时间估算太阳辐射
        double solarElevation = calculateSolarElevation(latitude, hour);
        double estimatedRadiation = Math.max(0, solarElevation * 10); // 简化估算
        
        // 估算温度（基于时间）
        double baseTemp = 20 + 5 * Math.sin((hour - 6) * Math.PI / 12);
        
        result.put("temperature", Math.round(baseTemp * 10) / 10.0);
        result.put("humidity", 60.0);
        result.put("cloudCover", 30.0);
        result.put("shortwaveRadiation", estimatedRadiation);
        result.put("directRadiation", estimatedRadiation * 0.7);
        result.put("diffuseRadiation", estimatedRadiation * 0.3);
        result.put("windSpeed", 3.0);
        result.put("estimated", true);
        result.put("time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * 计算太阳高度角（简化版）
     */
    private double calculateSolarElevation(double latitude, int hour) {
        // 简化计算：假设春分/秋分
        double declination = 0; // 简化
        double hourAngle = (hour - 12) * 15; // 度
        double latRad = Math.toRadians(latitude);
        double decRad = Math.toRadians(declination);
        double haRad = Math.toRadians(hourAngle);
        
        double sinElevation = Math.sin(latRad) * Math.sin(decRad) + 
                              Math.cos(latRad) * Math.cos(decRad) * Math.cos(haRad);
        return Math.toDegrees(Math.asin(sinElevation));
    }
    
    /**
     * 获取当前天气（使用默认坐标）
     */
    public Map<String, Object> getCurrentWeather() {
        return getCurrentWeather(defaultLatitude, defaultLongitude);
    }
    
    /**
     * 获取小时级天气预报（用于发电预测）
     * @param latitude 纬度
     * @param longitude 经度
     * @param days 预报天数 (1-16)
     * @return 小时级预报数据列表
     */
    public List<Map<String, Object>> getHourlyForecast(double latitude, double longitude, int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            String url = String.format(
                "%s?latitude=%.4f&longitude=%.4f&hourly=temperature_2m,relative_humidity_2m,precipitation_probability,precipitation,rain,weather_code,cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high,visibility,wind_speed_10m,shortwave_radiation,direct_radiation,diffuse_radiation,direct_normal_irradiance,global_tilted_irradiance,terrestrial_radiation&forecast_days=%d&timezone=Asia%%2FShanghai",
                FORECAST_URL, latitude, longitude, Math.min(days, 16));
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode hourly = root.get("hourly");
            
            if (hourly != null) {
                JsonNode times = hourly.get("time");
                JsonNode temps = hourly.get("temperature_2m");
                JsonNode humidity = hourly.get("relative_humidity_2m");
                JsonNode cloudCover = hourly.get("cloud_cover");
                JsonNode shortwave = hourly.get("shortwave_radiation");
                JsonNode direct = hourly.get("direct_radiation");
                JsonNode diffuse = hourly.get("diffuse_radiation");
                JsonNode dni = hourly.get("direct_normal_irradiance");
                JsonNode gti = hourly.get("global_tilted_irradiance");
                JsonNode weatherCode = hourly.get("weather_code");
                
                if (times != null) {
                    for (int i = 0; i < times.size(); i++) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("time", times.get(i).asText());
                        item.put("temperature", getArrayDouble(temps, i));
                        item.put("humidity", getArrayDouble(humidity, i));
                        item.put("cloudCover", getArrayDouble(cloudCover, i));
                        item.put("shortwaveRadiation", getArrayDouble(shortwave, i));
                        item.put("directRadiation", getArrayDouble(direct, i));
                        item.put("diffuseRadiation", getArrayDouble(diffuse, i));
                        item.put("directNormalIrradiance", getArrayDouble(dni, i));
                        item.put("globalTiltedIrradiance", getArrayDouble(gti, i));
                        item.put("weatherCode", getArrayInt(weatherCode, i));
                        result.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取Open-Meteo小时预报失败: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取小时级天气预报（使用默认坐标）
     */
    public List<Map<String, Object>> getHourlyForecast(int days) {
        return getHourlyForecast(defaultLatitude, defaultLongitude, days);
    }
    
    /**
     * 获取每日天气预报
     * @param latitude 纬度
     * @param longitude 经度
     * @param days 预报天数
     * @return 每日预报数据列表
     */
    public List<Map<String, Object>> getDailyForecast(double latitude, double longitude, int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            String url = String.format(
                "%s?latitude=%.4f&longitude=%.4f&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,daylight_duration,sunshine_duration,uv_index_max,precipitation_sum,rain_sum,precipitation_probability_max,wind_speed_10m_max,shortwave_radiation_sum&forecast_days=%d&timezone=Asia%%2FShanghai",
                FORECAST_URL, latitude, longitude, Math.min(days, 16));
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode daily = root.get("daily");
            
            if (daily != null) {
                JsonNode times = daily.get("time");
                JsonNode tempMax = daily.get("temperature_2m_max");
                JsonNode tempMin = daily.get("temperature_2m_min");
                JsonNode sunrise = daily.get("sunrise");
                JsonNode sunset = daily.get("sunset");
                JsonNode daylightDuration = daily.get("daylight_duration");
                JsonNode sunshineDuration = daily.get("sunshine_duration");
                JsonNode shortwaveSum = daily.get("shortwave_radiation_sum");
                JsonNode weatherCode = daily.get("weather_code");
                JsonNode precipSum = daily.get("precipitation_sum");
                
                if (times != null) {
                    for (int i = 0; i < times.size(); i++) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("date", times.get(i).asText());
                        item.put("temperatureMax", getArrayDouble(tempMax, i));
                        item.put("temperatureMin", getArrayDouble(tempMin, i));
                        item.put("sunrise", getArrayString(sunrise, i));
                        item.put("sunset", getArrayString(sunset, i));
                        item.put("daylightDuration", getArrayDouble(daylightDuration, i)); // 秒
                        item.put("sunshineDuration", getArrayDouble(sunshineDuration, i)); // 秒
                        item.put("shortwaveRadiationSum", getArrayDouble(shortwaveSum, i)); // MJ/m²
                        item.put("weatherCode", getArrayInt(weatherCode, i));
                        item.put("precipitationSum", getArrayDouble(precipSum, i));
                        result.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取Open-Meteo每日预报失败: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取每日天气预报（使用默认坐标）
     */
    public List<Map<String, Object>> getDailyForecast(int days) {
        return getDailyForecast(defaultLatitude, defaultLongitude, days);
    }
    
    /**
     * 获取历史天气数据
     * @param latitude 纬度
     * @param longitude 经度
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 历史天气数据列表
     */
    public List<Map<String, Object>> getHistoricalWeather(double latitude, double longitude, 
                                                           String startDate, String endDate) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            String url = String.format(
                "%s?latitude=%.4f&longitude=%.4f&start_date=%s&end_date=%s&daily=weather_code,temperature_2m_max,temperature_2m_min,sunshine_duration,shortwave_radiation_sum,precipitation_sum&timezone=Asia%%2FShanghai",
                HISTORICAL_URL, latitude, longitude, startDate, endDate);
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode daily = root.get("daily");
            
            if (daily != null) {
                JsonNode times = daily.get("time");
                JsonNode tempMax = daily.get("temperature_2m_max");
                JsonNode tempMin = daily.get("temperature_2m_min");
                JsonNode sunshineDuration = daily.get("sunshine_duration");
                JsonNode shortwaveSum = daily.get("shortwave_radiation_sum");
                JsonNode weatherCode = daily.get("weather_code");
                
                if (times != null) {
                    for (int i = 0; i < times.size(); i++) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("date", times.get(i).asText());
                        item.put("temperatureMax", getArrayDouble(tempMax, i));
                        item.put("temperatureMin", getArrayDouble(tempMin, i));
                        item.put("sunshineDuration", getArrayDouble(sunshineDuration, i));
                        item.put("shortwaveRadiationSum", getArrayDouble(shortwaveSum, i));
                        item.put("weatherCode", getArrayInt(weatherCode, i));
                        result.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取Open-Meteo历史天气失败: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 根据天气代码获取天气描述
     */
    public String getWeatherDescription(int code) {
        Map<Integer, String> weatherCodes = new HashMap<>();
        weatherCodes.put(0, "晴朗");
        weatherCodes.put(1, "大部晴朗");
        weatherCodes.put(2, "局部多云");
        weatherCodes.put(3, "多云");
        weatherCodes.put(45, "雾");
        weatherCodes.put(48, "雾凇");
        weatherCodes.put(51, "小毛毛雨");
        weatherCodes.put(53, "中毛毛雨");
        weatherCodes.put(55, "大毛毛雨");
        weatherCodes.put(61, "小雨");
        weatherCodes.put(63, "中雨");
        weatherCodes.put(65, "大雨");
        weatherCodes.put(71, "小雪");
        weatherCodes.put(73, "中雪");
        weatherCodes.put(75, "大雪");
        weatherCodes.put(80, "小阵雨");
        weatherCodes.put(81, "中阵雨");
        weatherCodes.put(82, "大阵雨");
        weatherCodes.put(95, "雷暴");
        weatherCodes.put(96, "雷暴伴小冰雹");
        weatherCodes.put(99, "雷暴伴大冰雹");
        
        return weatherCodes.getOrDefault(code, "未知");
    }
    
    /**
     * 估算云量对发电的影响系数 (0-1)
     * @param cloudCover 云量百分比 (0-100)
     * @return 发电效率系数
     */
    public double estimateCloudImpact(double cloudCover) {
        // 云量对发电的非线性影响
        // 0% 云量 -> 1.0 效率
        // 50% 云量 -> 约0.7 效率
        // 100% 云量 -> 约0.2 效率
        double factor = 1.0 - (cloudCover / 100.0) * 0.8;
        return Math.max(0.1, Math.min(1.0, factor));
    }
    
    private double getDouble(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asDouble() : 0.0;
    }
    
    private int getInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asInt() : 0;
    }
    
    private String getString(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : "";
    }
    
    private double getArrayDouble(JsonNode array, int index) {
        if (array != null && index < array.size() && !array.get(index).isNull()) {
            return array.get(index).asDouble();
        }
        return 0.0;
    }
    
    private int getArrayInt(JsonNode array, int index) {
        if (array != null && index < array.size() && !array.get(index).isNull()) {
            return array.get(index).asInt();
        }
        return 0;
    }
    
    private String getArrayString(JsonNode array, int index) {
        if (array != null && index < array.size() && !array.get(index).isNull()) {
            return array.get(index).asText();
        }
        return "";
    }
    
    // ==================== 太阳照射状态计算 ====================
    
    /**
     * 获取电站太阳照射状态
     * @param latitude 纬度
     * @param longitude 经度
     * @return 太阳照射状态信息
     */
    public Map<String, Object> getSolarIrradianceStatus(double latitude, double longitude) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> weather = getCurrentWeather(latitude, longitude);
        
        // 获取太阳辐射数据
        double shortwaveRadiation = ((Number) weather.getOrDefault("shortwaveRadiation", 0.0)).doubleValue();
        double directRadiation = ((Number) weather.getOrDefault("directRadiation", 0.0)).doubleValue();
        double diffuseRadiation = ((Number) weather.getOrDefault("diffuseRadiation", 0.0)).doubleValue();
        double cloudCover = ((Number) weather.getOrDefault("cloudCover", 0.0)).doubleValue();
        double temperature = ((Number) weather.getOrDefault("temperature", 25.0)).doubleValue();
        
        // 计算太阳高度角
        LocalDateTime now = LocalDateTime.now();
        double solarElevation = calculateSolarElevationAccurate(latitude, longitude, now);
        
        // 计算日出日落时间
        Map<String, LocalTime> sunTimes = calculateSunriseSunset(latitude, longitude, now.toLocalDate());
        LocalTime sunrise = sunTimes.get("sunrise");
        LocalTime sunset = sunTimes.get("sunset");
        LocalTime currentTime = now.toLocalTime();
        
        // 判断是否在日照时间内
        boolean isDaylight = currentTime.isAfter(sunrise) && currentTime.isBefore(sunset);
        
        // 判断太阳照射状态
        String sunStatus;
        String sunStatusCode;
        double generationPotential; // 发电潜力 0-100%
        
        if (!isDaylight) {
            sunStatus = "夜间";
            sunStatusCode = "NIGHT";
            generationPotential = 0;
        } else if (shortwaveRadiation >= 800) {
            sunStatus = "强烈日照";
            sunStatusCode = "STRONG";
            generationPotential = 95 + (shortwaveRadiation - 800) / 200 * 5;
        } else if (shortwaveRadiation >= 500) {
            sunStatus = "良好日照";
            sunStatusCode = "GOOD";
            generationPotential = 70 + (shortwaveRadiation - 500) / 300 * 25;
        } else if (shortwaveRadiation >= 200) {
            sunStatus = "中等日照";
            sunStatusCode = "MODERATE";
            generationPotential = 40 + (shortwaveRadiation - 200) / 300 * 30;
        } else if (shortwaveRadiation > 50) {
            sunStatus = "弱日照";
            sunStatusCode = "WEAK";
            generationPotential = 10 + (shortwaveRadiation - 50) / 150 * 30;
        } else {
            sunStatus = "无有效日照";
            sunStatusCode = "NONE";
            generationPotential = shortwaveRadiation / 50 * 10;
        }
        
        // 云量影响描述
        String cloudStatus;
        if (cloudCover < 10) {
            cloudStatus = "晴朗";
        } else if (cloudCover < 30) {
            cloudStatus = "少云";
        } else if (cloudCover < 60) {
            cloudStatus = "多云";
        } else if (cloudCover < 85) {
            cloudStatus = "阴天";
        } else {
            cloudStatus = "厚云遮蔽";
        }
        
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put("sunStatus", sunStatus);
        result.put("sunStatusCode", sunStatusCode);
        result.put("cloudStatus", cloudStatus);
        result.put("cloudCover", cloudCover);
        result.put("isDaylight", isDaylight);
        result.put("solarElevation", Math.round(solarElevation * 10) / 10.0);
        result.put("shortwaveRadiation", shortwaveRadiation);
        result.put("directRadiation", directRadiation);
        result.put("diffuseRadiation", diffuseRadiation);
        result.put("temperature", temperature);
        result.put("generationPotential", Math.min(100, Math.round(generationPotential * 10) / 10.0));
        result.put("sunrise", sunrise.format(DateTimeFormatter.ofPattern("HH:mm")));
        result.put("sunset", sunset.format(DateTimeFormatter.ofPattern("HH:mm")));
        result.put("currentTime", currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        result.put("updateTime", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return result;
    }
    
    /**
     * 精确计算太阳高度角
     */
    private double calculateSolarElevationAccurate(double latitude, double longitude, LocalDateTime dateTime) {
        int dayOfYear = dateTime.getDayOfYear();
        double hour = dateTime.getHour() + dateTime.getMinute() / 60.0;
        
        // 太阳赤纬角（度）
        double declination = 23.45 * Math.sin(Math.toRadians(360.0 / 365 * (dayOfYear - 81)));
        
        // 时角（度）- 考虑经度修正
        double solarNoon = 12 - longitude / 15; // 当地太阳正午时间
        double hourAngle = (hour - solarNoon) * 15;
        
        // 计算太阳高度角
        double latRad = Math.toRadians(latitude);
        double decRad = Math.toRadians(declination);
        double haRad = Math.toRadians(hourAngle);
        
        double sinElevation = Math.sin(latRad) * Math.sin(decRad) + 
                              Math.cos(latRad) * Math.cos(decRad) * Math.cos(haRad);
        return Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, sinElevation))));
    }
    
    /**
     * 计算日出日落时间
     */
    private Map<String, LocalTime> calculateSunriseSunset(double latitude, double longitude, LocalDate date) {
        Map<String, LocalTime> result = new HashMap<>();
        int dayOfYear = date.getDayOfYear();
        
        // 太阳赤纬角
        double declination = 23.45 * Math.sin(Math.toRadians(360.0 / 365 * (dayOfYear - 81)));
        
        double latRad = Math.toRadians(latitude);
        double decRad = Math.toRadians(declination);
        
        // 计算日出日落时角
        double cosHourAngle = -Math.tan(latRad) * Math.tan(decRad);
        cosHourAngle = Math.max(-1, Math.min(1, cosHourAngle)); // 限制范围
        
        double hourAngle = Math.toDegrees(Math.acos(cosHourAngle));
        
        // 经度修正
        double solarNoon = 12 - longitude / 15;
        
        double sunriseHour = solarNoon - hourAngle / 15;
        double sunsetHour = solarNoon + hourAngle / 15;
        
        // 转换为LocalTime
        int sunriseH = (int) sunriseHour;
        int sunriseM = (int) ((sunriseHour - sunriseH) * 60);
        int sunsetH = (int) sunsetHour;
        int sunsetM = (int) ((sunsetHour - sunsetH) * 60);
        
        result.put("sunrise", LocalTime.of(Math.max(0, Math.min(23, sunriseH)), Math.max(0, Math.min(59, sunriseM))));
        result.put("sunset", LocalTime.of(Math.max(0, Math.min(23, sunsetH)), Math.max(0, Math.min(59, sunsetM))));
        
        return result;
    }
}
