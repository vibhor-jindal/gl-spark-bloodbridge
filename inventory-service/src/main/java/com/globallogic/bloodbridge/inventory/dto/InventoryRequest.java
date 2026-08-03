package com.globallogic.bloodbridge.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequest {

    @NotBlank(message = "Blood bank name is required")
    private String bloodBankName;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Blood group is required")
    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Blood group must be one of A+, A-, B+, B-, AB+, AB-, O+, O-")
    private String bloodGroup;

    @NotNull(message = "Units available is required")
    @Min(value = 0, message = "Units available cannot be negative")
    private Integer unitsAvailable;

    @NotNull(message = "Collected date is required")
    private LocalDate collectedDate;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;
}
