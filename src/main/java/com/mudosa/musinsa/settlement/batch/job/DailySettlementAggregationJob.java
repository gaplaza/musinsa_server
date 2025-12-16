package com.mudosa.musinsa.settlement.batch.job;

import com.mudosa.musinsa.settlement.application.SettlementAggregationService;
import com.mudosa.musinsa.settlement.batch.common.BrandIdReader;
import com.mudosa.musinsa.settlement.batch.common.DateRangeCalculator;
import com.mudosa.musinsa.settlement.batch.config.BatchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Slf4j
@Configuration
@Profile("disabled")  // TODO: 배치 설정 완료 후 "!dev"로 복구 필요
@RequiredArgsConstructor
public class DailySettlementAggregationJob {

    private static final String JOB_NAME = "일일 정산 집계";

    private final SettlementAggregationService settlementService;
    private final BrandIdReader brandIdReader;
    private final BatchProperties batchProperties;

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
            .<Long, Long>chunk(batchProperties.getChunkSize(), transactionManager)
            .reader(brandIdReader.createReader(JOB_NAME))
            .processor(buildProcessor(null))
            .writer(buildWriter())
            .faultTolerant()
            .skip(DataAccessException.class)
            .skipLimit(batchProperties.getMaxSkipCount())
            .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Long, Long> buildProcessor(
            @Value("#{jobParameters['targetDate']}") String targetDateStr
    ) {
        return brandId -> {
            LocalDate targetDate;
            if (targetDateStr != null && !targetDateStr.isEmpty()) {
                targetDate = LocalDate.parse(targetDateStr);
                log.info("📅 JobParameters로 전달받은 targetDate 사용: {}", targetDate);
            } else {
                targetDate = DateRangeCalculator.getYesterday();
                log.info("📅 기본값 사용 (어제): {}", targetDate);
            }

            settlementService.aggregateToDaily(brandId, targetDate, targetDate);

            log.debug("브랜드 {} 일일 정산 집계 완료 (targetDate={})", brandId, targetDate);
            return brandId;
        };
    }

    private ItemWriter<Long> buildWriter() {
        return chunk -> log.info("{} 청크 처리 완료: {} 개 브랜드", JOB_NAME, chunk.size());
    }
}