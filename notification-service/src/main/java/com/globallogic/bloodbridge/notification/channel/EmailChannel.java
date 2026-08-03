package com.globallogic.bloodbridge.notification.channel;

import com.globallogic.bloodbridge.notification.model.Channel;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailChannel(JavaMailSender mailSender,
                         @Value("${bloodbridge.notifications.from-address:noreply@bloodbridge.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public Channel getType() {
        return Channel.EMAIL;
    }

    @Override
    public boolean send(String recipient, String subject, String message) {
        if (recipient == null || recipient.isBlank()) {
            return false;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(message, true);
            mailSender.send(mimeMessage);
            log.info("[EMAIL] Sent \"{}\" to {}", subject, recipient);
            return true;
        } catch (MailException | jakarta.mail.MessagingException ex) {
            log.warn("[EMAIL] Failed to send \"{}\" to {} - {}", subject, recipient, ex.getMessage());
            return false;
        }
    }
}
