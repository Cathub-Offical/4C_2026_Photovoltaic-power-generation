package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.ElectricityDataItem;
import com.ruoyi.system.mapper.ElectricityDataItemMapper;
import com.ruoyi.system.service.IElectricityDataItemService;

/**
 * 电力峰平谷数据Service实现
 */
@Service
public class ElectricityDataItemServiceImpl implements IElectricityDataItemService {
    
    @Autowired
    private ElectricityDataItemMapper electricityDataItemMapper;

    @Override
    public List<ElectricityDataItem> selectElectricityDataItemList(ElectricityDataItem item) {
        return electricityDataItemMapper.selectElectricityDataItemList(item);
    }

    @Override
    public List<Map<String, Object>> selectPeakValleyStats(String deviceId, String startTime, String endTime) {
        return electricityDataItemMapper.selectPeakValleyStats(deviceId, startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> selectPeakValleyReport(String deviceId, String startTime, String endTime) {
        return electricityDataItemMapper.selectPeakValleyReport(deviceId, startTime, endTime);
    }
}
