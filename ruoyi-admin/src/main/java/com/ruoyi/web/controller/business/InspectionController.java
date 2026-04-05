package com.ruoyi.web.controller.business;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.Inspection;
import com.ruoyi.system.service.IInspectionService;

@RestController
@RequestMapping("/inspection")
public class InspectionController extends BaseController {

    @Autowired
    private IInspectionService inspectionService;

    @GetMapping("/list")
    public TableDataInfo list(Inspection inspection,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {
        if (beginTime != null && !beginTime.isEmpty()) {
            inspection.getParams().put("beginTime", beginTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            inspection.getParams().put("endTime", endTime);
        }
        startPage();
        List<Inspection> list = inspectionService.selectInspectionList(inspection);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable String id) {
        return success(inspectionService.selectInspectionById(id));
    }

    @PostMapping
    public AjaxResult add(@RequestBody Inspection inspection) {
        return toAjax(inspectionService.insertInspection(inspection));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody Inspection inspection) {
        return toAjax(inspectionService.updateInspection(inspection));
    }

    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable String id) {
        return toAjax(inspectionService.deleteInspectionById(id));
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response, Inspection inspection,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {
        if (beginTime != null && !beginTime.isEmpty()) {
            inspection.getParams().put("beginTime", beginTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            inspection.getParams().put("endTime", endTime);
        }
        List<Inspection> list = inspectionService.selectInspectionList(inspection);
        ExcelUtil<Inspection> util = new ExcelUtil<>(Inspection.class);
        util.exportExcel(response, list, "设备点检记录");
    }
}
