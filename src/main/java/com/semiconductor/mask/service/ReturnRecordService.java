package com.semiconductor.mask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.semiconductor.mask.entity.BorrowOrder;
import com.semiconductor.mask.entity.ReturnRecord;
import com.semiconductor.mask.entity.SysUser;
import com.semiconductor.mask.mapper.ReturnRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class ReturnRecordService extends ServiceImpl<ReturnRecordMapper, ReturnRecord> {

    public IPage<ReturnRecord> pageQuery(Integer pageNum, Integer pageSize, String recordNo, Long borrowOrderId, Long maskId, Long returnUserId, Integer appearanceCheckDone) {
        Page<ReturnRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReturnRecord> wrapper = new LambdaQueryWrapper<>();
        if (recordNo != null && !recordNo.isEmpty()) {
            wrapper.like(ReturnRecord::getRecordNo, recordNo);
        }
        if (borrowOrderId != null) {
            wrapper.eq(ReturnRecord::getBorrowOrderId, borrowOrderId);
        }
        if (maskId != null) {
            wrapper.eq(ReturnRecord::getMaskId, maskId);
        }
        if (returnUserId != null) {
            wrapper.eq(ReturnRecord::getReturnUserId, returnUserId);
        }
        if (appearanceCheckDone != null) {
            wrapper.eq(ReturnRecord::getAppearanceCheckDone, appearanceCheckDone);
        }
        wrapper.orderByDesc(ReturnRecord::getReturnTime);
        return this.page(page, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReturnRecord createReturnRecord(BorrowOrder order, SysUser receiveUser, Long locationId) {
        ReturnRecord record = new ReturnRecord();
        record.setRecordNo(generateRecordNo());
        record.setBorrowOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setMaskId(order.getMaskId());
        record.setMaskCode(order.getMaskCode());
        record.setMaskName(order.getMaskName());
        record.setReturnUserId(order.getBorrowerId());
        record.setReturnUserName(order.getBorrowerName());
        record.setReceiveUserId(receiveUser.getId());
        record.setReceiveUserName(receiveUser.getUserName());
        record.setReturnTime(LocalDateTime.now());
        record.setLocationId(locationId);
        record.setAppearanceCheckDone(order.getAppearanceCheckDone());
        record.setAppearanceCheckResult(order.getAppearanceCheckResult());
        record.setAppearanceCheckRemark(order.getAppearanceCheckRemark());
        record.setAppearanceCheckUserId(order.getAppearanceCheckUserId());
        record.setAppearanceCheckTime(order.getAppearanceCheckTime());
        record.setHasDamage(0);
        this.save(record);

        log.info("归还记录创建成功，记录号：{}，借用单号：{}", record.getRecordNo(), order.getOrderNo());
        return record;
    }

    public ReturnRecord getByRecordNo(String recordNo) {
        LambdaQueryWrapper<ReturnRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReturnRecord::getRecordNo, recordNo);
        return this.getOne(wrapper);
    }

    private String generateRecordNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = this.count(new LambdaQueryWrapper<ReturnRecord>()
                .like(ReturnRecord::getRecordNo, "RR" + dateStr));
        return "RR" + dateStr + String.format("%04d", count + 1);
    }
}
