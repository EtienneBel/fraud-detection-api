package com.gdg.fraud.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotNull(message = "Transaction ID is required")
    private String transactionId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotNull(message = "Time is required")
    private Double time;

    // PCA features V1-V28
    private Double v1, v2, v3, v4, v5, v6, v7;
    private Double v8, v9, v10, v11, v12, v13, v14;
    private Double v15, v16, v17, v18, v19, v20, v21;
    private Double v22, v23, v24, v25, v26, v27, v28;

    public Double[] toFeatureArray() {
        return new Double[]{
            time, v1, v2, v3, v4, v5, v6, v7,
            v8, v9, v10, v11, v12, v13, v14,
            v15, v16, v17, v18, v19, v20, v21,
            v22, v23, v24, v25, v26, v27, v28,
            amount
        };
    }
}
