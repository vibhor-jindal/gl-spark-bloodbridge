package com.globallogic.bloodbridge.notification.channel;

import com.globallogic.bloodbridge.notification.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PushChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(PushChannel.class);

    @Override
    public Channel getType() {
        return Channel.PUSH;
    }

    @Override
    public boolean send(String recipient, String subject, String message) {
        if (recipient == null || recipient.isBlank()) {
            return false;
        }
        log.info("[PUSH-SIMULATED] To {} - {}", recipient, subject);
        return true;
    }
}
