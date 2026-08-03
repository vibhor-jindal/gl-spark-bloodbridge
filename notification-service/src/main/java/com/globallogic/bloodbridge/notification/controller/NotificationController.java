package com.globallogic.bloodbridge.notification.controller;

import com.globallogic.bloodbridge.notification.dto.NotificationLogResponse;
import com.globallogic.bloodbridge.notification.model.NotificationLog;
import com.globallogic.bloodbridge.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;

    @GetMapping("/{recipientId}")
    public ResponseEntity<List<NotificationLogResponse>> getHistory(@PathVariable Long recipientId) {
        List<NotificationLogResponse> history = notificationLogRepository
                .findByRecipientIdOrderByDeliveredAtDesc(recipientId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(history);
    }

    private NotificationLogResponse toResponse(NotificationLog log) {
        return NotificationLogResponse.builder()
                .notificationId(log.getNotificationId())
                .requestId(log.getRequestId())
                .recipientId(log.getRecipientId())
                .recipientType(log.getRecipientType())
                .channel(log.getChannel())
                .subject(log.getSubject())
                .status(log.getStatus())
                .deliveredAt(log.getDeliveredAt())
                .build();
    }
}
