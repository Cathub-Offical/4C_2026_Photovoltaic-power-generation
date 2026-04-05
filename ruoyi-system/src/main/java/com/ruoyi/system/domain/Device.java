package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 设备对象 device
 */
public class Device extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String id;
    private String powerStationId;
    private String code;
    private String name;
    private String deviceTypeId;
    private Double capacity;
    private String factory;
    private Double ratedAcPower;
    private String gridType;
    private Double modulePeakPower;
    private Integer ammeter;
    private Long userId;
    private Long deptId;

    // 非数据库字段
    private String powerStationName;
    private String deviceTypeName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPowerStationId() {
        return powerStationId;
    }

    public void setPowerStationId(String powerStationId) {
        this.powerStationId = powerStationId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(String deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public Double getCapacity() {
        return capacity;
    }

    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public Double getRatedAcPower() {
        return ratedAcPower;
    }

    public void setRatedAcPower(Double ratedAcPower) {
        this.ratedAcPower = ratedAcPower;
    }

    public String getGridType() {
        return gridType;
    }

    public void setGridType(String gridType) {
        this.gridType = gridType;
    }

    public Double getModulePeakPower() {
        return modulePeakPower;
    }

    public void setModulePeakPower(Double modulePeakPower) {
        this.modulePeakPower = modulePeakPower;
    }

    public Integer getAmmeter() {
        return ammeter;
    }

    public void setAmmeter(Integer ammeter) {
        this.ammeter = ammeter;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getPowerStationName() {
        return powerStationName;
    }

    public void setPowerStationName(String powerStationName) {
        this.powerStationName = powerStationName;
    }

    public String getDeviceTypeName() {
        return deviceTypeName;
    }

    public void setDeviceTypeName(String deviceTypeName) {
        this.deviceTypeName = deviceTypeName;
    }
}
