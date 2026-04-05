package com.ruoyi.system.service.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 历史数据服务
 * 集成Kaggle和UNISOLAR数据集用于历史训练和分析
 * 
 * 支持的数据集:
 * - Kaggle Solar Power Generation Data
 * - UNISOLAR (Universal Solar Irradiance Data)
 */
@Service
public class HistoricalDataService {

    private static final Logger log = LoggerFactory.getLogger(HistoricalDataService.class);
    
    @Value("${historical.data.path:data/historical}")
    private String dataPath;
    
    @Value("${historical.kaggle.path:data/kaggle}")
    private String kagglePath;
    
    @Value("${historical.unisolar.path:data/unisolar}")
    private String unisolarPath;
    
    @Autowired
    private OpenMeteoService openMeteoService;
    
    // 缓存加载的数据
    private Map<String, List<Map<String, Object>>> dataCache = new HashMap<>();
    
    /**
     * 加载Kaggle太阳能发电数据
     * 数据集: Solar Power Generation Data
     * 字段: DATE_TIME, PLANT_ID, SOURCE_KEY, DC_POWER, AC_POWER, DAILY_YIELD, TOTAL_YIELD
     */
    public List<Map<String, Object>> loadKaggleSolarData(String plantId) {
        String cacheKey = "kaggle_" + plantId;
        if (dataCache.containsKey(cacheKey)) {
            return dataCache.get(cacheKey);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            Path filePath = Paths.get(kagglePath, "Plant_" + plantId + "_Generation_Data.csv");
            if (!Files.exists(filePath)) {
                // 尝试通用文件名
                filePath = Paths.get(kagglePath, "generation_data.csv");
            }
            
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                if (lines.size() > 1) {
                    String[] headers = lines.get(0).split(",");
                    
                    for (int i = 1; i < lines.size(); i++) {
                        String[] values = lines.get(i).split(",");
                        Map<String, Object> row = new HashMap<>();
                        
                        for (int j = 0; j < Math.min(headers.length, values.length); j++) {
                            String header = headers[j].trim().toUpperCase();
                            String value = values[j].trim();
                            
                            if (header.contains("POWER") || header.contains("YIELD") || 
                                header.contains("IRRADIATION") || header.contains("TEMP")) {
                                row.put(header, parseDouble(value));
                            } else {
                                row.put(header, value);
                            }
                        }
                        
                        // 过滤指定电站
                        if (plantId == null || plantId.isEmpty() || 
                            plantId.equals(row.get("PLANT_ID"))) {
                            result.add(row);
                        }
                    }
                }
            } else {
                log.warn("Kaggle数据文件不存在: {}", filePath);
                // 返回模拟的历史数据用于演示
                result = generateSampleKaggleData(plantId);
            }
        } catch (Exception e) {
            log.error("加载Kaggle数据失败: {}", e.getMessage());
            result = generateSampleKaggleData(plantId);
        }
        
        dataCache.put(cacheKey, result);
        return result;
    }
    
    /**
     * 加载UNISOLAR辐射数据
     * 数据集: Universal Solar Irradiance Data
     * 字段: timestamp, ghi, dni, dhi, temp_air, wind_speed, humidity
     */
    public List<Map<String, Object>> loadUnisolarData(String location, String startDate, String endDate) {
        String cacheKey = "unisolar_" + location + "_" + startDate + "_" + endDate;
        if (dataCache.containsKey(cacheKey)) {
            return dataCache.get(cacheKey);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            Path filePath = Paths.get(unisolarPath, location + "_irradiance.csv");
            if (!Files.exists(filePath)) {
                filePath = Paths.get(unisolarPath, "irradiance_data.csv");
            }
            
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                if (lines.size() > 1) {
                    String[] headers = lines.get(0).split(",");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    LocalDate start = startDate != null ? LocalDate.parse(startDate, formatter) : LocalDate.MIN;
                    LocalDate end = endDate != null ? LocalDate.parse(endDate, formatter) : LocalDate.MAX;
                    
                    for (int i = 1; i < lines.size(); i++) {
                        String[] values = lines.get(i).split(",");
                        Map<String, Object> row = new HashMap<>();
                        
                        for (int j = 0; j < Math.min(headers.length, values.length); j++) {
                            String header = headers[j].trim().toLowerCase();
                            String value = values[j].trim();
                            
                            if (header.equals("timestamp") || header.equals("date")) {
                                row.put("timestamp", value);
                            } else {
                                row.put(header, parseDouble(value));
                            }
                        }
                        
                        // 日期过滤
                        String timestamp = (String) row.get("timestamp");
                        if (timestamp != null && timestamp.length() >= 10) {
                            LocalDate rowDate = LocalDate.parse(timestamp.substring(0, 10), formatter);
                            if (!rowDate.isBefore(start) && !rowDate.isAfter(end)) {
                                result.add(row);
                            }
                        }
                    }
                }
            } else {
                log.warn("UNISOLAR数据文件不存在: {}", filePath);
                // 使用Open-Meteo历史数据作为替代
                result = loadHistoricalFromOpenMeteo(location, startDate, endDate);
            }
        } catch (Exception e) {
            log.error("加载UNISOLAR数据失败: {}", e.getMessage());
            result = loadHistoricalFromOpenMeteo(location, startDate, endDate);
        }
        
        dataCache.put(cacheKey, result);
        return result;
    }
    
    /**
     * 从Open-Meteo获取历史天气数据作为替代
     */
    private List<Map<String, Object>> loadHistoricalFromOpenMeteo(String location, String startDate, String endDate) {
        // 解析位置坐标（格式: "lat,lon" 或使用默认值）
        double latitude = 23.1291;
        double longitude = 113.2644;
        
        if (location != null && location.contains(",")) {
            String[] parts = location.split(",");
            if (parts.length >= 2) {
                latitude = parseDouble(parts[0]);
                longitude = parseDouble(parts[1]);
            }
        }
        
        return openMeteoService.getHistoricalWeather(latitude, longitude, startDate, endDate);
    }
    
    /**
     * 获取历史发电量统计
     * @param plantId 电站ID
     * @param timeType DAY/MONTH/YEAR
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 聚合后的发电量数据
     */
    public List<Map<String, Object>> getHistoricalGenerationStats(
            String plantId, String timeType, String startDate, String endDate) {
        
        List<Map<String, Object>> rawData = loadKaggleSolarData(plantId);
        
        // 按时间聚合
        Map<String, Double> aggregated = new LinkedHashMap<>();
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter outputFormatter;
        
        switch (timeType.toUpperCase()) {
            case "DAY":
                outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
                break;
            case "MONTH":
                outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                break;
            case "YEAR":
                outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
                break;
            default:
                outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        }
        
        for (Map<String, Object> row : rawData) {
            String dateTime = (String) row.get("DATE_TIME");
            if (dateTime == null) continue;
            
            try {
                String bucket;
                if (dateTime.length() >= 10) {
                    bucket = dateTime.substring(0, timeType.equals("DAY") ? 13 : 
                                               timeType.equals("MONTH") ? 10 : 7);
                } else {
                    continue;
                }
                
                double yield = toDouble(row.get("DAILY_YIELD"));
                if (yield <= 0) {
                    yield = toDouble(row.get("AC_POWER")) / 1000.0; // 转换为kWh
                }
                
                aggregated.merge(bucket, yield, Double::sum);
            } catch (Exception e) {
                // 跳过解析错误的行
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : aggregated.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("timeBucket", entry.getKey());
            item.put("sumValue", Math.round(entry.getValue() * 100) / 100.0);
            result.add(item);
        }
        
        return result;
    }
    
    /**
     * 获取历史辐射统计
     */
    public List<Map<String, Object>> getHistoricalIrradianceStats(
            String location, String timeType, String startDate, String endDate) {
        
        List<Map<String, Object>> rawData = loadUnisolarData(location, startDate, endDate);
        
        Map<String, Map<String, Double>> aggregated = new LinkedHashMap<>();
        
        for (Map<String, Object> row : rawData) {
            String timestamp = (String) row.get("timestamp");
            if (timestamp == null) timestamp = (String) row.get("date");
            if (timestamp == null) continue;
            
            String bucket;
            if (timestamp.length() >= 10) {
                bucket = timestamp.substring(0, timeType.equals("DAY") ? 13 : 
                                           timeType.equals("MONTH") ? 10 : 7);
            } else {
                continue;
            }
            
            aggregated.computeIfAbsent(bucket, k -> new HashMap<>());
            Map<String, Double> bucketData = aggregated.get(bucket);
            
            double ghi = toDouble(row.get("ghi"));
            double dni = toDouble(row.get("dni"));
            double dhi = toDouble(row.get("dhi"));
            double temp = toDouble(row.get("temp_air"));
            if (temp == 0) temp = toDouble(row.get("temperatureMax"));
            
            bucketData.merge("ghi", ghi, Double::sum);
            bucketData.merge("dni", dni, Double::sum);
            bucketData.merge("dhi", dhi, Double::sum);
            bucketData.merge("temp", temp, (a, b) -> (a + b) / 2);
            bucketData.merge("count", 1.0, Double::sum);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entry : aggregated.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("timeBucket", entry.getKey());
            Map<String, Double> data = entry.getValue();
            double count = data.getOrDefault("count", 1.0);
            item.put("avgGHI", Math.round(data.getOrDefault("ghi", 0.0) / count * 10) / 10.0);
            item.put("avgDNI", Math.round(data.getOrDefault("dni", 0.0) / count * 10) / 10.0);
            item.put("avgDHI", Math.round(data.getOrDefault("dhi", 0.0) / count * 10) / 10.0);
            item.put("avgTemp", Math.round(data.getOrDefault("temp", 0.0) * 10) / 10.0);
            result.add(item);
        }
        
        return result;
    }
    
    /**
     * 训练数据准备 - 合并发电量和辐射数据
     */
    public List<Map<String, Object>> prepareTrainingData(
            String plantId, String location, String startDate, String endDate) {
        
        List<Map<String, Object>> generationData = getHistoricalGenerationStats(plantId, "DAY", startDate, endDate);
        List<Map<String, Object>> irradianceData = getHistoricalIrradianceStats(location, "DAY", startDate, endDate);
        
        // 按日期合并
        Map<String, Map<String, Object>> irradianceMap = irradianceData.stream()
            .collect(Collectors.toMap(
                m -> (String) m.get("timeBucket"),
                m -> m,
                (a, b) -> a
            ));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> gen : generationData) {
            String date = (String) gen.get("timeBucket");
            Map<String, Object> combined = new HashMap<>(gen);
            
            Map<String, Object> irr = irradianceMap.get(date);
            if (irr != null) {
                combined.put("avgGHI", irr.get("avgGHI"));
                combined.put("avgDNI", irr.get("avgDNI"));
                combined.put("avgDHI", irr.get("avgDHI"));
                combined.put("avgTemp", irr.get("avgTemp"));
            }
            
            result.add(combined);
        }
        
        return result;
    }
    
    /**
     * 生成示例Kaggle数据（当实际数据不可用时）
     */
    private List<Map<String, Object>> generateSampleKaggleData(String plantId) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        LocalDate startDate = LocalDate.now().minusYears(1);
        LocalDate endDate = LocalDate.now();
        
        Random random = new Random(plantId != null ? plantId.hashCode() : 42);
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // 每天生成24小时数据
            for (int hour = 0; hour < 24; hour++) {
                Map<String, Object> row = new HashMap<>();
                row.put("DATE_TIME", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + 
                        String.format(" %02d:00:00", hour));
                row.put("PLANT_ID", plantId != null ? plantId : "PLANT_1");
                
                // 只有白天有发电
                double dcPower = 0;
                double acPower = 0;
                if (hour >= 6 && hour <= 18) {
                    // 模拟日间发电曲线
                    double hourFactor = 1 - Math.pow((hour - 12) / 6.0, 2);
                    double basePower = 50000 * hourFactor; // 基础功率50kW
                    double variation = random.nextGaussian() * 5000;
                    dcPower = Math.max(0, basePower + variation);
                    acPower = dcPower * 0.95; // 逆变器效率
                }
                
                row.put("DC_POWER", dcPower);
                row.put("AC_POWER", acPower);
                row.put("DAILY_YIELD", acPower / 1000.0); // kWh
                row.put("TOTAL_YIELD", 0.0); // 累计值需要计算
                
                // 添加环境数据
                row.put("AMBIENT_TEMPERATURE", 25 + random.nextGaussian() * 5);
                row.put("MODULE_TEMPERATURE", 35 + random.nextGaussian() * 8);
                row.put("IRRADIATION", hour >= 6 && hour <= 18 ? 
                        (500 + random.nextDouble() * 500) * (1 - Math.pow((hour - 12) / 6.0, 2)) : 0);
                
                result.add(row);
            }
        }
        
        return result;
    }
    
    /**
     * 清除数据缓存
     */
    public void clearCache() {
        dataCache.clear();
    }
    
    /**
     * 获取可用的数据集列表
     */
    public List<Map<String, Object>> getAvailableDatasets() {
        List<Map<String, Object>> datasets = new ArrayList<>();
        
        // Kaggle数据集
        Map<String, Object> kaggle = new HashMap<>();
        kaggle.put("name", "Kaggle Solar Power Generation");
        kaggle.put("type", "generation");
        kaggle.put("source", "kaggle");
        kaggle.put("path", kagglePath);
        kaggle.put("available", Files.exists(Paths.get(kagglePath)));
        datasets.add(kaggle);
        
        // UNISOLAR数据集
        Map<String, Object> unisolar = new HashMap<>();
        unisolar.put("name", "UNISOLAR Irradiance Data");
        unisolar.put("type", "irradiance");
        unisolar.put("source", "unisolar");
        unisolar.put("path", unisolarPath);
        unisolar.put("available", Files.exists(Paths.get(unisolarPath)));
        datasets.add(unisolar);
        
        // Open-Meteo历史数据（始终可用）
        Map<String, Object> openMeteo = new HashMap<>();
        openMeteo.put("name", "Open-Meteo Historical Weather");
        openMeteo.put("type", "weather");
        openMeteo.put("source", "open-meteo");
        openMeteo.put("path", "API");
        openMeteo.put("available", true);
        datasets.add(openMeteo);
        
        return datasets;
    }
    
    private double parseDouble(String value) {
        try {
            return value != null && !value.isEmpty() ? Double.parseDouble(value.trim()) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return parseDouble(value.toString());
    }
}
