package com.globallogic.bloodbridge.notification.service;

import com.globallogic.bloodbridge.notification.channel.NotificationChannel;
import com.globallogic.bloodbridge.notification.model.Channel;
import com.globallogic.bloodbridge.notification.model.DeliveryStatus;
import com.globallogic.bloodbridge.notification.model.NotificationLog;
import com.globallogic.bloodbridge.notification.model.RecipientType;
import com.globallogic.bloodbridge.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final List<NotificationChannel> channels;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationLog dispatch(Long recipientId, RecipientType recipientType, Long requestId,
                                     String email, String phone, String subject, String message) {

        List<NotificationChannel> ordered = orderedChannels();
        Map<Channel, String> contacts = Map.of(
                Channel.EMAIL, email == null ? "" : email,
                Channel.SMS, phone == null ? "" : phone,
                Channel.PUSH, phone == null ? "" : phone);

        NotificationChannel lastAttempted = ordered.get(ordered.size() - 1);

        for (NotificationChannel channel : ordered) {
            String contact = contacts.get(channel.getType());
            lastAttempted = channel;

            boolean sent = channel.send(contact, subject, message);
            if (!sent) {
                sent = channel.send(contact, subject, message);
            }

            if (sent) {
                return save(recipientId, recipientType, requestId, channel.getType(), subject, message, DeliveryStatus.SENT);
            }
        }

        log.warn("All channels failed for recipientId={} requestId={}", recipientId, requestId);
        return save(recipientId, recipientType, requestId, lastAttempted.getType(), subject, message, DeliveryStatus.FAILED);
    }

    private List<NotificationChannel> orderedChannels() {
        return channels.stream()
                .sorted((a, b) -> Integer.compare(priority(a.getType()), priority(b.getType())))
                .toList();
    }

    private int priority(Channel channel) {
        return switch (channel) {
            case EMAIL -> 0;
            case SMS -> 1;
            case PUSH -> 2;
        };
    }

    private NotificationLog save(Long recipientId, RecipientType recipientType, Long requestId,
                                  Channel channel, String subject, String message, DeliveryStatus status) {
        NotificationLog entry = NotificationLog.builder()
                .recipientId(recipientId)
                .recipientType(recipientType)
                .requestId(requestId)
                .channel(channel)
                .subject(subject)
                .message(message)
                .status(status)
                .build();
        return notificationLogRepository.save(entry);
    }
}
