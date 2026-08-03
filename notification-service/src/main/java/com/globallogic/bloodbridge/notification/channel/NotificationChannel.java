package com.globallogic.bloodbridge.notification.channel;

import com.globallogic.bloodbridge.notification.model.Channel;

public interface NotificationChannel {

    Channel getType();

    boolean send(String recipient, String subject, String message);
}
