package com.globallogic.bloodbridge.matching.service;

import com.globallogic.bloodbridge.matching.event.RequestCreatedEvent;
import com.globallogic.bloodbridge.matching.exception.NoDonorsAvailableException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(RequestCreatedListener.class);

    private final MatchingService matchingService;

    @KafkaListener(
            topics = "request-created-events",
            groupId = "matching-service",
            properties = {
                    "spring.json.value.default.type=com.globallogic.bloodbridge.matching.event.RequestCreatedEvent",
                    "spring.json.use.type.headers=false"
            })
    public void onRequestCreated(RequestCreatedEvent event) {
        log.info("Auto-matching donors for requestId={}", event.getRequestId());
        try {
            matchingService.matchDonors(event.getRequestId());
        } catch (NoDonorsAvailableException ex) {
            log.info("No donors available yet for requestId={}", event.getRequestId());
        } catch (Exception ex) {
            log.warn("Auto-match failed for requestId={}: {}", event.getRequestId(), ex.getMessage());
        }
    }
}
