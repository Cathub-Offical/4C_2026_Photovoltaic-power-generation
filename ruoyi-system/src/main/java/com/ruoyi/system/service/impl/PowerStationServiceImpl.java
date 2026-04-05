package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.PowerStation;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.DataItem;
import com.ruoyi.system.mapper.PowerStationMapper;
import com.ruoyi.system.mapper.DeviceMapper;
import com.ruoyi.system.mapper.DataItemMapper;
import com.ruoyi.system.service.IDataItemService;
import com.ruoyi.system.service.IPowerStationService;

/**
 * 电站维护Service业务层处理
 */
@Service
public class PowerStationServiceImpl implements IPowerStationService {
    @Autowired
    private PowerStationMapper powerStationMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DataItemMapper dataItemMapper;

    @Autowired
    private IDataItemService dataItemService;

    /**
     * 查询电站列表
     */
    @Override
    public List<PowerStation> selectPowerStationList(PowerStation powerStation) {
        return powerStationMapper.selectPowerStationList(powerStation);
    }

    /**
     * 查询电站详情
     */
    @Override
    public PowerStation selectPowerStationById(String id) {
        return powerStationMapper.selectPowerStationById(id);
    }

    /**
     * 新增电站，并自动初始化设备和发电数据
     */
    @Override
    @Transactional
    public int insertPowerStation(PowerStation powerStation) {
        if (powerStation.getId() == null || powerStation.getId().isEmpty()) {
            powerStation.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        int rows = powerStationMapper.insertPowerStation(powerStation);

        // 自动创建设备和模拟数据（仅当该电站尚无设备时）
        List<Device> existingDevices = deviceMapper.selectDeviceByPowerStationId(powerStation.getId());
        if (existingDevices == null || existingDevices.isEmpty()) {
            initStationDevicesAndData(powerStation);
        }

        return rows;
    }

    /**
     * 为所有缺少设备的电站补充初始化数据
     */
    @Override
    @Transactional
    public int initAllStationsData() {
        List<PowerStation> stations = powerStationMapper.selectPowerStationList(new PowerStation());
        int count = 0;
        for (PowerStation station : stations) {
            // 检查该电站是否已有逆变器设备
            List<Device> existing = deviceMapper.selectDeviceByPowerStationId(station.getId());
            if (existing == null || existing.isEmpty()) {
                initStationDevicesAndData(station);
                count++;
            }
        }
        return count;
    }

    /**
     * 为新电站初始化设备（逆变器+电表）和模拟发电数据
     * 生成：
     * 1. DAY类型记录（每天每小时6-18点各一条）→ 支持日视图（按小时）和月视图（按天汇总）
     * 2. MONTH类型记录（每月一条）→ 支持年视图（按月）
     */
    private void initStationDevicesAndData(PowerStation station) {
        String stationId = station.getId();
        String stationCode = station.getCode() != null ? station.getCode() : "PV";
        Random random = new Random();

        // 装机容量（MW），用于推算单台逆变器功率
        double capacity = station.getInstalledCapacity() != null
                ? station.getInstalledCapacity().doubleValue() : 1.0;

        // 创建3台逆变器（ammeter=0）
        List<String> inverterIds = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Device inverter = new Device();
            String devId = UUID.randomUUID().toString().replace("-", "");
            inverter.setId(devId);
            inverter.setPowerStationId(stationId);
            inverter.setCode(stationCode + "-" + stationId.substring(0, 8) + "-INV-" + String.format("%03d", i));
            inverter.setName("逆变器-" + i);
            inverter.setFactory("阳光电源");
            inverter.setDeviceTypeId("1698627923435794433");
            inverter.setRatedAcPower(Math.round(capacity / 3.0 * 1000 * 10) / 10.0);
            inverter.setAmmeter(0);
            deviceMapper.insertDevice(inverter);
            inverterIds.add(devId);
        }

        // 创建1台电表（ammeter=1）
        Device meterDev = new Device();
        meterDev.setId(UUID.randomUUID().toString().replace("-", ""));
        meterDev.setPowerStationId(stationId);
        meterDev.setCode(stationCode + "-" + stationId.substring(0, 8) + "-MTR-001");
        meterDev.setName("电表-1");
        meterDev.setFactory("威胜电气");
        meterDev.setDeviceTypeId("100440185434537987");
        meterDev.setAmmeter(1);
        deviceMapper.insertDevice(meterDev);

        // 时间范围：上一年1月1日 到 当前年12月31日（确保年视图完整）
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int startYear = currentYear - 1;
        int endYear = currentYear;

        Calendar startCal = Calendar.getInstance();
        startCal.set(startYear, Calendar.JANUARY, 1, 0, 0, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.set(endYear, Calendar.DECEMBER, 31, 0, 0, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        String stationName = station.getName() != null ? station.getName() : "";

        for (String deviceId : inverterIds) {
            List<DataItem> batch = new ArrayList<>();
            // 用于累计每月总发电量，生成MONTH记录
            Map<String, Double> monthTotals = new LinkedHashMap<>();

            Calendar dayCal = Calendar.getInstance();
            dayCal.setTime(startCal.getTime());
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            dayCal.set(Calendar.MILLISECOND, 0);

            while (!dayCal.after(endCal)) {
                int y = dayCal.get(Calendar.YEAR);
                int m = dayCal.get(Calendar.MONTH);
                int d = dayCal.get(Calendar.DAY_OF_MONTH);

                int monthNum = m + 1;
                String monthKey = String.format("%04d-%02d", y, monthNum);

                // 季节系数（高值、平缓）
                double seasonFactor;
                if (monthNum >= 5 && monthNum <= 9) {
                    seasonFactor = 1.0 + random.nextDouble() * 0.1;
                } else if ((monthNum >= 3 && monthNum <= 4) || (monthNum >= 10 && monthNum <= 11)) {
                    seasonFactor = 0.9 + random.nextDouble() * 0.1;
                } else {
                    seasonFactor = 0.85 + random.nextDouble() * 0.1;
                }

                // 为每天的6-18时各生成一条DAY记录
                double dayTotal = 0;
                for (int hour = 6; hour <= 18; hour++) {
                    // 正午（11-14点）发电最多，早晚较少
                    double hourFactor;
                    if (hour >= 11 && hour <= 14) {
                        hourFactor = 0.9 + random.nextDouble() * 0.1;
                    } else if (hour >= 9 && hour <= 16) {
                        hourFactor = 0.7 + random.nextDouble() * 0.15;
                    } else {
                        hourFactor = 0.4 + random.nextDouble() * 0.15;
                    }

                    double hourValue = 15.0 * (capacity / 3.0) * seasonFactor * hourFactor;
                    hourValue = Math.round(hourValue * 10) / 10.0;
                    dayTotal += hourValue;

                    dayCal.set(Calendar.HOUR_OF_DAY, hour);
                    DataItem item = new DataItem();
                    item.setDeviceId(deviceId);
                    item.setTimeType("DAY");
                    item.setTimeCode(String.format("%04d%02d%02d%02d", y, monthNum, d, hour));
                    item.setDataTime(dayCal.getTime());
                    item.setValue(BigDecimal.valueOf(hourValue));
                    item.setStationId(stationId);
                    item.setStationName(stationName);
                    batch.add(item);

                    if (batch.size() >= 500) {
                        dataItemService.batchInsertDataItem(batch);
                        batch.clear();
                    }
                }

                // 累计月总量
                monthTotals.merge(monthKey, dayTotal, Double::sum);

                // 下一天
                dayCal.set(Calendar.HOUR_OF_DAY, 0);
                dayCal.add(Calendar.DAY_OF_MONTH, 1);
            }

            // 插入剩余DAY记录
            if (!batch.isEmpty()) {
                dataItemService.batchInsertDataItem(batch);
                batch.clear();
            }

            // 生成MONTH类型记录（年视图使用）
            for (Map.Entry<String, Double> entry : monthTotals.entrySet()) {
                String mk = entry.getKey(); // "yyyy-MM"
                double mv = Math.round(entry.getValue() * 10) / 10.0;
                int my = Integer.parseInt(mk.substring(0, 4));
                int mm = Integer.parseInt(mk.substring(5, 7));

                Calendar mCal = Calendar.getInstance();
                mCal.set(my, mm - 1, 1, 0, 0, 0);
                mCal.set(Calendar.MILLISECOND, 0);

                DataItem mItem = new DataItem();
                mItem.setDeviceId(deviceId);
                mItem.setTimeType("MONTH");
                mItem.setTimeCode(String.format("%04d%02d", my, mm));
                mItem.setDataTime(mCal.getTime());
                mItem.setValue(BigDecimal.valueOf(mv));
                mItem.setStationId(stationId);
                mItem.setStationName(stationName);
                batch.add(mItem);
            }
            if (!batch.isEmpty()) {
                dataItemService.batchInsertDataItem(batch);
                batch.clear();
            }
        }
    }

    /**
     * 修改电站
     */
    @Override
    public int updatePowerStation(PowerStation powerStation) {
        return powerStationMapper.updatePowerStation(powerStation);
    }

    /**
     * 删除电站
     */
    @Override
    public int deletePowerStationById(String id) {
        return powerStationMapper.deletePowerStationById(id);
    }

    /**
     * 批量删除电站
     */
    @Override
    public int deletePowerStationByIds(String[] ids) {
        return powerStationMapper.deletePowerStationByIds(ids);
    }
}
