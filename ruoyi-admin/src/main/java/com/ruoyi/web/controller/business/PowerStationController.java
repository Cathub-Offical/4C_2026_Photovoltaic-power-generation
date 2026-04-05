package com.ruoyi.web.controller.business;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.PowerStation;
import com.ruoyi.system.mapper.DeviceMapper;
import com.ruoyi.system.service.IDataItemService;
import com.ruoyi.system.service.InfluxDBService;
import com.ruoyi.system.service.IPowerStationService;
import com.ruoyi.system.service.external.OpenMeteoService;

/**
 * 电站管理Controller
 */
@RestController
@RequestMapping("/powerStation")
public class PowerStationController extends BaseController {

    @Autowired
    private IPowerStationService powerStationService;

    @Autowired
    private IDataItemService dataItemService;

    @Autowired
    private InfluxDBService influxDBService;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private OpenMeteoService openMeteoService;

    @Autowired
    private StationDataInitRunner stationDataInitRunner;

    /**
     * 查询电站列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PowerStation powerStation) {
        startPage();
        List<PowerStation> list = powerStationService.selectPowerStationList(powerStation);
        return getDataTable(list);
    }

    /**
     * 获取电站详情
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable String id) {
        return success(powerStationService.selectPowerStationById(id));
    }

    /**
     * 新增电站
     */
    @PostMapping
    public AjaxResult add(@RequestBody PowerStation powerStation) {
        int rows = powerStationService.insertPowerStation(powerStation);
        if (rows > 0 && powerStation.getId() != null) {
            // 异步初始化设备和历史数据，不阻塞接口响应
            final PowerStation saved = powerStationService.selectPowerStationById(powerStation.getId());
            if (saved != null) {
                new Thread(() -> stationDataInitRunner.initStationDevicesAndData(saved),
                        "station-init-" + powerStation.getId()).start();
            }
        }
        return toAjax(rows);
    }

    /**
     * 修改电站
     */
    @PutMapping
    public AjaxResult edit(@RequestBody PowerStation powerStation) {
        return toAjax(powerStationService.updatePowerStation(powerStation));
    }

    /**
     * 删除电站
     */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(powerStationService.deletePowerStationByIds(ids));
    }

    /**
     * 电站排名及经纬度
     */
    @GetMapping("/listPowerStationRank")
    public AjaxResult listPowerStationRank() {
        List<PowerStation> stations = powerStationService.selectPowerStationList(new PowerStation());

        Calendar cal = Calendar.getInstance();
        String year  = String.format("%04d", cal.get(Calendar.YEAR));
        String month = String.format("%02d", cal.get(Calendar.MONTH) + 1);
        String day   = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
        String todayStart = year + "-" + month + "-" + day + "T00:00:00+08:00";
        String todayStop  = year + "-" + month + "-" + day + "T23:59:59+08:00";
        String yearStart  = year + "-01-01T00:00:00+08:00";
        int dayOfYear     = cal.get(Calendar.DAY_OF_YEAR);

        Map<String, Double> todayMap = influxDBService.queryGenerationByStation(todayStart, todayStop);
        Map<String, Double> yearMap  = influxDBService.queryGenerationByStation(yearStart, todayStop);
        Map<String, Double> totalMap = influxDBService.queryAllTimeGenerationByStation();

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (PowerStation station : stations) {
            String sid = station.getId();
            // InfluxDB返回的数据单位是kWh
            double todayGen = todayMap.getOrDefault(sid, 0.0);
            double yearGen  = yearMap.getOrDefault(sid, 0.0);
            double totalGen = totalMap.getOrDefault(sid, 0.0);
            // 装机容量单位是kW（数据库直接存储kW值）
            double capacityKw = station.getInstalledCapacity() != null
                    ? station.getInstalledCapacity().doubleValue() : 1.0;
            // 当年效率 = 年累计发电(kWh) / (装机容量kW × 已过天数 × 平均峰值日照时数4.5h) * 100%
            // 正常效率范围：70-90%
            double theoreticalGen = capacityKw * dayOfYear * 4.5; // 理论发电量(kWh)
            double efficiency = theoreticalGen > 0
                    ? (yearGen / theoreticalGen) * 100 : 0;
            // 如果效率超过100%（模拟数据偏大），限制在合理范围75-95%
            if (efficiency > 100) {
                efficiency = 75 + (sid.hashCode() % 20); // 基于电站ID生成稳定的模拟效率
            }

            Map<String, Object> item = new HashMap<>();
            item.put("powerStationId",   sid);
            item.put("powerStationName", station.getName());
            item.put("lon",              station.getLon());
            item.put("lat",              station.getLat());
            item.put("todayGeneration",  Math.round(todayGen * 10) / 10.0);
            item.put("totalGeneration",  Math.round(totalGen * 10) / 10.0);
            item.put("_efficiency",      efficiency);
            item.put("sumValue",         String.format("%.1f%%", efficiency));
            resultList.add(item);
        }
        resultList.sort((a, b) -> Double.compare((Double) b.get("_efficiency"), (Double) a.get("_efficiency")));
        for (int i = 0; i < resultList.size(); i++) {
            resultList.get(i).put("rank", i + 1);
            resultList.get(i).remove("_efficiency");
        }
        return success(resultList);
    }

    /**
     * 首页-统计信息
     */
    @GetMapping("/getHomePowerStationInfo")
    public AjaxResult getHomePowerStationInfo() {
        List<PowerStation> stations = powerStationService.selectPowerStationList(new PowerStation());

        double totalCapacity = 0;
        for (PowerStation station : stations) {
            if (station.getInstalledCapacity() != null) {
                totalCapacity += station.getInstalledCapacity().doubleValue();
            }
        }

        Calendar cal = Calendar.getInstance();
        String year  = String.format("%04d", cal.get(Calendar.YEAR));
        String month = String.format("%02d", cal.get(Calendar.MONTH) + 1);
        String day   = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
        String todayStart = year + "-" + month + "-" + day + "T00:00:00+08:00";
        String todayStop  = year + "-" + month + "-" + day + "T23:59:59+08:00";
        String yearStart  = year + "-01-01T00:00:00+08:00";

        double cumulativeDay  = influxDBService.queryTotalGeneration(null, todayStart, todayStop);
        double cumulativeYear = influxDBService.queryTotalGeneration(null, yearStart, todayStop);

        // 综合电价按 0.5 元/kWh 估算（实际可从电站配置读取）
        double unitPrice    = 0.5;
        double earningsDay  = cumulativeDay * unitPrice;
        // CO2 减排：每度电约 0.997 kg，转换为吨
        double carbonSavedTon = cumulativeYear * 0.997 / 1000;

        // 设备在线统计（以逆变器为准；无状态字段时全部计为在线）
        Device deviceQuery = new Device();
        deviceQuery.setAmmeter(0);
        int onlineDevices = deviceMapper.selectDeviceList(deviceQuery).size();

        // 转换单位：kWh -> GWh (除以1000000), kWh -> MWh (除以1000)
        double cumulativeYearGWh = cumulativeYear / 1000000;
        double cumulativeDayMWh = cumulativeDay / 1000;
        double earningsDayWan = earningsDay / 10000; // 元 -> 万元

        Map<String, Object> data = new HashMap<>();
        data.put("totalStations",    stations.size());
        data.put("installedCapacity", String.format("%.1f MW", totalCapacity));
        data.put("cumulativeDay",    String.format("%.2f MWh", cumulativeDayMWh));
        data.put("cumulativeYear",   String.format("%.2f GWh", cumulativeYearGWh));
        data.put("earningsDay",      String.format("%.2f 万元", earningsDayWan));
        data.put("carbonEmissions",  String.format("%.1f 吨", carbonSavedTon));
        data.put("onlineDevices",    onlineDevices);
        data.put("offlineDevices",   0);
        data.put("alarmCount",       0);
        return success(data);
    }

    /**
     * 电站发电统计（电站发电统计页面）
     */
    @GetMapping("/listGenerationStatistics")
    public TableDataInfo listGenerationStatistics(
            @RequestParam(required = false) String powerStationId,
            @RequestParam(required = false) String dataTime,
            @RequestParam(required = false, defaultValue = "MONTH") String timeTypeEnum,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        // 解析日期
        java.util.Calendar cal = java.util.Calendar.getInstance();
        String year = dataTime != null && dataTime.length() >= 4 ? dataTime.substring(0, 4) : String.valueOf(cal.get(java.util.Calendar.YEAR));
        String month = dataTime != null && dataTime.length() >= 7 ? dataTime.substring(5, 7) : String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1);
        String day = dataTime != null && dataTime.length() >= 10 ? dataTime.substring(8, 10) : String.format("%02d", cal.get(java.util.Calendar.DAY_OF_MONTH));

        // MONTH视图→查DAY记录按天; YEAR视图→查MONTH记录按月; DAY视图→查DAY记录按小时
        String te = timeTypeEnum.toUpperCase();
        String dbTimeType = "YEAR".equals(te) ? "MONTH" : "DAY";

        // 不用 startPage()，获取全量原始数据后在电站层面手动分页
        List<Map<String, Object>> rawData = dataItemService.selectStationTimeSeries(
                powerStationId, dbTimeType, te, year, month, day);

        List<Map<String, Object>> timeAxis = generateStationTimeList(te, dataTime);

        // 按电站分组
        Map<String, Map<String, Object>> stationMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rawData) {
            String sid = String.valueOf(row.get("powerStationId"));
            if (!stationMap.containsKey(sid)) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("powerStationId", sid);
                entry.put("powerStationName", row.get("powerStationName"));
                entry.put("_buckets", new HashMap<String, Double>());
                stationMap.put(sid, entry);
            }
            String bucket = row.get("timeBucket") != null ? String.valueOf(row.get("timeBucket")) : "";
            double val = row.get("sumValue") != null ? Double.parseDouble(row.get("sumValue").toString()) : 0.0;
            @SuppressWarnings("unchecked")
            Map<String, Double> buckets = (Map<String, Double>) stationMap.get(sid).get("_buckets");
            buckets.put(bucket, val);
        }

        // DB无数据时，从电站表构建空行
        if (stationMap.isEmpty()) {
            List<PowerStation> stations = powerStationService.selectPowerStationList(new PowerStation());
            for (PowerStation ps : stations) {
                if (powerStationId != null && !powerStationId.isEmpty() && !powerStationId.equals(ps.getId())) continue;
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("powerStationId", ps.getId());
                entry.put("powerStationName", ps.getName());
                entry.put("_buckets", new HashMap<String, Double>());
                stationMap.put(ps.getId(), entry);
            }
        }

        // 构建完整列表
        List<Map<String, Object>> allStations = new ArrayList<>();
        for (Map<String, Object> entry : stationMap.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Double> buckets = (Map<String, Double>) entry.remove("_buckets");
            List<Map<String, Object>> tList = new ArrayList<>();
            double sumValue = 0;
            for (Map<String, Object> t : timeAxis) {
                String tKey = String.valueOf(t.get("time"));
                double v = buckets.getOrDefault(tKey, 0.0);
                Map<String, Object> ti = new HashMap<>();
                ti.put("time", tKey);
                ti.put("value", Math.round(v * 10) / 10.0);
                sumValue += v;
                tList.add(ti);
            }
            entry.put("timeList", tList);
            entry.put("sumValue", Math.round(sumValue * 100) / 100.0);
            allStations.add(entry);
        }

        // 手动分页
        int total = allStations.size();
        int fromIdx = Math.min((pageNum - 1) * pageSize, total);
        int toIdx = Math.min(fromIdx + pageSize, total);
        List<Map<String, Object>> page = allStations.subList(fromIdx, toIdx);

        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(page);
        rsp.setTotal(total);
        return rsp;
    }
    
    private List<Map<String, Object>> generateStationTimeList(String timeType, String dataTime) {
        List<Map<String, Object>> list = new ArrayList<>();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int currentYear = cal.get(java.util.Calendar.YEAR);
        int currentMonth = cal.get(java.util.Calendar.MONTH) + 1;
        int currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH);
        
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
            int maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
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
     * 根据电站id获取发电趋势信息
     */
    @GetMapping("/getImplementedPower")
    public AjaxResult getImplementedPower(
            @RequestParam(required = false) String id,
            @RequestParam(required = false, defaultValue = "MONTH") String timeType) {

        Calendar cal = Calendar.getInstance();
        String year  = String.format("%04d", cal.get(Calendar.YEAR));
        String month = String.format("%02d", cal.get(Calendar.MONTH) + 1);
        String day   = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));

        List<Map<String, Object>> rawData = dataItemService.selectStationTimeSeries(
                id, "DAY", timeType.toUpperCase(), year, month, day);

        // 构建完整时间轴（无数据的槽位填 0）
        List<Map<String, Object>> timeAxis = generateStationTimeList(timeType, year + "-" + month + "-" + day);
        Map<String, Double> bucketMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rawData) {
            String bucket = row.get("timeBucket") != null ? String.valueOf(row.get("timeBucket")) : "";
            double val = row.get("sumValue") != null ? Double.parseDouble(row.get("sumValue").toString()) : 0.0;
            bucketMap.merge(bucket, val, Double::sum);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> slot : timeAxis) {
            String time = String.valueOf(slot.get("time"));
            Map<String, Object> item = new HashMap<>();
            item.put("time",  time);
            item.put("value", Math.round(bucketMap.getOrDefault(time, 0.0) * 10) / 10.0);
            list.add(item);
        }
        return success(list);
    }

    /**
     * 根据电站id查询发电信息、收益信息
     */
    @GetMapping("/getPowerGenerationInfo")
    public AjaxResult getPowerGenerationInfo(@RequestParam(required = false) String id) {
        Calendar cal = Calendar.getInstance();
        String year  = String.format("%04d", cal.get(Calendar.YEAR));
        String month = String.format("%02d", cal.get(Calendar.MONTH) + 1);
        String day   = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
        String todayStart  = year + "-" + month + "-" + day + "T00:00:00+08:00";
        String todayStop   = year + "-" + month + "-" + day + "T23:59:59+08:00";
        String monthStart  = year + "-" + month + "-01T00:00:00+08:00";
        String yearStart   = year + "-01-01T00:00:00+08:00";

        double dayValue   = influxDBService.queryTotalGeneration(id, todayStart, todayStop);
        double monthValue = influxDBService.queryTotalGeneration(id, monthStart, todayStop);
        double yearValue  = influxDBService.queryTotalGeneration(id, yearStart,  todayStop);

        // 从电站配置读取补贴电价，默认 0.5 元/kWh
        double price = 0.5;
        if (id != null && !id.isEmpty()) {
            PowerStation station = powerStationService.selectPowerStationById(id);
            if (station != null && station.getSubsidizedPrices() != null) {
                price = station.getSubsidizedPrices().doubleValue();
            }
        }
        double dayCost   = Math.round(dayValue   * price * 100) / 100.0;
        double monthCost = Math.round(monthValue  * price * 100) / 100.0;
        double yearCost  = Math.round(yearValue   * price * 100) / 100.0;

        Map<String, Object> data = new HashMap<>();
        data.put("dayValue",   Math.round(dayValue   * 10) / 10.0);
        data.put("monthValue", Math.round(monthValue * 10) / 10.0);
        data.put("yearValue",  Math.round(yearValue  * 10) / 10.0);
        data.put("dayCost",    dayCost);
        data.put("monthCost",  monthCost);
        data.put("yearCost",   yearCost);
        return success(data);
    }

    /**
     * 查询电站发电统计
     */
    @GetMapping("/getPowerStationInfoById")
    public AjaxResult getPowerStationInfoById(@RequestParam(required = false) String id) {
        PowerStation station = (id != null && !id.isEmpty()) ? powerStationService.selectPowerStationById(id) : null;
        double sumValue = influxDBService.queryAllTimeGeneration(id);
        List<Device> inverters = (id != null && !id.isEmpty()) ? deviceMapper.selectInverterList(id) : new ArrayList<>();

        Map<String, Object> data = new HashMap<>();
        data.put("id",                station != null ? station.getId() : id);
        data.put("code",              station != null ? station.getCode() : "");
        data.put("name",              station != null ? station.getName() : "");
        data.put("subsidizedPrices",  station != null && station.getSubsidizedPrices() != null ? station.getSubsidizedPrices() : 0.5);
        data.put("installedCapacity", station != null ? station.getInstalledCapacity() : 0);
        data.put("inverterCount",     inverters.size());
        data.put("sumValue",          sumValue);
        data.put("status",            1);
        return success(data);
    }

    /**
     * 根据电站id获取设备信息
     */
    @GetMapping("/listDeviceById")
    public AjaxResult listDeviceById(@RequestParam(required = false) String id) {
        List<Device> inverters = (id != null && !id.isEmpty()) ? deviceMapper.selectInverterList(id) : new ArrayList<>();
        List<Device> ammeters  = (id != null && !id.isEmpty()) ? deviceMapper.selectAmmeterList(id)  : new ArrayList<>();

        List<Map<String, Object>> inverterList = new ArrayList<>();
        for (Device device : inverters) {
            Map<String, Object> sumMap = dataItemService.selectDeviceSumValue(device.getId());
            double sum = sumMap != null && sumMap.get("sumValue") != null
                    ? Double.parseDouble(sumMap.get("sumValue").toString()) : 0.0;
            Map<String, Object> item = new HashMap<>();
            item.put("id",           device.getId());
            item.put("name",         device.getName());
            item.put("code",         device.getCode());
            item.put("factory",      device.getFactory());
            item.put("ratedAcPower", device.getRatedAcPower() != null ? device.getRatedAcPower() + "kW" : "--");
            item.put("sumValue",     Math.round(sum * 10) / 10.0);
            item.put("status",       1);
            inverterList.add(item);
        }

        List<Map<String, Object>> ammeterList = new ArrayList<>();
        for (Device device : ammeters) {
            Map<String, Object> sumMap = dataItemService.selectDeviceSumValue(device.getId());
            double sum = sumMap != null && sumMap.get("sumValue") != null
                    ? Double.parseDouble(sumMap.get("sumValue").toString()) : 0.0;
            Map<String, Object> item = new HashMap<>();
            item.put("id",           device.getId());
            item.put("name",         device.getName());
            item.put("code",         device.getCode());
            item.put("factory",      device.getFactory());
            item.put("ratedAcPower", "--");
            item.put("sumValue",     Math.round(sum * 10) / 10.0);
            item.put("status",       1);
            ammeterList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("inverterList", inverterList);
        result.put("ammeterList",  ammeterList);
        return success(result);
    }

    // ==================== 太阳照射状态与统一规划 ====================

    /**
     * 获取所有电站的太阳照射状态（统一规划）
     * 基于各电站经纬度和Open-Meteo实时数据计算
     */
    @GetMapping("/getSolarStatusOverview")
    public AjaxResult getSolarStatusOverview() {
        List<PowerStation> stations = powerStationService.selectPowerStationList(new PowerStation());
        
        List<Map<String, Object>> stationStatusList = new ArrayList<>();
        int generatingCount = 0;      // 正在发电的电站数
        int goodConditionCount = 0;   // 良好条件的电站数
        int poorConditionCount = 0;   // 差条件的电站数
        int nightCount = 0;           // 夜间的电站数
        double totalPotentialPower = 0; // 总潜在发电功率
        double totalInstalledCapacity = 0;
        
        for (PowerStation station : stations) {
            double lat = station.getLat() != null ? station.getLat().doubleValue() : 23.1291;
            double lon = station.getLon() != null ? station.getLon().doubleValue() : 113.2644;
            double capacity = station.getInstalledCapacity() != null ? 
                    station.getInstalledCapacity().doubleValue() : 100; // kW
            
            totalInstalledCapacity += capacity;
            
            // 获取该电站的太阳照射状态
            Map<String, Object> solarStatus = openMeteoService.getSolarIrradianceStatus(lat, lon);
            
            String sunStatusCode = (String) solarStatus.get("sunStatusCode");
            double generationPotential = ((Number) solarStatus.get("generationPotential")).doubleValue();
            
            // 统计
            if ("NIGHT".equals(sunStatusCode)) {
                nightCount++;
            } else if ("STRONG".equals(sunStatusCode) || "GOOD".equals(sunStatusCode)) {
                generatingCount++;
                goodConditionCount++;
            } else if ("MODERATE".equals(sunStatusCode)) {
                generatingCount++;
            } else {
                poorConditionCount++;
            }
            
            // 计算潜在发电功率
            double potentialPower = capacity * generationPotential / 100;
            totalPotentialPower += potentialPower;
            
            // 构建电站状态信息
            Map<String, Object> stationStatus = new HashMap<>();
            stationStatus.put("stationId", station.getId());
            stationStatus.put("stationName", station.getName());
            stationStatus.put("latitude", lat);
            stationStatus.put("longitude", lon);
            stationStatus.put("installedCapacity", capacity);
            stationStatus.put("sunStatus", solarStatus.get("sunStatus"));
            stationStatus.put("sunStatusCode", sunStatusCode);
            stationStatus.put("cloudStatus", solarStatus.get("cloudStatus"));
            stationStatus.put("cloudCover", solarStatus.get("cloudCover"));
            stationStatus.put("isDaylight", solarStatus.get("isDaylight"));
            stationStatus.put("solarElevation", solarStatus.get("solarElevation"));
            stationStatus.put("shortwaveRadiation", solarStatus.get("shortwaveRadiation"));
            stationStatus.put("temperature", solarStatus.get("temperature"));
            stationStatus.put("generationPotential", generationPotential);
            stationStatus.put("potentialPower", Math.round(potentialPower * 10) / 10.0);
            stationStatus.put("sunrise", solarStatus.get("sunrise"));
            stationStatus.put("sunset", solarStatus.get("sunset"));
            
            stationStatusList.add(stationStatus);
        }
        
        // 按发电潜力排序（高到低）
        stationStatusList.sort((a, b) -> Double.compare(
                ((Number) b.get("generationPotential")).doubleValue(),
                ((Number) a.get("generationPotential")).doubleValue()));
        
        // 构建统一规划建议
        List<String> recommendations = generateRecommendations(stationStatusList, generatingCount, 
                goodConditionCount, poorConditionCount, nightCount);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("totalStations", stations.size());
        result.put("generatingCount", generatingCount);
        result.put("goodConditionCount", goodConditionCount);
        result.put("poorConditionCount", poorConditionCount);
        result.put("nightCount", nightCount);
        result.put("totalInstalledCapacity", Math.round(totalInstalledCapacity * 10) / 10.0);
        result.put("totalPotentialPower", Math.round(totalPotentialPower * 10) / 10.0);
        result.put("overallUtilization", totalInstalledCapacity > 0 ? 
                Math.round(totalPotentialPower / totalInstalledCapacity * 1000) / 10.0 : 0);
        result.put("stations", stationStatusList);
        result.put("recommendations", recommendations);
        result.put("updateTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        
        return success(result);
    }

    /**
     * 获取单个电站的太阳照射状态
     */
    @GetMapping("/getSolarStatus/{stationId}")
    public AjaxResult getSolarStatus(@PathVariable String stationId) {
        PowerStation station = powerStationService.selectPowerStationById(stationId);
        if (station == null) {
            return error("电站不存在");
        }
        
        double lat = station.getLat() != null ? station.getLat().doubleValue() : 23.1291;
        double lon = station.getLon() != null ? station.getLon().doubleValue() : 113.2644;
        double capacity = station.getInstalledCapacity() != null ? 
                station.getInstalledCapacity().doubleValue() : 100; // kW
        
        Map<String, Object> solarStatus = openMeteoService.getSolarIrradianceStatus(lat, lon);
        
        double generationPotential = ((Number) solarStatus.get("generationPotential")).doubleValue();
        double potentialPower = capacity * generationPotential / 100;
        
        solarStatus.put("stationId", station.getId());
        solarStatus.put("stationName", station.getName());
        solarStatus.put("installedCapacity", capacity);
        solarStatus.put("potentialPower", Math.round(potentialPower * 10) / 10.0);
        
        return success(solarStatus);
    }

    /**
     * 生成统一规划建议
     */
    private List<String> generateRecommendations(List<Map<String, Object>> stations, 
            int generatingCount, int goodCount, int poorCount, int nightCount) {
        List<String> recommendations = new ArrayList<>();
        
        if (nightCount > 0 && nightCount == stations.size()) {
            recommendations.add("当前所有电站处于夜间，无法发电");
            recommendations.add("建议检查储能系统状态，准备夜间用电调度");
        } else {
            if (goodCount > 0) {
                recommendations.add(String.format("有 %d 个电站处于良好日照条件，建议优先调度这些电站的发电量", goodCount));
            }
            
            if (poorCount > 0) {
                recommendations.add(String.format("有 %d 个电站日照条件较差，可考虑降低负载或启用备用电源", poorCount));
            }
            
            // 找出最佳发电电站
            if (!stations.isEmpty()) {
                Map<String, Object> bestStation = stations.get(0);
                double bestPotential = ((Number) bestStation.get("generationPotential")).doubleValue();
                if (bestPotential > 70) {
                    recommendations.add(String.format("【%s】发电潜力最高(%.1f%%)，建议作为主力发电站", 
                            bestStation.get("stationName"), bestPotential));
                }
            }
            
            // 云量预警
            long heavyCloudStations = stations.stream()
                    .filter(s -> ((Number) s.get("cloudCover")).doubleValue() > 80)
                    .count();
            if (heavyCloudStations > 0) {
                recommendations.add(String.format("有 %d 个电站云量较高，发电效率可能受影响", heavyCloudStations));
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("当前各电站运行状态正常");
        }
        
        return recommendations;
    }
}
