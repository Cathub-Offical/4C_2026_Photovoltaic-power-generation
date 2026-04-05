package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.ElectricityDataItem;

/**
 * 电力峰平谷数据Service接口
 */
public interface IElectricityDataItemService {
    
    List<ElectricityDataItem> selectElectricityDataItemList(ElectricityDataItem item);
    
    List<Map<String, Object>> selectPeakValleyStats(String deviceId, String startTime, String endTime);
    
    List<Map<String, Object>> selectPeakValleyReport(String deviceId, String startTime, String endTime);
}
