package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class Parts extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String name;
    private String specs;
    private String locationId;
    private String location;
    private Integer amount;
    private String powerStationId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecs() { return specs; }
    public void setSpecs(String specs) { this.specs = specs; }
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public String getPowerStationId() { return powerStationId; }
    public void setPowerStationId(String powerStationId) { this.powerStationId = powerStationId; }
}
