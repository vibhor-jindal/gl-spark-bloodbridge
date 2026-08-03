package com.globallogic.bloodbridge.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlert {
    private String bloodGroup;
    private String city;
    private Integer availableUnits;
    private Integer threshold;
}
