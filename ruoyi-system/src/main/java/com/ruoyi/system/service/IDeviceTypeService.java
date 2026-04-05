package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.DeviceType;

/**
 * 设备类型Service接口
 */
public interface IDeviceTypeService {
    
    List<DeviceType> selectDeviceTypeList(DeviceType deviceType);
    
    DeviceType selectDeviceTypeById(String id);
    
    int insertDeviceType(DeviceType deviceType);
    
    int updateDeviceType(DeviceType deviceType);
    
    int deleteDeviceTypeById(String id);
    
    int deleteDeviceTypeByIds(String[] ids);
}
