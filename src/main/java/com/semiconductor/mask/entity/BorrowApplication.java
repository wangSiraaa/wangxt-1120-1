package com.semiconductor.mask.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("borrow_application")
public class BorrowApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String applyNo;

    private Long applicantId;

    private String applicantName;

    private Long maskId;

    private String maskCode;

    private String maskName;

    private String machineBatch;

    private String purpose;

    private LocalDate expectReturnDate;

    private String applyStatus;

    private Long approveUserId;

    private LocalDateTime approveTime;

    private String approveRemark;

    private Integer isAbnormal;

    private String abnormalRemark;

    private Long borrowOrderId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
