package com.semiconductor.mask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.semiconductor.mask.common.Result;
import com.semiconductor.mask.entity.AppearanceCheckPhoto;
import com.semiconductor.mask.service.AppearanceCheckPhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appearance-check-photo")
public class AppearanceCheckPhotoController {

    @Autowired
    private AppearanceCheckPhotoService appearanceCheckPhotoService;

    @GetMapping("/page")
    public Result<IPage<AppearanceCheckPhoto>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long borrowOrderId,
            @RequestParam(required = false) Long maskId,
            @RequestParam(required = false) String checkResult,
            @RequestParam(required = false) String checkType) {
        return Result.success(appearanceCheckPhotoService.pageQuery(pageNum, pageSize, borrowOrderId, maskId, checkResult, checkType));
    }

    @GetMapping("/list/by-order/{borrowOrderId}")
    public Result<List<AppearanceCheckPhoto>> listByBorrowOrder(@PathVariable Long borrowOrderId) {
        return Result.success(appearanceCheckPhotoService.getByBorrowOrderId(borrowOrderId));
    }

    @GetMapping("/list/by-return/{returnRecordId}")
    public Result<List<AppearanceCheckPhoto>> listByReturnRecord(@PathVariable Long returnRecordId) {
        return Result.success(appearanceCheckPhotoService.getByReturnRecordId(returnRecordId));
    }

    @PostMapping("/save")
    public Result<AppearanceCheckPhoto> savePhoto(
            @RequestParam Long borrowOrderId,
            @RequestParam String orderNo,
            @RequestParam(required = false) Long returnRecordId,
            @RequestParam Long maskId,
            @RequestParam String maskCode,
            @RequestParam String photoPath,
            @RequestParam String photoName,
            @RequestParam(required = false) Long photoSize,
            @RequestParam String checkResult,
            @RequestParam(defaultValue = "RETURN") String checkType,
            @RequestParam Long checkUserId,
            @RequestParam String checkUserName) {
        return Result.success("照片保存成功", appearanceCheckPhotoService.savePhoto(
                borrowOrderId, orderNo, returnRecordId, maskId, maskCode,
                photoPath, photoName, photoSize, checkResult, checkType, checkUserId, checkUserName));
    }
}
