package com.globallogic.bloodbridge.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveResponse {
    private String bloodGroup;
    private String city;
    private Integer unitsReserved;
    private Integer remainingAvailable;
}
