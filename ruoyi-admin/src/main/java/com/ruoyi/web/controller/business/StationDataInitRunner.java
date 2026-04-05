package com.ruoyi.web.controller.business;

import java.math.BigDecimal;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.PowerStation;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.DataItem;
import com.ruoyi.system.mapper.PowerStationMapper;
import com.ruoyi.system.mapper.DeviceMapper;
import com.ruoyi.system.mapper.DataItemMapper;
import com.ruoyi.system.service.IDataItemService;

/**
 * 应用启动时自动为缺少设备的电站补充初始化数据
 */
@Component
public class StationDataInitRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StationDataInitRunner.class);

    @Autowired
    private PowerStationMapper powerStationMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DataItemMapper dataItemMapper;

    @Autowired
    private IDataItemService dataItemService;

    @Override
    @Transactional
    public void run(String... args) {
        List<PowerStation> stations = powerStationMapper.selectPowerStationList(new PowerStation());
        int initCount = 0;
        int refreshCount = 0;
        for (PowerStation station : stations) {
            List<Device> existing = deviceMapper.selectDeviceByPowerStationId(station.getId());
            if (existing == null || existing.isEmpty()) {
                // 没有设备，全新初始化
                log.info("初始化电站数据: {} ({})", station.getName(), station.getId());
                initStationDevicesAndData(station);
                initCount++;
            } else {
                log.debug("电站已初始化，跳过: {} ({})", station.getName(), station.getId());
            }
        }
        log.info("数据初始化完成: 新建{}个, 刷新{}个", initCount, refreshCount);
    }

    public void initStationDevicesAndData(PowerStation station) {
        String stationId = station.getId();
        String stationCode = station.getCode() != null ? station.getCode() : "PV";
        Random random = new Random();

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
            inverter.setRatedAcPower(Math.round(capacity / 3.0 * 10) / 10.0);
            inverter.setCapacity(Math.round(capacity / 3.0 * 100) / 100.0);
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

                double seasonFactor;
                if (monthNum >= 5 && monthNum <= 9) {
                    seasonFactor = 1.0 + random.nextDouble() * 0.1;
                } else if ((monthNum >= 3 && monthNum <= 4) || (monthNum >= 10 && monthNum <= 11)) {
                    seasonFactor = 0.9 + random.nextDouble() * 0.1;
                } else {
                    seasonFactor = 0.85 + random.nextDouble() * 0.1;
                }

                double dayTotal = 0;
                for (int hour = 6; hour <= 18; hour++) {
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

                monthTotals.merge(monthKey, dayTotal, Double::sum);

                dayCal.set(Calendar.HOUR_OF_DAY, 0);
                dayCal.add(Calendar.DAY_OF_MONTH, 1);
            }

            if (!batch.isEmpty()) {
                dataItemService.batchInsertDataItem(batch);
                batch.clear();
            }

            for (Map.Entry<String, Double> entry : monthTotals.entrySet()) {
                String mk = entry.getKey();
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
     * 为已有设备的电站重新生成数据（不创建新设备）
     */
    private void regenerateDataForExistingDevices(PowerStation station, List<Device> devices) {
        Random random = new Random();
        double capacity = station.getInstalledCapacity() != null
                ? station.getInstalledCapacity().doubleValue() : 1.0;

        List<String> inverterIds = new ArrayList<>();
        for (Device d : devices) {
            if (d.getAmmeter() != null && d.getAmmeter() == 0) {
                inverterIds.add(d.getId());
            }
        }
        if (inverterIds.isEmpty()) return;

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int startYear = currentYear - 1;

        Calendar startCal = Calendar.getInstance();
        startCal.set(startYear, Calendar.JANUARY, 1, 0, 0, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.set(currentYear, Calendar.DECEMBER, 31, 0, 0, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        String stationName2 = station.getName() != null ? station.getName() : "";
        String stationId2 = station.getId();

        for (String deviceId : inverterIds) {
            List<DataItem> batch = new ArrayList<>();
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

                double seasonFactor;
                if (monthNum >= 5 && monthNum <= 9) {
                    seasonFactor = 1.0 + random.nextDouble() * 0.1;
                } else if ((monthNum >= 3 && monthNum <= 4) || (monthNum >= 10 && monthNum <= 11)) {
                    seasonFactor = 0.9 + random.nextDouble() * 0.1;
                } else {
                    seasonFactor = 0.85 + random.nextDouble() * 0.1;
                }

                double dayTotal = 0;
                for (int hour = 6; hour <= 18; hour++) {
                    double hourFactor;
                    if (hour >= 11 && hour <= 14) {
                        hourFactor = 0.9 + random.nextDouble() * 0.1;
                    } else if (hour >= 9 && hour <= 16) {
                        hourFactor = 0.7 + random.nextDouble() * 0.15;
                    } else {
                        hourFactor = 0.4 + random.nextDouble() * 0.15;
                    }

                    double hourValue = (capacity / 3.0) * seasonFactor * hourFactor;
                    hourValue = Math.round(hourValue * 10) / 10.0;
                    dayTotal += hourValue;

                    dayCal.set(Calendar.HOUR_OF_DAY, hour);
                    DataItem item = new DataItem();
                    item.setDeviceId(deviceId);
                    item.setTimeType("DAY");
                    item.setTimeCode(String.format("%04d%02d%02d%02d", y, monthNum, d, hour));
                    item.setDataTime(dayCal.getTime());
                    item.setValue(BigDecimal.valueOf(hourValue));
                    item.setStationId(stationId2);
                    item.setStationName(stationName2);
                    batch.add(item);

                    if (batch.size() >= 500) {
                        dataItemService.batchInsertDataItem(batch);
                        batch.clear();
                    }
                }

                monthTotals.merge(monthKey, dayTotal, Double::sum);
                dayCal.set(Calendar.HOUR_OF_DAY, 0);
                dayCal.add(Calendar.DAY_OF_MONTH, 1);
            }

            if (!batch.isEmpty()) {
                dataItemService.batchInsertDataItem(batch);
                batch.clear();
            }

            for (Map.Entry<String, Double> entry : monthTotals.entrySet()) {
                String mk = entry.getKey();
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
                mItem.setStationId(stationId2);
                mItem.setStationName(stationName2);
                batch.add(mItem);
            }
            if (!batch.isEmpty()) {
                dataItemService.batchInsertDataItem(batch);
                batch.clear();
            }
        }
    }
}
