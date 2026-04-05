package com.ruoyi.web.controller.business;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.ElectricityTypeSetting;
import com.ruoyi.system.domain.ElectricityTypeSettingItem;
import com.ruoyi.system.service.IElectricityTypeSettingService;

@RestController
@RequestMapping("/electricityTypeSetting")
public class ElectricityTypeSettingController extends BaseController {

    @Autowired
    private IElectricityTypeSettingService settingService;

    @GetMapping("/list")
    public TableDataInfo list(ElectricityTypeSetting setting,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {
        if (beginTime != null && !beginTime.isEmpty()) {
            setting.getParams().put("beginTime", toMonthDay(beginTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            setting.getParams().put("endTime", toMonthDay(endTime));
        }
        startPage();
        List<ElectricityTypeSetting> list = settingService.selectSettingList(setting);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable String id) {
        return success(settingService.selectSettingById(id));
    }

    @GetMapping("/listBySettingId/{parentId}")
    public AjaxResult listBySettingId(@PathVariable String parentId) {
        return success(settingService.selectItemsByParentId(parentId));
    }

    @PostMapping
    public AjaxResult add(@RequestBody ElectricityTypeSetting setting) {
        setting.setBeginTime(toMonthDay(setting.getBeginTime()));
        setting.setEndTime(toMonthDay(setting.getEndTime()));
        return toAjax(settingService.insertSetting(setting));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody ElectricityTypeSetting setting) {
        setting.setBeginTime(toMonthDay(setting.getBeginTime()));
        setting.setEndTime(toMonthDay(setting.getEndTime()));
        return toAjax(settingService.updateSetting(setting));
    }

    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable String id) {
        return toAjax(settingService.deleteSettingById(id));
    }

    @PostMapping("/addItem")
    public AjaxResult addItem(@RequestBody List<ElectricityTypeSettingItem> items) {
        if (items == null || items.isEmpty()) {
            return error("items cannot be empty");
        }
        String parentId = items.get(0).getParentId();
        return toAjax(settingService.saveItems(parentId, items));
    }

    /** Strip year if full date YYYY-MM-DD → MM-DD, leave as-is if already MM-DD */
    private String toMonthDay(String date) {
        if (date == null) return null;
        // YYYY-MM-DD has 10 chars; MM-DD has 5
        if (date.length() == 10) {
            return date.substring(5);
        }
        return date;
    }
}
