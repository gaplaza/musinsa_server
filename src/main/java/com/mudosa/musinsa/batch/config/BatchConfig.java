package com.mudosa.musinsa.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@ConditionalOnProperty(
    name = "settlement.batch.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class BatchConfig {
}