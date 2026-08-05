package com.globallogic.bloodbridge.request.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String REQUEST_CREATED_TOPIC = "request-created-events";
    public static final String REQUEST_STATUS_CHANGED_TOPIC = "request-status-changed-events";
    public static final String DELIVERY_OTP_TOPIC = "delivery-otp-events";
    public static final String BLOOD_DELIVERED_TOPIC = "blood-delivered-events";

    @Bean
    public NewTopic requestCreatedTopic() {
        return TopicBuilder.name(REQUEST_CREATED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic requestStatusChangedTopic() {
        return TopicBuilder.name(REQUEST_STATUS_CHANGED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic deliveryOtpTopic() {
        return TopicBuilder.name(DELIVERY_OTP_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic bloodDeliveredTopic() {
        return TopicBuilder.name(BLOOD_DELIVERED_TOPIC).partitions(3).replicas(1).build();
    }
}
