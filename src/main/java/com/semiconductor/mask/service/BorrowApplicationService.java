package com.semiconductor.mask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.semiconductor.mask.common.BusinessException;
import com.semiconductor.mask.dto.BorrowApplyDTO;
import com.semiconductor.mask.entity.BorrowApplication;
import com.semiconductor.mask.entity.BorrowOrder;
import com.semiconductor.mask.entity.MaskInfo;
import com.semiconductor.mask.entity.SysUser;
import com.semiconductor.mask.mapper.BorrowApplicationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class BorrowApplicationService extends ServiceImpl<BorrowApplicationMapper, BorrowApplication> {

    @Autowired
    private MaskInfoService maskInfoService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private AnomalyHandleOrderService anomalyHandleOrderService;

    public IPage<BorrowApplication> pageQuery(Integer pageNum, Integer pageSize, String applyNo, Long applicantId, Long maskId, String applyStatus, Integer isAbnormal) {
        Page<BorrowApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<>();
        if (applyNo != null && !applyNo.isEmpty()) {
            wrapper.like(BorrowApplication::getApplyNo, applyNo);
        }
        if (applicantId != null) {
            wrapper.eq(BorrowApplication::getApplicantId, applicantId);
        }
        if (maskId != null) {
            wrapper.eq(BorrowApplication::getMaskId, maskId);
        }
        if (applyStatus != null && !applyStatus.isEmpty()) {
            wrapper.eq(BorrowApplication::getApplyStatus, applyStatus);
        }
        if (isAbnormal != null) {
            wrapper.eq(BorrowApplication::getIsAbnormal, isAbnormal);
        }
        wrapper.orderByDesc(BorrowApplication::getCreateTime);
        return this.page(page, wrapper);
    }

    public BorrowApplication getByApplyNo(String applyNo) {
        LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowApplication::getApplyNo, applyNo);
        return this.getOne(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowApplication submitApplication(BorrowApplyDTO dto) {
        SysUser applicant = sysUserService.getById(dto.getApplicantId());
        if (applicant == null) {
            throw new BusinessException("申请人不存在");
        }
        if (applicant.getStatus() != null && applicant.getStatus() == 0) {
            throw new BusinessException("申请人已被禁用");
        }

        MaskInfo mask = maskInfoService.getById(dto.getMaskId());
        if (mask == null) {
            throw new BusinessException("光罩不存在");
        }

        maskInfoService.checkCalibrationExpire(dto.getMaskId());
        mask = maskInfoService.getById(dto.getMaskId());

        if (!"IN_STOCK".equals(mask.getStatus())) {
            if ("LOCKED".equals(mask.getStatus())) {
                throw new BusinessException("光罩已锁定，原因：" + mask.getLockReason());
            }
            if ("BORROWED".equals(mask.getStatus())) {
                throw new BusinessException("光罩已被借出");
            }
            throw new BusinessException("光罩当前状态不可借用");
        }

        boolean cleanLevelMatch = maskInfoService.isCleanLevelMatch(dto.getCleanLevel(), mask.getCleanLevel());
        if (!cleanLevelMatch) {
            throw new BusinessException("申请洁净等级[" + dto.getCleanLevel() + "]不满足光罩要求[" + mask.getCleanLevel() + "]，无法借用");
        }

        if (anomalyHandleOrderService.isBatchFrozen(dto.getMachineBatch())) {
            throw new BusinessException("工艺批次[" + dto.getMachineBatch() + "]已被冻结，存在未处理的划伤异常，禁止提交借用申请");
        }

        BorrowApplication application = new BorrowApplication();
        application.setApplyNo(generateApplyNo());
        application.setApplicantId(dto.getApplicantId());
        application.setApplicantName(applicant.getUserName());
        application.setMaskId(dto.getMaskId());
        application.setMaskCode(mask.getMaskCode());
        application.setMaskName(mask.getMaskName());
        application.setMachineBatch(dto.getMachineBatch());
        application.setMachineCode(dto.getMachineCode());
        application.setCleanLevel(dto.getCleanLevel());
        application.setPurpose(dto.getPurpose());
        application.setExpectReturnDate(dto.getExpectReturnDate());
        application.setApplyStatus("PENDING");
        application.setIsAbnormal(0);
        application.setSupervisorReviewFlag(0);
        this.save(application);

        log.info("借用申请提交成功，申请单号：{}，申请人：{}，光罩：{}，机台：{}，批次：{}，洁净等级：{}",
                application.getApplyNo(), applicant.getUserName(), mask.getMaskName(),
                dto.getMachineCode(), dto.getMachineBatch(), dto.getCleanLevel());

        return application;
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowApplication approveApplication(Long applicationId, Long approveUserId, boolean approved, String remark) {
        BorrowApplication application = this.getById(applicationId);
        if (application == null) {
            throw new BusinessException("申请单不存在");
        }
        if (!"PENDING".equals(application.getApplyStatus())) {
            throw new BusinessException("申请单当前状态不可审批");
        }

        SysUser approver = sysUserService.getById(approveUserId);
        if (approver == null) {
            throw new BusinessException("审批人不存在");
        }

        MaskInfo mask = maskInfoService.getById(application.getMaskId());
        if (mask == null) {
            throw new BusinessException("光罩不存在");
        }
        if (!"IN_STOCK".equals(mask.getStatus())) {
            if ("LOCKED".equals(mask.getStatus())) {
                throw new BusinessException("光罩已锁定，无法批准借用");
            }
            if ("BORROWED".equals(mask.getStatus())) {
                throw new BusinessException("光罩已被借出，无法批准借用");
            }
        }

        application.setApproveUserId(approveUserId);
        application.setApproveTime(LocalDateTime.now());
        application.setApproveRemark(remark);

        if (approved) {
            application.setApplyStatus("APPROVED");
            log.info("借用申请批准通过，申请单号：{}，审批人：{}", application.getApplyNo(), approver.getUserName());
        } else {
            application.setApplyStatus("REJECTED");
            log.info("借用申请已驳回，申请单号：{}，审批人：{}，原因：{}",
                    application.getApplyNo(), approver.getUserName(), remark);
        }

        this.updateById(application);
        return application;
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowApplication cancelApplication(Long applicationId) {
        BorrowApplication application = this.getById(applicationId);
        if (application == null) {
            throw new BusinessException("申请单不存在");
        }
        if (!"PENDING".equals(application.getApplyStatus()) && !"APPROVED".equals(application.getApplyStatus())) {
            throw new BusinessException("申请单当前状态不可取消");
        }

        application.setApplyStatus("CANCELLED");
        this.updateById(application);

        log.info("借用申请已取消，申请单号：{}", application.getApplyNo());
        return application;
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowApplication handleAbnormalApplication(Long applicationId, Long supervisorId, String abnormalRemark, boolean approve) {
        BorrowApplication application = this.getById(applicationId);
        if (application == null) {
            throw new BusinessException("申请单不存在");
        }

        SysUser supervisor = sysUserService.getById(supervisorId);
        if (supervisor == null) {
            throw new BusinessException("主管不存在");
        }
        if (!"SUPERVISOR".equals(supervisor.getRoleType())) {
            throw new BusinessException("只有工艺主管可以处理异常借用");
        }

        application.setIsAbnormal(1);
        application.setAbnormalRemark(abnormalRemark);
        application.setApproveUserId(supervisorId);
        application.setApproveTime(LocalDateTime.now());

        if (approve) {
            application.setApplyStatus("APPROVED");
            log.info("异常借用申请主管已批准，申请单号：{}，主管：{}", application.getApplyNo(), supervisor.getUserName());
        } else {
            application.setApplyStatus("REJECTED");
            log.info("异常借用申请主管已驳回，申请单号：{}，主管：{}", application.getApplyNo(), supervisor.getUserName());
        }

        this.updateById(application);
        return application;
    }

    private String generateApplyNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = this.count(new LambdaQueryWrapper<BorrowApplication>()
                .like(BorrowApplication::getApplyNo, "AP" + dateStr));
        return "AP" + dateStr + String.format("%04d", count + 1);
    }
}
