package com.semiconductor.mask.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("mask_lock_record")
public class MaskLockRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordNo;

    private Long maskId;

    private String maskCode;

    private String maskName;

    private String lockType;

    private String lockReason;

    private LocalDateTime lockTime;

    private Long lockUserId;

    private String lockUserName;

    private LocalDateTime unlockTime;

    private Long unlockUserId;

    private String unlockUserName;

    private String unlockReason;

    private String lockStatus;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
