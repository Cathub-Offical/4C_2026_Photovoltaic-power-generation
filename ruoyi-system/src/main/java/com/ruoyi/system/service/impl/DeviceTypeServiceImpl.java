package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.DeviceType;
import com.ruoyi.system.mapper.DeviceTypeMapper;
import com.ruoyi.system.service.IDeviceTypeService;

/**
 * 设备类型Service业务层处理
 */
@Service
public class DeviceTypeServiceImpl implements IDeviceTypeService {
    
    @Autowired
    private DeviceTypeMapper deviceTypeMapper;

    @Override
    public List<DeviceType> selectDeviceTypeList(DeviceType deviceType) {
        return deviceTypeMapper.selectDeviceTypeList(deviceType);
    }

    @Override
    public DeviceType selectDeviceTypeById(String id) {
        return deviceTypeMapper.selectDeviceTypeById(id);
    }

    @Override
    public int insertDeviceType(DeviceType deviceType) {
        if (deviceType.getId() == null || deviceType.getId().isEmpty()) {
            deviceType.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        return deviceTypeMapper.insertDeviceType(deviceType);
    }

    @Override
    public int updateDeviceType(DeviceType deviceType) {
        return deviceTypeMapper.updateDeviceType(deviceType);
    }

    @Override
    public int deleteDeviceTypeById(String id) {
        return deviceTypeMapper.deleteDeviceTypeById(id);
    }

    @Override
    public int deleteDeviceTypeByIds(String[] ids) {
        return deviceTypeMapper.deleteDeviceTypeByIds(ids);
    }
}
