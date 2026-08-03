package com.globallogic.bloodbridge.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitsUpdateRequest {

    @NotNull(message = "Units available is required")
    @Min(value = 0, message = "Units available cannot be negative")
    private Integer unitsAvailable;
}
