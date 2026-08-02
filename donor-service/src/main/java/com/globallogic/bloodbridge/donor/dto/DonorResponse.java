package com.globallogic.bloodbridge.donor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonorResponse {
    private Long donorId;
    private Long userId;
    private String name;
    private String bloodGroup;
    private String phone;
    private String email;
    private String city;
    private Double latitude;
    private Double longitude;
    private Boolean isAvailable;
    private LocalDate lastDonationDate;
    private boolean eligibleToDonate;
}
