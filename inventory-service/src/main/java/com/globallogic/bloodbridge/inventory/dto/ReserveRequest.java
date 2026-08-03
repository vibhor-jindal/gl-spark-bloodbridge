package com.globallogic.bloodbridge.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReserveRequest {

    @NotBlank(message = "Blood group is required")
    private String bloodGroup;

    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Units needed is required")
    @Min(value = 1, message = "Units needed must be greater than zero")
    private Integer unitsNeeded;
}
