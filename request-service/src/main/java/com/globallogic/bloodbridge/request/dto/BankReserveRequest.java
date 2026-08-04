package com.globallogic.bloodbridge.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BankReserveRequest {
    @NotNull
    private Long bloodBankUserId;

    private Long batchId;

    @NotBlank
    private String bloodBankName;
}
