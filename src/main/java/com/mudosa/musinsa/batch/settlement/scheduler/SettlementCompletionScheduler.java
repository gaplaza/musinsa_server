package com.mudosa.musinsa.batch.settlement.scheduler;

import com.mudosa.musinsa.settlement.domain.model.SettlementDaily;
import com.mudosa.musinsa.settlement.domain.model.SettlementMonthly;
import com.mudosa.musinsa.settlement.domain.model.SettlementStatus;
import com.mudosa.musinsa.settlement.domain.model.SettlementWeekly;
import com.mudosa.musinsa.settlement.domain.repository.SettlementDailyRepository;
import com.mudosa.musinsa.settlement.domain.repository.SettlementMonthlyRepository;
import com.mudosa.musinsa.settlement.domain.repository.SettlementWeeklyRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "settlement.batch.scheduler.enabled", havingValue = "true")
public class SettlementCompletionScheduler {

    private final SettlementDailyRepository dailyRepository;
    private final SettlementWeeklyRepository weeklyRepository;
    private final SettlementMonthlyRepository monthlyRepository;

    private static final int DAYS_UNTIL_COMPLETION = 3;

    @PostConstruct
    public void init() {
        completeOldSettlements();
    }

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void completeOldSettlements() {
        log.info("=== [정산 자동 완료 스케줄러] 시작 ===");

        LocalDate cutoffDate = LocalDate.now().minusDays(DAYS_UNTIL_COMPLETION);

        int dailyCount = completeDailySettlements(cutoffDate);
        int weeklyCount = completeWeeklySettlements(cutoffDate);
        int monthlyCount = completeMonthlySettlements(cutoffDate);

        int totalCount = dailyCount + weeklyCount + monthlyCount;

        if (totalCount == 0) {
            log.info("[정산 자동 완료] 처리할 정산 없음");
        } else {
            log.info("=== [정산 자동 완료 스케줄러] 완료 - 일별 {}건, 주별 {}건, 월별 {}건 입금 완료 처리 ===",
                dailyCount, weeklyCount, monthlyCount);
        }
    }

    private int completeDailySettlements(LocalDate cutoffDate) {
        List<SettlementDaily> pendingSettlements = dailyRepository
            .findBySettlementStatusAndSettlementDateBefore(SettlementStatus.PENDING, cutoffDate);

        if (pendingSettlements.isEmpty()) {
            return 0;
        }

        for (SettlementDaily settlement : pendingSettlements) {
            settlement.complete();
        }
        dailyRepository.saveAll(pendingSettlements);

        return pendingSettlements.size();
    }

    private int completeWeeklySettlements(LocalDate cutoffDate) {
        List<SettlementWeekly> pendingSettlements = weeklyRepository
            .findBySettlementStatusAndWeekEndDateBefore(SettlementStatus.PENDING, cutoffDate);

        if (pendingSettlements.isEmpty()) {
            return 0;
        }

        for (SettlementWeekly settlement : pendingSettlements) {
            settlement.complete();
        }
        weeklyRepository.saveAll(pendingSettlements);

        return pendingSettlements.size();
    }

    private int completeMonthlySettlements(LocalDate cutoffDate) {
        List<SettlementMonthly> pendingSettlements = monthlyRepository
            .findBySettlementStatusAndMonthEndDateBefore(SettlementStatus.PENDING, cutoffDate);

        if (pendingSettlements.isEmpty()) {
            return 0;
        }

        for (SettlementMonthly settlement : pendingSettlements) {
            settlement.complete();
        }
        monthlyRepository.saveAll(pendingSettlements);

        return pendingSettlements.size();
    }
}