package com.mudosa.musinsa.settlement.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("disabled")  // TODO: 배치 설정 완료 후 "!dev"로 복구 필요
public class SettlementBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailySettlementJob;
    private final Job weeklySettlementJob;
    private final Job monthlySettlementJob;
    private final Job yearlySettlementJob;

    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduleDailySettlement() {
        try {
            log.info("[스케줄러] 일일 정산 배치 시작 - 오전 1시");

            JobParameters params = createJobParameters();
            jobLauncher.run(dailySettlementJob, params);

            log.info("[스케줄러] 일일 정산 배치 완료");
        } catch (Exception e) {
            log.error("[스케줄러] 일일 정산 배치 실패", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * MON")
    public void scheduleWeeklySettlement() {
        try {
            log.info("[스케줄러] 주간 정산 배치 시작 - 매주 월요일 오전 2시");

            JobParameters params = createJobParameters();
            jobLauncher.run(weeklySettlementJob, params);

            log.info("[스케줄러] 주간 정산 배치 완료");
        } catch (Exception e) {
            log.error("[스케줄러] 주간 정산 배치 실패", e);
        }
    }

    @Scheduled(cron = "0 0 3 1 * ?")
    public void scheduleMonthlySettlement() {
        try {
            log.info("[스케줄러] 월간 정산 배치 시작 - 매월 1일 오전 3시");

            JobParameters params = createJobParameters();
            jobLauncher.run(monthlySettlementJob, params);

            log.info("[스케줄러] 월간 정산 배치 완료");
        } catch (Exception e) {
            log.error("[스케줄러] 월간 정산 배치 실패", e);
        }
    }

    @Scheduled(cron = "0 0 4 1 1 ?")
    public void scheduleYearlySettlement() {
        try {
            log.info("[스케줄러] 연간 정산 배치 시작 - 매년 1월 1일 오전 4시");

            JobParameters params = createJobParameters();
            jobLauncher.run(yearlySettlementJob, params);

            log.info("[스케줄러] 연간 정산 배치 완료");
        } catch (Exception e) {
            log.error("[스케줄러] 연간 정산 배치 실패", e);
        }
    }

    private JobParameters createJobParameters() {
        return new JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();
    }
}
