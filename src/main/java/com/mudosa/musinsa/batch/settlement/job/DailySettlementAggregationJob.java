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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "settlement.batch.enabled", havingValue = "true", matchIfMissing = true)
public class DailySettlementAggregationJob {

    private static final String JOB_NAME = "일일 정산 집계";

    private final SettlementAggregationService settlementService;

    @Bean
    public Job dailySettlementJob(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new JobBuilder("dailySettlementJob", jobRepository)
            .start(buildStep(jobRepository, transactionManager))
            .build();
    }

    private Step buildStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("dailySettlementStep", jobRepository)
            .tasklet(dailyTasklet(null), transactionManager)
            .build();
    }

    @Bean
    @StepScope
    public Tasklet dailyTasklet(@Value("#{jobParameters['targetDate']}") String targetDateStr) {
        return (contribution, chunkContext) -> {
            LocalDate targetDate;
            if (targetDateStr != null && !targetDateStr.isEmpty()) {
                targetDate = LocalDate.parse(targetDateStr);
            } else {
                targetDate = DateRangeCalculator.getYesterday();
            }

            log.info("{} 시작 - targetDate: {}", JOB_NAME, targetDate);
            settlementService.aggregateToDaily(targetDate, targetDate);
            log.info("{} 완료", JOB_NAME);
            return RepeatStatus.FINISHED;
        };
    }
}