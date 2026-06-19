package com.semiconductor.mask.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("mask_location_move")
public class MaskLocationMove implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String moveNo;

    private Long maskId;

    private String maskCode;

    private String maskName;

    private Long borrowOrderId;

    private String orderNo;

    private Long fromLocationId;

    private String fromLocationCode;

    private String fromCleanLevel;

    private Long toLocationId;

    private String toLocationCode;

    private String toCleanLevel;

    private String moveType;

    private String moveReason;

    private String cleanLevelMismatch;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime moveTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
