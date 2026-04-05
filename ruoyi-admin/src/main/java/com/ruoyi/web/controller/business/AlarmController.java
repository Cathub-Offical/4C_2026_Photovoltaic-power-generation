package com.ruoyi.web.controller.business;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.Alarm;
import com.ruoyi.system.service.IAlarmService;

/**
 * 告警管理Controller
 */
@RestController
@RequestMapping("/alarm")
public class AlarmController extends BaseController {

    @Autowired
    private IAlarmService alarmService;

    /**
     * 查询告警列表
     */
    @GetMapping("/list")
    public TableDataInfo list(Alarm alarm,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {
        if (beginTime != null && !beginTime.isEmpty()) {
            alarm.getParams().put("beginTime", beginTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            alarm.getParams().put("endTime", endTime);
        }
        startPage();
        List<Alarm> list = alarmService.selectAlarmList(alarm);
        return getDataTable(list);
    }

    /**
     * 首页告警列表
     */
    @GetMapping("/listHomeAlarm")
    public AjaxResult listHomeAlarm() {
        List<Alarm> allAlarms = alarmService.selectAlarmList(new Alarm());
        long unprocessed = allAlarms.stream().filter(a -> "1".equals(a.getStatus())).count();
        long processed = allAlarms.stream().filter(a -> "2".equals(a.getStatus())).count();
        long level1 = allAlarms.stream().filter(a -> a.getLevel() != null && a.getLevel() == 1).count();
        long level2 = allAlarms.stream().filter(a -> a.getLevel() != null && a.getLevel() == 2).count();
        long level3 = allAlarms.stream().filter(a -> a.getLevel() != null && a.getLevel() == 3).count();
        // 最新5条报警记录
        List<Map<String, Object>> recentList = new ArrayList<>();
        int limit = Math.min(5, allAlarms.size());
        for (int i = 0; i < limit; i++) {
            Alarm a = allAlarms.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("deviceName", a.getDeviceName() != null ? a.getDeviceName() : a.getDeviceCode());
            item.put("alarmContent", a.getErrorDescription());
            item.put("level", a.getLevel());
            item.put("alarmTime", a.getDataTime());
            item.put("status", a.getStatus());
            recentList.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("todayAlarmCount", allAlarms.size());
        result.put("todayUnprocessed", unprocessed);
        result.put("todayProcessed", processed);
        result.put("todayLevel1", level1);
        result.put("todayLevel2", level2);
        result.put("todayLevel3", level3);
        result.put("recentAlarms", recentList);
        return success(result);
    }
    
    private String getLevelName(Integer level) {
        if (level == null) return "提示";
        switch (level) {
            case 1: return "严重";
            case 2: return "一般";
            case 3: return "提示";
            default: return "提示";
        }
    }

    /**
     * 获取告警详情
     */
    @GetMapping("/detail/{id}")
    public AjaxResult getInfo(@PathVariable String id) {
        return success(alarmService.selectAlarmById(id));
    }

    /**
     * 处理告警
     */
    @PostMapping("/alarmHandling")
    public AjaxResult alarmHandling(@RequestBody Alarm alarm) {
        alarm.setStatus("2"); // 已解决
        alarm.setProcessingTime(new Date());
        return toAjax(alarmService.updateAlarm(alarm));
    }

    /**
     * 告警等级分析
     */
    @GetMapping("/getAlarmLevelAnalysis")
    public AjaxResult getAlarmLevelAnalysis(
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) String dateType) {
        Map<String, Object> data = new HashMap<>();
        data.put("severe", 5);
        data.put("general", 12);
        data.put("notice", 8);
        
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", "03-" + String.format("%02d", i + 10));
            item.put("severe", (int)(Math.random() * 5));
            item.put("general", (int)(Math.random() * 10));
            item.put("notice", (int)(Math.random() * 8));
            trend.add(item);
        }
        data.put("trend", trend);
        return success(data);
    }

    /**
     * 编辑告警等级
     */
    @GetMapping("/editAlarmLevel")
    public AjaxResult editAlarmLevel(
            @RequestParam(required = false) String alarmId,
            @RequestParam(required = false) Integer level) {
        Alarm alarm = new Alarm();
        alarm.setId(alarmId);
        alarm.setLevel(level);
        return toAjax(alarmService.updateAlarm(alarm));
    }
}
