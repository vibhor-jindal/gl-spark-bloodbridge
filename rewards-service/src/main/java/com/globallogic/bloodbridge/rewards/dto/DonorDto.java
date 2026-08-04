package com.globallogic.bloodbridge.rewards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonorDto {
    private Long donorId;
    private String name;
    private String city;
}
