package com.ruoyi.system.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Solargis API 服务
 * 用于对标分析 - 提供高精度太阳辐射和光伏发电潜力数据
 * API文档: https://solargis.com/docs/api
 */
@Service
public class SolargisService {

    private static final Logger log = LoggerFactory.getLogger(SolargisService.class);
    private static final String BASE_URL = "https://api.solargis.com";
    
    @Value("${solargis.api-key:}")
    private String apiKey;
    
    @Value("${solar.default-latitude:23.1291}")
    private double defaultLatitude;
    
    @Value("${solar.default-longitude:113.2644}")
    private double defaultLongitude;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public SolargisService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 获取长期平均太阳辐射数据（用于对标分析）
     * @param latitude 纬度
     * @param longitude 经度
     * @return 长期平均数据
     */
    public Map<String, Object> getLongTermAverage(double latitude, double longitude) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isConfigured()) {
            // 返回基于地理位置的估算数据
            return estimateLongTermAverage(latitude, longitude);
        }
        
        try {
            String url = String.format(
                "%s/data/lta?lat=%.4f&lon=%.4f&params=GHI,DNI,DIF,TEMP,PVOUT",
                BASE_URL, latitude, longitude);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                result.put("ghi", getDouble(root, "GHI"));           // 全球水平辐射 kWh/m²
                result.put("dni", getDouble(root, "DNI"));           // 直接法向辐射 kWh/m²
                result.put("dif", getDouble(root, "DIF"));           // 散射辐射 kWh/m²
                result.put("temperature", getDouble(root, "TEMP"));  // 平均温度 °C
                result.put("pvout", getDouble(root, "PVOUT"));       // 光伏发电潜力 kWh/kWp
            }
        } catch (Exception e) {
            log.error("获取Solargis长期平均数据失败: {}", e.getMessage());
            return estimateLongTermAverage(latitude, longitude);
        }
        
        return result;
    }
    
    /**
     * 获取月度长期平均数据
     * @param latitude 纬度
     * @param longitude 经度
     * @return 12个月的平均数据列表
     */
    public List<Map<String, Object>> getMonthlyLongTermAverage(double latitude, double longitude) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (!isConfigured()) {
            return estimateMonthlyLongTermAverage(latitude, longitude);
        }
        
        try {
            String url = String.format(
                "%s/data/lta/monthly?lat=%.4f&lon=%.4f&params=GHI,DNI,DIF,TEMP,PVOUT",
                BASE_URL, latitude, longitude);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode months = root.get("monthly");
                
                if (months != null && months.isArray()) {
                    for (int i = 0; i < months.size(); i++) {
                        JsonNode month = months.get(i);
                        Map<String, Object> item = new HashMap<>();
                        item.put("month", i + 1);
                        item.put("ghi", getDouble(month, "GHI"));
                        item.put("dni", getDouble(month, "DNI"));
                        item.put("dif", getDouble(month, "DIF"));
                        item.put("temperature", getDouble(month, "TEMP"));
                        item.put("pvout", getDouble(month, "PVOUT"));
                        result.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取Solargis月度数据失败: {}", e.getMessage());
            return estimateMonthlyLongTermAverage(latitude, longitude);
        }
        
        return result;
    }
    
    /**
     * 计算性能比 (Performance Ratio)
     * @param actualGeneration 实际发电量 kWh
     * @param installedCapacity 装机容量 kWp
     * @param ghi 全球水平辐射 kWh/m²
     * @param stc 标准测试条件辐射 (通常为1 kW/m²)
     * @return 性能比 (0-1)
     */
    public double calculatePerformanceRatio(double actualGeneration, double installedCapacity, 
                                            double ghi, double stc) {
        if (installedCapacity <= 0 || ghi <= 0 || stc <= 0) {
            return 0.0;
        }
        // PR = 实际发电量 / (装机容量 × 辐射量 / STC辐射)
        double theoreticalGeneration = installedCapacity * (ghi / stc);
        return Math.min(1.0, actualGeneration / theoreticalGeneration);
    }
    
    /**
     * 计算容量因子 (Capacity Factor)
     * @param actualGeneration 实际发电量 kWh
     * @param installedCapacity 装机容量 kWp
     * @param hours 运行小时数
     * @return 容量因子 (0-1)
     */
    public double calculateCapacityFactor(double actualGeneration, double installedCapacity, double hours) {
        if (installedCapacity <= 0 || hours <= 0) {
            return 0.0;
        }
        return actualGeneration / (installedCapacity * hours);
    }
    
    /**
     * 对标分析 - 比较实际发电与理论发电
     * @param latitude 纬度
     * @param longitude 经度
     * @param installedCapacity 装机容量 kWp
     * @param actualMonthlyGeneration 实际月发电量列表 (12个月)
     * @return 对标分析结果
     */
    public Map<String, Object> benchmarkAnalysis(double latitude, double longitude,
                                                  double installedCapacity,
                                                  List<Double> actualMonthlyGeneration) {
        Map<String, Object> result = new HashMap<>();
        
        List<Map<String, Object>> monthlyLTA = getMonthlyLongTermAverage(latitude, longitude);
        
        List<Map<String, Object>> monthlyComparison = new ArrayList<>();
        double totalActual = 0;
        double totalTheoretical = 0;
        
        for (int i = 0; i < 12; i++) {
            Map<String, Object> comparison = new HashMap<>();
            comparison.put("month", i + 1);
            
            double actual = i < actualMonthlyGeneration.size() ? actualMonthlyGeneration.get(i) : 0;
            totalActual += actual;
            comparison.put("actualGeneration", actual);
            
            double pvout = 0;
            if (i < monthlyLTA.size()) {
                pvout = ((Number) monthlyLTA.get(i).getOrDefault("pvout", 0.0)).doubleValue();
            }
            double theoretical = pvout * installedCapacity;
            totalTheoretical += theoretical;
            comparison.put("theoreticalGeneration", Math.round(theoretical * 10) / 10.0);
            
            double deviation = theoretical > 0 ? (actual - theoretical) / theoretical * 100 : 0;
            comparison.put("deviation", Math.round(deviation * 10) / 10.0);
            
            String rating;
            if (deviation >= -5) rating = "优秀";
            else if (deviation >= -15) rating = "良好";
            else if (deviation >= -25) rating = "一般";
            else rating = "需改进";
            comparison.put("rating", rating);
            
            monthlyComparison.add(comparison);
        }
        
        result.put("monthlyComparison", monthlyComparison);
        result.put("totalActualGeneration", Math.round(totalActual * 10) / 10.0);
        result.put("totalTheoreticalGeneration", Math.round(totalTheoretical * 10) / 10.0);
        
        double overallDeviation = totalTheoretical > 0 ? 
            (totalActual - totalTheoretical) / totalTheoretical * 100 : 0;
        result.put("overallDeviation", Math.round(overallDeviation * 10) / 10.0);
        
        double pr = totalTheoretical > 0 ? totalActual / totalTheoretical : 0;
        result.put("performanceRatio", Math.round(pr * 1000) / 10.0); // 百分比
        
        return result;
    }
    
    /**
     * 检查API是否已配置
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
    
    /**
     * 基于地理位置估算长期平均数据（当API不可用时）
     */
    private Map<String, Object> estimateLongTermAverage(double latitude, double longitude) {
        Map<String, Object> result = new HashMap<>();
        
        // 基于纬度估算太阳辐射
        // 赤道附近辐射最强，随纬度增加而减少
        double latFactor = Math.cos(Math.toRadians(Math.abs(latitude)));
        
        // 年均GHI估算 (kWh/m²/年)
        // 赤道约2000-2200，中纬度约1200-1600
        double baseGHI = 1800;
        double ghi = baseGHI * latFactor * (0.8 + Math.random() * 0.2);
        result.put("ghi", Math.round(ghi * 10) / 10.0);
        
        // DNI通常是GHI的60-80%
        result.put("dni", Math.round(ghi * 0.7 * 10) / 10.0);
        
        // DIF是GHI的20-40%
        result.put("dif", Math.round(ghi * 0.3 * 10) / 10.0);
        
        // 温度估算
        double baseTemp = 25 - Math.abs(latitude) * 0.5;
        result.put("temperature", Math.round(baseTemp * 10) / 10.0);
        
        // PVOUT估算 (kWh/kWp/年)
        // 典型值: 1000-1800 kWh/kWp
        double pvout = ghi * 0.75 * 0.85; // 考虑系统效率
        result.put("pvout", Math.round(pvout * 10) / 10.0);
        
        result.put("source", "estimated");
        
        return result;
    }
    
    /**
     * 估算月度长期平均数据
     */
    private List<Map<String, Object>> estimateMonthlyLongTermAverage(double latitude, double longitude) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 月度辐射系数（北半球）
        double[] monthlyFactors = {0.7, 0.75, 0.85, 0.95, 1.05, 1.1, 1.1, 1.05, 0.95, 0.85, 0.75, 0.7};
        
        // 如果是南半球，调整季节
        if (latitude < 0) {
            double[] temp = new double[12];
            for (int i = 0; i < 12; i++) {
                temp[i] = monthlyFactors[(i + 6) % 12];
            }
            monthlyFactors = temp;
        }
        
        Map<String, Object> yearlyAvg = estimateLongTermAverage(latitude, longitude);
        double yearlyGHI = ((Number) yearlyAvg.get("ghi")).doubleValue();
        double yearlyPVOUT = ((Number) yearlyAvg.get("pvout")).doubleValue();
        
        for (int i = 0; i < 12; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", i + 1);
            
            double factor = monthlyFactors[i];
            double monthlyGHI = (yearlyGHI / 12) * factor;
            item.put("ghi", Math.round(monthlyGHI * 10) / 10.0);
            item.put("dni", Math.round(monthlyGHI * 0.7 * 10) / 10.0);
            item.put("dif", Math.round(monthlyGHI * 0.3 * 10) / 10.0);
            
            // 月度温度变化
            double baseTemp = ((Number) yearlyAvg.get("temperature")).doubleValue();
            double tempVariation = 10 * Math.cos(Math.toRadians((i - 6) * 30));
            if (latitude < 0) tempVariation = -tempVariation;
            item.put("temperature", Math.round((baseTemp + tempVariation) * 10) / 10.0);
            
            double monthlyPVOUT = (yearlyPVOUT / 12) * factor;
            item.put("pvout", Math.round(monthlyPVOUT * 10) / 10.0);
            
            result.add(item);
        }
        
        return result;
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    
    private double getDouble(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asDouble() : 0.0;
    }
}
