package com.gdg.fraud.dto;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudResponse {
    private String transactionId;
    private String prediction;      // "FRAUD" or "LEGITIMATE"
    private Double confidenceScore;
    private Boolean isFraud;
    private String riskLevel;       // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    private Instant timestamp;
    private String message;
    private Long processingTimeMs;
}
