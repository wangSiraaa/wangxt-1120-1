package com.semiconductor.mask.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("anomaly_handle_order")
public class AnomalyHandleOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String anomalyNo;

    private Long borrowOrderId;

    private String orderNo;

    private Long maskId;

    private String maskCode;

    private String maskName;

    private String machineBatch;

    private String anomalyType;

    private String anomalyDesc;

    private Long reportUserId;

    private String reportUserName;

    private LocalDateTime reportTime;

    private String handleStatus;

    private Long handleUserId;

    private String handleUserName;

    private LocalDateTime handleTime;

    private String handleResult;

    private String handleRemark;

    private Integer batchFreezeFlag;

    private String batchFreezeReason;

    private LocalDateTime batchFreezeTime;

    private LocalDateTime batchUnfreezeTime;

    private Long batchUnfreezeUserId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
