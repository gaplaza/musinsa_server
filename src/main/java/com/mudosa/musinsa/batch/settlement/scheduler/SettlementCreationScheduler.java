package com.mudosa.musinsa.batch.settlement.scheduler;

import com.mudosa.musinsa.common.notification.SlackNotificationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "settlement.batch.scheduler.creation.enabled", havingValue = "true")
public class SettlementCreationScheduler {

    private final JobLauncher jobLauncher;
    @Qualifier("settlementCreationBatchJob")
    private final Job settlementCreationBatchJob;
    private final SlackNotificationService slackNotificationService;

    @PostConstruct
    public void init() {
        log.info("SettlementCreationScheduler Bean 등록 완료. 매 분 0초에 정산 생성 배치가 실행됩니다.");
    }

    @Scheduled(cron = "0 * * * * ?")
    public void scheduleSettlementCreation() {
        log.info("=== [정산 생성 스케줄러] 시작 ===");

        try {
            JobParameters params = createJobParameters();
            var jobExecution = jobLauncher.run(settlementCreationBatchJob, params);

            var exitStatus = jobExecution.getExitStatus();
            var executionContext = jobExecution.getExecutionContext();

            long readCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(step -> step.getReadCount())
                    .sum();
            long writeCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(step -> step.getWriteCount())
                    .sum();

            log.info("=== [정산 생성 스케줄러] 완료 - 읽기 {}건, 쓰기 {}건, 상태: {} ===",
                    readCount, writeCount, exitStatus.getExitCode());

            if (!exitStatus.getExitCode().equals("COMPLETED")) {
                log.warn("[정산 생성 스케줄러] 비정상 종료 - ExitCode: {}", exitStatus.getExitCode());
            }
        } catch (Exception e) {
            log.error("[정산 생성 스케줄러] 실패", e);
            slackNotificationService.sendBatchFailureAlert("정산 생성 배치", e);
        }
    }

    private JobParameters createJobParameters() {
        return new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("executionType", "scheduled")
                .toJobParameters();
    }
}
