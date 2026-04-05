package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class PartsRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String id;
    private String partsId;
    private String code;
    private String name;
    private String specs;
    private Integer amount;
    private String locationId;
    private String location;
    private Integer status;
    private String movementDate;
    private String startDate;
    private String endDate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPartsId() { return partsId; }
    public void setPartsId(String partsId) { this.partsId = partsId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecs() { return specs; }
    public void setSpecs(String specs) { this.specs = specs; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getMovementDate() { return movementDate; }
    public void setMovementDate(String movementDate) { this.movementDate = movementDate; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
