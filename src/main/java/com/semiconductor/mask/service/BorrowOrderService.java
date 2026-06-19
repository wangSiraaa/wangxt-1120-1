package com.semiconductor.mask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.semiconductor.mask.common.BusinessException;
import com.semiconductor.mask.entity.*;
import com.semiconductor.mask.mapper.BorrowOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class BorrowOrderService extends ServiceImpl<BorrowOrderMapper, BorrowOrder> {

    @Autowired
    private MaskInfoService maskInfoService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private BorrowApplicationService borrowApplicationService;

    @Autowired
    private ReturnRecordService returnRecordService;

    @Autowired
    private MaskLocationMoveService maskLocationMoveService;

    @Autowired
    private AnomalyHandleOrderService anomalyHandleOrderService;

    @Autowired
    private AppearanceCheckPhotoService appearanceCheckPhotoService;

    public IPage<BorrowOrder> pageQuery(Integer pageNum, Integer pageSize, String orderNo, Long maskId, Long borrowerId, String orderStatus, Integer isAbnormal) {
        Page<BorrowOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BorrowOrder> wrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(BorrowOrder::getOrderNo, orderNo);
        }
        if (maskId != null) {
            wrapper.eq(BorrowOrder::getMaskId, maskId);
        }
        if (borrowerId != null) {
            wrapper.eq(BorrowOrder::getBorrowerId, borrowerId);
        }
        if (orderStatus != null && !orderStatus.isEmpty()) {
            wrapper.eq(BorrowOrder::getOrderStatus, orderStatus);
        }
        if (isAbnormal != null) {
            wrapper.eq(BorrowOrder::getIsAbnormal, isAbnormal);
        }
        wrapper.orderByDesc(BorrowOrder::getCreateTime);
        return this.page(page, wrapper);
    }

    public BorrowOrder getByOrderNo(String orderNo) {
        LambdaQueryWrapper<BorrowOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowOrder::getOrderNo, orderNo);
        return this.getOne(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowOrder createOrderFromApplication(Long applicationId, Long libUserId) {
        BorrowApplication application = borrowApplicationService.getById(applicationId);
        if (application == null) {
            throw new BusinessException("申请单不存在");
        }
        if (!"APPROVED".equals(application.getApplyStatus())) {
            throw new BusinessException("申请单未批准，无法生成借用单");
        }
        if (application.getBorrowOrderId() != null) {
            throw new BusinessException("申请单已生成借用单，请勿重复操作");
        }

        SysUser libUser = sysUserService.getById(libUserId);
        if (libUser == null) {
            throw new BusinessException("出库操作人不存在");
        }

        MaskInfo mask = maskInfoService.getById(application.getMaskId());
        if (mask == null) {
            throw new BusinessException("光罩不存在");
        }
        if (!"IN_STOCK".equals(mask.getStatus())) {
            throw new BusinessException("光罩当前状态不可出借");
        }

        BorrowOrder order = new BorrowOrder();
        order.setOrderNo(generateOrderNo());
        order.setApplyId(applicationId);
        order.setMaskId(application.getMaskId());
        order.setMaskCode(application.getMaskCode());
        order.setMaskName(application.getMaskName());
        order.setBorrowerId(application.getApplicantId());
        order.setBorrowerName(application.getApplicantName());
        order.setMachineBatch(application.getMachineBatch());
        order.setMachineCode(application.getMachineCode());
        order.setCleanLevel(application.getCleanLevel());
        order.setOrderStatus("CREATED");
        order.setIsAbnormal(application.getIsAbnormal());
        order.setAppearanceCheckDone(0);
        order.setBatchFreezeFlag(0);
        order.setSupervisorReviewStatus("NONE");
        this.save(order);

        application.setBorrowOrderId(order.getId());
        application.setApplyStatus("BORROWED");
        borrowApplicationService.updateById(application);

        log.info("借用单创建成功，单号：{}，关联申请单：{}，机台：{}，洁净等级：{}",
                order.getOrderNo(), application.getApplyNo(), order.getMachineCode(), order.getCleanLevel());
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowOrder outStock(Long orderId, Long libUserId) {
        BorrowOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("借用单不存在");
        }
        if (!"CREATED".equals(order.getOrderStatus())) {
            throw new BusinessException("借用单当前状态不可出库");
        }

        SysUser libUser = sysUserService.getById(libUserId);
        if (libUser == null) {
            throw new BusinessException("出库操作人不存在");
        }

        MaskInfo mask = maskInfoService.getById(order.getMaskId());
        if (mask == null) {
            throw new BusinessException("光罩不存在");
        }

        maskInfoService.checkCalibrationExpire(order.getMaskId());
        mask = maskInfoService.getById(order.getMaskId());

        if (!"IN_STOCK".equals(mask.getStatus())) {
            throw new BusinessException("光罩当前状态不可出库");
        }

        SysUser borrower = sysUserService.getById(order.getBorrowerId());
        if (borrower == null) {
            throw new BusinessException("借用人不存在");
        }

        boolean cleanLevelMatch = maskInfoService.isCleanLevelMatch(borrower.getCleanLevel(), mask.getCleanLevel());
        if (!cleanLevelMatch) {
            throw new BusinessException("借用人洁净等级不满足光罩要求，禁止出库");
        }

        boolean transferMismatch = maskLocationMoveService.isTransferCleanLevelMismatch(order.getCleanLevel(), mask.getLocationId());

        if (mask.getLocationId() != null) {
            storageLocationService.decrementCount(mask.getLocationId());
            order.setOutLocationId(mask.getLocationId());
        }

        maskLocationMoveService.recordMove(order, mask, libUserId, libUser.getUserName(), "OUT_STOCK");

        mask.setStatus("BORROWED");
        mask.setLocationId(null);
        maskInfoService.updateById(mask);

        order.setOutLibUserId(libUserId);
        order.setOutLibUserName(libUser.getUserName());
        order.setOutLibTime(LocalDateTime.now());

        if (transferMismatch) {
            order.setOrderStatus("SUPERVISOR_REVIEW");
            order.setSupervisorReviewStatus("PENDING");
            order.setIsAbnormal(1);
            order.setAbnormalRemark("出库途经非匹配洁净区，自动转主管复核");

            BorrowApplication application = borrowApplicationService.getById(order.getApplyId());
            if (application != null) {
                application.setSupervisorReviewFlag(1);
                application.setSupervisorReviewReason("出库途经非匹配洁净区，需主管复核后方可继续");
                application.setIsAbnormal(1);
                application.setAbnormalRemark("出库途经非匹配洁净区，自动转主管复核");
                borrowApplicationService.updateById(application);
            }

            log.warn("光罩出库途经非匹配洁净区，借用单转主管复核：orderNo={}, 光罩等级={}, 途经区域等级不匹配",
                    order.getOrderNo(), order.getCleanLevel());
        } else {
            order.setOrderStatus("OUT_STOCK");
        }

        this.updateById(order);

        log.info("光罩出库成功，借用单号：{}，光罩：{}，操作人：{}，状态：{}",
                order.getOrderNo(), mask.getMaskCode(), libUser.getUserName(), order.getOrderStatus());

        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowOrder supervisorReviewPass(Long orderId, Long supervisorId, String reviewRemark) {
        BorrowOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("借用单不存在");
        }
        if (!"SUPERVISOR_REVIEW".equals(order.getOrderStatus())) {
            throw new BusinessException("借用单当前状态不可主管复核");
        }

        SysUser supervisor = sysUserService.getById(supervisorId);
        if (supervisor == null) {
            throw new BusinessException("主管不存在");
        }
        if (!"SUPERVISOR".equals(supervisor.getRoleType())) {
            throw new BusinessException("只有工艺主管可以复核");
        }

        order.setSupervisorReviewStatus("APPROVED");
        order.setSupervisorReviewUserId(supervisorId);
        order.setSupervisorReviewTime(LocalDateTime.now());
        order.setSupervisorReviewRemark(reviewRemark);
        order.setOrderStatus("OUT_STOCK");
        this.updateById(order);

        log.info("主管复核通过，借用单：{}，主管：{}，意见：{}", order.getOrderNo(), supervisor.getUserName(), reviewRemark);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowOrder supervisorReviewReject(Long orderId, Long supervisorId, String reviewRemark) {
        BorrowOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("借用单不存在");
        }
        if (!"SUPERVISOR_REVIEW".equals(order.getOrderStatus())) {
            throw new BusinessException("借用单当前状态不可主管复核");
        }

        SysUser supervisor = sysUserService.getById(supervisorId);
        if (supervisor == null) {
            throw new BusinessException("主管不存在");
        }
        if (!"SUPERVISOR".equals(supervisor.getRoleType())) {
            throw new BusinessException("只有工艺主管可以复核");
        }

        order.setSupervisorReviewStatus("REJECTED");
        order.setSupervisorReviewUserId(supervisorId);
        order.setSupervisorReviewTime(LocalDateTime.now());
        order.setSupervisorReviewRemark(reviewRemark);
        order.setOrderStatus("REJECTED");
        this.updateById(order);

        MaskInfo mask = maskInfoService.getById(order.getMaskId());
        if (mask != null && "BORROWED".equals(mask.getStatus())) {
            if (order.getOutLocationId() != null) {
                mask.setLocationId(order.getOutLocationId());
                storageLocationService.incrementCount(order.getOutLocationId());
            }
            mask.setStatus("IN_STOCK");
            maskInfoService.updateById(mask);
        }

        BorrowApplication application = borrowApplicationService.getById(order.getApplyId());
        if (application != null) {
            application.setApplyStatus("REJECTED");
            borrowApplicationService.updateById(application);
        }

        log.info("主管复核驳回，借用单：{}，主管：{}，原因：{}", order.getOrderNo(), supervisor.getUserName(), reviewRemark);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowOrder inStock(Long orderId, Long libUserId, Long locationId) {
        BorrowOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("借用单不存在");
        }
        if (!"OUT_STOCK".equals(order.getOrderStatus())) {
            throw new BusinessException("借用单当前状态不可入库");
        }

        if (order.getAppearanceCheckDone() == null || order.getAppearanceCheckDone() == 0) {
            throw new BusinessException("未完成外观检查，不能入库");
        }

        SysUser libUser = sysUserService.getById(libUserId);
        if (libUser == null) {
            throw new BusinessException("入库操作人不存在");
        }

        MaskInfo mask = maskInfoService.getById(order.getMaskId());
        if (mask == null) {
            throw new BusinessException("光罩不存在");
        }

        if (locationId != null) {
            StorageLocation location = storageLocationService.getById(locationId);
            if (location == null) {
                throw new BusinessException("入库库位不存在");
            }
            if (location.getCurrentCount() >= location.getCapacity()) {
                throw new BusinessException("入库库位容量已满");
            }
            if (!location.getCleanLevel().equals(mask.getCleanLevel())) {
                throw new BusinessException("库位洁净等级与光罩不匹配");
            }
        } else {
            throw new BusinessException("请选择入库库位");
        }

        storageLocationService.incrementCount(locationId);

        maskLocationMoveService.recordInStockMove(order, mask, locationId, libUserId, libUser.getUserName());

        mask.setStatus("IN_STOCK");
        mask.setLocationId(locationId);
        maskInfoService.updateById(mask);

        order.setInLibUserId(libUserId);
        order.setInLibUserName(libUser.getUserName());
        order.setInLibTime(LocalDateTime.now());
        order.setInLocationId(locationId);
        order.setOrderStatus("IN_STOCK");

        if (order.getOutLibTime() != null) {
            long days = ChronoUnit.DAYS.between(order.getOutLibTime().toLocalDate(), LocalDate.now());
            order.setActualBorrowDays((int) days);
        }

        this.updateById(order);

        BorrowApplication application = borrowApplicationService.getById(order.getApplyId());
        if (application != null) {
            application.setApplyStatus("RETURNED");
            borrowApplicationService.updateById(application);
        }

        returnRecordService.createReturnRecord(order, libUser, locationId);

        log.info("光罩入库成功，借用单号：{}，光罩：{}，操作人：{}",
                order.getOrderNo(), mask.getMaskCode(), libUser.getUserName());

        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowOrder appearanceCheck(Long orderId, Long checkUserId, String checkResult, String checkRemark) {
        BorrowOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("借用单不存在");
        }
        if (!"OUT_STOCK".equals(order.getOrderStatus())) {
            throw new BusinessException("借用单当前状态不可进行外观检查");
        }

        SysUser checkUser = sysUserService.getById(checkUserId);
        if (checkUser == null) {
            throw new BusinessException("检查人不存在");
        }

        order.setAppearanceCheckDone(1);
        order.setAppearanceCheckResult(checkResult);
        order.setAppearanceCheckRemark(checkRemark);
        order.setAppearanceCheckUserId(checkUserId);
        order.setAppearanceCheckTime(LocalDateTime.now());

        if ("SCRATCH".equals(checkResult) || "FAIL".equals(checkResult)) {
            order.setIsAbnormal(1);
            order.setAbnormalRemark("外观检查发现划伤/不合格：" + checkRemark);

            AnomalyHandleOrder anomaly = anomalyHandleOrderService.createFromScratchDamage(
                    order, "外观检查发现划伤：" + checkRemark, checkUserId, checkUser.getUserName());

            anomalyHandleOrderService.freezeSameBatchBorrowOrders(order.getMachineBatch());

            order.setBatchFreezeFlag(1);

            log.warn("外观检查发现划伤！借用单：{}，批次：{}，已生成异常处理单：{}，同批次后续借用已冻结",
                    order.getOrderNo(), order.getMachineBatch(), anomaly.getAnomalyNo());
        }

        this.updateById(order);

        log.info("光罩外观检查完成，借用单号：{}，结果：{}，检查人：{}",
                order.getOrderNo(), checkResult, checkUser.getUserName());

        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowOrder closeOrder(Long orderId) {
        BorrowOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("借用单不存在");
        }
        if (!"IN_STOCK".equals(order.getOrderStatus())) {
            throw new BusinessException("借用单未入库，不能关闭");
        }
        if (order.getAppearanceCheckDone() == null || order.getAppearanceCheckDone() == 0) {
            throw new BusinessException("未完成外观检查，不能关闭借用单");
        }

        order.setOrderStatus("CLOSED");
        this.updateById(order);

        log.info("借用单已关闭，单号：{}", order.getOrderNo());
        return order;
    }

    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = this.count(new LambdaQueryWrapper<BorrowOrder>()
                .like(BorrowOrder::getOrderNo, "BO" + dateStr));
        return "BO" + dateStr + String.format("%04d", count + 1);
    }
}
