package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.ElectricityDataItem;

/**
 * 电力峰平谷数据Mapper接口
 */
public interface ElectricityDataItemMapper {
    
    List<ElectricityDataItem> selectElectricityDataItemList(ElectricityDataItem item);
    
    List<Map<String, Object>> selectPeakValleyStats(
            @Param("deviceId") String deviceId,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);
    
    List<Map<String, Object>> selectPeakValleyReport(
            @Param("deviceId") String deviceId,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);
}
