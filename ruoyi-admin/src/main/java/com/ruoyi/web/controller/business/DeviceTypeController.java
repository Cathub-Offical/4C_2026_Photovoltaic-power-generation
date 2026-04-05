package com.ruoyi.web.controller.business;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.DeviceType;
import com.ruoyi.system.service.IDeviceTypeService;

/**
 * 设备类型Controller
 */
@RestController
@RequestMapping("/deviceType")
public class DeviceTypeController extends BaseController {

    @Autowired
    private IDeviceTypeService deviceTypeService;

    /**
     * 查询设备类型列表
     */
    @GetMapping("/list")
    public TableDataInfo list(DeviceType deviceType) {
        startPage();
        List<DeviceType> list = deviceTypeService.selectDeviceTypeList(deviceType);
        return getDataTable(list);
    }

    /**
     * 获取设备类型详情
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable String id) {
        return success(deviceTypeService.selectDeviceTypeById(id));
    }

    /**
     * 新增设备类型
     */
    @PostMapping
    public AjaxResult add(@RequestBody DeviceType deviceType) {
        return toAjax(deviceTypeService.insertDeviceType(deviceType));
    }

    @PostMapping("/create")
    public AjaxResult create(@RequestBody DeviceType deviceType) {
        return toAjax(deviceTypeService.insertDeviceType(deviceType));
    }

    /**
     * 修改设备类型
     */
    @PutMapping
    public AjaxResult editPut(@RequestBody DeviceType deviceType) {
        return toAjax(deviceTypeService.updateDeviceType(deviceType));
    }

    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DeviceType deviceType) {
        return toAjax(deviceTypeService.updateDeviceType(deviceType));
    }

    /**
     * 删除设备类型
     */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(deviceTypeService.deleteDeviceTypeByIds(ids));
    }

    /**
     * 查询点位模板
     */
    @GetMapping("/index/{id}")
    public AjaxResult indexTemplateList(@PathVariable String id) {
        return success(new java.util.ArrayList<>());
    }

    /**
     * 新增编辑点位模板
     */
    @PostMapping("/index/{id}")
    public AjaxResult updateTemplate(@PathVariable String id, @RequestBody(required = false) Object data) {
        return success();
    }
}
