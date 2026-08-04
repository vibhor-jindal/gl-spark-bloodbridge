package com.globallogic.bloodbridge.notification.service;

import com.globallogic.bloodbridge.notification.client.AuthServiceClient;
import com.globallogic.bloodbridge.notification.dto.UserDto;
import com.globallogic.bloodbridge.notification.event.DeliveryOtpEvent;
import com.globallogic.bloodbridge.notification.event.DonorMatchedEvent;
import com.globallogic.bloodbridge.notification.event.RequestConfirmedEvent;
import com.globallogic.bloodbridge.notification.event.RequestCreatedEvent;
import com.globallogic.bloodbridge.notification.model.RecipientType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationDispatchService dispatchService;
    private final AuthServiceClient authServiceClient;

    @KafkaListener(
            topics = "request-created-events",
            groupId = "notification-service",
            properties = {
                    "spring.json.value.default.type=com.globallogic.bloodbridge.notification.event.RequestCreatedEvent",
                    "spring.json.use.type.headers=false"
            })
    public void onRequestCreated(RequestCreatedEvent event) {
        log.info("Consumed RequestCreatedEvent requestId={} city={}", event.getRequestId(), event.getCity());

        String subject = "New " + event.getUrgency() + " blood request in " + event.getCity();
        String message = "Emergency request #" + event.getRequestId() + " needs " + event.getUnitsNeeded()
                + " unit(s) of " + event.getBloodGroup() + " for " + event.getPatientName()
                + " at " + event.getHospitalName() + ", " + event.getCity()
                + ". Open BloodBridge to confirm availability or reserve stock.";

        try {
            List<UserDto> banks = authServiceClient.listUsers("BLOOD_BANK");
            for (UserDto bank : banks) {
                dispatchService.dispatch(bank.getUserId(), RecipientType.BLOOD_BANK, event.getRequestId(),
                        bank.getEmail(), null, subject, message);
            }
            log.info("Alerted {} blood bank(s) for request {}", banks.size(), event.getRequestId());
        } catch (Exception ex) {
            log.warn("Could not alert blood banks for request {}: {}", event.getRequestId(), ex.getMessage());
        }
    }

    @KafkaListener(
            topics = "donor-matched-events",
            groupId = "notification-service",
            properties = {
                    "spring.json.value.default.type=com.globallogic.bloodbridge.notification.event.DonorMatchedEvent",
                    "spring.json.use.type.headers=false"
            })
    public void onDonorMatched(DonorMatchedEvent event) {
        log.info("Consumed DonorMatchedEvent requestId={} donorId={} donorUserId={}",
                event.getRequestId(), event.getDonorId(), event.getDonorUserId());

        String subject = "You're matched for a " + event.getUrgency() + " blood request";
        String message = "Hi " + event.getDonorName() + ", you have been matched to a "
                + event.getBloodGroup() + " blood request for " + event.getUnitsNeeded()
                + " unit(s) at " + event.getHospitalName()
                + ". Please open BloodBridge to accept or decline.";

        Long recipientId = event.getDonorUserId() != null ? event.getDonorUserId() : event.getDonorId();
        // Prefer event email, then auth user by donorUserId (syncs blank/stale donor-profile emails).
        String email = resolveEmail(event.getDonorEmail(), event.getDonorUserId());
        if (email == null || email.isBlank()) {
            log.warn("No email resolved for matched donor donorId={} donorUserId={} — dispatch will fail visibly",
                    event.getDonorId(), event.getDonorUserId());
        }
        dispatchService.dispatch(recipientId, RecipientType.DONOR, event.getRequestId(),
                email, event.getDonorPhone(), subject, message);
    }

    /**
     * Prefer a valid address from the event/donor profile; otherwise look up auth by userId.
     * Auth is also used when the preferred value is blank or does not look like an email.
     */
    private String resolveEmail(String preferred, Long userId) {
        if (NotificationDispatchService.looksLikeEmail(preferred)) {
            return preferred.trim();
        }
        if (userId == null) {
            return preferred != null && !preferred.isBlank() ? preferred.trim() : null;
        }
        try {
            UserDto user = authServiceClient.getUser(userId);
            if (user != null && NotificationDispatchService.looksLikeEmail(user.getEmail())) {
                if (preferred == null || preferred.isBlank()) {
                    log.info("Resolved blank donor email from auth for userId={}", userId);
                } else {
                    log.info("Preferred email invalid for userId={}; using auth email", userId);
                }
                return user.getEmail().trim();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve email for userId={}: {}", userId, ex.getMessage());
        }
        return preferred != null && !preferred.isBlank() ? preferred.trim() : null;
    }

    @KafkaListener(
            topics = "request-confirmed-events",
            groupId = "notification-service",
            properties = {
                    "spring.json.value.default.type=com.globallogic.bloodbridge.notification.event.RequestConfirmedEvent",
                    "spring.json.use.type.headers=false"
            })
    public void onRequestConfirmed(RequestConfirmedEvent event) {
        log.info("Consumed RequestConfirmedEvent requestId={} donorId={}", event.getRequestId(), event.getDonorId());

        String email = resolveEmail(null, event.getRequesterId());
        String subject = "A donor has confirmed for your blood request";
        String message = "Good news! " + event.getDonorName()
                + " has confirmed for your blood request #" + event.getRequestId()
                + ". Delivery will start shortly — watch for your OTP email.";

        dispatchService.dispatch(event.getRequesterId(), RecipientType.REQUESTER, event.getRequestId(),
                email, null, subject, message);
    }

    @KafkaListener(
            topics = "delivery-otp-events",
            groupId = "notification-service",
            properties = {
                    "spring.json.value.default.type=com.globallogic.bloodbridge.notification.event.DeliveryOtpEvent",
                    "spring.json.use.type.headers=false"
            })
    public void onDeliveryOtp(DeliveryOtpEvent event) {
        log.info("Consumed DeliveryOtpEvent requestId={} requesterId={}", event.getRequestId(), event.getRequesterId());

        // OTP must go to the requester (not the blood bank).
        String email = resolveEmail(null, event.getRequesterId());
        if (email == null || email.isBlank()) {
            log.warn("Could not resolve requester email for OTP requestId={} requesterId={}",
                    event.getRequestId(), event.getRequesterId());
        }

        String subject = "BloodBridge delivery OTP — request #" + event.getRequestId();
        String message = "Your blood is out for delivery.\n\n"
                + "Patient: " + event.getPatientName() + "\n"
                + "Blood: " + event.getBloodGroup() + " × " + event.getUnitsNeeded() + " unit(s)\n"
                + "Hospital: " + event.getHospitalName() + "\n\n"
                + "Your confirmation OTP is: " + event.getOtp() + "\n"
                + "Enter this OTP in BloodBridge once you receive the blood. Valid for 30 minutes.";

        dispatchService.dispatch(event.getRequesterId(), RecipientType.REQUESTER, event.getRequestId(),
                email, null, subject, message);
    }
}
