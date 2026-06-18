package com.semiconductor.mask.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("return_record")
public class ReturnRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordNo;

    private Long borrowOrderId;

    private String orderNo;

    private Long maskId;

    private String maskCode;

    private String maskName;

    private Long returnUserId;

    private String returnUserName;

    private Long receiveUserId;

    private String receiveUserName;

    private LocalDateTime returnTime;

    private Long locationId;

    private Integer appearanceCheckDone;

    private String appearanceCheckResult;

    private String appearanceCheckRemark;

    private Long appearanceCheckUserId;

    private LocalDateTime appearanceCheckTime;

    private Integer hasDamage;

    private String damageRemark;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
