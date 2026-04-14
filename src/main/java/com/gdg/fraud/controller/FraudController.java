package com.gdg.fraud.controller;

import com.gdg.fraud.dto.*;
import com.gdg.fraud.model.PredictionResult;
import com.gdg.fraud.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FraudController {

    private final VertexAIService vertexAIService;
    private final PubSubPublisher pubSubPublisher;
    private final BigQueryService bigQueryService;

    @PostMapping("/verify")
    public ResponseEntity<FraudResponse> verifyTransaction(
            @Valid @RequestBody TransactionRequest request) {

        long startTime = System.currentTimeMillis();
        log.info("=== Verifying tx: {} (amount: {}) ===",
            request.getTransactionId(), request.getAmount());

        // Step 1: Get prediction from Vertex AI
        PredictionResult prediction = vertexAIService.predict(request);

        // Step 2: Build response with risk assessment
        String riskLevel = determineRiskLevel(prediction.getFraudScore());
        long processingTime = System.currentTimeMillis() - startTime;

        FraudResponse response = FraudResponse.builder()
            .transactionId(request.getTransactionId())
            .prediction(prediction.getIsFraud() ? "FRAUD" : "LEGITIMATE")
            .confidenceScore(prediction.getIsFraud()
                ? prediction.getFraudScore()
                : prediction.getLegitimateScore())
            .isFraud(prediction.getIsFraud())
            .riskLevel(riskLevel)
            .timestamp(Instant.now())
            .processingTimeMs(processingTime)
            .message(buildMessage(prediction, riskLevel))
            .build();

        // Step 3: Publish event to Pub/Sub (async -- non-blocking)
        pubSubPublisher.publishResult(response);

        // Step 4: Log to BigQuery for audit trail (async -- non-blocking)
        bigQueryService.logPrediction(response, request.getAmount(),
            Arrays.toString(request.toFeatureArray()));

        log.info("=== tx {} : {} | Risk: {} | Confidence: {} | {}ms ===",
            request.getTransactionId(), response.getPrediction(),
            riskLevel, response.getConfidenceScore(), processingTime);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify/batch")
    public ResponseEntity<Map<String, Object>> verifyBatch(
            @Valid @RequestBody List<TransactionRequest> requests) {

        List<FraudResponse> results = requests.stream()
            .map(req -> verifyTransaction(req).getBody())
            .toList();

        long fraudCount = results.stream()
            .filter(FraudResponse::getIsFraud).count();

        return ResponseEntity.ok(Map.of(
            "totalProcessed", results.size(),
            "fraudDetected", fraudCount,
            "legitimateCount", results.size() - fraudCount,
            "results", results
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "fraud-detection-api",
            "version", "1.0.0"
        ));
    }

    private String determineRiskLevel(double fraudScore) {
        if (fraudScore > 0.9) return "CRITICAL";
        if (fraudScore > 0.7) return "HIGH";
        if (fraudScore > 0.5) return "MEDIUM";
        return "LOW";
    }

    private String buildMessage(PredictionResult prediction, String riskLevel) {
        if (!prediction.getIsFraud()) {
            return "Transaction verified as legitimate. Low risk.";
        }
        return switch (riskLevel) {
            case "CRITICAL" -> "CRITICAL: Transaction blocked. Very high fraud probability.";
            case "HIGH" -> "HIGH RISK: Transaction flagged. Manual review recommended.";
            default -> "MEDIUM RISK: Suspicious transaction. Additional verification suggested.";
        };
    }
}
