package com.ruoyi.system.domain;

import java.math.BigDecimal;

public class ElectricityTypeSettingItem {
    private String id;
    private String parentId;
    private String type;
    private BigDecimal electricityPrice;
    private String beginTime;
    private String endTime;
    private String remark;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getElectricityPrice() { return electricityPrice; }
    public void setElectricityPrice(BigDecimal electricityPrice) { this.electricityPrice = electricityPrice; }
    public String getBeginTime() { return beginTime; }
    public void setBeginTime(String beginTime) { this.beginTime = beginTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
