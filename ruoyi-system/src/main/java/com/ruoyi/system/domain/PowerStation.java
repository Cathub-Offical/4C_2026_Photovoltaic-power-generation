package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 电站维护对象 power_station
 */
public class PowerStation extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private String id;

    /** 父级id */
    private String parentId;

    /** 编号 */
    private String code;

    /** 名称 */
    private String name;

    /** 补贴电价 */
    private BigDecimal subsidizedPrices;

    /** 电站装机容量 */
    private BigDecimal installedCapacity;

    /** 并网电压 */
    private BigDecimal gridVoltage;

    /** 经度 */
    private BigDecimal lon;

    /** 纬度 */
    private BigDecimal lat;

    /** 所属用户id */
    private String owningUserId;

    /** 用户id */
    private Long userId;

    /** 部门id */
    private Long deptId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
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

    public BigDecimal getSubsidizedPrices() {
        return subsidizedPrices;
    }

    public void setSubsidizedPrices(BigDecimal subsidizedPrices) {
        this.subsidizedPrices = subsidizedPrices;
    }

    public BigDecimal getInstalledCapacity() {
        return installedCapacity;
    }

    public void setInstalledCapacity(BigDecimal installedCapacity) {
        this.installedCapacity = installedCapacity;
    }

    public BigDecimal getGridVoltage() {
        return gridVoltage;
    }

    public void setGridVoltage(BigDecimal gridVoltage) {
        this.gridVoltage = gridVoltage;
    }

    public BigDecimal getLon() {
        return lon;
    }

    public void setLon(BigDecimal lon) {
        this.lon = lon;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public void setLat(BigDecimal lat) {
        this.lat = lat;
    }

    public String getOwningUserId() {
        return owningUserId;
    }

    public void setOwningUserId(String owningUserId) {
        this.owningUserId = owningUserId;
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
}
