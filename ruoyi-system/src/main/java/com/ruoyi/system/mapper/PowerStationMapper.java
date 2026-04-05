package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.PowerStation;

/**
 * 电站维护Mapper接口
 */
public interface PowerStationMapper {
    /**
     * 查询电站列表
     */
    public List<PowerStation> selectPowerStationList(PowerStation powerStation);

    /**
     * 查询电站详情
     */
    public PowerStation selectPowerStationById(String id);

    /**
     * 新增电站
     */
    public int insertPowerStation(PowerStation powerStation);

    /**
     * 修改电站
     */
    public int updatePowerStation(PowerStation powerStation);

    /**
     * 删除电站
     */
    public int deletePowerStationById(String id);

    /**
     * 批量删除电站
     */
    public int deletePowerStationByIds(String[] ids);
}
