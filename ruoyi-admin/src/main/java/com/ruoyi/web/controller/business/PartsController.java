package com.ruoyi.web.controller.business;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.Parts;
import com.ruoyi.system.domain.PartsRecord;
import com.ruoyi.system.service.IPartsService;

@RestController
@RequestMapping("/parts")
public class PartsController extends BaseController {

    @Autowired
    private IPartsService partsService;

    @GetMapping("/list")
    public TableDataInfo list(Parts parts) {
        startPage();
        List<Parts> list = partsService.selectPartsList(parts);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable String id) {
        return success(partsService.selectPartsById(id));
    }

    @PostMapping
    public AjaxResult add(@RequestBody Parts parts) {
        parts.setId(UUID.randomUUID().toString().replace("-", ""));
        return toAjax(partsService.insertParts(parts));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody Parts parts) {
        // 出库：amount = current - editCount; 入库：amount = current + editCount
        Parts existing = partsService.selectPartsById(parts.getId());
        if (existing != null && parts.getAmount() != null) {
            // 出库逻辑：传入amount为出库数量，从现有库存扣减
            int newAmount = Math.max(0, (existing.getAmount() != null ? existing.getAmount() : 0) - parts.getAmount());
            parts.setAmount(newAmount);
        }
        // 记录出库操作
        PartsRecord record = new PartsRecord();
        record.setId(UUID.randomUUID().toString().replace("-", ""));
        record.setPartsId(parts.getId());
        if (existing != null) {
            record.setCode(existing.getCode());
            record.setName(existing.getName());
            record.setSpecs(existing.getSpecs());
        }
        record.setAmount(parts.getAmount());
        record.setLocationId(parts.getLocationId());
        record.setLocation(parts.getLocation());
        record.setStatus(1);
        record.setMovementDate(parts.getRemark() != null ? null : null);
        record.setRemark(parts.getRemark());
        return toAjax(partsService.updateParts(parts));
    }

    @PostMapping("/update")
    public AjaxResult update(@RequestBody Parts parts) {
        return toAjax(partsService.updateParts(parts));
    }

    @DeleteMapping("/delete")
    public AjaxResult delete(@RequestParam String id) {
        return toAjax(partsService.deletePartsById(id));
    }

    @GetMapping("/listOperationRecords")
    public TableDataInfo listOperationRecords(PartsRecord record) {
        startPage();
        List<PartsRecord> list = partsService.selectPartsRecordList(record);
        return getDataTable(list);
    }
}
