package com.gdg.fraud.service;

import com.google.cloud.aiplatform.v1.*;
import com.google.protobuf.Struct;
import com.gdg.fraud.dto.TransactionRequest;
import com.gdg.fraud.model.PredictionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;

@Slf4j
@Service
public class VertexAIService {

    @org.springframework.beans.factory.annotation.Value("${gcp.project-id}")
    private String projectId;

    @org.springframework.beans.factory.annotation.Value("${gcp.region}")
    private String region;

    @org.springframework.beans.factory.annotation.Value("${gcp.vertex-ai.endpoint-id}")
    private String endpointId;

    private PredictionServiceClient predictionClient;

    @PostConstruct
    public void init() throws Exception {
        PredictionServiceSettings settings = PredictionServiceSettings.newBuilder()
            .setEndpoint(region + "-aiplatform.googleapis.com:443")
            .build();
        predictionClient = PredictionServiceClient.create(settings);
        log.info("Vertex AI client initialized for endpoint: {}", endpointId);
    }

    @PreDestroy
    public void cleanup() {
        if (predictionClient != null) {
            predictionClient.close();
        }
    }

    public PredictionResult predict(TransactionRequest tx) {
        try {
            EndpointName endpoint = EndpointName.of(projectId, region, endpointId);

            // Build the feature map matching the training dataset columns
            Map<String, com.google.protobuf.Value> fields = new LinkedHashMap<>();
            fields.put("Time", toValue(tx.getTime()));
            fields.put("V1", toValue(tx.getV1()));
            fields.put("V2", toValue(tx.getV2()));
            fields.put("V3", toValue(tx.getV3()));
            fields.put("V4", toValue(tx.getV4()));
            fields.put("V5", toValue(tx.getV5()));
            fields.put("V6", toValue(tx.getV6()));
            fields.put("V7", toValue(tx.getV7()));
            fields.put("V8", toValue(tx.getV8()));
            fields.put("V9", toValue(tx.getV9()));
            fields.put("V10", toValue(tx.getV10()));
            fields.put("V11", toValue(tx.getV11()));
            fields.put("V12", toValue(tx.getV12()));
            fields.put("V13", toValue(tx.getV13()));
            fields.put("V14", toValue(tx.getV14()));
            fields.put("V15", toValue(tx.getV15()));
            fields.put("V16", toValue(tx.getV16()));
            fields.put("V17", toValue(tx.getV17()));
            fields.put("V18", toValue(tx.getV18()));
            fields.put("V19", toValue(tx.getV19()));
            fields.put("V20", toValue(tx.getV20()));
            fields.put("V21", toValue(tx.getV21()));
            fields.put("V22", toValue(tx.getV22()));
            fields.put("V23", toValue(tx.getV23()));
            fields.put("V24", toValue(tx.getV24()));
            fields.put("V25", toValue(tx.getV25()));
            fields.put("V26", toValue(tx.getV26()));
            fields.put("V27", toValue(tx.getV27()));
            fields.put("V28", toValue(tx.getV28()));
            fields.put("Amount", toValue(tx.getAmount()));

            com.google.protobuf.Value instance = com.google.protobuf.Value.newBuilder()
                .setStructValue(Struct.newBuilder().putAllFields(fields))
                .build();

            PredictResponse response = predictionClient.predict(
                endpoint, List.of(instance),
                com.google.protobuf.Value.getDefaultInstance()
            );

            return parsePrediction(response);

        } catch (Exception e) {
            log.error("Prediction failed for tx: {}", tx.getTransactionId(), e);
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }

    private PredictionResult parsePrediction(PredictResponse response) {
        var prediction = response.getPredictions(0).getStructValue();
        var classes = prediction.getFieldsOrThrow("classes")
            .getListValue().getValuesList();
        var scores = prediction.getFieldsOrThrow("scores")
            .getListValue().getValuesList();

        // Find index of fraud class ("1")
        int fraudIdx = -1;
        for (int i = 0; i < classes.size(); i++) {
            if ("1".equals(classes.get(i).getStringValue())) {
                fraudIdx = i;
                break;
            }
        }

        double fraudScore = fraudIdx >= 0
            ? scores.get(fraudIdx).getNumberValue() : 0.0;

        return PredictionResult.builder()
            .isFraud(fraudScore > 0.5)
            .fraudScore(fraudScore)
            .legitimateScore(1.0 - fraudScore)
            .build();
    }

    private com.google.protobuf.Value toValue(Double val) {
        return com.google.protobuf.Value.newBuilder()
            .setNumberValue(val != null ? val : 0.0)
            .build();
    }
}
