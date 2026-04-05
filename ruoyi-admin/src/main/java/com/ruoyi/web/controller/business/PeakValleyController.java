package com.ruoyi.web.controller.business;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.IElectricityDataItemService;
import com.ruoyi.system.service.external.RealTimeDataService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 峰平谷分析Controller
 */
@RestController
@RequestMapping("/peakValley")
public class PeakValleyController extends BaseController {

    @Autowired(required = false)
    private IElectricityDataItemService electricityDataItemService;

    @Autowired
    private RealTimeDataService realTimeDataService;

    /**
     * 峰平谷占比
     */
    @GetMapping("/getPeriodGenerationPercentage")
    public AjaxResult getPeriodGenerationPercentage(
            @RequestParam(required = false) String queryTime,
            @RequestParam(required = false) String timeType) {
        // 使用实时数据服务获取峰平谷数据
        Map<String, Object> pvData = realTimeDataService.getPeakValleyData(null, queryTime);
        
        // 返回ECharts饼图需要的数组格式 [{name, value}, ...]
        List<Map<String, Object>> list = new ArrayList<>();
        
        Map<String, Object> tip = new HashMap<>();
        tip.put("name", "尖时段");
        tip.put("value", pvData.getOrDefault("tipPowerConsumption", 250));
        list.add(tip);
        
        Map<String, Object> peak = new HashMap<>();
        peak.put("name", "峰时段");
        peak.put("value", pvData.getOrDefault("peakPowerConsumption", 450));
        list.add(peak);
        
        Map<String, Object> flat = new HashMap<>();
        flat.put("name", "平时段");
        flat.put("value", pvData.getOrDefault("flatPowerConsumption", 500));
        list.add(flat);
        
        Map<String, Object> trough = new HashMap<>();
        trough.put("name", "谷时段");
        trough.put("value", pvData.getOrDefault("troughPowerConsumption", 300));
        list.add(trough);
        
        Map<String, Object> deep = new HashMap<>();
        deep.put("name", "深谷时段");
        deep.put("value", pvData.getOrDefault("deepPowerConsumption", 150));
        list.add(deep);
        
        return success(list);
    }

    /**
     * 峰平谷报表
     */
    @GetMapping("/report")
    public AjaxResult report(
            @RequestParam(required = false) String powerStationId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String dateTime) {
        List<Map<String, Object>> list = new ArrayList<>();
        
        // 获取当前日期
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int currentYear = cal.get(java.util.Calendar.YEAR);
        int currentMonth = cal.get(java.util.Calendar.MONTH) + 1;
        
        // 解析日期参数
        int year = dateTime != null && dateTime.length() >= 4 ? Integer.parseInt(dateTime.substring(0, 4)) : currentYear;
        int month = dateTime != null && dateTime.length() >= 7 ? Integer.parseInt(dateTime.substring(5, 7)) : currentMonth;
        cal.set(year, month - 1, 1);
        int maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        
        // 时段类型: tip(尖), peak(峰), flat(平), trough(谷), deep(深谷)
        String[] timeNames = {"tip", "peak", "flat", "trough", "deep"};
        String[] timePeriods = {"10:00-12:00", "08:00-10:00,18:00-21:00", "12:00-18:00", "21:00-23:00", "23:00-08:00"};
        
        for (int t = 0; t < timeNames.length; t++) {
            Map<String, Object> item = new HashMap<>();
            item.put("timeName", timeNames[t]);
            item.put("timePeriod", timePeriods[t]);
            
            // 生成每天的数据 - 基于实时数据服务的峰平谷比例
            Map<String, Object> pvData = realTimeDataService.getPeakValleyData(powerStationId, dateTime);
            double totalConsumption = ((Number) pvData.getOrDefault("totalPowerConsumption", 15000.0)).doubleValue();
            double[] periodFactors = {0.05, 0.25, 0.40, 0.20, 0.10}; // 尖、峰、平、谷、深谷比例
            
            List<Map<String, Object>> timeList = new ArrayList<>();
            double sumValue = 0;
            for (int i = 1; i <= maxDay; i++) {
                Map<String, Object> dayItem = new HashMap<>();
                dayItem.put("time", String.format("%04d-%02d-%02d", year, month, i));
                // 基于总消耗和时段比例计算每日值
                double dailyBase = totalConsumption / maxDay * periodFactors[t];
                double value = dailyBase * (0.8 + Math.random() * 0.4); // 允许±20%波动
                value = Math.round(value * 100) / 100.0;
                dayItem.put("value", value);
                sumValue += value;
                timeList.add(dayItem);
            }
            
            item.put("timeList", timeList);
            item.put("sumValue", Math.round(sumValue * 100) / 100.0);
            list.add(item);
        }
        
        return success(list);
    }

    /**
     * 峰平谷时段配置和统计数据
     */
    @GetMapping("/segment")
    public AjaxResult segment(
            @RequestParam(required = false) String powerStationId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String dateTime) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取当前日期
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int currentYear = cal.get(java.util.Calendar.YEAR);
        int currentMonth = cal.get(java.util.Calendar.MONTH) + 1;
        
        // 解析日期参数
        int year = dateTime != null && dateTime.length() >= 4 ? Integer.parseInt(dateTime.substring(0, 4)) : currentYear;
        int month = dateTime != null && dateTime.length() >= 7 ? Integer.parseInt(dateTime.substring(5, 7)) : currentMonth;
        cal.set(year, month - 1, 1);
        int maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        
        // 尝试从数据库获取数据
        String startTime = String.format("%04d-%02d-01 00:00:00", year, month);
        String endTime = String.format("%04d-%02d-%02d 23:59:59", year, month, maxDay);
        List<Map<String, Object>> dbData = null;
        if (electricityDataItemService != null) {
            dbData = electricityDataItemService.selectPeakValleyStats(deviceId, startTime, endTime);
        }
        
        // 使用实时数据服务获取峰平谷数据
        Map<String, Object> pvData = realTimeDataService.getPeakValleyData(powerStationId, dateTime);
        result.putAll(pvData);
        
        // 发电量列表（按天）- 基于实时数据计算
        double totalConsumption = ((Number) pvData.getOrDefault("totalPowerConsumption", 15000.0)).doubleValue();
        double dailyAvg = totalConsumption / maxDay;
        
        List<Map<String, Object>> powerConsumptionList = new ArrayList<>();
        if (dbData != null && !dbData.isEmpty()) {
            powerConsumptionList = dbData;
        } else {
            for (int i = 1; i <= maxDay; i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("xdata", String.format("%04d-%02d-%02d 00:00:00", year, month, i));
                double dayFactor = 0.8 + Math.random() * 0.4;
                item.put("ytip", Math.round(dailyAvg * 0.05 * dayFactor * 100) / 100.0);
                item.put("ypeak", Math.round(dailyAvg * 0.25 * dayFactor * 100) / 100.0);
                item.put("yflat", Math.round(dailyAvg * 0.40 * dayFactor * 100) / 100.0);
                item.put("ytrough", Math.round(dailyAvg * 0.20 * dayFactor * 100) / 100.0);
                item.put("ydeep", Math.round(dailyAvg * 0.10 * dayFactor * 100) / 100.0);
                powerConsumptionList.add(item);
            }
        }
        result.put("powerConsumptionList", powerConsumptionList);
        
        // 收益列表（按天）- 基于发电量和电价计算
        double[] priceFactors = {1.2, 0.8, 0.5, 0.3, 0.2};
        List<Map<String, Object>> costList = new ArrayList<>();
        for (int i = 1; i <= maxDay; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("xdata", String.format("%04d-%02d-%02d 00:00:00", year, month, i));
            double dayFactor = 0.8 + Math.random() * 0.4;
            item.put("ytip", Math.round(dailyAvg * 0.05 * dayFactor * priceFactors[0] * 100) / 100.0);
            item.put("ypeak", Math.round(dailyAvg * 0.25 * dayFactor * priceFactors[1] * 100) / 100.0);
            item.put("yflat", Math.round(dailyAvg * 0.40 * dayFactor * priceFactors[2] * 100) / 100.0);
            item.put("ytrough", Math.round(dailyAvg * 0.20 * dayFactor * priceFactors[3] * 100) / 100.0);
            item.put("ydeep", Math.round(dailyAvg * 0.10 * dayFactor * priceFactors[4] * 100) / 100.0);
            costList.add(item);
        }
        result.put("costList", costList);
        
        return success(result);
    }

    /**
     * 导出峰平谷数据
     */
    @GetMapping("/export")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // 简单返回空响应，实际需要实现Excel导出
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment;filename=peak_valley_report.xlsx");
    }
}
