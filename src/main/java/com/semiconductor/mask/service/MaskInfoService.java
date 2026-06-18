package com.semiconductor.mask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.semiconductor.mask.common.BusinessException;
import com.semiconductor.mask.entity.MaskInfo;
import com.semiconductor.mask.entity.MaskLockRecord;
import com.semiconductor.mask.entity.StorageLocation;
import com.semiconductor.mask.mapper.MaskInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class MaskInfoService extends ServiceImpl<MaskInfoMapper, MaskInfo> {

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private MaskLockRecordService maskLockRecordService;

    public IPage<MaskInfo> pageQuery(Integer pageNum, Integer pageSize, String maskCode, String maskName, String status, String cleanLevel) {
        Page<MaskInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaskInfo> wrapper = new LambdaQueryWrapper<>();
        if (maskCode != null && !maskCode.isEmpty()) {
            wrapper.like(MaskInfo::getMaskCode, maskCode);
        }
        if (maskName != null && !maskName.isEmpty()) {
            wrapper.like(MaskInfo::getMaskName, maskName);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MaskInfo::getStatus, status);
        }
        if (cleanLevel != null && !cleanLevel.isEmpty()) {
            wrapper.eq(MaskInfo::getCleanLevel, cleanLevel);
        }
        wrapper.orderByDesc(MaskInfo::getCreateTime);
        return this.page(page, wrapper);
    }

    public MaskInfo getByMaskCode(String maskCode) {
        LambdaQueryWrapper<MaskInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaskInfo::getMaskCode, maskCode);
        return this.getOne(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean addMask(MaskInfo maskInfo) {
        if (getByMaskCode(maskInfo.getMaskCode()) != null) {
            throw new BusinessException("光罩编码已存在");
        }
        if (maskInfo.getLocationId() != null) {
            StorageLocation location = storageLocationService.getById(maskInfo.getLocationId());
            if (location == null) {
                throw new BusinessException("库位不存在");
            }
            if (location.getCurrentCount() >= location.getCapacity()) {
                throw new BusinessException("库位容量已满");
            }
        }
        if (maskInfo.getCalibrationCycleDays() == null) {
            maskInfo.setCalibrationCycleDays(90);
        }
        if (maskInfo.getLastCalibrationDate() != null && maskInfo.getNextCalibrationDate() == null) {
            maskInfo.setNextCalibrationDate(maskInfo.getLastCalibrationDate().plusDays(maskInfo.getCalibrationCycleDays()));
        }
        if (maskInfo.getStatus() == null || maskInfo.getStatus().isEmpty()) {
            maskInfo.setStatus("IN_STOCK");
        }
        boolean result = this.save(maskInfo);
        if (result && maskInfo.getLocationId() != null) {
            storageLocationService.incrementCount(maskInfo.getLocationId());
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateMask(MaskInfo maskInfo) {
        MaskInfo oldMask = this.getById(maskInfo.getId());
        if (oldMask == null) {
            throw new BusinessException("光罩不存在");
        }
        if (maskInfo.getLocationId() != null && !maskInfo.getLocationId().equals(oldMask.getLocationId())) {
            StorageLocation newLocation = storageLocationService.getById(maskInfo.getLocationId());
            if (newLocation == null) {
                throw new BusinessException("新库位不存在");
            }
            if (newLocation.getCurrentCount() >= newLocation.getCapacity()) {
                throw new BusinessException("新库位容量已满");
            }
            if (oldMask.getLocationId() != null) {
                storageLocationService.decrementCount(oldMask.getLocationId());
            }
            storageLocationService.incrementCount(maskInfo.getLocationId());
        }
        if (maskInfo.getLastCalibrationDate() != null && maskInfo.getCalibrationCycleDays() != null) {
            maskInfo.setNextCalibrationDate(maskInfo.getLastCalibrationDate().plusDays(maskInfo.getCalibrationCycleDays()));
        }
        return this.updateById(maskInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMask(Long id) {
        MaskInfo mask = this.getById(id);
        if (mask == null) {
            throw new BusinessException("光罩不存在");
        }
        if ("BORROWED".equals(mask.getStatus())) {
            throw new BusinessException("光罩已借出，不能删除");
        }
        if (mask.getLocationId() != null) {
            storageLocationService.decrementCount(mask.getLocationId());
        }
        return this.removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void checkCalibrationExpire(Long maskId) {
        MaskInfo mask = this.getById(maskId);
        if (mask == null) {
            throw new BusinessException("光罩不存在");
        }
        if (mask.getNextCalibrationDate() != null && mask.getNextCalibrationDate().isBefore(LocalDate.now())) {
            if (!"LOCKED".equals(mask.getStatus())) {
                lockMask(maskId, "CALIBRATION", "校验周期已过，请重新校验后使用", null);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public MaskLockRecord lockMask(Long maskId, String lockType, String lockReason, Long lockUserId) {
        MaskInfo mask = this.getById(maskId);
        if (mask == null) {
            throw new BusinessException("光罩不存在");
        }
        if ("BORROWED".equals(mask.getStatus())) {
            throw new BusinessException("光罩已借出，无法锁定");
        }
        if ("LOCKED".equals(mask.getStatus())) {
            throw new BusinessException("光罩已处于锁定状态");
        }

        mask.setStatus("LOCKED");
        mask.setLockReason(lockReason);
        mask.setLockTime(LocalDateTime.now());
        mask.setLockUserId(lockUserId);
        this.updateById(mask);

        MaskLockRecord record = new MaskLockRecord();
        record.setRecordNo(generateLockRecordNo());
        record.setMaskId(maskId);
        record.setMaskCode(mask.getMaskCode());
        record.setMaskName(mask.getMaskName());
        record.setLockType(lockType);
        record.setLockReason(lockReason);
        record.setLockTime(LocalDateTime.now());
        record.setLockUserId(lockUserId);
        record.setLockStatus("LOCKED");
        maskLockRecordService.save(record);

        log.info("光罩锁定成功，maskId: {}, lockType: {}, reason: {}", maskId, lockType, lockReason);
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public MaskLockRecord unlockMask(Long lockRecordId, String unlockReason, Long unlockUserId) {
        MaskLockRecord record = maskLockRecordService.getById(lockRecordId);
        if (record == null) {
            throw new BusinessException("锁定记录不存在");
        }
        if (!"LOCKED".equals(record.getLockStatus())) {
            throw new BusinessException("该记录已解锁");
        }

        MaskInfo mask = this.getById(record.getMaskId());
        if (mask != null) {
            mask.setStatus("IN_STOCK");
            mask.setLockReason(null);
            mask.setLockTime(null);
            mask.setLockUserId(null);
            this.updateById(mask);
        }

        record.setLockStatus("UNLOCKED");
        record.setUnlockTime(LocalDateTime.now());
        record.setUnlockUserId(unlockUserId);
        record.setUnlockReason(unlockReason);
        maskLockRecordService.updateById(record);

        log.info("光罩解锁成功，maskId: {}, reason: {}", record.getMaskId(), unlockReason);
        return record;
    }

    public boolean isCleanLevelMatch(String userCleanLevel, String maskCleanLevel) {
        int userLevel = getCleanLevelValue(userCleanLevel);
        int maskLevel = getCleanLevelValue(maskCleanLevel);
        return userLevel <= maskLevel;
    }

    private int getCleanLevelValue(String cleanLevel) {
        switch (cleanLevel) {
            case "CLASS_10":
                return 1;
            case "CLASS_100":
                return 2;
            case "CLASS_1000":
                return 3;
            default:
                return 99;
        }
    }

    public List<MaskInfo> getAvailableMasks(String cleanLevel) {
        LambdaQueryWrapper<MaskInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaskInfo::getStatus, "IN_STOCK");
        if (cleanLevel != null && !cleanLevel.isEmpty()) {
            wrapper.eq(MaskInfo::getCleanLevel, cleanLevel);
        }
        wrapper.orderByDesc(MaskInfo::getCreateTime);
        return this.list(wrapper);
    }

    private String generateLockRecordNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = maskLockRecordService.count(new LambdaQueryWrapper<MaskLockRecord>()
                .like(MaskLockRecord::getRecordNo, "LOCK" + dateStr));
        return "LOCK" + dateStr + String.format("%04d", count + 1);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchCheckCalibrationExpire() {
        List<MaskInfo> masks = this.list(new LambdaQueryWrapper<MaskInfo>()
                .eq(MaskInfo::getStatus, "IN_STOCK")
                .isNotNull(MaskInfo::getNextCalibrationDate));
        for (MaskInfo mask : masks) {
            if (mask.getNextCalibrationDate().isBefore(LocalDate.now())) {
                try {
                    lockMask(mask.getId(), "CALIBRATION", "校验周期已过，请重新校验后使用", null);
                } catch (Exception e) {
                    log.error("自动锁定光罩失败，maskId: {}", mask.getId(), e);
                }
            }
        }
    }
}
