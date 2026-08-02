package com.globallogic.bloodbridge.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {
    private String status;
    private Long confirmedDonorId;
}
