package com.globallogic.bloodbridge.request.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOtpEvent {
    private Long requestId;
    private Long requesterId;
    private String otp;
    private String patientName;
    private String hospitalName;
    private String bloodGroup;
    private Integer unitsNeeded;
}
