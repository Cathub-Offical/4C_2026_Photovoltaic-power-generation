package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.mapper.DeviceMapper;
import com.ruoyi.system.service.IDeviceService;

/**
 * 设备Service实现
 */
@Service
public class DeviceServiceImpl implements IDeviceService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Override
    public List<Device> selectDeviceList(Device device) {
        return deviceMapper.selectDeviceList(device);
    }

    @Override
    public Device selectDeviceById(String id) {
        return deviceMapper.selectDeviceById(id);
    }

    @Override
    public List<Device> selectDeviceByPowerStationId(String powerStationId) {
        return deviceMapper.selectDeviceByPowerStationId(powerStationId);
    }

    @Override
    public List<Device> selectInverterList(String powerStationId) {
        return deviceMapper.selectInverterList(powerStationId);
    }

    @Override
    public List<Device> selectAmmeterList(String powerStationId) {
        return deviceMapper.selectAmmeterList(powerStationId);
    }

    @Override
    public int insertDevice(Device device) {
        if (device.getId() == null || device.getId().isEmpty()) {
            device.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        }
        return deviceMapper.insertDevice(device);
    }

    @Override
    public int updateDevice(Device device) {
        return deviceMapper.updateDevice(device);
    }

    @Override
    public int deleteDeviceById(String id) {
        return deviceMapper.deleteDeviceById(id);
    }

    @Override
    public int deleteDeviceByIds(String[] ids) {
        return deviceMapper.deleteDeviceByIds(ids);
    }
}
