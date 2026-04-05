package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.Device;
import org.apache.ibatis.annotations.Param;

/**
 * 设备Mapper接口
 */
public interface DeviceMapper {
    
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
    List<Device> selectDeviceByPowerStationId(@Param("powerStationId") String powerStationId);

    /**
     * 查询逆变器列表（非电表）
     */
    List<Device> selectInverterList(@Param("powerStationId") String powerStationId);

    /**
     * 查询电表列表
     */
    List<Device> selectAmmeterList(@Param("powerStationId") String powerStationId);

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
