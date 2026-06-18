package com.semiconductor.mask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.semiconductor.mask.entity.MaskLockRecord;
import com.semiconductor.mask.mapper.MaskLockRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MaskLockRecordService extends ServiceImpl<MaskLockRecordMapper, MaskLockRecord> {

    public IPage<MaskLockRecord> pageQuery(Integer pageNum, Integer pageSize, String recordNo, Long maskId, String lockType, String lockStatus) {
        Page<MaskLockRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaskLockRecord> wrapper = new LambdaQueryWrapper<>();
        if (recordNo != null && !recordNo.isEmpty()) {
            wrapper.like(MaskLockRecord::getRecordNo, recordNo);
        }
        if (maskId != null) {
            wrapper.eq(MaskLockRecord::getMaskId, maskId);
        }
        if (lockType != null && !lockType.isEmpty()) {
            wrapper.eq(MaskLockRecord::getLockType, lockType);
        }
        if (lockStatus != null && !lockStatus.isEmpty()) {
            wrapper.eq(MaskLockRecord::getLockStatus, lockStatus);
        }
        wrapper.orderByDesc(MaskLockRecord::getLockTime);
        return this.page(page, wrapper);
    }
}
