package com.globallogic.bloodbridge.request.service;

import com.globallogic.bloodbridge.request.config.KafkaTopicConfig;
import com.globallogic.bloodbridge.request.event.RequestCreatedEvent;
import com.globallogic.bloodbridge.request.event.RequestStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RequestEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRequestCreated(RequestCreatedEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.REQUEST_CREATED_TOPIC, String.valueOf(event.getRequestId()), event);
        log.info("Published RequestCreatedEvent requestId={}", event.getRequestId());
    }

    public void publishStatusChanged(RequestStatusChangedEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.REQUEST_STATUS_CHANGED_TOPIC, String.valueOf(event.getRequestId()), event);
        log.info("Published RequestStatusChangedEvent requestId={} status={}", event.getRequestId(), event.getStatus());
    }
}
