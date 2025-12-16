package com.mudosa.musinsa.batch.settlement.service;

import com.mudosa.musinsa.settlement.application.dto.DailyAggregationDto;
import com.mudosa.musinsa.settlement.application.dto.MonthlyAggregationDto;
import com.mudosa.musinsa.settlement.application.dto.WeeklyAggregationDto;
import com.mudosa.musinsa.settlement.application.dto.YearlyAggregationDto;
import com.mudosa.musinsa.settlement.domain.model.AggregationStatus;
import com.mudosa.musinsa.settlement.domain.model.SettlementDaily;
import com.mudosa.musinsa.settlement.domain.model.SettlementMonthly;
import com.mudosa.musinsa.settlement.domain.model.SettlementPerTransaction;
import com.mudosa.musinsa.settlement.domain.model.SettlementStatus;
import com.mudosa.musinsa.settlement.domain.model.SettlementWeekly;
import com.mudosa.musinsa.settlement.domain.model.SettlementYearly;
import com.mudosa.musinsa.settlement.domain.repository.*;
import com.mudosa.musinsa.settlement.domain.service.SettlementNumberGenerator;
import com.mudosa.musinsa.settlement.infrastructure.JdbcSettlementBatchRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementAggregationService {

    private final SettlementPerTransactionRepository perTransactionRepository;
    private final SettlementPerTransactionMapper perTransactionMapper;
    private final SettlementDailyRepository dailyRepository;
    private final SettlementDailyMapper dailyMapper;
    private final SettlementWeeklyRepository weeklyRepository;
    private final SettlementWeeklyMapper weeklyMapper;
    private final SettlementMonthlyRepository monthlyRepository;
    private final SettlementMonthlyMapper monthlyMapper;
    private final SettlementYearlyRepository yearlyRepository;
    private final SettlementYearlyMapper yearlyMapper;
    private final SettlementNumberGenerator settlementNumberGenerator;
    private final MeterRegistry meterRegistry;
    private final JdbcSettlementBatchRepository jdbcBatchRepository;

    @Value("${settlement.batch.insert-mode:jpa}")
    private String insertMode;

    private volatile long lastProcessedCount = 0;
    private volatile long lastAggregatedCount = 0;
    private volatile double lastProcessingSpeed = 0;

    private volatile long totalProcessedCount = 0;

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("settlement.batch.processed.count", () -> lastProcessedCount)
            .description("Number of settlements processed in last batch")
            .register(meterRegistry);

        Gauge.builder("settlement.batch.aggregated.count", () -> lastAggregatedCount)
            .description("Number of daily settlements created/updated in last batch")
            .register(meterRegistry);

        Gauge.builder("settlement.batch.processing.speed", () -> lastProcessingSpeed)
            .description("Processing speed in last batch (records per minute)")
            .register(meterRegistry);

        Timer.builder("settlement.aggregation.select")
            .tag("type", "select_groupby")
            .description("Time for SELECT + GROUP BY query")
            .register(meterRegistry);

        Timer.builder("settlement.aggregation.process")
            .tag("type", "entity_creation")
            .description("Time for entity creation")
            .register(meterRegistry);

        Timer.builder("settlement.aggregation.insert")
            .tag("type", "jdbc_batch")
            .description("Time for JDBC batch insert")
            .register(meterRegistry);

        Timer.builder("settlement.aggregation.update")
            .tag("type", "bulk")
            .description("Time for bulk update")
            .register(meterRegistry);

        log.info("Settlement metrics registered (including Timer metrics)");

        recoverProcessingStatus();
    }

    private void recoverProcessingStatus() {
        int processingCount = perTransactionMapper.countProcessing();
        if (processingCount > 0) {
            log.warn("[멱등성 복구] PROCESSING 상태 {}건 발견 - NOT_AGGREGATED로 롤백", processingCount);
            int resetCount = perTransactionMapper.resetProcessingToNotAggregated();
            log.info("[멱등성 복구] {}건 롤백 완료", resetCount);
        } else {
            log.info("[멱등성 복구] PROCESSING 상태 없음 - 정상");
        }
    }

    @Transactional
    public Map<String, Integer> aggregateIncremental() {
        long startTime = System.currentTimeMillis();

        Timer.Sample queryTimer = Timer.start(meterRegistry);
        List<DailyAggregationDto> aggregations = perTransactionMapper.aggregateNotAggregated();
        long queryTime = (long) queryTimer.stop(meterRegistry.timer("settlement.aggregation.select", "type", "select_groupby")) * 1000000;

        if (aggregations.isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("[집계] 처리 대상 0건 (소요: {}ms)", duration);
            return Map.of("insertCount", 0, "updateCount", 0);
        }

        log.info("[ 집계 대상 ]");
        log.info("  NOT_AGGREGATED 건별 정산  : {}건 (GROUP BY 후)", aggregations.size());
        log.info("");

        int updatedCount = 0;
        int createdCount = 0;
        int skippedCount = 0;

        log.info("[ INSERT 모드 ] {}", insertMode.toUpperCase());

        if ("jpa-batch".equalsIgnoreCase(insertMode)) {
            var counts = aggregateIncrementalJpaBatch(aggregations);
            createdCount = counts.get("created");
            updatedCount = counts.get("updated");
            skippedCount = counts.get("skipped");
        } else if ("mybatis".equalsIgnoreCase(insertMode)) {
            var counts = aggregateIncrementalBatch(aggregations);
            createdCount = counts.get("created");
            updatedCount = counts.get("updated");
            skippedCount = counts.get("skipped");
        } else if ("jdbc".equalsIgnoreCase(insertMode)) {
            var counts = aggregateIncrementalJdbc(aggregations);
            createdCount = counts.get("created");
            updatedCount = counts.get("updated");
            skippedCount = counts.get("skipped");
        } else {
            Timer.Sample insertTimer = Timer.start(meterRegistry);

            for (DailyAggregationDto dto : aggregations) {
                java.util.Optional<SettlementDaily> existingOpt = dailyRepository
                    .findByBrandIdAndSettlementDate(dto.getBrandId(), dto.getSettlementDate());

                SettlementDaily daily;

                if (existingOpt.isPresent()) {
                    daily = existingOpt.get();

                    if (daily.getSettlementStatus() == SettlementStatus.CONFIRMED) {
                        skippedCount++;
                        continue;
                    }

                    daily.addAggregatedData(
                        dto.getTotalOrderCount().intValue(),
                        dto.getTotalSalesAmount(),
                        dto.getTotalCommissionAmount(),
                        dto.getTotalTaxAmount(),
                        dto.getTotalPgFeeAmount()
                    );
                    updatedCount++;
                } else {

                    String settlementNumber = settlementNumberGenerator.generateDailyNumber(
                        dto.getSettlementDate()
                    );
                    daily = SettlementDaily.createFromAggregation(
                        dto.getBrandId(),
                        dto.getSettlementDate(),
                        settlementNumber,
                        "Asia/Seoul",
                        dto.getTotalOrderCount().intValue(),
                        dto.getTotalSalesAmount(),
                        dto.getTotalCommissionAmount(),
                        dto.getTotalTaxAmount(),
                        dto.getTotalPgFeeAmount()
                    );
                    createdCount++;
                }

                daily.startProcessing();
                dailyRepository.save(daily);

                aggregateToWeeklyIncremental(dto);
                aggregateToMonthlyIncremental(dto);
                aggregateToYearlyIncremental(dto);
            }

            insertTimer.stop(meterRegistry.timer("settlement.aggregation.insert", "type", "jpa_individual"));
        }

        int bulkUpdatedCount = perTransactionMapper.updateAggregationStatusToAggregated();

        long duration = System.currentTimeMillis() - startTime;
        double durationSeconds = duration / 1000.0;

        double perMinute = durationSeconds > 0 ? (bulkUpdatedCount / durationSeconds) * 60 : 0;
        double perSecond = durationSeconds > 0 ? bulkUpdatedCount / durationSeconds : 0;
        double achievement = perMinute > 0 ? (perMinute / 100000.0) * 100 : 0;

        this.lastProcessedCount = bulkUpdatedCount;
        this.lastAggregatedCount = aggregations.size();
        this.lastProcessingSpeed = perMinute;
        this.totalProcessedCount += bulkUpdatedCount;

        log.info("");
        log.info("================================================================================");
        log.info("[Minute Settlement Aggregation] 완료");
        log.info("================================================================================");
        log.info("");
        log.info("[ 이번 배치 처리 결과 ]");
        log.info("  건별 정산 처리           : {}건 (NOT_AGGREGATED → AGGREGATED)", String.format("%,d", bulkUpdatedCount));
        log.info("  브랜드별 Daily 집계 생성  : {}건 (브랜드 × 날짜 조합)", String.format("%,d", aggregations.size()));
        if (skippedCount > 0) {
            log.info("  CONFIRMED 스킵          : {}건", String.format("%,d", skippedCount));
        }
        log.info("");
        log.info("[ 성능 ]");
        log.info("  소요 시간               : {}초", String.format("%.3f", durationSeconds));
        log.info("  처리 속도               : {}건/분 ({}건/초)", String.format("%,.0f", perMinute), String.format("%,.1f", perSecond));
        log.info("  목표 달성률             : {}%", String.format("%.1f", achievement));
        log.info("");
        log.info("[ 누적 처리량 (앱 시작 후) ]");
        log.info("  총 처리 건수            : {}건", String.format("%,d", totalProcessedCount));
        log.info("");
        log.info("================================================================================");

        return Map.of("insertCount", createdCount, "updateCount", updatedCount);
    }

    
    private void aggregateToWeeklyIncremental(DailyAggregationDto dto) {
        LocalDate date = dto.getSettlementDate();
        WeekFields weekFields = WeekFields.ISO;
        int year = date.getYear();
        int month = date.getMonthValue();
        int weekOfMonth = date.get(weekFields.weekOfMonth());

        LocalDate weekStart = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = date.with(java.time.DayOfWeek.SUNDAY);

        var existingOpt = weeklyRepository.findByBrandIdAndSettlementYearAndSettlementMonthAndWeekOfMonth(
            dto.getBrandId(), year, month, weekOfMonth
        );

        SettlementWeekly weekly;
        if (existingOpt.isPresent()) {
            weekly = existingOpt.get();
            if (weekly.getSettlementStatus() != SettlementStatus.CONFIRMED) {
                weekly.addAggregatedData(
                    dto.getTotalOrderCount().intValue(),
                    dto.getTotalSalesAmount(),
                    dto.getTotalCommissionAmount(),
                    dto.getTotalTaxAmount(),
                    dto.getTotalPgFeeAmount()
                );
            }
        } else {
            String settlementNumber = settlementNumberGenerator.generateWeeklyNumber(year, weekOfMonth);
            weekly = SettlementWeekly.createFromAggregation(
                dto.getBrandId(),
                year,
                weekOfMonth,
                weekStart,
                weekEnd,
                settlementNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            weekly.startProcessing();
        }
        weeklyRepository.save(weekly);
    }

    
    private void aggregateToMonthlyIncremental(DailyAggregationDto dto) {
        LocalDate date = dto.getSettlementDate();
        int year = date.getYear();
        int month = date.getMonthValue();

        var existingOpt = monthlyRepository.findByBrandIdAndSettlementYearAndSettlementMonth(
            dto.getBrandId(), year, month
        );

        SettlementMonthly monthly;
        if (existingOpt.isPresent()) {
            monthly = existingOpt.get();
            if (monthly.getSettlementStatus() != SettlementStatus.CONFIRMED) {
                monthly.addAggregatedData(
                    dto.getTotalOrderCount().intValue(),
                    dto.getTotalSalesAmount(),
                    dto.getTotalCommissionAmount(),
                    dto.getTotalTaxAmount(),
                    dto.getTotalPgFeeAmount()
                );
            }
        } else {
            String settlementNumber = settlementNumberGenerator.generateMonthlyNumber(year, month);
            monthly = SettlementMonthly.createFromAggregation(
                dto.getBrandId(),
                year,
                month,
                settlementNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            monthly.startProcessing();
        }
        monthlyRepository.save(monthly);
    }

    
    private void aggregateToYearlyIncremental(DailyAggregationDto dto) {
        int year = dto.getSettlementDate().getYear();

        var existingOpt = yearlyRepository.findByBrandIdAndSettlementYear(dto.getBrandId(), year);

        SettlementYearly yearly;
        if (existingOpt.isPresent()) {
            yearly = existingOpt.get();
            if (yearly.getSettlementStatus() != SettlementStatus.CONFIRMED) {
                yearly.addAggregatedData(
                    dto.getTotalOrderCount().intValue(),
                    dto.getTotalSalesAmount(),
                    dto.getTotalCommissionAmount(),
                    dto.getTotalTaxAmount(),
                    dto.getTotalPgFeeAmount()
                );
            }
        } else {
            String settlementNumber = settlementNumberGenerator.generateYearlyNumber(year);
            yearly = SettlementYearly.createFromAggregation(
                dto.getBrandId(),
                year,
                settlementNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            yearly.startProcessing();
        }
        yearlyRepository.save(yearly);
    }

    @Transactional
    public void aggregateToDaily(LocalDate startDate, LocalDate endDate) {
        List<DailyAggregationDto> aggregations = perTransactionMapper.aggregateByDaily(
            startDate, endDate
        );

        if (aggregations.isEmpty()) {
            log.warn("집계할 거래 데이터가 없습니다");
            return;
        }

        int updatedCount = 0;
        int createdCount = 0;

        for (DailyAggregationDto dto : aggregations) {
            java.util.Optional<SettlementDaily> existingOpt = dailyRepository
                .findByBrandIdAndSettlementDate(dto.getBrandId(), dto.getSettlementDate());

            SettlementDaily daily;

            if (existingOpt.isPresent()) {
                daily = existingOpt.get();
                daily.setAggregatedData(
                    dto.getTotalOrderCount().intValue(),
                    dto.getTotalSalesAmount(),
                    dto.getTotalCommissionAmount(),
                    dto.getTotalTaxAmount(),
                    dto.getTotalPgFeeAmount()
                );
                updatedCount++;
            } else {
                String settlementNumber = settlementNumberGenerator.generateDailyNumber(
                    dto.getSettlementDate()
                );
                daily = SettlementDaily.createFromAggregation(
                    dto.getBrandId(),
                    dto.getSettlementDate(),
                    settlementNumber,
                    "Asia/Seoul",
                    dto.getTotalOrderCount().intValue(),
                    dto.getTotalSalesAmount(),
                    dto.getTotalCommissionAmount(),
                    dto.getTotalTaxAmount(),
                    dto.getTotalPgFeeAmount()
                );
                daily.startProcessing();
                createdCount++;
            }

            daily.confirm();
            dailyRepository.save(daily);
        }

        log.info("일일 정산 {}건 확정 완료 (신규 {}건, 업데이트 {}건)",
            aggregations.size(), createdCount, updatedCount);
    }

    @Transactional
    public void aggregateToWeekly(LocalDate startDate, LocalDate endDate) {
        List<WeeklyAggregationDto> aggregations = dailyMapper.aggregateByWeekly(startDate, endDate);

        if (aggregations.isEmpty()) {
            log.warn("주간 집계할 일일 정산 데이터가 없습니다");
            return;
        }

        for (WeeklyAggregationDto dto : aggregations) {
            String settlementNumber = settlementNumberGenerator.generateWeeklyNumber(
                dto.getYear(), dto.getWeekOfMonth()
            );

            SettlementWeekly weekly = SettlementWeekly.createFromAggregation(
                dto.getBrandId(),
                dto.getYear(),
                dto.getWeekOfMonth(),
                dto.getWeekStartDate(),
                dto.getWeekEndDate(),
                settlementNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );

            weekly.startProcessing();
            weekly.confirm();

            weeklyRepository.save(weekly);
        }

        log.info("주간 정산 {}건 생성 완료", aggregations.size());
    }

    @Transactional
    public void aggregateToMonthly(LocalDate startDate, LocalDate endDate) {
        List<MonthlyAggregationDto> aggregations = dailyMapper.aggregateByMonthly(startDate, endDate);

        if (aggregations.isEmpty()) {
            log.warn("월간 집계할 일일 정산 데이터가 없습니다");
            return;
        }

        for (MonthlyAggregationDto dto : aggregations) {
            String settlementNumber = settlementNumberGenerator.generateMonthlyNumber(
                dto.getYear(), dto.getMonth()
            );

            SettlementMonthly monthly = SettlementMonthly.createFromAggregation(
                dto.getBrandId(),
                dto.getYear(),
                dto.getMonth(),
                settlementNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );

            monthly.startProcessing();
            monthly.confirm();

            monthlyRepository.save(monthly);
        }

        log.info("월간 정산 {}건 생성 완료", aggregations.size());
    }

    @Transactional
    public void aggregateToYearly(int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<YearlyAggregationDto> aggregations = monthlyMapper.aggregateByYearly(startDate, endDate);

        if (aggregations.isEmpty()) {
            log.warn("연간 집계할 월간 정산 데이터가 없습니다");
            return;
        }

        for (YearlyAggregationDto dto : aggregations) {
            String settlementNumber = settlementNumberGenerator.generateYearlyNumber(dto.getYear());

            SettlementYearly yearly = SettlementYearly.createFromAggregation(
                dto.getBrandId(),
                dto.getYear(),
                settlementNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );

            yearly.startProcessing();
            yearly.confirm();

            yearlyRepository.save(yearly);
        }

        log.info("연간 정산 {}건 생성 완료", aggregations.size());
    }

    private Map<String, Integer> aggregateIncrementalBatch(List<DailyAggregationDto> aggregations) {
        List<SettlementDaily> dailyList = new ArrayList<>();
        List<SettlementWeekly> weeklyList = new ArrayList<>();
        List<SettlementMonthly> monthlyList = new ArrayList<>();
        List<SettlementYearly> yearlyList = new ArrayList<>();

        int created = 0;
        int updated = 0;
        int skipped = 0;

        Timer.Sample processSample = Timer.start();

        for (DailyAggregationDto dto : aggregations) {
            String dailyNumber = settlementNumberGenerator.generateDailyNumber(dto.getSettlementDate());
            SettlementDaily daily = SettlementDaily.createFromAggregation(
                dto.getBrandId(),
                dto.getSettlementDate(),
                dailyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            daily.startProcessing();
            dailyList.add(daily);

            LocalDate date = dto.getSettlementDate();
            WeekFields weekFields = WeekFields.ISO;
            int year = date.getYear();
            int month = date.getMonthValue();
            int weekOfMonth = date.get(weekFields.weekOfMonth());
            LocalDate weekStart = date.with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = date.with(java.time.DayOfWeek.SUNDAY);

            String weeklyNumber = settlementNumberGenerator.generateWeeklyNumber(year, weekOfMonth);
            SettlementWeekly weekly = SettlementWeekly.createFromAggregation(
                dto.getBrandId(),
                year,
                weekOfMonth,
                weekStart,
                weekEnd,
                weeklyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            weekly.startProcessing();
            weeklyList.add(weekly);

            String monthlyNumber = settlementNumberGenerator.generateMonthlyNumber(year, month);
            SettlementMonthly monthly = SettlementMonthly.createFromAggregation(
                dto.getBrandId(),
                year,
                month,
                monthlyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            monthly.startProcessing();
            monthlyList.add(monthly);

            String yearlyNumber = settlementNumberGenerator.generateYearlyNumber(year);
            SettlementYearly yearly = SettlementYearly.createFromAggregation(
                dto.getBrandId(),
                year,
                yearlyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            yearly.startProcessing();
            yearlyList.add(yearly);

            created++;
        }

        processSample.stop(Timer.builder("settlement.aggregation.process")
            .tag("type", "entity_creation")
            .register(meterRegistry));

        Timer.Sample insertSample = Timer.start();

        if (!dailyList.isEmpty()) {
            dailyMapper.batchInsert(dailyList);
        }
        if (!weeklyList.isEmpty()) {
            weeklyMapper.batchInsert(weeklyList);
        }
        if (!monthlyList.isEmpty()) {
            monthlyMapper.batchInsert(monthlyList);
        }
        if (!yearlyList.isEmpty()) {
            yearlyMapper.batchInsert(yearlyList);
        }

        insertSample.stop(Timer.builder("settlement.aggregation.insert")
            .tag("type", "mybatis_batch")
            .register(meterRegistry));

        return Map.of("created", created, "updated", updated, "skipped", skipped);
    }

    private Map<String, Integer> aggregateIncrementalJdbc(List<DailyAggregationDto> aggregations) {
        List<SettlementDaily> dailyList = new ArrayList<>();
        List<SettlementWeekly> weeklyList = new ArrayList<>();
        List<SettlementMonthly> monthlyList = new ArrayList<>();
        List<SettlementYearly> yearlyList = new ArrayList<>();

        int created = 0;
        int updated = 0;
        int skipped = 0;

        Timer.Sample processSample = Timer.start();

        for (DailyAggregationDto dto : aggregations) {
            String dailyNumber = settlementNumberGenerator.generateDailyNumber(dto.getSettlementDate());
            SettlementDaily daily = SettlementDaily.createFromAggregation(
                dto.getBrandId(),
                dto.getSettlementDate(),
                dailyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            daily.startProcessing();
            dailyList.add(daily);

            LocalDate date = dto.getSettlementDate();
            WeekFields weekFields = WeekFields.ISO;
            int year = date.getYear();
            int month = date.getMonthValue();
            int weekOfMonth = date.get(weekFields.weekOfMonth());
            LocalDate weekStart = date.with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = date.with(java.time.DayOfWeek.SUNDAY);

            String weeklyNumber = settlementNumberGenerator.generateWeeklyNumber(year, weekOfMonth);
            SettlementWeekly weekly = SettlementWeekly.createFromAggregation(
                dto.getBrandId(),
                year,
                weekOfMonth,
                weekStart,
                weekEnd,
                weeklyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            weekly.startProcessing();
            weeklyList.add(weekly);

            String monthlyNumber = settlementNumberGenerator.generateMonthlyNumber(year, month);
            SettlementMonthly monthly = SettlementMonthly.createFromAggregation(
                dto.getBrandId(),
                year,
                month,
                monthlyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            monthly.startProcessing();
            monthlyList.add(monthly);

            String yearlyNumber = settlementNumberGenerator.generateYearlyNumber(year);
            SettlementYearly yearly = SettlementYearly.createFromAggregation(
                dto.getBrandId(),
                year,
                yearlyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            yearly.startProcessing();
            yearlyList.add(yearly);

            created++;
        }

        processSample.stop(Timer.builder("settlement.aggregation.process")
            .tag("type", "entity_creation")
            .register(meterRegistry));

        Timer.Sample insertSample = Timer.start();

        if (!dailyList.isEmpty()) {
            jdbcBatchRepository.batchInsertDaily(dailyList);
        }
        if (!weeklyList.isEmpty()) {
            jdbcBatchRepository.batchInsertWeekly(weeklyList);
        }
        if (!monthlyList.isEmpty()) {
            jdbcBatchRepository.batchInsertMonthly(monthlyList);
        }
        if (!yearlyList.isEmpty()) {
            jdbcBatchRepository.batchInsertYearly(yearlyList);
        }

        insertSample.stop(Timer.builder("settlement.aggregation.insert")
            .tag("type", "jdbc_batch")
            .register(meterRegistry));

        return Map.of("created", created, "updated", updated, "skipped", skipped);
    }

    private Map<String, Integer> aggregateIncrementalJpaBatch(List<DailyAggregationDto> aggregations) {
        List<SettlementDaily> dailyList = new ArrayList<>();
        List<SettlementWeekly> weeklyList = new ArrayList<>();
        List<SettlementMonthly> monthlyList = new ArrayList<>();
        List<SettlementYearly> yearlyList = new ArrayList<>();

        int created = 0;
        int updated = 0;
        int skipped = 0;

        Timer.Sample processSample = Timer.start();

        for (DailyAggregationDto dto : aggregations) {
            String dailyNumber = settlementNumberGenerator.generateDailyNumber(dto.getSettlementDate());
            SettlementDaily daily = SettlementDaily.createFromAggregation(
                dto.getBrandId(),
                dto.getSettlementDate(),
                dailyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            daily.startProcessing();
            dailyList.add(daily);

            LocalDate date = dto.getSettlementDate();
            WeekFields weekFields = WeekFields.ISO;
            int year = date.getYear();
            int month = date.getMonthValue();
            int weekOfMonth = date.get(weekFields.weekOfMonth());
            LocalDate weekStart = date.with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = date.with(java.time.DayOfWeek.SUNDAY);

            String weeklyNumber = settlementNumberGenerator.generateWeeklyNumber(year, weekOfMonth);
            SettlementWeekly weekly = SettlementWeekly.createFromAggregation(
                dto.getBrandId(),
                year,
                weekOfMonth,
                weekStart,
                weekEnd,
                weeklyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            weekly.startProcessing();
            weeklyList.add(weekly);

            String monthlyNumber = settlementNumberGenerator.generateMonthlyNumber(year, month);
            SettlementMonthly monthly = SettlementMonthly.createFromAggregation(
                dto.getBrandId(),
                year,
                month,
                monthlyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            monthly.startProcessing();
            monthlyList.add(monthly);

            String yearlyNumber = settlementNumberGenerator.generateYearlyNumber(year);
            SettlementYearly yearly = SettlementYearly.createFromAggregation(
                dto.getBrandId(),
                year,
                yearlyNumber,
                "Asia/Seoul",
                dto.getTotalOrderCount().intValue(),
                dto.getTotalSalesAmount(),
                dto.getTotalCommissionAmount(),
                dto.getTotalTaxAmount(),
                dto.getTotalPgFeeAmount()
            );
            yearly.startProcessing();
            yearlyList.add(yearly);

            created++;
        }

        processSample.stop(Timer.builder("settlement.aggregation.process")
            .tag("type", "entity_creation")
            .register(meterRegistry));

        Timer.Sample insertSample = Timer.start();

        if (!dailyList.isEmpty()) {
            dailyRepository.saveAll(dailyList);
        }
        if (!weeklyList.isEmpty()) {
            weeklyRepository.saveAll(weeklyList);
        }
        if (!monthlyList.isEmpty()) {
            monthlyRepository.saveAll(monthlyList);
        }
        if (!yearlyList.isEmpty()) {
            yearlyRepository.saveAll(yearlyList);
        }

        insertSample.stop(Timer.builder("settlement.aggregation.insert")
            .tag("type", "jpa_batch")
            .register(meterRegistry));

        return Map.of("created", created, "updated", updated, "skipped", skipped);
    }
}