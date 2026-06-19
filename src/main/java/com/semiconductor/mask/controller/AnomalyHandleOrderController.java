package com.semiconductor.mask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.semiconductor.mask.common.Result;
import com.semiconductor.mask.entity.AnomalyHandleOrder;
import com.semiconductor.mask.service.AnomalyHandleOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/anomaly-handle-order")
public class AnomalyHandleOrderController {

    @Autowired
    private AnomalyHandleOrderService anomalyHandleOrderService;

    @GetMapping("/page")
    public Result<IPage<AnomalyHandleOrder>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String anomalyNo,
            @RequestParam(required = false) Long maskId,
            @RequestParam(required = false) String anomalyType,
            @RequestParam(required = false) String handleStatus,
            @RequestParam(required = false) Integer batchFreezeFlag) {
        return Result.success(anomalyHandleOrderService.pageQuery(pageNum, pageSize, anomalyNo, maskId, anomalyType, handleStatus, batchFreezeFlag));
    }

    @GetMapping("/{id}")
    public Result<AnomalyHandleOrder> getById(@PathVariable Long id) {
        return Result.success(anomalyHandleOrderService.getById(id));
    }

    @GetMapping("/no/{anomalyNo}")
    public Result<AnomalyHandleOrder> getByAnomalyNo(@PathVariable String anomalyNo) {
        return Result.success(anomalyHandleOrderService.getByAnomalyNo(anomalyNo));
    }

    @PostMapping("/handle/{id}")
    public Result<AnomalyHandleOrder> handleAnomaly(
            @PathVariable Long id,
            @RequestParam Long handleUserId,
            @RequestParam String handleResult,
            @RequestParam(required = false) String handleRemark) {
        return Result.success("异常处理完成", anomalyHandleOrderService.handleAnomaly(id, handleUserId, handleResult, handleRemark));
    }

    @PostMapping("/unfreeze-batch/{id}")
    public Result<AnomalyHandleOrder> unfreezeBatch(
            @PathVariable Long id,
            @RequestParam Long unfreezeUserId,
            @RequestParam(required = false) String unfreezeReason) {
        return Result.success("批次解冻成功", anomalyHandleOrderService.unfreezeBatch(id, unfreezeUserId, unfreezeReason));
    }

    @GetMapping("/batch-frozen")
    public Result<Boolean> isBatchFrozen(@RequestParam String machineBatch) {
        return Result.success(anomalyHandleOrderService.isBatchFrozen(machineBatch));
    }
}
