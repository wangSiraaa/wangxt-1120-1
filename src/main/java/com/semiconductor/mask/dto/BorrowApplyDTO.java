package com.semiconductor.mask.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class BorrowApplyDTO {

    @NotNull(message = "申请人ID不能为空")
    private Long applicantId;

    @NotNull(message = "光罩ID不能为空")
    private Long maskId;

    @NotBlank(message = "机台批次不能为空")
    private String machineBatch;

    private String purpose;

    private LocalDate expectReturnDate;
}
