-- 光罩借用管理系统数据库初始化脚本

CREATE DATABASE IF NOT EXISTS mask_manage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mask_manage;

-- 1. 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_code VARCHAR(64) NOT NULL UNIQUE COMMENT '用户编码',
    user_name VARCHAR(128) NOT NULL COMMENT '用户姓名',
    role_type VARCHAR(32) NOT NULL COMMENT '角色类型：ENGINEER-工艺工程师 MASK_LIB-光罩库 SUPERVISOR-工艺主管',
    clean_level VARCHAR(16) NOT NULL DEFAULT 'CLASS_100' COMMENT '用户洁净等级：CLASS_10 CLASS_100 CLASS_1000',
    phone VARCHAR(32) COMMENT '联系电话',
    email VARCHAR(128) COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    INDEX idx_user_code (user_code),
    INDEX idx_role_type (role_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 2. 光罩表
DROP TABLE IF EXISTS mask_info;
CREATE TABLE mask_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    mask_code VARCHAR(64) NOT NULL UNIQUE COMMENT '光罩编码',
    mask_name VARCHAR(128) NOT NULL COMMENT '光罩名称',
    mask_type VARCHAR(64) COMMENT '光罩类型',
    clean_level VARCHAR(16) NOT NULL DEFAULT 'CLASS_100' COMMENT '洁净等级：CLASS_10 CLASS_100 CLASS_1000',
    last_calibration_date DATE COMMENT '上次校验日期',
    calibration_cycle_days INT NOT NULL DEFAULT 90 COMMENT '校验周期（天）',
    next_calibration_date DATE COMMENT '下次校验日期',
    location_id BIGINT COMMENT '所在库位ID',
    status VARCHAR(32) NOT NULL DEFAULT 'IN_STOCK' COMMENT '状态：IN_STOCK-在库 BORROWED-借出 LOCKED-锁定 SCRAPPED-报废',
    lock_reason VARCHAR(256) COMMENT '锁定原因',
    lock_time DATETIME COMMENT '锁定时间',
    lock_user_id BIGINT COMMENT '锁定人ID',
    spec VARCHAR(512) COMMENT '规格说明',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    INDEX idx_mask_code (mask_code),
    INDEX idx_status (status),
    INDEX idx_location_id (location_id),
    INDEX idx_clean_level (clean_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='光罩信息表';

-- 3. 库位表
DROP TABLE IF EXISTS storage_location;
CREATE TABLE storage_location (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    location_code VARCHAR(64) NOT NULL UNIQUE COMMENT '库位编码',
    location_name VARCHAR(128) NOT NULL COMMENT '库位名称',
    clean_level VARCHAR(16) NOT NULL DEFAULT 'CLASS_100' COMMENT '洁净等级：CLASS_10 CLASS_100 CLASS_1000',
    area VARCHAR(64) COMMENT '区域',
    row_num INT COMMENT '排号',
    col_num INT COMMENT '列号',
    layer_num INT COMMENT '层号',
    capacity INT NOT NULL DEFAULT 1 COMMENT '容量',
    current_count INT NOT NULL DEFAULT 0 COMMENT '当前数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    INDEX idx_location_code (location_code),
    INDEX idx_clean_level (clean_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位表';

-- 4. 借用申请表
DROP TABLE IF EXISTS borrow_application;
CREATE TABLE borrow_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    apply_no VARCHAR(64) NOT NULL UNIQUE COMMENT '申请单号',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    applicant_name VARCHAR(128) COMMENT '申请人姓名',
    mask_id BIGINT NOT NULL COMMENT '光罩ID',
    mask_code VARCHAR(64) COMMENT '光罩编码',
    mask_name VARCHAR(128) COMMENT '光罩名称',
    machine_batch VARCHAR(128) NOT NULL COMMENT '机台批次',
    purpose VARCHAR(512) COMMENT '借用用途',
    expect_return_date DATE COMMENT '预计归还日期',
    apply_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审批 APPROVED-已批准 REJECTED-已驳回 CANCELLED-已取消 BORROWED-已借出 RETURNED-已归还',
    approve_user_id BIGINT COMMENT '审批人ID',
    approve_time DATETIME COMMENT '审批时间',
    approve_remark VARCHAR(512) COMMENT '审批意见',
    is_abnormal TINYINT NOT NULL DEFAULT 0 COMMENT '是否异常借用：0-否 1-是',
    abnormal_remark VARCHAR(512) COMMENT '异常说明',
    borrow_order_id BIGINT COMMENT '关联借用单ID',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    INDEX idx_apply_no (apply_no),
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_mask_id (mask_id),
    INDEX idx_apply_status (apply_status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借用申请表';

-- 5. 借用单（出入库记录）
DROP TABLE IF EXISTS borrow_order;
CREATE TABLE borrow_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '借用单号',
    apply_id BIGINT COMMENT '关联申请单ID',
    mask_id BIGINT NOT NULL COMMENT '光罩ID',
    mask_code VARCHAR(64) COMMENT '光罩编码',
    mask_name VARCHAR(128) COMMENT '光罩名称',
    borrower_id BIGINT NOT NULL COMMENT '借用人ID',
    borrower_name VARCHAR(128) COMMENT '借用人姓名',
    out_lib_user_id BIGINT COMMENT '出库操作人ID',
    out_lib_user_name VARCHAR(128) COMMENT '出库操作人姓名',
    out_lib_time DATETIME COMMENT '出库时间',
    out_location_id BIGINT COMMENT '出库库位ID',
    in_lib_user_id BIGINT COMMENT '入库操作人ID',
    in_lib_user_name VARCHAR(128) COMMENT '入库操作人姓名',
    in_lib_time DATETIME COMMENT '入库时间',
    in_location_id BIGINT COMMENT '入库库位ID',
    order_status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '单据状态：CREATED-已创建 OUT_STOCK-已出库 IN_STOCK-已入库 CLOSED-已关闭',
    is_abnormal TINYINT NOT NULL DEFAULT 0 COMMENT '是否异常：0-否 1-是',
    abnormal_remark VARCHAR(512) COMMENT '异常说明',
    appearance_check_done TINYINT NOT NULL DEFAULT 0 COMMENT '外观检查是否完成：0-否 1-是',
    appearance_check_result VARCHAR(32) COMMENT '外观检查结果：PASS-合格 FAIL-不合格',
    appearance_check_remark VARCHAR(512) COMMENT '外观检查备注',
    appearance_check_user_id BIGINT COMMENT '外观检查人ID',
    appearance_check_time DATETIME COMMENT '外观检查时间',
    machine_batch VARCHAR(128) COMMENT '机台批次',
    actual_borrow_days INT COMMENT '实际借用天数',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    INDEX idx_order_no (order_no),
    INDEX idx_mask_id (mask_id),
    INDEX idx_borrower_id (borrower_id),
    INDEX idx_order_status (order_status),
    INDEX idx_out_lib_time (out_lib_time),
    INDEX idx_in_lib_time (in_lib_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借用单（出入库记录）';

-- 6. 锁定记录表
DROP TABLE IF EXISTS mask_lock_record;
CREATE TABLE mask_lock_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    record_no VARCHAR(64) NOT NULL UNIQUE COMMENT '记录编号',
    mask_id BIGINT NOT NULL COMMENT '光罩ID',
    mask_code VARCHAR(64) COMMENT '光罩编码',
    mask_name VARCHAR(128) COMMENT '光罩名称',
    lock_type VARCHAR(32) NOT NULL COMMENT '锁定类型：CALIBRATION-校验周期 EXCEPTION-异常 OTHER-其他',
    lock_reason VARCHAR(512) NOT NULL COMMENT '锁定原因',
    lock_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '锁定时间',
    lock_user_id BIGINT COMMENT '锁定人ID',
    lock_user_name VARCHAR(128) COMMENT '锁定人姓名',
    unlock_time DATETIME COMMENT '解锁时间',
    unlock_user_id BIGINT COMMENT '解锁人ID',
    unlock_user_name VARCHAR(128) COMMENT '解锁人姓名',
    unlock_reason VARCHAR(512) COMMENT '解锁原因',
    lock_status VARCHAR(32) NOT NULL DEFAULT 'LOCKED' COMMENT '状态：LOCKED-已锁定 UNLOCKED-已解锁',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    INDEX idx_record_no (record_no),
    INDEX idx_mask_id (mask_id),
    INDEX idx_lock_status (lock_status),
    INDEX idx_lock_time (lock_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='光罩锁定记录表';

-- 7. 归还记录表
DROP TABLE IF EXISTS return_record;
CREATE TABLE return_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    record_no VARCHAR(64) NOT NULL UNIQUE COMMENT '记录编号',
    borrow_order_id BIGINT NOT NULL COMMENT '借用单ID',
    order_no VARCHAR(64) COMMENT '借用单号',
    mask_id BIGINT NOT NULL COMMENT '光罩ID',
    mask_code VARCHAR(64) COMMENT '光罩编码',
    mask_name VARCHAR(128) COMMENT '光罩名称',
    return_user_id BIGINT NOT NULL COMMENT '归还人ID',
    return_user_name VARCHAR(128) COMMENT '归还人姓名',
    receive_user_id BIGINT COMMENT '接收人ID',
    receive_user_name VARCHAR(128) COMMENT '接收人姓名',
    return_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归还时间',
    location_id BIGINT COMMENT '归还库位ID',
    appearance_check_done TINYINT NOT NULL DEFAULT 0 COMMENT '外观检查是否完成：0-否 1-是',
    appearance_check_result VARCHAR(32) COMMENT '外观检查结果：PASS-合格 FAIL-不合格',
    appearance_check_remark VARCHAR(512) COMMENT '外观检查备注',
    appearance_check_user_id BIGINT COMMENT '外观检查人ID',
    appearance_check_time DATETIME COMMENT '外观检查时间',
    has_damage TINYINT NOT NULL DEFAULT 0 COMMENT '是否有损坏：0-否 1-是',
    damage_remark VARCHAR(512) COMMENT '损坏说明',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    INDEX idx_record_no (record_no),
    INDEX idx_borrow_order_id (borrow_order_id),
    INDEX idx_mask_id (mask_id),
    INDEX idx_return_time (return_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归还记录表';

-- 初始化测试数据
INSERT INTO sys_user (user_code, user_name, role_type, clean_level, phone, email) VALUES
('ENG001', '张工程师', 'ENGINEER', 'CLASS_100', '13800138001', 'zhang@semiconductor.com'),
('ENG002', '李工程师', 'ENGINEER', 'CLASS_10', '13800138002', 'li@semiconductor.com'),
('LIB001', '王库管', 'MASK_LIB', 'CLASS_10', '13800138003', 'wang@semiconductor.com'),
('SUP001', '赵主管', 'SUPERVISOR', 'CLASS_10', '13800138004', 'zhao@semiconductor.com');

INSERT INTO storage_location (location_code, location_name, clean_level, area, row_num, col_num, layer_num, capacity, current_count) VALUES
('LOC-A-01-01', 'A区1排1列1层', 'CLASS_10', 'A区', 1, 1, 1, 5, 0),
('LOC-A-01-02', 'A区1排1列2层', 'CLASS_10', 'A区', 1, 1, 2, 5, 0),
('LOC-B-01-01', 'B区1排1列1层', 'CLASS_100', 'B区', 1, 1, 1, 10, 0),
('LOC-B-01-02', 'B区1排1列2层', 'CLASS_100', 'B区', 1, 1, 2, 10, 0),
('LOC-C-01-01', 'C区1排1列1层', 'CLASS_1000', 'C区', 1, 1, 1, 20, 0);

INSERT INTO mask_info (mask_code, mask_name, mask_type, clean_level, last_calibration_date, calibration_cycle_days, next_calibration_date, location_id, status, spec) VALUES
('MASK-001', '光罩A-1号', '铬版', 'CLASS_10', '2026-05-01', 90, '2026-07-30', 1, 'IN_STOCK', '6英寸 0.13um工艺'),
('MASK-002', '光罩A-2号', '铬版', 'CLASS_10', '2026-03-01', 90, '2026-05-30', 2, 'IN_STOCK', '6英寸 0.13um工艺'),
('MASK-003', '光罩B-1号', '乳胶版', 'CLASS_100', '2026-06-01', 90, '2026-08-30', 3, 'IN_STOCK', '5英寸 0.35um工艺'),
('MASK-004', '光罩B-2号', '乳胶版', 'CLASS_100', '2026-04-01', 90, '2026-06-30', 4, 'IN_STOCK', '5英寸 0.35um工艺'),
('MASK-005', '光罩C-1号', '普通版', 'CLASS_1000', '2026-01-01', 180, '2026-06-30', 5, 'IN_STOCK', '4英寸 0.5um工艺');
