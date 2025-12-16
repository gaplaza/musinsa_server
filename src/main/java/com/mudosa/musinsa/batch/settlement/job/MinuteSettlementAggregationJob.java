package com.mudosa.musinsa.batch.settlement.job;

import com.mudosa.musinsa.batch.settlement.service.SettlementAggregationService;
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
public class MinuteSettlementAggregationJob {

    private static final String JOB_NAME = "분 단위 정산 증분 집계";

    private final SettlementAggregationService settlementService;

    @Bean
    public Job minuteSettlementJob(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new JobBuilder("minuteSettlementJob", jobRepository)
            .start(buildStep(jobRepository, transactionManager))
            .build();
    }

    private Step buildStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("minuteSettlementStep", jobRepository)
            .tasklet(minuteTasklet(null), transactionManager)
            .build();
    }

    @Bean
    @StepScope
    public Tasklet minuteTasklet(@Value("#{jobParameters['targetDate']}") String targetDateStr) {
        return (contribution, chunkContext) -> {
            var result = settlementService.aggregateIncremental();

            var executionContext = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext();
            executionContext.putLong("insertCount", result.get("insertCount").longValue());
            executionContext.putLong("updateCount", result.get("updateCount").longValue());

            return RepeatStatus.FINISHED;
        };
    }
}
