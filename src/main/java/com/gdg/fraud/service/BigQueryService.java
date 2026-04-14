package com.gdg.fraud.service;

import com.google.cloud.bigquery.*;
import com.gdg.fraud.dto.FraudResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Service
public class BigQueryService {

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.bigquery.dataset}")
    private String dataset;

    @Value("${gcp.bigquery.table}")
    private String table;

    private BigQuery bigQuery;

    @PostConstruct
    public void init() {
        bigQuery = BigQueryOptions.getDefaultInstance().getService();
        log.info("BigQuery client initialized for {}.{}", dataset, table);
    }

    @Async
    public void logPrediction(FraudResponse response, Double amount, String features) {
        try {
            TableId tableId = TableId.of(projectId, dataset, table);

            Map<String, Object> row = new HashMap<>();
            row.put("transaction_id", response.getTransactionId());
            row.put("amount", amount);
            row.put("prediction", response.getPrediction());
            row.put("confidence", response.getConfidenceScore());
            row.put("timestamp", response.getTimestamp().toString());
            row.put("features", features);

            InsertAllRequest request = InsertAllRequest.newBuilder(tableId)
                .addRow(row)
                .build();

            InsertAllResponse bqResponse = bigQuery.insertAll(request);

            if (bqResponse.hasErrors()) {
                log.error("BigQuery errors for tx {}: {}",
                    response.getTransactionId(), bqResponse.getInsertErrors());
            } else {
                log.info("Logged to BigQuery: tx={}", response.getTransactionId());
            }
        } catch (Exception e) {
            log.error("BigQuery log failed for tx: {}",
                response.getTransactionId(), e);
        }
    }
}
