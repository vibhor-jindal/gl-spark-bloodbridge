package com.globallogic.bloodbridge.request.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonorDto {
    private Long donorId;
    private Long userId;
    private String name;
}
