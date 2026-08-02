package com.globallogic.bloodbridge.request.dto;

import com.globallogic.bloodbridge.request.model.RequestStatus;
import com.globallogic.bloodbridge.request.model.Urgency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestResponse {
    private Long requestId;
    private Long requesterId;
    private String patientName;
    private String bloodGroup;
    private Integer unitsNeeded;
    private String hospitalName;
    private String city;
    private Double latitude;
    private Double longitude;
    private Urgency urgency;
    private RequestStatus status;
    private Long confirmedDonorId;
    private LocalDateTime createdAt;
}
