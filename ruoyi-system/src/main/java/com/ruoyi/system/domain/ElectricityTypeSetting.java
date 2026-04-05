package com.ruoyi.system.domain;

import java.util.List;
import com.ruoyi.common.core.domain.BaseEntity;

public class ElectricityTypeSetting extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String id;
    private String beginTime;
    private String endTime;
    private List<ElectricityTypeSettingItem> items;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBeginTime() { return beginTime; }
    public void setBeginTime(String beginTime) { this.beginTime = beginTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public List<ElectricityTypeSettingItem> getItems() { return items; }
    public void setItems(List<ElectricityTypeSettingItem> items) { this.items = items; }
}
