package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.DataItem;

/**
 * 数据项Service接口
 */
public interface IDataItemService {
    
    /**
     * 批量写入数据项（同时写入 MySQL 和 InfluxDB）
     */
    int batchInsertDataItem(List<DataItem> list);

    /**
     * 查询数据项列表
     */
    List<DataItem> selectDataItemList(DataItem dataItem);

    /**
     * 根据设备ID和时间类型查询数据
     */
    List<DataItem> selectDataItemByDeviceAndTimeType(String deviceId, String timeType, String startTime, String endTime);

    /**
     * 查询设备发电统计
     */
    List<Map<String, Object>> selectDeviceGenerationStats(String deviceId, String timeType, String year, String month);

    /**
     * 查询设备发电量汇总
     */
    Map<String, Object> selectDeviceSumValue(String deviceId);

    /**
     * 按电站+时间分桶查询发电量时序数据
     */
    List<Map<String, Object>> selectStationTimeSeries(
            String powerStationId, String dbTimeType, String viewType, String year, String month, String day);

    /**
     * 按电站+时间分桶查询发电量（用于同比/环比）
     */
    List<Map<String, Object>> selectStationCompareData(
            String powerStationId, String timeType, String startDate, String endDate);
}
