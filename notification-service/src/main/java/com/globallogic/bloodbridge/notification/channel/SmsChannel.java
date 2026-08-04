package com.globallogic.bloodbridge.notification.channel;

import com.globallogic.bloodbridge.notification.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMS channel kept in codebase but not registered as a Spring bean —
 * product flow is email + in-app alerts only.
 */
public class SmsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsChannel.class);

    @Override
    public Channel getType() {
        return Channel.SMS;
    }

    @Override
    public boolean send(String recipient, String subject, String message) {
        if (recipient == null || recipient.isBlank()) {
            return false;
        }
        log.info("[SMS-DISABLED] Would have sent to {} - {}", recipient, subject);
        return false;
    }
}
