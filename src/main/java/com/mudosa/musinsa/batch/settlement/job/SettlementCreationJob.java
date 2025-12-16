package com.mudosa.musinsa.batch.settlement.job;

import com.mudosa.musinsa.batch.settlement.dto.PaymentSettlementDto;
import com.mudosa.musinsa.batch.settlement.partitioner.PaymentPartitioner;
import com.mudosa.musinsa.batch.settlement.service.SettlementAggregationService;
import com.mudosa.musinsa.common.notification.SlackNotificationService;
import com.mudosa.musinsa.common.vo.Money;
import com.mudosa.musinsa.settlement.domain.model.SettlementPerTransaction;
import com.mudosa.musinsa.settlement.domain.model.TransactionType;
import com.mudosa.musinsa.settlement.domain.service.PgFeeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "settlement.batch.enabled", havingValue = "true")
public class SettlementCreationJob {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final PgFeeCalculator pgFeeCalculator;
    private final SlackNotificationService slackNotificationService;
    private final com.mudosa.musinsa.batch.settlement.listener.DetailedPerformanceListener detailedPerformanceListener;
    private final MeterRegistry meterRegistry;
    private final SettlementAggregationService settlementAggregationService;

    @Qualifier("batchTaskExecutor")
    private final TaskExecutor batchTaskExecutor;

    private static final int GRID_SIZE = 4;

    private volatile long lastPaymentReadCount = 0;
    private volatile long lastSettlementWriteCount = 0;
    private volatile double lastProcessingSpeed = 0;
    private volatile long totalPaymentProcessed = 0;
    private volatile long totalSettlementCreated = 0;

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("settlement.creation.payment.read.count", () -> lastPaymentReadCount)
            .description("Number of payments read in last creation batch")
            .register(meterRegistry);

        Gauge.builder("settlement.creation.settlement.write.count", () -> lastSettlementWriteCount)
            .description("Number of settlements created in last creation batch")
            .register(meterRegistry);

        Gauge.builder("settlement.creation.processing.speed", () -> lastProcessingSpeed)
            .description("Payment processing speed (records per minute)")
            .register(meterRegistry);

        Gauge.builder("settlement.creation.total.payment.processed", () -> totalPaymentProcessed)
            .description("Total payments processed since app start")
            .register(meterRegistry);

        Gauge.builder("settlement.creation.total.settlement.created", () -> totalSettlementCreated)
            .description("Total settlements created since app start")
            .register(meterRegistry);

        log.info("Settlement Creation metrics registered");
    }

    @Bean
    public Partitioner paymentPartitioner() {
        return new PaymentPartitioner(jdbcTemplate);
    }

    @Bean
    public Job settlementCreationBatchJob(JobRepository jobRepository,
            Step partitionedCreationStep,
            Step aggregationStep) {
        return new JobBuilder("settlementCreationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobExecutionListener())
                .start(partitionedCreationStep)  // Step 1: 파티셔닝된 건별 정산 생성 (4개 병렬)
                .next(aggregationStep)           // Step 2: 일별/주별/월별/연별 집계
                .build();
    }

    @Bean
    public Step partitionedCreationStep(JobRepository jobRepository,
            Step settlementCreationWorkerStep) {
        return new StepBuilder("partitionedCreationStep", jobRepository)
                .partitioner("settlementCreationWorkerStep", paymentPartitioner())
                .step(settlementCreationWorkerStep)
                .gridSize(GRID_SIZE)
                .taskExecutor(batchTaskExecutor)
                .listener(partitionStepListener())
                .build();
    }

    @Bean
    public StepExecutionListener partitionStepListener() {
        return new StepExecutionListener() {
            private long startTime;

            @Override
            public void beforeStep(StepExecution stepExecution) {
                startTime = System.currentTimeMillis();
                log.info("[Partitioned Step] 시작 - gridSize: {}", GRID_SIZE);
            }

            @Override
            public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                log.info("[Partitioned Step] 완료 - 총 소요시간: {}ms ({} 파티션 병렬 처리)",
                        duration, GRID_SIZE);
                return org.springframework.batch.core.ExitStatus.COMPLETED;
            }
        };
    }

    @Bean
    public org.springframework.batch.core.JobExecutionListener jobExecutionListener() {
        return new org.springframework.batch.core.JobExecutionListener() {
            private long jobStartTime;

            @Override
            public void beforeJob(org.springframework.batch.core.JobExecution jobExecution) {
                jobStartTime = System.currentTimeMillis();
                log.info("================================================================================");
                log.info("[Job 시작] settlementCreationJob");
                log.info("================================================================================");
            }

            @Override
            public void afterJob(org.springframework.batch.core.JobExecution jobExecution) {
                long jobEndTime = System.currentTimeMillis();
                long totalDuration = jobEndTime - jobStartTime;
                double seconds = totalDuration / 1000.0;

                long totalRead = 0;
                long totalWrite = 0;
                for (org.springframework.batch.core.StepExecution stepExecution : jobExecution.getStepExecutions()) {
                    String stepName = stepExecution.getStepName();
                    if (stepName.contains(":partition")) {
                        totalRead += stepExecution.getReadCount();
                        totalWrite += stepExecution.getWriteCount();
                        log.debug("[Job 집계] {} - Read: {}, Write: {}",
                            stepName, stepExecution.getReadCount(), stepExecution.getWriteCount());
                    }
                }

                double speed = seconds > 0 ? (totalRead / seconds) * 60 : 0;
                double achievement = (speed / 50000.0) * 100;

                log.info("");
                log.info("================================================================================");
                log.info("[Job 완료] settlementCreationJob");
                log.info("================================================================================");
                log.info("  처리 건수 (Read)   : {} 건", String.format("%,d", totalRead));
                log.info("  처리 건수 (Write)  : {} 건", String.format("%,d", totalWrite));
                log.info("  총 소요 시간       : {}초", String.format("%.2f", seconds));
                log.info("  처리 속도          : {} 건/분", String.format("%,.0f", speed));
                log.info("  목표 대비 달성률   : {}%", String.format("%.1f", achievement));
                log.info("================================================================================");
                log.info("");
            }
        };
    }

    @Bean
    public Step settlementCreationWorkerStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        final int CHUNK_SIZE = 2000;  // [테스트용] MySQL 튜닝 효과 측정
        log.info("[Worker Step 설정] chunk-size: {}", CHUNK_SIZE);
        return new StepBuilder("settlementCreationWorkerStep", jobRepository)
                .<PaymentSettlementDto, SettlementPerTransaction>chunk(CHUNK_SIZE, transactionManager)
                .reader(paymentSettlementReader(null, null))  // StepScope에서 실제 값 주입
                .processor(settlementProcessor())
                .writer(settlementWriter())
                .listener(workerStepListener())
                .build();
    }

    @Bean
    public StepExecutionListener workerStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                String stepName = stepExecution.getStepName();
                Long minId = stepExecution.getExecutionContext().getLong("minId", -1);
                Long maxId = stepExecution.getExecutionContext().getLong("maxId", -1);
                log.info("[Worker {}] 시작 - 범위: {} ~ {}", stepName, minId, maxId);
            }

            @Override
            public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {
                String stepName = stepExecution.getStepName();
                long readCount = stepExecution.getReadCount();
                long writeCount = stepExecution.getWriteCount();
                log.info("[Worker {}] 완료 - Read: {}, Write: {}", stepName, readCount, writeCount);
                return org.springframework.batch.core.ExitStatus.COMPLETED;
            }
        };
    }

    @Bean
    public Step aggregationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("aggregationStep", jobRepository)
                .tasklet(aggregationTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet aggregationTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== [집계 Step] 시작 ===");
            var result = settlementAggregationService.aggregateIncremental();
            log.info("=== [집계 Step] 완료 - 신규: {}건, 업데이트: {}건 ===",
                    result.get("insertCount"), result.get("updateCount"));
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public StepExecutionListener performanceLogger() {
        return new StepExecutionListener() {
            private long startTime;

            @Override
            public void beforeStep(StepExecution stepExecution) {
                startTime = System.currentTimeMillis();
            }

            @Override
            public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {
                long endTime = System.currentTimeMillis();
                long totalDuration = endTime - startTime;

                long readCount = stepExecution.getReadCount();
                long writeCount = stepExecution.getWriteCount();
                long commitCount = stepExecution.getCommitCount();

                if (readCount == 0) {
                    log.debug("[Step 1] 처리 대상 0건 (소요: {}ms)", totalDuration);
                    return org.springframework.batch.core.ExitStatus.COMPLETED;
                }

                double readThroughput = readCount > 0 ? (readCount / (totalDuration / 1000.0) * 60) : 0;
                double writeThroughput = writeCount > 0 ? (writeCount / (totalDuration / 1000.0) * 60) : 0;

                lastPaymentReadCount = readCount;
                lastSettlementWriteCount = writeCount;
                lastProcessingSpeed = readThroughput;
                totalPaymentProcessed += readCount;
                totalSettlementCreated += writeCount;

                log.info("");
                log.info("================================================================================");
                log.info("Settlement Creation Batch 완료");
                log.info("================================================================================");
                log.info("");
                log.info("[ 처리 결과 ]");
                log.info("  Read  (Payment 읽기)      : {} 건", String.format("%,d", readCount));
                log.info("  Write (Settlement 생성)   : {} 건", String.format("%,d", writeCount));
                log.info("  Commit                   : {} 회", commitCount);
                log.info("");
                log.info("[ 소요 시간 ]");
                log.info("  Total Duration           : {}.{}초", totalDuration / 1000, String.format("%03d", totalDuration % 1000));
                log.info("");
                log.info("[ 처리 속도 ]");
                log.info("  Payment 처리             : {}/분 ({}/초)",
                    String.format("%,.0f", readThroughput),
                    String.format("%,.1f", readThroughput / 60));
                log.info("  Settlement 생성          : {}/분 ({}/초)",
                    String.format("%,.0f", writeThroughput),
                    String.format("%,.1f", writeThroughput / 60));
                log.info("");
                log.info("[ 목표 대비 ]");
                log.info("  목표                    : 50,000건/분 (833.3건/초)");
                log.info("  실제                    : {}/분 ({}/초)",
                    String.format("%,.0f", readThroughput),
                    String.format("%,.1f", readThroughput / 60));
                log.info("  달성률                  : {}%",
                    String.format("%.1f", (readThroughput / 50000.0) * 100));
                log.info("");
                log.info("[ 누적 처리량 (앱 시작 후) ]");
                log.info("  총 Payment 처리          : {} 건", String.format("%,d", totalPaymentProcessed));
                log.info("  총 Settlement 생성       : {} 건", String.format("%,d", totalSettlementCreated));
                log.info("");
                log.info("================================================================================");
                log.info("");

                return org.springframework.batch.core.ExitStatus.COMPLETED;
            }
        };
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<PaymentSettlementDto> paymentSettlementReader(
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId) {

        if (minId == null || maxId == null || (minId == 0 && maxId == 0)) {
            log.info("[Reader] 빈 파티션 - 처리 대상 없음");
            return new JdbcCursorItemReaderBuilder<PaymentSettlementDto>()
                    .dataSource(dataSource)
                    .name("paymentSettlementReader")
                    .sql("SELECT * FROM payment WHERE 1=0")  // 빈 결과
                    .rowMapper((rs, rowNum) -> null)
                    .build();
        }

        log.info("[Reader] 초기화 - 파티션 범위: {} ~ {}", minId, maxId);

        String sql = """
            SELECT
                p.payment_id AS paymentId,
                p.pg_transaction_id AS pgTransactionId,
                p.pg_provider AS pgProvider,
                p.method AS paymentMethod,
                p.order_id AS orderId,
                pba.brand_id AS brandId,
                pba.amount AS totalAmount,
                pba.commission_rate AS commissionRate
            FROM payment p
            INNER JOIN payment_brand_amount pba ON p.payment_id = pba.payment_id
            WHERE p.settled_at IS NULL
              AND p.payment_status = 'APPROVED'
              AND p.payment_id BETWEEN ? AND ?
            """;

        return new JdbcCursorItemReaderBuilder<PaymentSettlementDto>()
                .dataSource(dataSource)
                .name("paymentSettlementReader")
                .sql(sql)
                .preparedStatementSetter(ps -> {
                    ps.setLong(1, minId);
                    ps.setLong(2, maxId);
                })
                .rowMapper((rs, rowNum) -> new PaymentSettlementDto(
                        rs.getLong("paymentId"),
                        rs.getString("pgTransactionId"),
                        rs.getString("pgProvider"),
                        rs.getString("paymentMethod"),
                        rs.getLong("orderId"),
                        rs.getLong("brandId"),
                        rs.getBigDecimal("totalAmount"),
                        rs.getBigDecimal("commissionRate")
                ))
                .build();
    }

    @Bean
    @StepScope
    public org.springframework.batch.item.ItemProcessor<PaymentSettlementDto, SettlementPerTransaction> settlementProcessor() {
        return dto -> {
            try {
                log.debug("[Processor] DTO 처리 시작 - PaymentId: {}, BrandId: {}, Amount: {}",
                        dto.getPaymentId(), dto.getBrandId(), dto.getTotalAmount());

                Money transactionAmount = new Money(dto.getTotalAmount());
                Money pgFeeAmount = pgFeeCalculator.calculate(
                        dto.getPgProvider(),
                        dto.getPaymentMethod(),
                        transactionAmount
                );

                SettlementPerTransaction settlement = SettlementPerTransaction.createTransaction(
                        dto.getBrandId(),
                        dto.getPaymentId(),
                        dto.getPgTransactionId(),
                        transactionAmount,
                        dto.getCommissionRate(),
                        pgFeeAmount,
                        TransactionType.ORDER,
                        "Asia/Seoul"
                );

                log.debug("[Processor] Settlement 생성 완료 - PaymentId: {}, BrandId: {}, FinalAmount: {}",
                        dto.getPaymentId(), dto.getBrandId(), settlement.calculateFinalSettlementAmount());

                return settlement;
            } catch (Exception e) {
                log.error("[Processor] Settlement 생성 실패 - PaymentId: {}, BrandId: {}, Error: {}",
                        dto.getPaymentId(), dto.getBrandId(), e.getMessage(), e);
                slackNotificationService.sendSettlementCreationFailure(dto.getPaymentId(), e);
                throw e;
            }
        };
    }

    @Bean
    @StepScope
    public ItemWriter<SettlementPerTransaction> settlementWriter() {
        return settlements -> {
            long start = System.currentTimeMillis();

            List<SettlementPerTransaction> settlementList = new ArrayList<>();
            settlements.forEach(settlementList::add);

            if (settlementList.isEmpty()) {
                log.debug("[Writer] 처리할 Settlement 없음");
                return;
            }

            long writeStart = System.currentTimeMillis();
            List<Long> paymentIds = settlementList.stream()
                    .map(SettlementPerTransaction::getPaymentId)
                    .distinct()
                    .collect(Collectors.toList());

            log.info("[Writer] Chunk 저장 시작 - Payment {}건, Settlement {}건",
                    paymentIds.size(), settlementList.size());

            jdbcTemplate.execute("SET UNIQUE_CHECKS=0");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");

            try {
            String insertSql = """
                INSERT INTO settlements_per_transaction
                (brand_id, payment_id, pg_transaction_id, transaction_type,
                 transaction_amount, commission_rate, commission_amount, tax_amount, pg_fee_amount,
                 transaction_date, transaction_date_local, timezone_offset, aggregation_status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            jdbcTemplate.batchUpdate(insertSql, settlementList, settlementList.size(),
                (PreparedStatement ps, SettlementPerTransaction s) -> {
                    ps.setLong(1, s.getBrandId());
                    ps.setLong(2, s.getPaymentId());
                    ps.setString(3, s.getPgTransactionId());
                    ps.setString(4, s.getTransactionType().name());
                    ps.setBigDecimal(5, s.getTransactionAmount().getAmount());
                    ps.setBigDecimal(6, s.getCommissionRate());
                    ps.setBigDecimal(7, s.getCommissionAmount().getAmount());
                    ps.setBigDecimal(8, s.getTaxAmount().getAmount());
                    ps.setBigDecimal(9, s.getPgFeeAmount().getAmount());
                    ps.setTimestamp(10, Timestamp.valueOf(s.getTransactionDate()));
                    ps.setDate(11, java.sql.Date.valueOf(s.getTransactionDateLocal()));
                    ps.setString(12, s.getTimezoneOffset());
                    ps.setString(13, s.getAggregationStatus().name());
                    ps.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
                });
            log.debug("[Writer] Settlement JDBC Batch INSERT 완료 - {}건", settlementList.size());

            String updateSql = "UPDATE payment SET settled_at = ?, updated_at = ? WHERE payment_id = ?";
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            jdbcTemplate.batchUpdate(updateSql, paymentIds, paymentIds.size(),
                (PreparedStatement ps, Long paymentId) -> {
                    ps.setTimestamp(1, now);
                    ps.setTimestamp(2, now);
                    ps.setLong(3, paymentId);
                });
            log.debug("[Writer] Payment settled_at JDBC Batch UPDATE 완료 - {}건", paymentIds.size());

            detailedPerformanceListener.addWriteTime(System.currentTimeMillis() - writeStart);

            log.info("[Writer] Chunk 완료 - {}ms, Settlement {}건",
                    System.currentTimeMillis() - start, settlementList.size());
            } finally {
                jdbcTemplate.execute("SET UNIQUE_CHECKS=1");
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
            }
        };
    }
}