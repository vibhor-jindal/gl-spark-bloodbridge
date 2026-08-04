package com.globallogic.bloodbridge.request.dto;

import com.globallogic.bloodbridge.request.model.RequestStatus;
import com.globallogic.bloodbridge.request.model.Urgency;
import lombok.Data;

@Data
public class AdminRequestUpdate {
    private String patientName;
    private String bloodGroup;
    private Integer unitsNeeded;
    private String hospitalName;
    private String city;
    private Urgency urgency;
    private RequestStatus status;
    private Long confirmedDonorId;
}
