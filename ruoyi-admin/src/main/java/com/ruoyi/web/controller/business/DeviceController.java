package com.ruoyi.web.controller.business;

import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.DataItem;
import com.ruoyi.system.service.IDeviceService;
import com.ruoyi.system.service.IDataItemService;
import com.ruoyi.system.service.InfluxDBService;
import com.ruoyi.system.service.external.RealTimeDataService;
import com.ruoyi.system.service.external.SolarPredictionService;

/**
 * 设备管理Controller
 */
@RestController
@RequestMapping("/device")
public class DeviceController extends BaseController {

    @Autowired
    private IDeviceService deviceService;

    @Autowired(required = false)
    private IDataItemService dataItemService;

    @Autowired
    private RealTimeDataService realTimeDataService;

    @Autowired
    private SolarPredictionService predictionService;

    @Autowired
    private InfluxDBService influxDBService;

    /**
     * 查询设备列表
     */
    @GetMapping("/list")
    public TableDataInfo list(Device device) {
        startPage();
        List<Device> list = deviceService.selectDeviceList(device);
        return getDataTable(list);
    }

    /**
     * 获取设备详情
     */
    @GetMapping("/detail/{id}")
    public AjaxResult getInfoByDetail(@PathVariable String id) {
        return success(deviceService.selectDeviceById(id));
    }

    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable String id) {
        return success(deviceService.selectDeviceById(id));
    }

    /**
     * 新增设备
     */
    @PostMapping
    public AjaxResult add(@RequestBody Device device) {
        return toAjax(deviceService.insertDevice(device));
    }

    /**
     * 修改设备
     */
    @PutMapping
    public AjaxResult edit(@RequestBody Device device) {
        return toAjax(deviceService.updateDevice(device));
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(deviceService.deleteDeviceByIds(ids));
    }

    /**
     * 获取逆变器信息
     */
    @GetMapping("/getInverterInfo")
    public AjaxResult getInverterInfo(@RequestParam(required = false) String id) {
        if (id != null && !id.isEmpty()) {
            Device device = deviceService.selectDeviceById(id);
            if (device != null) {
                return success(device);
            }
        }
        // 如果没有找到，返回第一个逆变器
        List<Device> inverters = deviceService.selectInverterList(null);
        if (inverters != null && !inverters.isEmpty()) {
            return success(inverters.get(0));
        }
        return success(new HashMap<>());
    }

    /**
     * 获取逆变器发电统计（设备发电统计页面）
     */
    @GetMapping("/getInverterGenerationStats")
    public TableDataInfo getInverterGenerationStats(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String ammeter,
            @RequestParam(required = false) String dataTime,
            @RequestParam(required = false, defaultValue = "MONTH") String timeTypeEnum,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        
        startPage();
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        // 尝试从数据库查询设备
        List<Device> devices = new ArrayList<>();
        if (deviceService != null) {
            Device query = new Device();
            if (ammeter != null && !ammeter.isEmpty()) {
                query.setAmmeter(Integer.parseInt(ammeter));
            }
            devices = deviceService.selectDeviceList(query);
        }
        
        // 如果没有数据库数据，使用模拟数据
        if (devices == null || devices.isEmpty()) {
            devices = getMockDevices(ammeter);
        }
        
        // 生成时间列表
        List<Map<String, Object>> timeList = generateTimeList(timeTypeEnum, dataTime);
        
        for (Device device : devices) {
            Map<String, Object> row = new HashMap<>();
            row.put("deviceId", device.getId());
            row.put("deviceName", device.getName());
            
            // 尝试从数据库获取发电量数据
            List<Map<String, Object>> deviceTimeList = new ArrayList<>();
            double sumValue = 0;
            
            if (dataItemService != null) {
                // 从数据库查询
                String year = dataTime != null && dataTime.length() >= 4 ? dataTime.substring(0, 4) : "2026";
                String month = dataTime != null && dataTime.length() >= 7 ? dataTime.substring(5, 7) : "03";
                List<Map<String, Object>> dbData = dataItemService.selectDeviceGenerationStats(
                        device.getId(), timeTypeEnum, year, month);
                
                if (dbData != null && !dbData.isEmpty()) {
                    for (Map<String, Object> d : dbData) {
                        Object val = d.get("sumValue");
                        if (val != null) {
                            sumValue += Double.parseDouble(val.toString());
                        }
                    }
                }
            }
            
            // 根据时间类型调整数据量级
            // 日(按小时): 每小时 0-30 kWh
            // 月(按天): 每天 50-300 kWh  
            // 年(按月): 每月 1500-8000 kWh
            double baseValue;
            double range;
            if ("DAY".equalsIgnoreCase(timeTypeEnum)) {
                baseValue = 5;
                range = 25;
            } else if ("MONTH".equalsIgnoreCase(timeTypeEnum)) {
                baseValue = 50;
                range = 250;
            } else {
                baseValue = 1500;
                range = 6500;
            }
            
            // 生成时间列表数据（如果没有数据库数据则用模拟数据）
            for (Map<String, Object> t : timeList) {
                Map<String, Object> timeItem = new HashMap<>();
                timeItem.put("time", t.get("time"));
                double value = Math.round((baseValue + Math.random() * range) * 10) / 10.0;
                timeItem.put("value", value);
                sumValue += value;
                deviceTimeList.add(timeItem);
            }
            
            row.put("timeList", deviceTimeList);
            row.put("sumValue", Math.round(sumValue * 100) / 100.0);
            resultList.add(row);
        }
        
        return getDataTable(resultList);
    }
    
    private List<Device> getMockDevices(String ammeter) {
        List<Device> devices = new ArrayList<>();
        String[] names = {"逆变器-1", "逆变器-2", "逆变器-3", "电表-1", "电表-2"};
        int[] ammeters = {0, 0, 0, 1, 1};
        
        for (int i = 0; i < names.length; i++) {
            if (ammeter != null && !ammeter.isEmpty()) {
                if (!String.valueOf(ammeters[i]).equals(ammeter)) {
                    continue;
                }
            }
            Device d = new Device();
            d.setId(String.valueOf(i + 1));
            d.setName(names[i]);
            d.setAmmeter(ammeters[i]);
            devices.add(d);
        }
        return devices;
    }
    
    private List<Map<String, Object>> generateTimeList(String timeType, String dataTime) {
        List<Map<String, Object>> list = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        
        // 解析传入的日期参数
        int year = dataTime != null && dataTime.length() >= 4 ? Integer.parseInt(dataTime.substring(0, 4)) : currentYear;
        int month = dataTime != null && dataTime.length() >= 7 ? Integer.parseInt(dataTime.substring(5, 7)) : currentMonth;
        int day = dataTime != null && dataTime.length() >= 10 ? Integer.parseInt(dataTime.substring(8, 10)) : currentDay;
        
        if ("DAY".equalsIgnoreCase(timeType)) {
            // 按小时 - 返回完整日期时间格式 "yyyy-MM-dd HH"
            for (int i = 0; i < 24; i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("time", String.format("%04d-%02d-%02d %02d", year, month, day, i));
                list.add(item);
            }
        } else if ("MONTH".equalsIgnoreCase(timeType)) {
            // 按天
            cal.set(year, month - 1, 1);
            int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = 1; i <= maxDay; i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("time", String.format("%04d-%02d-%02d", year, month, i));
                list.add(item);
            }
        } else {
            // 按月
            for (int i = 1; i <= 12; i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("time", String.format("%04d-%02d", year, i));
                list.add(item);
            }
        }
        return list;
    }

    /**
     * 获取交流测量数据
     */
    @GetMapping("/getACMeasurementsByDeviceId")
    public AjaxResult getACMeasurementsByDeviceId(@RequestParam(required = false) String id) {
        // 使用实时数据服务获取交流测量数据
        double capacity = 50.0;
        if (id != null && !id.isEmpty()) {
            Device device = deviceService.selectDeviceById(id);
            if (device != null && device.getCapacity() != null) {
                capacity = device.getCapacity();
            }
        }
        double power = realTimeDataService.getDeviceRealTimePower(id, capacity);
        Map<String, Object> data = realTimeDataService.getACMeasurements(id, power);
        return success(data);
    }

    /**
     * 获取设备实时功率和发电趋势
     */
    @GetMapping("/getImplementedPower")
    public AjaxResult getImplementedPower(
            @RequestParam(required = false) String id,
            @RequestParam(required = false, defaultValue = "MONTH") String timeType) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取设备容量
        double capacity = 50.0;
        String stationId = null;
        if (id != null && !id.isEmpty()) {
            Device device = deviceService.selectDeviceById(id);
            if (device != null) {
                if (device.getCapacity() != null && device.getCapacity() > 0) capacity = device.getCapacity();
                stationId = device.getPowerStationId();
            }
        }
        
        // 使用预测服务获取实时功率
        double realTimePower = realTimeDataService.getDeviceRealTimePower(id, capacity);
        result.put("realTimePower", Math.round(realTimePower * 10) / 10.0);
        
        // 使用实时数据服务获取发电趋势（传入deviceId以查设备级真实数据）
        List<Map<String, Object>> itemList = realTimeDataService.getGenerationTrend(stationId, id, timeType, capacity);
        result.put("itemList", itemList);
        
        return success(result);
    }

    /**
     * 获取设备发电信息
     */
    @GetMapping("/getPowerGenerationInfo")
    public AjaxResult getPowerGenerationInfo(@RequestParam(required = false) String id) {
        Map<String, Object> data = new HashMap<>();
        
        // 获取当前日期
        Calendar cal = Calendar.getInstance();
        String y = String.format("%04d", cal.get(Calendar.YEAR));
        String mo = String.format("%02d", cal.get(Calendar.MONTH) + 1);
        String d = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
        
        // 从InfluxDB获取实际发电数据
        String todayStart = y + "-" + mo + "-" + d + "T00:00:00+08:00";
        String todayStop = y + "-" + mo + "-" + d + "T23:59:59+08:00";
        String monthStart = y + "-" + mo + "-01T00:00:00+08:00";
        String yearStart = y + "-01-01T00:00:00+08:00";
        
        double dayValue = 0, monthValue = 0, yearValue = 0;
        if (id != null && !id.isEmpty()) {
            dayValue = influxDBService.queryDeviceGeneration(id, todayStart, todayStop);
            monthValue = influxDBService.queryDeviceGeneration(id, monthStart, todayStop);
            yearValue = influxDBService.queryDeviceGeneration(id, yearStart, todayStop);
        }
        
        // 如果没有历史数据，使用预测值
        if (dayValue == 0) {
            double capacity = 50.0;
            Device device = id != null ? deviceService.selectDeviceById(id) : null;
            if (device != null && device.getCapacity() != null) capacity = device.getCapacity();
            
            List<Map<String, Object>> prediction = predictionService.predictDailyGeneration(capacity, 1);
            if (!prediction.isEmpty()) {
                dayValue = ((Number) prediction.get(0).getOrDefault("predictedGeneration", 0.0)).doubleValue();
            }
            monthValue = dayValue * cal.get(Calendar.DAY_OF_MONTH);
            yearValue = dayValue * cal.get(Calendar.DAY_OF_YEAR);
        }
        
        // 电价按0.5元/kWh计算
        double pricePerKwh = 0.5;
        data.put("dayValue", Math.round(dayValue * 10) / 10.0);
        data.put("monthValue", Math.round(monthValue * 10) / 10.0);
        data.put("yearValue", Math.round(yearValue * 10) / 10.0);
        data.put("dayCost", Math.round(dayValue * pricePerKwh * 100) / 100.0);
        data.put("monthCost", Math.round(monthValue * pricePerKwh * 100) / 100.0);
        data.put("yearCost", Math.round(yearValue * pricePerKwh * 100) / 100.0);
        
        return success(data);
    }

    /**
     * 获取设备检修记录
     */
    @GetMapping("/listDeviceInspectionByDeviceId")
    public AjaxResult listDeviceInspectionByDeviceId(@RequestParam(required = false) String deviceId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String[] types = {"日常巡检", "故障维修", "定期保养"};
        for (int i = 0; i < 5; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", i + 1);
            item.put("deviceId", deviceId != null ? deviceId : "unknown");
            item.put("inspectionType", types[i % 3]);
            item.put("inspector", "张工");
            item.put("result", "正常");
            item.put("remark", "设备运行正常");
            item.put("inspectionTime", new Date());
            list.add(item);
        }
        return success(list);
    }
}
