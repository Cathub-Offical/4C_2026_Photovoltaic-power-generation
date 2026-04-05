package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Alarm;

/**
 * 告警Service接口
 */
public interface IAlarmService {
    
    List<Alarm> selectAlarmList(Alarm alarm);
    
    Alarm selectAlarmById(String id);
    
    int insertAlarm(Alarm alarm);
    
    int updateAlarm(Alarm alarm);
    
    int deleteAlarmById(String id);
    
    int deleteAlarmByIds(String[] ids);
    
    int countUnresolvedAlarm();
}
