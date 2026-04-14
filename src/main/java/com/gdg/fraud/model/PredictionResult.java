package com.gdg.fraud.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResult {
    private Boolean isFraud;
    private Double fraudScore;
    private Double legitimateScore;
}
