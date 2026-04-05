package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.DataItem;
import com.ruoyi.system.mapper.DataItemMapper;
import com.ruoyi.system.service.IDataItemService;
import com.ruoyi.system.service.InfluxDBService;

/**
 * 数据项Service实现
 * 写入：MySQL（兜底存储）+ InfluxDB（时序查询）双写
 * 查询：优先走 InfluxDB，失败时降级 MySQL
 */
@Service
public class DataItemServiceImpl implements IDataItemService {

    private static final Logger log = LoggerFactory.getLogger(DataItemServiceImpl.class);

    @Autowired
    private DataItemMapper dataItemMapper;

    @Autowired
    private InfluxDBService influxDBService;

    @Override
    public int batchInsertDataItem(List<DataItem> list) {
        int rows = dataItemMapper.batchInsertDataItem(list);
        // 只将小时级(DAY)数据写入InfluxDB，月/年汇总数据会导致时序图出现异常峰值
        List<DataItem> hourlyOnly = list.stream()
            .filter(item -> "DAY".equalsIgnoreCase(item.getTimeType()))
            .collect(java.util.stream.Collectors.toList());
        if (!hourlyOnly.isEmpty()) {
            influxDBService.writeBatch(hourlyOnly);
        }
        return rows;
    }

    @Override
    public List<DataItem> selectDataItemList(DataItem dataItem) {
        return dataItemMapper.selectDataItemList(dataItem);
    }

    @Override
    public List<DataItem> selectDataItemByDeviceAndTimeType(String deviceId, String timeType, String startTime, String endTime) {
        return dataItemMapper.selectDataItemByDeviceAndTimeType(deviceId, timeType, startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> selectDeviceGenerationStats(String deviceId, String timeType, String year, String month) {
        try {
            return influxDBService.queryDeviceGenerationStats(deviceId, timeType, year, month);
        } catch (Exception e) {
            log.warn("InfluxDB 查询失败，降级 MySQL: {}", e.getMessage());
            return dataItemMapper.selectDeviceGenerationStats(deviceId, timeType, year, month);
        }
    }

    @Override
    public Map<String, Object> selectDeviceSumValue(String deviceId) {
        try {
            return influxDBService.queryDeviceSumValue(deviceId);
        } catch (Exception e) {
            log.warn("InfluxDB 查询失败，降级 MySQL: {}", e.getMessage());
            return dataItemMapper.selectDeviceSumValue(deviceId);
        }
    }

    @Override
    public List<Map<String, Object>> selectStationTimeSeries(
            String powerStationId, String dbTimeType, String viewType, String year, String month, String day) {
        try {
            return influxDBService.queryStationTimeSeries(powerStationId, viewType, year, month, day);
        } catch (Exception e) {
            log.warn("InfluxDB 查询失败，降级 MySQL: {}", e.getMessage());
            return dataItemMapper.selectStationTimeSeries(powerStationId, dbTimeType, viewType, year, month, day);
        }
    }

    @Override
    public List<Map<String, Object>> selectStationCompareData(
            String powerStationId, String timeType, String startDate, String endDate) {
        try {
            return influxDBService.queryStationCompareData(powerStationId, timeType, startDate, endDate);
        } catch (Exception e) {
            log.warn("InfluxDB 查询失败，降级 MySQL: {}", e.getMessage());
            return dataItemMapper.selectStationCompareData(powerStationId, timeType, startDate, endDate);
        }
    }
}
