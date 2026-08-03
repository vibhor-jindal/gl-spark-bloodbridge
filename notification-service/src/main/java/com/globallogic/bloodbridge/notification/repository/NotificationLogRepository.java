package com.globallogic.bloodbridge.notification.repository;

import com.globallogic.bloodbridge.notification.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByRecipientIdOrderByDeliveredAtDesc(Long recipientId);
}
