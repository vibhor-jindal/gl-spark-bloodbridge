package com.globallogic.bloodbridge.notification.dto;

import com.globallogic.bloodbridge.notification.model.Channel;
import com.globallogic.bloodbridge.notification.model.DeliveryStatus;
import com.globallogic.bloodbridge.notification.model.RecipientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogResponse {
    private Long notificationId;
    private Long requestId;
    private Long recipientId;
    private RecipientType recipientType;
    private Channel channel;
    private String subject;
    private DeliveryStatus status;
    private LocalDateTime deliveredAt;
}
