package com.globallogic.bloodbridge.matching.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String DONOR_MATCHED_TOPIC = "donor-matched-events";
    public static final String REQUEST_CONFIRMED_TOPIC = "request-confirmed-events";

    @Bean
    public NewTopic donorMatchedTopic() {
        return TopicBuilder.name(DONOR_MATCHED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic requestConfirmedTopic() {
        return TopicBuilder.name(REQUEST_CONFIRMED_TOPIC).partitions(3).replicas(1).build();
    }
}
