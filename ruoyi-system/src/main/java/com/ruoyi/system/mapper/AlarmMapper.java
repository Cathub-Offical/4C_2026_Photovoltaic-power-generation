package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.Alarm;

/**
 * 告警Mapper接口
 */
public interface AlarmMapper {
    
    List<Alarm> selectAlarmList(Alarm alarm);
    
    Alarm selectAlarmById(String id);
    
    int insertAlarm(Alarm alarm);
    
    int updateAlarm(Alarm alarm);
    
    int deleteAlarmById(String id);
    
    int deleteAlarmByIds(String[] ids);
    
    int countUnresolvedAlarm();
}
