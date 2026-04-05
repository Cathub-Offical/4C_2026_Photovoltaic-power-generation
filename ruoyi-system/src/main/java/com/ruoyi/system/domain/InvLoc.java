package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class InvLoc extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String id;
    private String location;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
