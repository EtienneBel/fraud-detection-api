package com.gdg.fraud.service;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.gdg.fraud.dto.FraudResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PubSubPublisher {

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gcp.pubsub.topic}")
    private String topic;

    @Async
    public void publishResult(FraudResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            pubSubTemplate.publish(topic, json);
            log.info("Published to Pub/Sub: tx={}, prediction={}",
                response.getTransactionId(), response.getPrediction());
        } catch (Exception e) {
            log.error("Pub/Sub publish failed for tx: {}",
                response.getTransactionId(), e);
        }
    }
}
