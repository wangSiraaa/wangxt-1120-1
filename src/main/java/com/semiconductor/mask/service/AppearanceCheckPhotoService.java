package com.semiconductor.mask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.semiconductor.mask.entity.AppearanceCheckPhoto;
import com.semiconductor.mask.mapper.AppearanceCheckPhotoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AppearanceCheckPhotoService extends ServiceImpl<AppearanceCheckPhotoMapper, AppearanceCheckPhoto> {

    public IPage<AppearanceCheckPhoto> pageQuery(Integer pageNum, Integer pageSize, Long borrowOrderId, Long maskId, String checkResult, String checkType) {
        Page<AppearanceCheckPhoto> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AppearanceCheckPhoto> wrapper = new LambdaQueryWrapper<>();
        if (borrowOrderId != null) {
            wrapper.eq(AppearanceCheckPhoto::getBorrowOrderId, borrowOrderId);
        }
        if (maskId != null) {
            wrapper.eq(AppearanceCheckPhoto::getMaskId, maskId);
        }
        if (checkResult != null && !checkResult.isEmpty()) {
            wrapper.eq(AppearanceCheckPhoto::getCheckResult, checkResult);
        }
        if (checkType != null && !checkType.isEmpty()) {
            wrapper.eq(AppearanceCheckPhoto::getCheckType, checkType);
        }
        wrapper.orderByDesc(AppearanceCheckPhoto::getCheckTime);
        return this.page(page, wrapper);
    }

    public List<AppearanceCheckPhoto> getByBorrowOrderId(Long borrowOrderId) {
        LambdaQueryWrapper<AppearanceCheckPhoto> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppearanceCheckPhoto::getBorrowOrderId, borrowOrderId);
        wrapper.orderByAsc(AppearanceCheckPhoto::getCheckTime);
        return this.list(wrapper);
    }

    public List<AppearanceCheckPhoto> getByReturnRecordId(Long returnRecordId) {
        LambdaQueryWrapper<AppearanceCheckPhoto> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppearanceCheckPhoto::getReturnRecordId, returnRecordId);
        wrapper.orderByAsc(AppearanceCheckPhoto::getCheckTime);
        return this.list(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public AppearanceCheckPhoto savePhoto(Long borrowOrderId, String orderNo, Long returnRecordId,
                                           Long maskId, String maskCode, String photoPath, String photoName,
                                           Long photoSize, String checkResult, String checkType,
                                           Long checkUserId, String checkUserName) {
        AppearanceCheckPhoto photo = new AppearanceCheckPhoto();
        photo.setBorrowOrderId(borrowOrderId);
        photo.setOrderNo(orderNo);
        photo.setReturnRecordId(returnRecordId);
        photo.setMaskId(maskId);
        photo.setMaskCode(maskCode);
        photo.setPhotoPath(photoPath);
        photo.setPhotoName(photoName);
        photo.setPhotoSize(photoSize);
        photo.setCheckResult(checkResult);
        photo.setCheckType(checkType);
        photo.setCheckUserId(checkUserId);
        photo.setCheckUserName(checkUserName);
        photo.setCheckTime(LocalDateTime.now());
        this.save(photo);

        log.info("外观检查照片已保存：orderId={}, mask={}, checkType={}, result={}",
                borrowOrderId, maskCode, checkType, checkResult);
        return photo;
    }
}
