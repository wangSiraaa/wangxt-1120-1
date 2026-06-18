package com.semiconductor.mask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.semiconductor.mask.common.Result;
import com.semiconductor.mask.entity.BorrowOrder;
import com.semiconductor.mask.service.BorrowOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/borrow-order")
public class BorrowOrderController {

    @Autowired
    private BorrowOrderService borrowOrderService;

    @GetMapping("/page")
    public Result<IPage<BorrowOrder>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long maskId,
            @RequestParam(required = false) Long borrowerId,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) Integer isAbnormal) {
        return Result.success(borrowOrderService.pageQuery(pageNum, pageSize, orderNo, maskId, borrowerId, orderStatus, isAbnormal));
    }

    @GetMapping("/{id}")
    public Result<BorrowOrder> getById(@PathVariable Long id) {
        return Result.success(borrowOrderService.getById(id));
    }

    @GetMapping("/no/{orderNo}")
    public Result<BorrowOrder> getByOrderNo(@PathVariable String orderNo) {
        return Result.success(borrowOrderService.getByOrderNo(orderNo));
    }

    @PostMapping("/create-from-application/{applicationId}")
    public Result<BorrowOrder> createFromApplication(
            @PathVariable Long applicationId,
            @RequestParam Long libUserId) {
        return Result.success("借用单创建成功", borrowOrderService.createOrderFromApplication(applicationId, libUserId));
    }

    @PostMapping("/out-stock/{orderId}")
    public Result<BorrowOrder> outStock(
            @PathVariable Long orderId,
            @RequestParam Long libUserId) {
        return Result.success("出库成功", borrowOrderService.outStock(orderId, libUserId));
    }

    @PostMapping("/in-stock/{orderId}")
    public Result<BorrowOrder> inStock(
            @PathVariable Long orderId,
            @RequestParam Long libUserId,
            @RequestParam Long locationId) {
        return Result.success("入库成功", borrowOrderService.inStock(orderId, libUserId, locationId));
    }

    @PostMapping("/appearance-check/{orderId}")
    public Result<BorrowOrder> appearanceCheck(
            @PathVariable Long orderId,
            @RequestParam Long checkUserId,
            @RequestParam String checkResult,
            @RequestParam(required = false) String checkRemark) {
        return Result.success("外观检查完成", borrowOrderService.appearanceCheck(orderId, checkUserId, checkResult, checkRemark));
    }

    @PostMapping("/close/{orderId}")
    public Result<BorrowOrder> close(@PathVariable Long orderId) {
        return Result.success("借用单已关闭", borrowOrderService.closeOrder(orderId));
    }
}
