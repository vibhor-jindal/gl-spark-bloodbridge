package com.globallogic.bloodbridge.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OtpConfirmRequest {
    @NotBlank
    @Size(min = 4, max = 8)
    private String otp;
}
