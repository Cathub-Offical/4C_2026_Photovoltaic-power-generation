package com.ruoyi.web.controller.business;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.InvLoc;
import com.ruoyi.system.service.IInvLocService;

@RestController
@RequestMapping("/inv-loc")
public class InvLocController extends BaseController {

    @Autowired
    private IInvLocService invLocService;

    @GetMapping("/list")
    public AjaxResult list() {
        List<InvLoc> list = invLocService.selectInvLocList();
        return success(list);
    }

    @PostMapping("/create")
    public AjaxResult create(@RequestBody InvLoc invLoc) {
        return toAjax(invLocService.insertInvLoc(invLoc));
    }

    @PutMapping("/edit")
    public AjaxResult edit(@RequestBody InvLoc invLoc) {
        return toAjax(invLocService.updateInvLoc(invLoc));
    }

    @DeleteMapping("/delete/{id}")
    public AjaxResult delete(@PathVariable String id) {
        return toAjax(invLocService.deleteInvLocById(id));
    }
}
