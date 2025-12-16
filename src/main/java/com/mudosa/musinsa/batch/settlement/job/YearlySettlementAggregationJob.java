package com.mudosa.musinsa.batch.settlement.job;

import com.mudosa.musinsa.batch.settlement.service.SettlementAggregationService;
import com.mudosa.musinsa.batch.settlement.common.DateRangeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "settlement.batch.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class YearlySettlementAggregationJob {

    private final SettlementAggregationService settlementService;

    @Bean
    public Job yearlySettlementJob(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new JobBuilder("yearlySettlementJob", jobRepository)
            .start(buildStep(jobRepository, transactionManager))
            .build();
    }

    private Step buildStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("yearlySettlementStep", jobRepository)
            .tasklet(yearlyTasklet(null), transactionManager)
            .build();
    }

    @Bean
    @StepScope
    public Tasklet yearlyTasklet(@Value("#{jobParameters['targetDate']}") String targetDateStr) {
        return (contribution, chunkContext) -> {
            int targetYear;

            if (targetDateStr != null && !targetDateStr.isEmpty()) {
                LocalDate targetDate = LocalDate.parse(targetDateStr);
                targetYear = targetDate.getYear();
            } else {
                targetYear = DateRangeCalculator.getLastYear();
            }

            settlementService.aggregateToYearly(targetYear);
            return RepeatStatus.FINISHED;
        };
    }
}