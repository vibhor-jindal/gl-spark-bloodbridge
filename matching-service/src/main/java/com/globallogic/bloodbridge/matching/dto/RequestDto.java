package com.globallogic.bloodbridge.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDto {
    private Long requestId;
    private Long requesterId;
    private String bloodGroup;
    private String city;
    private Double latitude;
    private Double longitude;
    private String urgency;
    private String status;
}
