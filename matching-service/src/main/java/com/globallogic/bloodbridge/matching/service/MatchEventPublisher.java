package com.globallogic.bloodbridge.matching.service;

import com.globallogic.bloodbridge.matching.config.KafkaTopicConfig;
import com.globallogic.bloodbridge.matching.event.DonorMatchedEvent;
import com.globallogic.bloodbridge.matching.event.RequestConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MatchEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishDonorMatched(DonorMatchedEvent event) {
        try {
            kafkaTemplate.send(KafkaTopicConfig.DONOR_MATCHED_TOPIC, String.valueOf(event.getRequestId()), event);
            log.info("Published DonorMatchedEvent requestId={} donorId={}", event.getRequestId(), event.getDonorId());
        } catch (Exception ex) {
            // Match row must still persist even if notification publish fails.
            log.error("Failed to publish DonorMatchedEvent requestId={} donorId={}: {}",
                    event.getRequestId(), event.getDonorId(), ex.getMessage());
        }
    }

    public void publishRequestConfirmed(RequestConfirmedEvent event) {
        try {
            kafkaTemplate.send(KafkaTopicConfig.REQUEST_CONFIRMED_TOPIC, String.valueOf(event.getRequestId()), event);
            log.info("Published RequestConfirmedEvent requestId={} donorId={}", event.getRequestId(), event.getDonorId());
        } catch (Exception ex) {
            log.error("Failed to publish RequestConfirmedEvent requestId={} donorId={}: {}",
                    event.getRequestId(), event.getDonorId(), ex.getMessage());
        }
    }
}
