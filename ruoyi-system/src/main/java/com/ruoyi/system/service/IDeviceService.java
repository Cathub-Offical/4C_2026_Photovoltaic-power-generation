package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Device;

/**
 * 设备Service接口
 */
public interface IDeviceService {
    
    /**
     * 查询设备列表
     */
    List<Device> selectDeviceList(Device device);

    /**
     * 根据ID查询设备
     */
    Device selectDeviceById(String id);

    /**
     * 根据电站ID查询设备列表
     */
    List<Device> selectDeviceByPowerStationId(String powerStationId);

    /**
     * 查询逆变器列表
     */
    List<Device> selectInverterList(String powerStationId);

    /**
     * 查询电表列表
     */
    List<Device> selectAmmeterList(String powerStationId);

    /**
     * 新增设备
     */
    int insertDevice(Device device);

    /**
     * 修改设备
     */
    int updateDevice(Device device);

    /**
     * 删除设备
     */
    int deleteDeviceById(String id);

    /**
     * 批量删除设备
     */
    int deleteDeviceByIds(String[] ids);
}
