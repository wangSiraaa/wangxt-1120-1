package com.semiconductor.mask.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("borrow_order")
public class BorrowOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long applyId;

    private Long maskId;

    private String maskCode;

    private String maskName;

    private Long borrowerId;

    private String borrowerName;

    private Long outLibUserId;

    private String outLibUserName;

    private LocalDateTime outLibTime;

    private Long outLocationId;

    private Long inLibUserId;

    private String inLibUserName;

    private LocalDateTime inLibTime;

    private Long inLocationId;

    private String orderStatus;

    private Integer isAbnormal;

    private String abnormalRemark;

    private Integer appearanceCheckDone;

    private String appearanceCheckResult;

    private String appearanceCheckRemark;

    private Long appearanceCheckUserId;

    private LocalDateTime appearanceCheckTime;

    private String machineBatch;

    private Integer actualBorrowDays;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
