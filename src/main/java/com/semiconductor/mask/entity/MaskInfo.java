package com.semiconductor.mask.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("mask_info")
public class MaskInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String maskCode;

    private String maskName;

    private String maskType;

    private String cleanLevel;

    private LocalDate lastCalibrationDate;

    private Integer calibrationCycleDays;

    private LocalDate nextCalibrationDate;

    private Long locationId;

    private String status;

    private String lockReason;

    private LocalDateTime lockTime;

    private Long lockUserId;

    private String spec;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
