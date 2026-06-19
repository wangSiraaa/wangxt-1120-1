package com.semiconductor.mask.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("appearance_check_photo")
public class AppearanceCheckPhoto implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long borrowOrderId;

    private String orderNo;

    private Long returnRecordId;

    private Long maskId;

    private String maskCode;

    private String photoPath;

    private String photoName;

    private Long photoSize;

    private String checkResult;

    private String checkType;

    private Long checkUserId;

    private String checkUserName;

    private LocalDateTime checkTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
