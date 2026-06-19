package com.semiconductor.mask.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class BorrowApplyDTO {

    @NotNull(message = "申请人ID不能为空")
    private Long applicantId;

    @NotNull(message = "光罩ID不能为空")
    private Long maskId;

    @NotBlank(message = "机台批次不能为空")
    private String machineBatch;

    @NotBlank(message = "机台编号不能为空")
    private String machineCode;

    @NotBlank(message = "申请洁净等级不能为空")
    private String cleanLevel;

    private String purpose;

    @NotNull(message = "预计归还日期不能为空")
    private LocalDate expectReturnDate;
}
