package com.globallogic.bloodbridge.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestConfirmedEvent {
    private Long requestId;
    private Long requesterId;
    private Long donorId;
    private String donorName;
    private String donorPhone;
}
