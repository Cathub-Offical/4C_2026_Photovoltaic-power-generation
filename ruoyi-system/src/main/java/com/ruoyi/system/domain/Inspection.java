package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

public class Inspection extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String id;

    @Excel(name = "电站名称", sort = 1)
    private String powerStationName;

    @Excel(name = "设备名称", sort = 2)
    private String deviceName;

    @Excel(name = "设备编码", sort = 3)
    private String deviceCode;

    @Excel(name = "检修结果", sort = 7)
    private String inspectionResult;

    @Excel(name = "检修类型", sort = 4, readConverterExp = "0=日常点检,1=定期检修,2=故障检修")
    private Integer inspectionType;

    @Excel(name = "持续时长(h)", sort = 8)
    private BigDecimal downtime;

    @Excel(name = "操作人员", sort = 9)
    private String inspectionStaff;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", sort = 5, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date inspectionStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", sort = 6, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date inspectionEndTime;

    @Excel(name = "备件名称/编号", sort = 10)
    private String sparePartNameOrNumber;

    @Excel(name = "预估损电量(kWh)", sort = 11)
    private BigDecimal estimatedPowerLoss;

    private String annex;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPowerStationName() { return powerStationName; }
    public void setPowerStationName(String powerStationName) { this.powerStationName = powerStationName; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }

    public String getInspectionResult() { return inspectionResult; }
    public void setInspectionResult(String inspectionResult) { this.inspectionResult = inspectionResult; }

    public Integer getInspectionType() { return inspectionType; }
    public void setInspectionType(Integer inspectionType) { this.inspectionType = inspectionType; }

    public BigDecimal getDowntime() { return downtime; }
    public void setDowntime(BigDecimal downtime) { this.downtime = downtime; }

    public String getInspectionStaff() { return inspectionStaff; }
    public void setInspectionStaff(String inspectionStaff) { this.inspectionStaff = inspectionStaff; }

    public Date getInspectionStartTime() { return inspectionStartTime; }
    public void setInspectionStartTime(Date inspectionStartTime) { this.inspectionStartTime = inspectionStartTime; }

    public Date getInspectionEndTime() { return inspectionEndTime; }
    public void setInspectionEndTime(Date inspectionEndTime) { this.inspectionEndTime = inspectionEndTime; }

    public String getSparePartNameOrNumber() { return sparePartNameOrNumber; }
    public void setSparePartNameOrNumber(String sparePartNameOrNumber) { this.sparePartNameOrNumber = sparePartNameOrNumber; }

    public BigDecimal getEstimatedPowerLoss() { return estimatedPowerLoss; }
    public void setEstimatedPowerLoss(BigDecimal estimatedPowerLoss) { this.estimatedPowerLoss = estimatedPowerLoss; }

    public String getAnnex() { return annex; }
    public void setAnnex(String annex) { this.annex = annex; }
}
