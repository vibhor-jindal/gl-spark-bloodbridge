package com.globallogic.bloodbridge.inventory.dto;

import com.globallogic.bloodbridge.inventory.model.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long batchId;
    private String bloodBankName;
    private String city;
    private String bloodGroup;
    private Integer unitsAvailable;
    private LocalDate collectedDate;
    private LocalDate expiryDate;
    private BatchStatus status;
}
