package com.globallogic.bloodbridge.notification.service;

import com.globallogic.bloodbridge.notification.client.AuthServiceClient;
import com.globallogic.bloodbridge.notification.dto.UserDto;
import com.globallogic.bloodbridge.notification.event.DonorMatchedEvent;
import com.globallogic.bloodbridge.notification.event.RequestConfirmedEvent;
import com.globallogic.bloodbridge.notification.model.RecipientType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationDispatchService dispatchService;
    private final AuthServiceClient authServiceClient;

    @KafkaListener(topics = "donor-matched-events", groupId = "notification-service")
    public void onDonorMatched(DonorMatchedEvent event) {
        log.info("Consumed DonorMatchedEvent requestId={} donorId={}", event.getRequestId(), event.getDonorId());

        String subject = "You're matched for a " + event.getUrgency() + " blood request";
        String message = "Hi " + event.getDonorName() + ", you have been matched to a "
                + event.getBloodGroup() + " blood request for " + event.getUnitsNeeded()
                + " unit(s) at " + event.getHospitalName() + ". Please open the app to respond.";

        dispatchService.dispatch(event.getDonorId(), RecipientType.DONOR, event.getRequestId(),
                event.getDonorEmail(), event.getDonorPhone(), subject, message);
    }

    @KafkaListener(topics = "request-confirmed-events", groupId = "notification-service")
    public void onRequestConfirmed(RequestConfirmedEvent event) {
        log.info("Consumed RequestConfirmedEvent requestId={} donorId={}", event.getRequestId(), event.getDonorId());

        UserDto requester = authServiceClient.getUser(event.getRequesterId());

        String subject = "A donor has confirmed for your blood request";
        String message = "Good news! " + event.getDonorName() + " (" + event.getDonorPhone()
                + ") has confirmed for your blood request #" + event.getRequestId() + ".";

        dispatchService.dispatch(event.getRequesterId(), RecipientType.REQUESTER, event.getRequestId(),
                requester.getEmail(), null, subject, message);
    }
}
