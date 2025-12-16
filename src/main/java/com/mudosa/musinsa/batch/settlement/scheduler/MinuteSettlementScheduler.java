package com.mudosa.musinsa.batch.settlement.scheduler;

import com.mudosa.musinsa.common.notification.SlackNotificationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "settlement.batch.scheduler.minute.enabled", havingValue = "true")
public class MinuteSettlementScheduler {

    private final JobLauncher jobLauncher;
    private final Job minuteSettlementJob;
    private final SlackNotificationService slackNotificationService;

    @PostConstruct
    public void init() {
        log.info("MinuteSettlementScheduler Bean 등록 완료. 10초마다 실시간 집계가 실행됩니다.");
    }

    @Scheduled(cron = "*/10 * * * * ?")
    public void scheduleMinuteSettlement() {
        log.info("=== [분 단위 정산 스케줄러] 시작 ===");
        try {
            JobParameters params = createJobParameters();
            var jobExecution = jobLauncher.run(minuteSettlementJob, params);

            var exitStatus = jobExecution.getExitStatus();
            var executionContext = jobExecution.getExecutionContext();

            long insertCount = executionContext.getLong("insertCount", 0L);
            long updateCount = executionContext.getLong("updateCount", 0L);
            long totalCount = insertCount + updateCount;

            log.info("=== [분 단위 정산 스케줄러] 완료 - 총 {}건 처리 (신규: {}건, 업데이트: {}건) ===",
                totalCount, insertCount, updateCount);

            if (!exitStatus.getExitCode().equals("COMPLETED")) {
                log.warn("[분 단위 정산 스케줄러] 비정상 종료 - ExitCode: {}", exitStatus.getExitCode());
            }
        } catch (Exception e) {
            log.error("[분 단위 정산 스케줄러] 실패", e);
            slackNotificationService.sendBatchFailureAlert("분 단위 정산 배치", e);
        }
    }

    private JobParameters createJobParameters() {
        return new JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .addString("executionType", "minute")
            .toJobParameters();
    }
}
