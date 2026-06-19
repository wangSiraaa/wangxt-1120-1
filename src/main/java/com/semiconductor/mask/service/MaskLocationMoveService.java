package com.semiconductor.mask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.semiconductor.mask.entity.BorrowOrder;
import com.semiconductor.mask.entity.MaskInfo;
import com.semiconductor.mask.entity.MaskLocationMove;
import com.semiconductor.mask.entity.StorageLocation;
import com.semiconductor.mask.mapper.MaskLocationMoveMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class MaskLocationMoveService extends ServiceImpl<MaskLocationMoveMapper, MaskLocationMove> {

    @Autowired
    private StorageLocationService storageLocationService;

    public IPage<MaskLocationMove> pageQuery(Integer pageNum, Integer pageSize, String moveNo, Long maskId, Long borrowOrderId, String moveType, String cleanLevelMismatch) {
        Page<MaskLocationMove> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaskLocationMove> wrapper = new LambdaQueryWrapper<>();
        if (moveNo != null && !moveNo.isEmpty()) {
            wrapper.like(MaskLocationMove::getMoveNo, moveNo);
        }
        if (maskId != null) {
            wrapper.eq(MaskLocationMove::getMaskId, maskId);
        }
        if (borrowOrderId != null) {
            wrapper.eq(MaskLocationMove::getBorrowOrderId, borrowOrderId);
        }
        if (moveType != null && !moveType.isEmpty()) {
            wrapper.eq(MaskLocationMove::getMoveType, moveType);
        }
        if (cleanLevelMismatch != null && !cleanLevelMismatch.isEmpty()) {
            wrapper.eq(MaskLocationMove::getCleanLevelMismatch, cleanLevelMismatch);
        }
        wrapper.orderByDesc(MaskLocationMove::getMoveTime);
        return this.page(page, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public MaskLocationMove recordMove(BorrowOrder order, MaskInfo mask, Long operatorId, String operatorName, String moveType) {
        StorageLocation fromLocation = null;
        if ("OUT_STOCK".equals(moveType) && mask.getLocationId() != null) {
            fromLocation = storageLocationService.getById(mask.getLocationId());
        }

        MaskLocationMove move = new MaskLocationMove();
        move.setMoveNo(generateMoveNo());
        move.setMaskId(mask.getId());
        move.setMaskCode(mask.getMaskCode());
        move.setMaskName(mask.getMaskName());
        move.setBorrowOrderId(order.getId());
        move.setOrderNo(order.getOrderNo());
        move.setMoveType(moveType);
        move.setOperatorId(operatorId);
        move.setOperatorName(operatorName);
        move.setMoveTime(LocalDateTime.now());

        if (fromLocation != null) {
            move.setFromLocationId(fromLocation.getId());
            move.setFromLocationCode(fromLocation.getLocationCode());
            move.setFromCleanLevel(fromLocation.getCleanLevel());
        }

        if (order.getCleanLevel() != null && fromLocation != null) {
            boolean mismatch = !order.getCleanLevel().equals(fromLocation.getCleanLevel());
            move.setCleanLevelMismatch(mismatch ? "YES" : "NO");
            if (mismatch) {
                move.setMoveReason("转运途经非匹配洁净区：光罩等级=" + order.getCleanLevel() + "，途经区域等级=" + fromLocation.getCleanLevel());
            }
        } else {
            move.setCleanLevelMismatch("NO");
        }

        this.save(move);
        log.info("库位移动记录已保存：moveNo={}, mask={}, type={}, 洁净区不匹配={}",
                move.getMoveNo(), mask.getMaskCode(), moveType, move.getCleanLevelMismatch());
        return move;
    }

    @Transactional(rollbackFor = Exception.class)
    public MaskLocationMove recordInStockMove(BorrowOrder order, MaskInfo mask, Long toLocationId, Long operatorId, String operatorName) {
        StorageLocation toLocation = storageLocationService.getById(toLocationId);

        MaskLocationMove move = new MaskLocationMove();
        move.setMoveNo(generateMoveNo());
        move.setMaskId(mask.getId());
        move.setMaskCode(mask.getMaskCode());
        move.setMaskName(mask.getMaskName());
        move.setBorrowOrderId(order.getId());
        move.setOrderNo(order.getOrderNo());
        move.setMoveType("IN_STOCK");
        move.setOperatorId(operatorId);
        move.setOperatorName(operatorName);
        move.setMoveTime(LocalDateTime.now());

        if (toLocation != null) {
            move.setToLocationId(toLocation.getId());
            move.setToLocationCode(toLocation.getLocationCode());
            move.setToCleanLevel(toLocation.getCleanLevel());
        }

        if (mask.getCleanLevel() != null && toLocation != null) {
            boolean mismatch = !mask.getCleanLevel().equals(toLocation.getCleanLevel());
            move.setCleanLevelMismatch(mismatch ? "YES" : "NO");
        } else {
            move.setCleanLevelMismatch("NO");
        }

        this.save(move);
        log.info("入库库位移动记录已保存：moveNo={}, mask={}, toLocation={}", move.getMoveNo(), mask.getMaskCode(), toLocation.getLocationCode());
        return move;
    }

    public boolean isTransferCleanLevelMismatch(String maskCleanLevel, Long fromLocationId) {
        if (fromLocationId == null || maskCleanLevel == null) {
            return false;
        }
        StorageLocation fromLocation = storageLocationService.getById(fromLocationId);
        if (fromLocation == null) {
            return false;
        }
        return !maskCleanLevel.equals(fromLocation.getCleanLevel());
    }

    private String generateMoveNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = this.count(new LambdaQueryWrapper<MaskLocationMove>()
                .like(MaskLocationMove::getMoveNo, "MV" + dateStr));
        return "MV" + dateStr + String.format("%04d", count + 1);
    }
}
