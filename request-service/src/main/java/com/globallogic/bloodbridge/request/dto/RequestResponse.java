package com.globallogic.bloodbridge.request.dto;

import com.globallogic.bloodbridge.request.model.FulfillmentSource;
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
    private FulfillmentSource fulfillmentSource;
    private Long bloodBankUserId;
    private Long reservedBatchId;
    private boolean otpPending;
    /** Present while out for delivery; null after fulfillment or before delivery starts. */
    private LocalDateTime otpExpiresAt;
    /** True when status is OUT_FOR_DELIVERY and OTP expiry is missing or in the past. */
    private boolean otpExpired;
    private LocalDateTime createdAt;
}
