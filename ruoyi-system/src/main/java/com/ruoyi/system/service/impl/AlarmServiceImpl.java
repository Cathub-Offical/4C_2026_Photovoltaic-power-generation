package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.Alarm;
import com.ruoyi.system.mapper.AlarmMapper;
import com.ruoyi.system.service.IAlarmService;

/**
 * 告警Service业务层处理
 */
@Service
public class AlarmServiceImpl implements IAlarmService {
    
    @Autowired
    private AlarmMapper alarmMapper;

    @Override
    public List<Alarm> selectAlarmList(Alarm alarm) {
        return alarmMapper.selectAlarmList(alarm);
    }

    @Override
    public Alarm selectAlarmById(String id) {
        return alarmMapper.selectAlarmById(id);
    }

    @Override
    public int insertAlarm(Alarm alarm) {
        if (alarm.getId() == null || alarm.getId().isEmpty()) {
            alarm.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        return alarmMapper.insertAlarm(alarm);
    }

    @Override
    public int updateAlarm(Alarm alarm) {
        return alarmMapper.updateAlarm(alarm);
    }

    @Override
    public int deleteAlarmById(String id) {
        return alarmMapper.deleteAlarmById(id);
    }

    @Override
    public int deleteAlarmByIds(String[] ids) {
        return alarmMapper.deleteAlarmByIds(ids);
    }

    @Override
    public int countUnresolvedAlarm() {
        return alarmMapper.countUnresolvedAlarm();
    }
}
