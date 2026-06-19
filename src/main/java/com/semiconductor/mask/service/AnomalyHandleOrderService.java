package com.semiconductor.mask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.semiconductor.mask.common.BusinessException;
import com.semiconductor.mask.entity.AnomalyHandleOrder;
import com.semiconductor.mask.entity.BorrowApplication;
import com.semiconductor.mask.entity.BorrowOrder;
import com.semiconductor.mask.entity.SysUser;
import com.semiconductor.mask.mapper.AnomalyHandleOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class AnomalyHandleOrderService extends ServiceImpl<AnomalyHandleOrderMapper, AnomalyHandleOrder> {

    @Autowired
    private BorrowApplicationService borrowApplicationService;

    @Autowired
    private BorrowOrderService borrowOrderService;

    @Autowired
    private SysUserService sysUserService;

    public IPage<AnomalyHandleOrder> pageQuery(Integer pageNum, Integer pageSize, String anomalyNo, Long maskId, String anomalyType, String handleStatus, Integer batchFreezeFlag) {
        Page<AnomalyHandleOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AnomalyHandleOrder> wrapper = new LambdaQueryWrapper<>();
        if (anomalyNo != null && !anomalyNo.isEmpty()) {
            wrapper.like(AnomalyHandleOrder::getAnomalyNo, anomalyNo);
        }
        if (maskId != null) {
            wrapper.eq(AnomalyHandleOrder::getMaskId, maskId);
        }
        if (anomalyType != null && !anomalyType.isEmpty()) {
            wrapper.eq(AnomalyHandleOrder::getAnomalyType, anomalyType);
        }
        if (handleStatus != null && !handleStatus.isEmpty()) {
            wrapper.eq(AnomalyHandleOrder::getHandleStatus, handleStatus);
        }
        if (batchFreezeFlag != null) {
            wrapper.eq(AnomalyHandleOrder::getBatchFreezeFlag, batchFreezeFlag);
        }
        wrapper.orderByDesc(AnomalyHandleOrder::getCreateTime);
        return this.page(page, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public AnomalyHandleOrder createFromScratchDamage(BorrowOrder order, String damageDesc, Long reportUserId, String reportUserName) {
        AnomalyHandleOrder anomaly = new AnomalyHandleOrder();
        anomaly.setAnomalyNo(generateAnomalyNo());
        anomaly.setBorrowOrderId(order.getId());
        anomaly.setOrderNo(order.getOrderNo());
        anomaly.setMaskId(order.getMaskId());
        anomaly.setMaskCode(order.getMaskCode());
        anomaly.setMaskName(order.getMaskName());
        anomaly.setMachineBatch(order.getMachineBatch());
        anomaly.setAnomalyType("SCRATCH_DAMAGE");
        anomaly.setAnomalyDesc(damageDesc);
        anomaly.setReportUserId(reportUserId);
        anomaly.setReportUserName(reportUserName);
        anomaly.setReportTime(LocalDateTime.now());
        anomaly.setHandleStatus("PENDING");
        anomaly.setBatchFreezeFlag(1);
        anomaly.setBatchFreezeReason("外观检查发现划伤，自动冻结同工艺批次[" + order.getMachineBatch() + "]后续借用");
        anomaly.setBatchFreezeTime(LocalDateTime.now());
        this.save(anomaly);

        log.info("异常处理单已生成：anomalyNo={}, 类型=划伤, 工艺批次={}, 批次冻结=true",
                anomaly.getAnomalyNo(), order.getMachineBatch());
        return anomaly;
    }

    @Transactional(rollbackFor = Exception.class)
    public void freezeSameBatchBorrowOrders(String machineBatch) {
        LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowApplication::getMachineBatch, machineBatch);
        wrapper.eq(BorrowApplication::getApplyStatus, "PENDING");
        List<BorrowApplication> pendingApplies = borrowApplicationService.list(wrapper);

        for (BorrowApplication apply : pendingApplies) {
            apply.setIsAbnormal(1);
            apply.setAbnormalRemark("同工艺批次[" + machineBatch + "]光罩发现划伤，自动转主管复核");
            apply.setSupervisorReviewFlag(1);
            apply.setSupervisorReviewReason("同工艺批次发现划伤异常，需主管复核后方可借用");
            borrowApplicationService.updateById(apply);
            log.info("已冻结借用申请：applyNo={}, 批次={}, 转主管复核", apply.getApplyNo(), machineBatch);
        }

        LambdaQueryWrapper<BorrowOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(BorrowOrder::getMachineBatch, machineBatch);
        orderWrapper.eq(BorrowOrder::getOrderStatus, "CREATED");
        List<BorrowOrder> createdOrders = borrowOrderService.list(orderWrapper);

        for (BorrowOrder order : createdOrders) {
            order.setBatchFreezeFlag(1);
            order.setSupervisorReviewStatus("PENDING");
            borrowOrderService.updateById(order);
            log.info("已冻结借用单：orderNo={}, 批次={}", order.getOrderNo(), machineBatch);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public AnomalyHandleOrder handleAnomaly(Long anomalyId, Long handleUserId, String handleResult, String handleRemark) {
        AnomalyHandleOrder anomaly = this.getById(anomalyId);
        if (anomaly == null) {
            throw new BusinessException("异常处理单不存在");
        }
        if (!"PENDING".equals(anomaly.getHandleStatus())) {
            throw new BusinessException("异常处理单当前状态不可处理");
        }

        SysUser handleUser = sysUserService.getById(handleUserId);
        if (handleUser == null) {
            throw new BusinessException("处理人不存在");
        }

        anomaly.setHandleUserId(handleUserId);
        anomaly.setHandleUserName(handleUser.getUserName());
        anomaly.setHandleTime(LocalDateTime.now());
        anomaly.setHandleResult(handleResult);
        anomaly.setHandleRemark(handleRemark);
        anomaly.setHandleStatus("HANDLED");
        this.updateById(anomaly);

        log.info("异常处理单已处理：anomalyNo={}, 处理人={}, 结果={}",
                anomaly.getAnomalyNo(), handleUser.getUserName(), handleResult);
        return anomaly;
    }

    @Transactional(rollbackFor = Exception.class)
    public AnomalyHandleOrder unfreezeBatch(Long anomalyId, Long unfreezeUserId, String unfreezeReason) {
        AnomalyHandleOrder anomaly = this.getById(anomalyId);
        if (anomaly == null) {
            throw new BusinessException("异常处理单不存在");
        }
        if (anomaly.getBatchFreezeFlag() == null || anomaly.getBatchFreezeFlag() == 0) {
            throw new BusinessException("该异常处理单未冻结批次");
        }

        anomaly.setBatchFreezeFlag(0);
        anomaly.setBatchUnfreezeTime(LocalDateTime.now());
        anomaly.setBatchUnfreezeUserId(unfreezeUserId);
        anomaly.setHandleStatus("UNFROZEN");
        this.updateById(anomaly);

        log.info("批次冻结已解除：anomalyNo={}, 批次={}, 解冻人={}", anomaly.getAnomalyNo(), anomaly.getMachineBatch(), unfreezeUserId);
        return anomaly;
    }

    public boolean isBatchFrozen(String machineBatch) {
        LambdaQueryWrapper<AnomalyHandleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnomalyHandleOrder::getMachineBatch, machineBatch);
        wrapper.eq(AnomalyHandleOrder::getBatchFreezeFlag, 1);
        wrapper.eq(AnomalyHandleOrder::getHandleStatus, "PENDING");
        return this.count(wrapper) > 0;
    }

    public AnomalyHandleOrder getByAnomalyNo(String anomalyNo) {
        LambdaQueryWrapper<AnomalyHandleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnomalyHandleOrder::getAnomalyNo, anomalyNo);
        return this.getOne(wrapper);
    }

    private String generateAnomalyNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = this.count(new LambdaQueryWrapper<AnomalyHandleOrder>()
                .like(AnomalyHandleOrder::getAnomalyNo, "AN" + dateStr));
        return "AN" + dateStr + String.format("%04d", count + 1);
    }
}
