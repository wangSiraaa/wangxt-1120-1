package com.semiconductor.mask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.semiconductor.mask.common.Result;
import com.semiconductor.mask.entity.MaskLocationMove;
import com.semiconductor.mask.service.MaskLocationMoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mask-location-move")
public class MaskLocationMoveController {

    @Autowired
    private MaskLocationMoveService maskLocationMoveService;

    @GetMapping("/page")
    public Result<IPage<MaskLocationMove>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String moveNo,
            @RequestParam(required = false) Long maskId,
            @RequestParam(required = false) Long borrowOrderId,
            @RequestParam(required = false) String moveType,
            @RequestParam(required = false) String cleanLevelMismatch) {
        return Result.success(maskLocationMoveService.pageQuery(pageNum, pageSize, moveNo, maskId, borrowOrderId, moveType, cleanLevelMismatch));
    }

    @GetMapping("/{id}")
    public Result<MaskLocationMove> getById(@PathVariable Long id) {
        return Result.success(maskLocationMoveService.getById(id));
    }
}
