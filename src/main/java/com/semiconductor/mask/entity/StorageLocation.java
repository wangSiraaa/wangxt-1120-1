package com.semiconductor.mask.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("storage_location")
public class StorageLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String locationCode;

    private String locationName;

    private String cleanLevel;

    private String area;

    private Integer rowNum;

    private Integer colNum;

    private Integer layerNum;

    private Integer capacity;

    private Integer currentCount;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
