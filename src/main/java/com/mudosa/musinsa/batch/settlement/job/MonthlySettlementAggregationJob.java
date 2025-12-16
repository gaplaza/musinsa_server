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
import java.time.temporal.TemporalAdjusters;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "settlement.batch.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MonthlySettlementAggregationJob {

    private final SettlementAggregationService settlementService;

    @Bean
    public Job monthlySettlementJob(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new JobBuilder("monthlySettlementJob", jobRepository)
            .start(buildStep(jobRepository, transactionManager))
            .build();
    }

    private Step buildStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("monthlySettlementStep", jobRepository)
            .tasklet(monthlyTasklet(null), transactionManager)
            .build();
    }

    @Bean
    @StepScope
    public Tasklet monthlyTasklet(@Value("#{jobParameters['targetDate']}") String targetDateStr) {
        return (contribution, chunkContext) -> {
            LocalDate startDate;
            LocalDate endDate;

            if (targetDateStr != null && !targetDateStr.isEmpty()) {
                LocalDate targetDate = LocalDate.parse(targetDateStr);
                startDate = targetDate.with(TemporalAdjusters.firstDayOfMonth());
                endDate = targetDate.with(TemporalAdjusters.lastDayOfMonth());
            } else {
                startDate = DateRangeCalculator.getLastMonthStart();
                endDate = DateRangeCalculator.getLastMonthEnd();
            }

            settlementService.aggregateToMonthly(startDate, endDate);
            return RepeatStatus.FINISHED;
        };
    }
}