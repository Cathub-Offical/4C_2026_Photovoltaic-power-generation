package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.DataItem;
import org.apache.ibatis.annotations.Param;

/**
 * 数据项Mapper接口
 */
public interface DataItemMapper {
    
    /**
     * 查询数据项列表
     */
    List<DataItem> selectDataItemList(DataItem dataItem);

    /**
     * 根据设备ID和时间类型查询数据
     */
    List<DataItem> selectDataItemByDeviceAndTimeType(
            @Param("deviceId") String deviceId,
            @Param("timeType") String timeType,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    /**
     * 查询设备发电统计（按时间类型分组）
     */
    List<Map<String, Object>> selectDeviceGenerationStats(
            @Param("deviceId") String deviceId,
            @Param("timeType") String timeType,
            @Param("year") String year,
            @Param("month") String month);

    /**
     * 查询设备发电量汇总
     */
    Map<String, Object> selectDeviceSumValue(@Param("deviceId") String deviceId);

    /**
     * 按电站+时间分桶查询发电量时序数据
     */
    List<Map<String, Object>> selectStationTimeSeries(
            @Param("powerStationId") String powerStationId,
            @Param("dbTimeType") String dbTimeType,
            @Param("viewType") String viewType,
            @Param("year") String year,
            @Param("month") String month,
            @Param("day") String day);

    /**
     * 按电站+时间分桶查询发电量（用于同比/环比）
     */
    List<Map<String, Object>> selectStationCompareData(
            @Param("powerStationId") String powerStationId,
            @Param("timeType") String timeType,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 批量插入数据项
     */
    int batchInsertDataItem(@Param("list") List<DataItem> list);

    /**
     * 根据设备ID删除数据项
     */
    int deleteDataItemsByDeviceId(@Param("deviceId") String deviceId);
}
