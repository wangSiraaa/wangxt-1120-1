package com.semiconductor.mask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.semiconductor.mask.common.Result;
import com.semiconductor.mask.entity.MaskInfo;
import com.semiconductor.mask.entity.MaskLockRecord;
import com.semiconductor.mask.service.MaskInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mask")
public class MaskInfoController {

    @Autowired
    private MaskInfoService maskInfoService;

    @GetMapping("/page")
    public Result<IPage<MaskInfo>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String maskCode,
            @RequestParam(required = false) String maskName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cleanLevel) {
        return Result.success(maskInfoService.pageQuery(pageNum, pageSize, maskCode, maskName, status, cleanLevel));
    }

    @GetMapping("/{id}")
    public Result<MaskInfo> getById(@PathVariable Long id) {
        return Result.success(maskInfoService.getById(id));
    }

    @GetMapping("/code/{maskCode}")
    public Result<MaskInfo> getByMaskCode(@PathVariable String maskCode) {
        return Result.success(maskInfoService.getByMaskCode(maskCode));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody MaskInfo maskInfo) {
        return Result.success(maskInfoService.addMask(maskInfo));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody MaskInfo maskInfo) {
        return Result.success(maskInfoService.updateMask(maskInfo));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(maskInfoService.deleteMask(id));
    }

    @GetMapping("/available")
    public Result<List<MaskInfo>> getAvailableMasks(@RequestParam(required = false) String cleanLevel) {
        return Result.success(maskInfoService.getAvailableMasks(cleanLevel));
    }

    @PostMapping("/lock/{maskId}")
    public Result<MaskLockRecord> lockMask(
            @PathVariable Long maskId,
            @RequestParam String lockType,
            @RequestParam String lockReason,
            @RequestParam(required = false) Long lockUserId) {
        return Result.success(maskInfoService.lockMask(maskId, lockType, lockReason, lockUserId));
    }

    @PostMapping("/unlock/{lockRecordId}")
    public Result<MaskLockRecord> unlockMask(
            @PathVariable Long lockRecordId,
            @RequestParam String unlockReason,
            @RequestParam(required = false) Long unlockUserId) {
        return Result.success(maskInfoService.unlockMask(lockRecordId, unlockReason, unlockUserId));
    }

    @PostMapping("/batch-check-calibration")
    public Result<Void> batchCheckCalibrationExpire() {
        maskInfoService.batchCheckCalibrationExpire();
        return Result.success();
    }
}
