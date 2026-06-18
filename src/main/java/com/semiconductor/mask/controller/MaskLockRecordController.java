package com.semiconductor.mask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.semiconductor.mask.common.Result;
import com.semiconductor.mask.entity.MaskLockRecord;
import com.semiconductor.mask.service.MaskLockRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lock-record")
public class MaskLockRecordController {

    @Autowired
    private MaskLockRecordService maskLockRecordService;

    @GetMapping("/page")
    public Result<IPage<MaskLockRecord>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String recordNo,
            @RequestParam(required = false) Long maskId,
            @RequestParam(required = false) String lockType,
            @RequestParam(required = false) String lockStatus) {
        return Result.success(maskLockRecordService.pageQuery(pageNum, pageSize, recordNo, maskId, lockType, lockStatus));
    }

    @GetMapping("/{id}")
    public Result<MaskLockRecord> getById(@PathVariable Long id) {
        return Result.success(maskLockRecordService.getById(id));
    }
}
