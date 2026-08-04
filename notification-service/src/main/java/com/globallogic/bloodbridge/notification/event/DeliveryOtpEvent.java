package com.globallogic.bloodbridge.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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
