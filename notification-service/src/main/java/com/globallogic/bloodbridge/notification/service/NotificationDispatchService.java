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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final List<NotificationChannel> channels;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationLog dispatch(Long recipientId, RecipientType recipientType, Long requestId,
                                     String email, String phone, String subject, String message) {

        String emailContact = email == null ? "" : email.trim();
        String phoneContact = phone == null ? "" : phone.trim();

        // Prefer real EMAIL. Never treat simulated PUSH as success when a real address is available —
        // PUSH always "succeeds" and previously masked SMTP / empty-email failures.
        if (looksLikeEmail(emailContact)) {
            Optional<NotificationChannel> emailChannel = channelOf(Channel.EMAIL);
            if (emailChannel.isEmpty()) {
                log.warn("No EMAIL channel bean for recipientId={} requestId={}", recipientId, requestId);
                return save(recipientId, recipientType, requestId, Channel.EMAIL, subject, message, DeliveryStatus.FAILED);
            }

            boolean sent = trySend(emailChannel.get(), emailContact, subject, message);
            if (sent) {
                return save(recipientId, recipientType, requestId, Channel.EMAIL, subject, message, DeliveryStatus.SENT);
            }

            log.warn("EMAIL delivery failed for recipientId={} requestId={} to={} — not falling back to PUSH",
                    recipientId, requestId, emailContact);
            return save(recipientId, recipientType, requestId, Channel.EMAIL, subject, message, DeliveryStatus.FAILED);
        }

        // No usable email: optional PUSH only (simulated in-app log), using phone as contact if present.
        String pushContact = !phoneContact.isBlank() ? phoneContact : emailContact;
        Optional<NotificationChannel> pushChannel = channelOf(Channel.PUSH);
        if (pushChannel.isPresent() && !pushContact.isBlank()) {
            boolean sent = trySend(pushChannel.get(), pushContact, subject, message);
            if (sent) {
                log.info("No email for recipientId={}; recorded PUSH fallback contact={}", recipientId, pushContact);
                return save(recipientId, recipientType, requestId, Channel.PUSH, subject, message, DeliveryStatus.SENT);
            }
        }

        log.warn("No deliverable channel for recipientId={} requestId={} (email blank/invalid)", recipientId, requestId);
        return save(recipientId, recipientType, requestId, Channel.EMAIL, subject, message, DeliveryStatus.FAILED);
    }

    static boolean looksLikeEmail(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int at = value.indexOf('@');
        return at > 0 && at < value.length() - 1 && value.indexOf('@', at + 1) < 0;
    }

    private boolean trySend(NotificationChannel channel, String contact, String subject, String message) {
        boolean sent = channel.send(contact, subject, message);
        if (!sent) {
            sent = channel.send(contact, subject, message);
        }
        return sent;
    }

    private Optional<NotificationChannel> channelOf(Channel type) {
        return channels.stream().filter(c -> c.getType() == type).findFirst();
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
