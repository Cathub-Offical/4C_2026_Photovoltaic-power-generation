package com.ruoyi.system.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.ruoyi.system.domain.DataItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * InfluxDB 读写服务
 * measurement: power_generation
 * tags:        device_id, station_id, station_name
 * fields:      value (double)
 */
@Service
public class InfluxDBService {

    private static final Logger log = LoggerFactory.getLogger(InfluxDBService.class);
    private static final String MEASUREMENT = "power_generation";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.org}")
    private String org;

    // -------------------------------------------------------------------------
    // 写入
    // -------------------------------------------------------------------------

    /**
     * 批量写入发电数据
     *
     * @param items     数据项列表（DataItem.stationId / stationName 需已填充）
     */
    public void writeBatch(List<DataItem> items) {
        if (items == null || items.isEmpty()) return;
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            List<Point> points = new ArrayList<>(items.size());
            for (DataItem item : items) {
                if (item.getDataTime() == null || item.getValue() == null) continue;
                Point p = Point.measurement(MEASUREMENT)
                        .addTag("device_id", item.getDeviceId())
                        .addTag("station_id", item.getStationId() != null ? item.getStationId() : "")
                        .addTag("station_name", item.getStationName() != null ? item.getStationName() : "")
                        .addField("value", item.getValue().doubleValue())
                        .time(item.getDataTime().toInstant(), WritePrecision.MS);
                points.add(p);
            }
            writeApi.writePoints(bucket, org, points);
        } catch (Exception e) {
            log.error("InfluxDB 批量写入失败", e);
        }
    }

    // -------------------------------------------------------------------------
    // 查询 — 电站时序（listGenerationStatistics 使用）
    // -------------------------------------------------------------------------

    /**
     * 按电站查询时序发电量，替代 DataItemMapper.selectStationTimeSeries
     *
     * @param stationId  为 null/空 时查所有电站
     * @param viewType   DAY | MONTH | YEAR
     * @param year       年
     * @param month      月（DAY/MONTH 视图需要）
     * @param day        日（DAY 视图需要）
     * @return List<Map> 每行：{powerStationId, powerStationName, timeBucket, sumValue}
     */
    public List<Map<String, Object>> queryStationTimeSeries(
            String stationId, String viewType, String year, String month, String day) {

        String startISO = buildStart(viewType, year, month, day);
        String stopISO  = buildStop(viewType, year, month, day);
        String window   = viewToWindow(viewType);

        StringBuilder flux = new StringBuilder();
        flux.append(String.format("from(bucket: \"%s\")\n", bucket));
        flux.append(String.format("  |> range(start: %s, stop: %s)\n", startISO, stopISO));
        flux.append(String.format("  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n", MEASUREMENT));
        if (stationId != null && !stationId.isEmpty()) {
            flux.append(String.format("  |> filter(fn: (r) => r[\"station_id\"] == \"%s\")\n", stationId));
        }
        flux.append("  |> filter(fn: (r) => r[\"_field\"] == \"value\")\n");
        flux.append(String.format("  |> aggregateWindow(every: %s, fn: sum, createEmpty: true)\n", window));
        flux.append("  |> group(columns: [\"station_id\", \"station_name\"])\n");
        flux.append("  |> sort(columns: [\"_time\"])");

        return executeStationQuery(flux.toString(), viewType);
    }

    /**
     * 按电站查询同比/环比数据，替代 DataItemMapper.selectStationCompareData
     */
    public List<Map<String, Object>> queryStationCompareData(
            String stationId, String timeType, String startDate, String endDate) {

        String startISO = startDate + "T00:00:00+08:00";
        String stopISO  = endDate   + "T23:59:59+08:00";
        String window   = "DAY".equalsIgnoreCase(timeType) ? "1d"
                        : "MONTH".equalsIgnoreCase(timeType) ? "1mo" : "1y";

        StringBuilder flux = new StringBuilder();
        flux.append(String.format("from(bucket: \"%s\")\n", bucket));
        flux.append(String.format("  |> range(start: %s, stop: %s)\n", startISO, stopISO));
        flux.append(String.format("  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n", MEASUREMENT));
        if (stationId != null && !stationId.isEmpty()) {
            flux.append(String.format("  |> filter(fn: (r) => r[\"station_id\"] == \"%s\")\n", stationId));
        }
        flux.append("  |> filter(fn: (r) => r[\"_field\"] == \"value\")\n");
        flux.append(String.format("  |> aggregateWindow(every: %s, fn: sum, createEmpty: true)\n", window));
        flux.append("  |> group(columns: [\"station_id\", \"station_name\"])\n");
        flux.append("  |> sort(columns: [\"_time\"])");

        // Use the correct formatter: DAY-aggregated → "yyyy-MM-dd", MONTH-aggregated → "yyyy-MM"
        String formatViewType = "DAY".equalsIgnoreCase(timeType) ? "MONTH" : "YEAR";
        return executeStationQuery(flux.toString(), formatViewType);
    }

    // -------------------------------------------------------------------------
    // 查询 — 设备维度
    // -------------------------------------------------------------------------

    /**
     * 查询指定设备在给定时间范围内的发电量
     */
    public double queryDeviceGeneration(String deviceId, String startISO, String stopISO) {
        String flux = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: %s, stop: %s)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n" +
                "  |> filter(fn: (r) => r[\"device_id\"] == \"%s\")\n" +
                "  |> filter(fn: (r) => r[\"_field\"] == \"value\")\n" +
                "  |> sum(column: \"_value\")",
                bucket, startISO, stopISO, MEASUREMENT, deviceId);
        List<FluxTable> tables = query(flux);
        double sum = 0.0;
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                sum += toDouble(record.getValue());
            }
        }
        return Math.round(sum * 10) / 10.0;
    }

    /**
     * 查询设备发电统计，替代 DataItemMapper.selectDeviceGenerationStats
     */
    public List<Map<String, Object>> queryDeviceGenerationStats(
            String deviceId, String timeType, String year, String month) {

        String startISO = "MONTH".equalsIgnoreCase(timeType)
                ? year + "-" + month + "-01T00:00:00+08:00"
                : year + "-01-01T00:00:00+08:00";
        String stopISO  = "MONTH".equalsIgnoreCase(timeType)
                ? buildMonthStop(year, month)
                : (Integer.parseInt(year) + 1) + "-01-01T00:00:00+08:00";

        StringBuilder flux = new StringBuilder();
        flux.append(String.format("from(bucket: \"%s\")\n", bucket));
        flux.append(String.format("  |> range(start: %s, stop: %s)\n", startISO, stopISO));
        flux.append(String.format("  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n", MEASUREMENT));
        if (deviceId != null && !deviceId.isEmpty()) {
            flux.append(String.format("  |> filter(fn: (r) => r[\"device_id\"] == \"%s\")\n", deviceId));
        }
        flux.append("  |> filter(fn: (r) => r[\"_field\"] == \"value\")\n");
        flux.append("  |> group(columns: [\"device_id\"])\n");
        flux.append("  |> sum(column: \"_value\")");

        List<FluxTable> tables = query(flux.toString());
        List<Map<String, Object>> result = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("deviceId", record.getValueByKey("device_id"));
                row.put("deviceName", "");
                row.put("sumValue", toDouble(record.getValue()));
                result.add(row);
            }
        }
        return result;
    }

    /**
     * 查询设备累计发电量，替代 DataItemMapper.selectDeviceSumValue
     */
    public Map<String, Object> queryDeviceSumValue(String deviceId) {
        String flux = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: 0)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n" +
                "  |> filter(fn: (r) => r[\"device_id\"] == \"%s\")\n" +
                "  |> filter(fn: (r) => r[\"_field\"] == \"value\")\n" +
                "  |> sum(column: \"_value\")",
                bucket, MEASUREMENT, deviceId);

        List<FluxTable> tables = query(flux);
        Map<String, Object> result = new HashMap<>();
        double sum = 0.0;
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                sum += toDouble(record.getValue());
            }
        }
        result.put("sumValue", sum);
        return result;
    }

    // -------------------------------------------------------------------------
    // 查询 — 首页统计
    // -------------------------------------------------------------------------

    /**
     * 查询所有（或指定）电站在给定时间范围内的发电量总和
     */
    public double queryTotalGeneration(String stationId, String startISO, String stopISO) {
        StringBuilder flux = new StringBuilder();
        flux.append(String.format("from(bucket: \"%s\")\n", bucket));
        flux.append(String.format("  |> range(start: %s, stop: %s)\n", startISO, stopISO));
        flux.append(String.format("  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n", MEASUREMENT));
        if (stationId != null && !stationId.isEmpty()) {
            flux.append(String.format("  |> filter(fn: (r) => r[\"station_id\"] == \"%s\")\n", stationId));
        }
        flux.append("  |> filter(fn: (r) => r[\"_field\"] == \"value\")\n");
        flux.append("  |> sum(column: \"_value\")");

        List<FluxTable> tables = query(flux.toString());
        double total = 0.0;
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                total += toDouble(record.getValue());
            }
        }
        return Math.round(total * 10) / 10.0;
    }

    /**
     * 查询所有（或指定）电站的全量（不限时间）累计发电量
     */
    public double queryAllTimeGeneration(String stationId) {
        return queryTotalGeneration(stationId, "1970-01-01T00:00:00Z", "now()");
    }

    /**
     * 按电站查询各自全量累计发电量 Map（station_id -> sumValue）
     */
    public Map<String, Double> queryAllTimeGenerationByStation() {
        return queryGenerationByStation("1970-01-01T00:00:00Z", "now()");
    }

    /**
     * 按电站查询各自今日 / 累计发电量 Map（station_id -> sumValue）
     */
    public Map<String, Double> queryGenerationByStation(String startISO, String stopISO) {
        StringBuilder flux = new StringBuilder();
        flux.append(String.format("from(bucket: \"%s\")\n", bucket));
        flux.append(String.format("  |> range(start: %s, stop: %s)\n", startISO, stopISO));
        flux.append(String.format("  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n", MEASUREMENT));
        flux.append("  |> filter(fn: (r) => r[\"_field\"] == \"value\")\n");
        flux.append("  |> group(columns: [\"station_id\"])\n");
        flux.append("  |> sum(column: \"_value\")");

        List<FluxTable> tables = query(flux.toString());
        Map<String, Double> result = new LinkedHashMap<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String sid = (String) record.getValueByKey("station_id");
                if (sid != null) result.put(sid, toDouble(record.getValue()));
            }
        }
        return result;
    }

    /**
     * 查询指定设备今日小时级发电量
     */
    public List<Map<String, Object>> queryDeviceHourly(String deviceId, String year, String month, String day) {
        String startISO = year + "-" + pad(month) + "-" + pad(day) + "T00:00:00+08:00";
        String stopISO  = year + "-" + pad(month) + "-" + pad(day) + "T23:59:59+08:00";

        String flux = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: %s, stop: %s)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n" +
                "  |> filter(fn: (r) => r[\"device_id\"] == \"%s\")\n" +
                "  |> filter(fn: (r) => r[\"_field\"] == \"value\")\n" +
                "  |> aggregateWindow(every: 1h, fn: sum, createEmpty: true)\n" +
                "  |> sort(columns: [\"_time\"])",
                bucket, startISO, stopISO, MEASUREMENT, deviceId);

        List<FluxTable> tables = query(flux);
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                if (record.getTime() == null) continue;
                ZonedDateTime zdt = record.getTime().atZone(ZONE);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("timeBucket", zdt.format(fmt));
                row.put("sumValue",   toDouble(record.getValue()));
                result.add(row);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // 内部工具
    // -------------------------------------------------------------------------

    private List<FluxTable> query(String flux) {
        try {
            return influxDBClient.getQueryApi().query(flux, org);
        } catch (Exception e) {
            log.error("InfluxDB 查询失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 将 FluxTable 结果映射为 {powerStationId, powerStationName, timeBucket, sumValue} */
    private List<Map<String, Object>> executeStationQuery(String flux, String viewType) {
        List<FluxTable> tables = query(flux);
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = bucketFormatter(viewType);

        for (FluxTable table : tables) {
            String sid   = tagFromTable(table, "station_id");
            String sname = tagFromTable(table, "station_name");
            for (FluxRecord record : table.getRecords()) {
                if (record.getTime() == null) continue;
                ZonedDateTime zdt = record.getTime().atZone(ZONE);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("powerStationId",   sid);
                row.put("powerStationName", sname);
                row.put("timeBucket",       zdt.format(fmt));
                row.put("sumValue",         toDouble(record.getValue()));
                result.add(row);
            }
        }
        return result;
    }

    private String tagFromTable(FluxTable table, String tagKey) {
        if (table.getRecords().isEmpty()) return "";
        Object v = table.getRecords().get(0).getValueByKey(tagKey);
        return v != null ? v.toString() : "";
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (Exception e) { return 0.0; }
    }

    // ---- 时间范围构建 ----

    private String buildStart(String viewType, String year, String month, String day) {
        if ("DAY".equalsIgnoreCase(viewType)) {
            return year + "-" + pad(month) + "-" + pad(day) + "T00:00:00+08:00";
        } else if ("MONTH".equalsIgnoreCase(viewType)) {
            return year + "-" + pad(month) + "-01T00:00:00+08:00";
        } else {
            return year + "-01-01T00:00:00+08:00";
        }
    }

    private String buildStop(String viewType, String year, String month, String day) {
        if ("DAY".equalsIgnoreCase(viewType)) {
            return year + "-" + pad(month) + "-" + pad(day) + "T23:59:59+08:00";
        } else if ("MONTH".equalsIgnoreCase(viewType)) {
            return buildMonthStop(year, month);
        } else {
            return (Integer.parseInt(year) + 1) + "-01-01T00:00:00+08:00";
        }
    }

    private String buildMonthStop(String year, String month) {
        int y = Integer.parseInt(year);
        int m = Integer.parseInt(month);
        int nextYear  = (m == 12) ? y + 1 : y;
        int nextMonth = (m == 12) ? 1 : m + 1;
        return nextYear + "-" + String.format("%02d", nextMonth) + "-01T00:00:00+08:00";
    }

    private String viewToWindow(String viewType) {
        if ("DAY".equalsIgnoreCase(viewType))   return "1h";
        if ("MONTH".equalsIgnoreCase(viewType)) return "1d";
        return "1mo";
    }

    private DateTimeFormatter bucketFormatter(String viewType) {
        if ("DAY".equalsIgnoreCase(viewType))   return DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
        if ("MONTH".equalsIgnoreCase(viewType)) return DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return DateTimeFormatter.ofPattern("yyyy-MM");
    }

    private String pad(String s) {
        if (s == null || s.length() >= 2) return s;
        return "0" + s;
    }
}
