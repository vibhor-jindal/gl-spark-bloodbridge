package com.globallogic.bloodbridge.analytics.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonorMatchedEvent {
    private Long requestId;
    private Long donorId;
    private String donorName;
    private String donorEmail;
    private String donorPhone;
    private String bloodGroup;
    private String hospitalName;
    private Integer unitsNeeded;
    private String urgency;
}
