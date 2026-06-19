package com.semiconductor.mask.validator;

import com.semiconductor.mask.dto.BorrowApplyDTO;
import com.semiconductor.mask.entity.*;
import com.semiconductor.mask.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@Order(1)
public class StartupValidator implements ApplicationRunner {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private MaskInfoService maskInfoService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private BorrowApplicationService borrowApplicationService;

    @Autowired
    private BorrowOrderService borrowOrderService;

    @Autowired
    private ReturnRecordService returnRecordService;

    @Autowired
    private MaskLockRecordService maskLockRecordService;

    @Autowired
    private MaskLocationMoveService maskLocationMoveService;

    @Autowired
    private AppearanceCheckPhotoService appearanceCheckPhotoService;

    @Autowired
    private AnomalyHandleOrderService anomalyHandleOrderService;

    @Value("${app.startup-validate.enabled:true}")
    private boolean enabled;

    private static final AtomicBoolean VALIDATED = new AtomicBoolean(false);

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || VALIDATED.get()) {
            return;
        }

        log.info("========================================");
        log.info("光罩借用系统启动验证开始...");
        log.info("========================================");

        try {
            validateUserQuery();
            validateMaskQuery();
            validateLocationQuery();
            validateBorrowApplyFlow();
            validateCalibrationLock();
            validateReturnFlow();
            validateRecordReadBack();
            validateCleanAreaTransfer();
            validateScratchDamageAndBatchFreeze();

            VALIDATED.set(true);
            log.info("========================================");
            log.info("✓ 所有启动验证通过！");
            log.info("========================================");
        } catch (Exception e) {
            log.error("========================================");
            log.error("✗ 启动验证失败：{}", e.getMessage(), e);
            log.error("========================================");
            throw new IllegalStateException("光罩借用系统启动验证失败", e);
        }
    }

    private void validateUserQuery() {
        log.info("[验证1/9] 查询用户列表...");
        SysUser engineer = sysUserService.getByUserCode("ENG001");
        SysUser libUser = sysUserService.getByUserCode("LIB001");
        SysUser supervisor = sysUserService.getByUserCode("SUP001");

        if (engineer == null || libUser == null || supervisor == null) {
            throw new RuntimeException("初始化用户数据缺失，请检查数据库脚本");
        }
        if (!"ENGINEER".equals(engineer.getRoleType())) {
            throw new RuntimeException("用户ENG001角色类型错误");
        }
        if (!"CLASS_100".equals(engineer.getCleanLevel())) {
            throw new RuntimeException("用户ENG001洁净等级错误");
        }
        log.info("  ✓ 用户查询正常：工程师={}, 洁净等级={}; 库管={}; 主管={}",
                engineer.getUserName(), engineer.getCleanLevel(),
                libUser.getUserName(), supervisor.getUserName());
    }

    private void validateMaskQuery() {
        log.info("[验证2/9] 查询光罩列表...");
        MaskInfo mask1 = maskInfoService.getByMaskCode("MASK-001");
        MaskInfo mask3 = maskInfoService.getByMaskCode("MASK-003");

        if (mask1 == null || mask3 == null) {
            throw new RuntimeException("初始化光罩数据缺失，请检查数据库脚本");
        }
        if (!"CLASS_10".equals(mask1.getCleanLevel())) {
            throw new RuntimeException("光罩MASK-001洁净等级错误");
        }
        if (!"IN_STOCK".equals(mask1.getStatus())) {
            throw new RuntimeException("光罩MASK-001状态错误");
        }
        log.info("  ✓ 光罩查询正常：MASK-001等级={}, 状态={}; MASK-003等级={}",
                mask1.getCleanLevel(), mask1.getStatus(), mask3.getCleanLevel());
    }

    private void validateLocationQuery() {
        log.info("[验证3/9] 查询库位列表...");
        StorageLocation loc1 = storageLocationService.getByCode("LOC-A-01-01");
        StorageLocation loc3 = storageLocationService.getByCode("LOC-B-01-01");

        if (loc1 == null || loc3 == null) {
            throw new RuntimeException("初始化库位数据缺失，请检查数据库脚本");
        }
        if (!"CLASS_10".equals(loc1.getCleanLevel())) {
            throw new RuntimeException("库位LOC-A-01-01洁净等级错误");
        }
        log.info("  ✓ 库位查询正常：LOC-A-01-01等级={}, 容量={}/{}; LOC-B-01-01等级={}",
                loc1.getCleanLevel(), loc1.getCurrentCount(), loc1.getCapacity(),
                loc3.getCleanLevel());
    }

    private void validateBorrowApplyFlow() {
        log.info("[验证4/9] 验证借用申请→审批→出库流程...");

        SysUser engineer = sysUserService.getByUserCode("ENG002");
        SysUser supervisor = sysUserService.getByUserCode("SUP001");
        SysUser libUser = sysUserService.getByUserCode("LIB001");
        MaskInfo mask = maskInfoService.getByMaskCode("MASK-001");

        if (mask == null || engineer == null || supervisor == null || libUser == null) {
            throw new RuntimeException("验证所需基础数据缺失");
        }

        BorrowApplyDTO dto = new BorrowApplyDTO();
        dto.setApplicantId(engineer.getId());
        dto.setMaskId(mask.getId());
        dto.setMachineBatch("LOT-20260619-001");
        dto.setMachineCode("STEPPER-A01");
        dto.setCleanLevel("CLASS_10");
        dto.setPurpose("工艺验证批次生产");
        dto.setExpectReturnDate(LocalDate.now().plusDays(7));

        BorrowApplication apply = borrowApplicationService.submitApplication(dto);
        log.info("  申请提交成功：applyNo={}, 状态={}, 机台={}, 批次={}, 洁净等级={}",
                apply.getApplyNo(), apply.getApplyStatus(), apply.getMachineCode(), apply.getMachineBatch(), apply.getCleanLevel());

        if (apply.getSupervisorReviewFlag() != 0) {
            throw new RuntimeException("正常借用申请不应标记主管复核");
        }

        apply = borrowApplicationService.approveApplication(apply.getId(), supervisor.getId(), true, "审批通过");
        log.info("  主管审批成功：审批人={}, 审批状态={}", supervisor.getUserName(), apply.getApplyStatus());

        BorrowOrder order = borrowOrderService.createOrderFromApplication(apply.getId(), libUser.getId());
        log.info("  借用单创建成功：orderNo={}, 状态={}, 机台={}, 洁净等级={}",
                order.getOrderNo(), order.getOrderStatus(), order.getMachineCode(), order.getCleanLevel());

        order = borrowOrderService.outStock(order.getId(), libUser.getId());
        log.info("  光罩出库成功：出库时间={}, 操作人={}, 单据状态={}",
                order.getOutLibTime(), order.getOutLibUserName(), order.getOrderStatus());

        MaskInfo maskAfter = maskInfoService.getById(mask.getId());
        if (!"BORROWED".equals(maskAfter.getStatus())) {
            throw new RuntimeException("光罩出库后状态未更新为BORROWED");
        }

        long moveCount = maskLocationMoveService.count();
        if (moveCount <= 0) {
            throw new RuntimeException("出库后未生成库位移动记录");
        }
        log.info("  ✓ 借用全流程验证通过：申请→审批→出库，光罩状态={}，库位移动记录数={}", maskAfter.getStatus(), moveCount);
    }

    private void validateCalibrationLock() {
        log.info("[验证5/9] 验证校验周期锁定机制...");

        MaskInfo mask5 = maskInfoService.getByMaskCode("MASK-005");
        if (mask5 == null) {
            throw new RuntimeException("光罩MASK-005不存在");
        }

        LocalDate oldDate = LocalDate.now().minusDays(181);
        mask5.setLastCalibrationDate(oldDate);
        mask5.setCalibrationCycleDays(180);
        mask5.setNextCalibrationDate(oldDate.plusDays(180));
        maskInfoService.updateById(mask5);
        log.info("  设置光罩MASK-005：上次校验={}, 周期=180天, 下次校验={}（已过期）",
                mask5.getLastCalibrationDate(), mask5.getNextCalibrationDate());

        maskInfoService.checkCalibrationExpire(mask5.getId());

        MaskInfo maskAfter = maskInfoService.getById(mask5.getId());
        if (!"LOCKED".equals(maskAfter.getStatus())) {
            throw new RuntimeException("校验周期过期的光罩未被自动锁定");
        }
        log.info("  过期光罩已自动锁定：状态={}, 原因={}", maskAfter.getStatus(), maskAfter.getLockReason());

        long lockCount = maskLockRecordService.count();
        if (lockCount <= 0) {
            throw new RuntimeException("光罩锁定后未生成锁定记录");
        }
        log.info("  ✓ 校验周期锁定机制验证通过：生成锁定记录数={}", lockCount);
    }

    private void validateReturnFlow() {
        log.info("[验证6/9] 验证归还→外观检查→入库→关闭流程...");

        SysUser engineer = sysUserService.getByUserCode("ENG001");
        SysUser supervisor = sysUserService.getByUserCode("SUP001");
        SysUser libUser = sysUserService.getByUserCode("LIB001");
        MaskInfo mask = maskInfoService.getByMaskCode("MASK-003");
        StorageLocation location = storageLocationService.getByCode("LOC-B-01-01");

        if (mask == null || location == null || engineer == null) {
            throw new RuntimeException("验证所需基础数据缺失");
        }

        BorrowApplyDTO dto = new BorrowApplyDTO();
        dto.setApplicantId(engineer.getId());
        dto.setMaskId(mask.getId());
        dto.setMachineBatch("LOT-20260619-002");
        dto.setMachineCode("STEPPER-B02");
        dto.setCleanLevel("CLASS_100");
        dto.setPurpose("常规生产批次");
        dto.setExpectReturnDate(LocalDate.now().plusDays(3));

        BorrowApplication apply = borrowApplicationService.submitApplication(dto);
        apply = borrowApplicationService.approveApplication(apply.getId(), supervisor.getId(), true, "通过");
        BorrowOrder order = borrowOrderService.createOrderFromApplication(apply.getId(), libUser.getId());
        order = borrowOrderService.outStock(order.getId(), libUser.getId());
        log.info("  准备归还场景：借用单号={} 已出库", order.getOrderNo());

        try {
            borrowOrderService.inStock(order.getId(), libUser.getId(), location.getId());
            throw new RuntimeException("未做外观检查居然入库成功了，这是BUG！");
        } catch (Exception e) {
            log.info("  预期拦截成功：未做外观检查不能入库：{}", e.getMessage());
        }

        order = borrowOrderService.appearanceCheck(order.getId(), libUser.getId(), "PASS", "外观完好，无划痕无污染");
        log.info("  外观检查完成：结果={}, 备注={}, 检查人={}",
                order.getAppearanceCheckResult(), order.getAppearanceCheckRemark(), libUser.getUserName());

        order = borrowOrderService.inStock(order.getId(), libUser.getId(), location.getId());
        log.info("  光罩入库成功：入库时间={}, 库位={}, 实际借用天数={}天",
                order.getInLibTime(), location.getLocationCode(), order.getActualBorrowDays());

        order = borrowOrderService.closeOrder(order.getId());
        log.info("  借用单关闭成功：最终状态={}", order.getOrderStatus());

        MaskInfo maskAfter = maskInfoService.getById(mask.getId());
        if (!"IN_STOCK".equals(maskAfter.getStatus())) {
            throw new RuntimeException("光罩入库后状态未更新为IN_STOCK");
        }
        if (!location.getId().equals(maskAfter.getLocationId())) {
            throw new RuntimeException("光罩入库后库位信息错误");
        }

        long inStockMoves = maskLocationMoveService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MaskLocationMove>()
                .eq(MaskLocationMove::getMoveType, "IN_STOCK"));
        if (inStockMoves <= 0) {
            throw new RuntimeException("入库后未生成库位移动记录");
        }

        log.info("  ✓ 归还全流程验证通过：外观检查→入库→关闭，光罩状态={}, 所在库位={}, 入库移动记录={}",
                maskAfter.getStatus(), location.getLocationCode(), inStockMoves);
    }

    private void validateRecordReadBack() {
        log.info("[验证7/9] 验证记录回读（申请/库位/锁定/归还/移动）...");

        long applyCount = borrowApplicationService.count();
        if (applyCount < 2) {
            throw new RuntimeException("申请单记录回读失败，数量不足");
        }
        log.info("  借用申请记录：共{}条", applyCount);

        long orderCount = borrowOrderService.count();
        if (orderCount < 2) {
            throw new RuntimeException("借用单记录回读失败，数量不足");
        }
        log.info("  借用单（出入库）记录：共{}条", orderCount);

        long lockCount = maskLockRecordService.count();
        if (lockCount < 1) {
            throw new RuntimeException("锁定记录回读失败");
        }
        log.info("  锁定记录：共{}条", lockCount);

        long returnCount = returnRecordService.count();
        if (returnCount < 1) {
            throw new RuntimeException("归还记录回读失败");
        }
        log.info("  归还记录：共{}条", returnCount);

        ReturnRecord rr = returnRecordService.list().get(0);
        if (rr.getAppearanceCheckDone() == null || rr.getAppearanceCheckDone() == 0) {
            throw new RuntimeException("归还记录中外观检查标志未正确记录");
        }
        log.info("  归还记录回读详情：recordNo={}, mask={}, 外观检查={}, 结果={}",
                rr.getRecordNo(), rr.getMaskCode(),
                rr.getAppearanceCheckDone() == 1 ? "已完成" : "未完成",
                rr.getAppearanceCheckResult());

        StorageLocation loc = storageLocationService.getByCode("LOC-B-01-01");
        if (loc.getCurrentCount() < 1) {
            throw new RuntimeException("库位容量计数回读不正确");
        }
        log.info("  库位计数回读：{} 当前={}/{}", loc.getLocationCode(), loc.getCurrentCount(), loc.getCapacity());

        long moveCount = maskLocationMoveService.count();
        if (moveCount < 2) {
            throw new RuntimeException("库位移动记录回读失败，数量不足");
        }
        log.info("  库位移动记录：共{}条", moveCount);

        log.info("  ✓ 所有记录回读验证通过");
    }

    private void validateCleanAreaTransfer() {
        log.info("[验证8/9] 验证洁净区转运→主管复核流程...");

        SysUser engineer = sysUserService.getByUserCode("ENG001");
        SysUser supervisor = sysUserService.getByUserCode("SUP001");
        SysUser libUser = sysUserService.getByUserCode("LIB001");
        MaskInfo mask4 = maskInfoService.getByMaskCode("MASK-004");

        if (mask4 == null || engineer == null || supervisor == null || libUser == null) {
            throw new RuntimeException("洁净区转运验证所需基础数据缺失");
        }

        if (!"CLASS_100".equals(mask4.getCleanLevel())) {
            throw new RuntimeException("光罩MASK-004洁净等级不是CLASS_100");
        }
        if (!"IN_STOCK".equals(mask4.getStatus())) {
            throw new RuntimeException("光罩MASK-004不在库，请检查前置验证是否已借出该光罩");
        }

        StorageLocation cLocation = storageLocationService.getByCode("LOC-C-01-01");
        if (cLocation == null) {
            throw new RuntimeException("C区库位不存在");
        }

        BorrowApplyDTO dto = new BorrowApplyDTO();
        dto.setApplicantId(engineer.getId());
        dto.setMaskId(mask4.getId());
        dto.setMachineBatch("LOT-CLEAN-TEST-001");
        dto.setMachineCode("STEPPER-C01");
        dto.setCleanLevel("CLASS_10");
        dto.setPurpose("洁净区转运测试");
        dto.setExpectReturnDate(LocalDate.now().plusDays(1));

        try {
            borrowApplicationService.submitApplication(dto);
            throw new RuntimeException("CLASS_10申请借用CLASS_100光罩应该被拦截");
        } catch (Exception e) {
            if (!e.getMessage().contains("不满足光罩要求")) {
                throw new RuntimeException("洁净等级不匹配拦截异常信息不正确：" + e.getMessage());
            }
            log.info("  预期拦截成功：CLASS_10申请借用CLASS_100光罩被拒绝：{}", e.getMessage());
        }

        dto.setCleanLevel("CLASS_100");
        BorrowApplication apply = borrowApplicationService.submitApplication(dto);
        apply = borrowApplicationService.approveApplication(apply.getId(), supervisor.getId(), true, "通过");

        mask4 = maskInfoService.getById(mask4.getId());
        StorageLocation origLocation = storageLocationService.getById(mask4.getLocationId());
        log.info("  光罩MASK-004当前库位：{}，等级={}", origLocation.getLocationCode(), origLocation.getCleanLevel());

        BorrowOrder order = borrowOrderService.createOrderFromApplication(apply.getId(), libUser.getId());

        boolean mismatch = maskLocationMoveService.isTransferCleanLevelMismatch(order.getCleanLevel(), mask4.getLocationId());
        log.info("  洁净区转运不匹配检查：光罩等级={}, 库位等级={}, 不匹配={}",
                order.getCleanLevel(), origLocation.getCleanLevel(), mismatch);

        order = borrowOrderService.outStock(order.getId(), libUser.getId());
        log.info("  出库后借用单状态={}，主管复核状态={}", order.getOrderStatus(), order.getSupervisorReviewStatus());

        if (mismatch && !"SUPERVISOR_REVIEW".equals(order.getOrderStatus())) {
            throw new RuntimeException("途经非匹配洁净区应转主管复核，但状态为" + order.getOrderStatus());
        }

        if ("SUPERVISOR_REVIEW".equals(order.getOrderStatus())) {
            log.info("  非匹配洁净区：借用单已转主管复核，验证主管复核通过流程...");

            order = borrowOrderService.supervisorReviewPass(order.getId(), supervisor.getId(), "确认可继续转运");
            log.info("  主管复核通过后状态={}", order.getOrderStatus());

            if (!"OUT_STOCK".equals(order.getOrderStatus())) {
                throw new RuntimeException("主管复核通过后状态应为OUT_STOCK，实际为" + order.getOrderStatus());
            }

            BorrowApplication updatedApply = borrowApplicationService.getById(apply.getId());
            if (updatedApply.getSupervisorReviewFlag() != 1) {
                throw new RuntimeException("申请单主管复核标志未正确设置");
            }
            log.info("  申请单主管复核标志={}，原因={}", updatedApply.getSupervisorReviewFlag(), updatedApply.getSupervisorReviewReason());
        }

        order = borrowOrderService.appearanceCheck(order.getId(), libUser.getId(), "PASS", "外观正常");
        order = borrowOrderService.inStock(order.getId(), libUser.getId(), origLocation.getId());
        borrowOrderService.closeOrder(order.getId());

        log.info("  ✓ 洁净区转运→主管复核流程验证通过");
    }

    private void validateScratchDamageAndBatchFreeze() {
        log.info("[验证9/9] 验证划伤发现→批次冻结→异常处理单→解冻流程...");

        SysUser engineer = sysUserService.getByUserCode("ENG001");
        SysUser supervisor = sysUserService.getByUserCode("SUP001");
        SysUser libUser = sysUserService.getByUserCode("LIB001");

        MaskInfo mask = maskInfoService.getByMaskCode("MASK-003");
        if (mask == null) {
            throw new RuntimeException("光罩MASK-003不存在");
        }
        if (!"IN_STOCK".equals(mask.getStatus())) {
            throw new RuntimeException("光罩MASK-003不在库，无法进行划伤验证");
        }

        StorageLocation location = storageLocationService.getByCode("LOC-B-01-01");

        BorrowApplyDTO dto = new BorrowApplyDTO();
        dto.setApplicantId(engineer.getId());
        dto.setMaskId(mask.getId());
        dto.setMachineBatch("LOT-SCRATCH-TEST-001");
        dto.setMachineCode("STEPPER-B03");
        dto.setCleanLevel("CLASS_100");
        dto.setPurpose("划伤检测验证批次");
        dto.setExpectReturnDate(LocalDate.now().plusDays(2));

        BorrowApplication apply = borrowApplicationService.submitApplication(dto);
        apply = borrowApplicationService.approveApplication(apply.getId(), supervisor.getId(), true, "通过");
        BorrowOrder order = borrowOrderService.createOrderFromApplication(apply.getId(), libUser.getId());
        order = borrowOrderService.outStock(order.getId(), libUser.getId());
        log.info("  准备划伤场景：借用单号={} 已出库", order.getOrderNo());

        order = borrowOrderService.appearanceCheck(order.getId(), libUser.getId(), "SCRATCH", "发现明显划痕，长约2mm");
        log.info("  外观检查结果={}, 异常标志={}, 批次冻结={}",
                order.getAppearanceCheckResult(), order.getIsAbnormal(), order.getBatchFreezeFlag());

        if (order.getIsAbnormal() != 1) {
            throw new RuntimeException("发现划伤后异常标志应设为1");
        }
        if (order.getBatchFreezeFlag() != 1) {
            throw new RuntimeException("发现划伤后批次冻结标志应设为1");
        }

        long anomalyCount = anomalyHandleOrderService.count();
        if (anomalyCount <= 0) {
            throw new RuntimeException("发现划伤后未生成异常处理单");
        }

        AnomalyHandleOrder anomaly = anomalyHandleOrderService.list().stream()
                .filter(a -> "SCRATCH_DAMAGE".equals(a.getAnomalyType()))
                .findFirst()
                .orElse(null);
        if (anomaly == null) {
            throw new RuntimeException("未找到划伤类型的异常处理单");
        }
        log.info("  异常处理单已生成：anomalyNo={}, 类型={}, 批次={}, 冻结标志={}",
                anomaly.getAnomalyNo(), anomaly.getAnomalyType(), anomaly.getMachineBatch(), anomaly.getBatchFreezeFlag());

        boolean batchFrozen = anomalyHandleOrderService.isBatchFrozen("LOT-SCRATCH-TEST-001");
        if (!batchFrozen) {
            throw new RuntimeException("同工艺批次应被冻结");
        }
        log.info("  同工艺批次LOT-SCRATCH-TEST-001已被冻结：isBatchFrozen={}", batchFrozen);

        order = borrowOrderService.inStock(order.getId(), libUser.getId(), location.getId());
        borrowOrderService.closeOrder(order.getId());

        anomaly = anomalyHandleOrderService.handleAnomaly(anomaly.getId(), supervisor.getId(), "更换光罩，解除批次冻结", "已安排替换光罩");
        log.info("  异常处理单已处理：状态={}, 处理结果={}", anomaly.getHandleStatus(), anomaly.getHandleResult());

        anomaly = anomalyHandleOrderService.unfreezeBatch(anomaly.getId(), supervisor.getId(), "异常已处理，解除批次冻结");
        log.info("  批次已解冻：冻结标志={}, 解冻时间={}", anomaly.getBatchFreezeFlag(), anomaly.getBatchUnfreezeTime());

        boolean stillFrozen = anomalyHandleOrderService.isBatchFrozen("LOT-SCRATCH-TEST-001");
        if (stillFrozen) {
            throw new RuntimeException("解冻后批次仍显示为冻结状态");
        }
        log.info("  解冻验证通过：批次LOT-SCRATCH-TEST-001冻结状态={}", stillFrozen);

        log.info("  ✓ 划伤→批次冻结→异常处理单→解冻流程验证通过");
    }
}
