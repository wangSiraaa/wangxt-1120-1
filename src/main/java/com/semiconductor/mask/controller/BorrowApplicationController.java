package com.semiconductor.mask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.semiconductor.mask.common.Result;
import com.semiconductor.mask.dto.BorrowApplyDTO;
import com.semiconductor.mask.entity.BorrowApplication;
import com.semiconductor.mask.service.BorrowApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/borrow-application")
public class BorrowApplicationController {

    @Autowired
    private BorrowApplicationService borrowApplicationService;

    @GetMapping("/page")
    public Result<IPage<BorrowApplication>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String applyNo,
            @RequestParam(required = false) Long applicantId,
            @RequestParam(required = false) Long maskId,
            @RequestParam(required = false) String applyStatus,
            @RequestParam(required = false) Integer isAbnormal) {
        return Result.success(borrowApplicationService.pageQuery(pageNum, pageSize, applyNo, applicantId, maskId, applyStatus, isAbnormal));
    }

    @GetMapping("/{id}")
    public Result<BorrowApplication> getById(@PathVariable Long id) {
        return Result.success(borrowApplicationService.getById(id));
    }

    @GetMapping("/no/{applyNo}")
    public Result<BorrowApplication> getByApplyNo(@PathVariable String applyNo) {
        return Result.success(borrowApplicationService.getByApplyNo(applyNo));
    }

    @PostMapping("/apply")
    public Result<BorrowApplication> apply(@Valid @RequestBody BorrowApplyDTO dto) {
        return Result.success("申请提交成功", borrowApplicationService.submitApplication(dto));
    }

    @PostMapping("/approve/{id}")
    public Result<BorrowApplication> approve(
            @PathVariable Long id,
            @RequestParam Long approveUserId,
            @RequestParam(defaultValue = "true") boolean approved,
            @RequestParam(required = false) String remark) {
        return Result.success(borrowApplicationService.approveApplication(id, approveUserId, approved, remark));
    }

    @PostMapping("/cancel/{id}")
    public Result<BorrowApplication> cancel(@PathVariable Long id) {
        return Result.success(borrowApplicationService.cancelApplication(id));
    }

    @PostMapping("/handle-abnormal/{id}")
    public Result<BorrowApplication> handleAbnormal(
            @PathVariable Long id,
            @RequestParam Long supervisorId,
            @RequestParam String abnormalRemark,
            @RequestParam(defaultValue = "true") boolean approve) {
        return Result.success(borrowApplicationService.handleAbnormalApplication(id, supervisorId, abnormalRemark, approve));
    }
}
