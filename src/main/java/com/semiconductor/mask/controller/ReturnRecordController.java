package com.semiconductor.mask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.semiconductor.mask.common.Result;
import com.semiconductor.mask.entity.ReturnRecord;
import com.semiconductor.mask.service.ReturnRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/return-record")
public class ReturnRecordController {

    @Autowired
    private ReturnRecordService returnRecordService;

    @GetMapping("/page")
    public Result<IPage<ReturnRecord>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String recordNo,
            @RequestParam(required = false) Long borrowOrderId,
            @RequestParam(required = false) Long maskId,
            @RequestParam(required = false) Long returnUserId,
            @RequestParam(required = false) Integer appearanceCheckDone) {
        return Result.success(returnRecordService.pageQuery(pageNum, pageSize, recordNo, borrowOrderId, maskId, returnUserId, appearanceCheckDone));
    }

    @GetMapping("/{id}")
    public Result<ReturnRecord> getById(@PathVariable Long id) {
        return Result.success(returnRecordService.getById(id));
    }

    @GetMapping("/no/{recordNo}")
    public Result<ReturnRecord> getByRecordNo(@PathVariable String recordNo) {
        return Result.success(returnRecordService.getByRecordNo(recordNo));
    }
}
