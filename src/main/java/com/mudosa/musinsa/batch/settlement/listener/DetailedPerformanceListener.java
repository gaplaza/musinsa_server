package com.mudosa.musinsa.batch.settlement.listener;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetailedPerformanceListener implements ChunkListener, StepExecutionListener {

    private final MeterRegistry meterRegistry;

    private long stepStartTime;
    private long chunkStartTime;
    private long lastLogTime;

    private AtomicInteger totalChunks = new AtomicInteger(0);
    private AtomicLong totalChunkTime = new AtomicLong(0);
    private AtomicLong totalItems = new AtomicLong(0);

    private long minChunkTime = Long.MAX_VALUE;
    private long maxChunkTime = 0;

    private AtomicLong totalReadTime = new AtomicLong(0);
    private AtomicLong totalProcessTime = new AtomicLong(0);
    private AtomicLong totalWriteTime = new AtomicLong(0);

    private static final long LOG_INTERVAL_MS = 5000;

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("settlement.creation.read.seconds", () -> totalReadTime.get() / 1000.0)
            .description("Total READ time in Settlement Creation batch (seconds)")
            .register(meterRegistry);

        Gauge.builder("settlement.creation.process.seconds", () -> totalProcessTime.get() / 1000.0)
            .description("Total PROCESS time in Settlement Creation batch (seconds)")
            .register(meterRegistry);

        Gauge.builder("settlement.creation.write.seconds", () -> totalWriteTime.get() / 1000.0)
            .description("Total WRITE time in Settlement Creation batch (seconds)")
            .register(meterRegistry);

        Gauge.builder("settlement.creation.total.seconds", () -> (totalReadTime.get() + totalProcessTime.get() + totalWriteTime.get()) / 1000.0)
            .description("Total duration of Settlement Creation batch (seconds)")
            .register(meterRegistry);

        log.info("Settlement Creation component metrics registered");
    }

    public void addReadTime(long millis) {
        totalReadTime.addAndGet(millis);
    }

    public void addProcessTime(long millis) {
        totalProcessTime.addAndGet(millis);
    }

    public void addWriteTime(long millis) {
        totalWriteTime.addAndGet(millis);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepStartTime = System.currentTimeMillis();
        lastLogTime = stepStartTime;

        totalChunks.set(0);
        totalChunkTime.set(0);
        totalItems.set(0);
        minChunkTime = Long.MAX_VALUE;
        maxChunkTime = 0;

        totalReadTime.set(0);
        totalProcessTime.set(0);
        totalWriteTime.set(0);
    }

    @Override
    public void beforeChunk(ChunkContext context) {
        chunkStartTime = System.currentTimeMillis();
    }

    @Override
    public void afterChunk(ChunkContext context) {
        long chunkEndTime = System.currentTimeMillis();
        long chunkDuration = chunkEndTime - chunkStartTime;

        int chunkNumber = totalChunks.incrementAndGet();
        totalChunkTime.addAndGet(chunkDuration);

        StepExecution stepExecution = context.getStepContext().getStepExecution();
        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long commitCount = stepExecution.getCommitCount();

        totalItems.set(readCount);

        if (chunkDuration < minChunkTime) {
            minChunkTime = chunkDuration;
        }
        if (chunkDuration > maxChunkTime) {
            maxChunkTime = chunkDuration;
        }

        double avgChunkTime = totalChunkTime.get() / (double) chunkNumber;

        long elapsedTime = chunkEndTime - stepStartTime;
        double elapsedSeconds = elapsedTime / 1000.0;
        double currentTps = elapsedSeconds > 0 ? readCount / elapsedSeconds : 0;
        double currentTpm = currentTps * 60;

        double chunkTps = chunkDuration > 0 ? (readCount / commitCount) / (chunkDuration / 1000.0) : 0;

        if (chunkEndTime - lastLogTime >= LOG_INTERVAL_MS) {
            log.info("--------------------------------------------------------------------------------");
            log.info("[ Chunk #{} 완료 ]", chunkNumber);
            log.info("  처리 시간               : {}ms", chunkDuration);
            log.info("  평균 Chunk 시간          : {}ms", String.format("%.1f", avgChunkTime));
            log.info("  최소/최대 Chunk 시간     : {} / {}ms", minChunkTime, maxChunkTime);
            log.info("");
            log.info("[ 누적 통계 ]");
            log.info("  Read  (읽기)            : {} 건", readCount);
            log.info("  Write (쓰기)            : {} 건", writeCount);
            log.info("  Commit                 : {} 회", commitCount);
            log.info("");
            log.info("[ 실시간 처리 속도 ]");
            log.info("  현재 TPS               : {}/초", String.format("%.1f", currentTps));
            log.info("  현재 TPM               : {}/분", String.format("%.0f", currentTpm));
            log.info("  Chunk TPS              : {}/초", String.format("%.1f", chunkTps));
            log.info("");
            log.info("[ 목표 대비 ]");
            log.info("  목표                   : 50,000/분 (833.3/초)");
            log.info("  현재                   : {}/분 ({}/초)",
                String.format("%.0f", currentTpm),
                String.format("%.1f", currentTps));
            double achievement = currentTpm > 0 ? (currentTpm / 50000.0) * 100 : 0;
            log.info("  달성률                 : {}%", String.format("%.1f", achievement));
            log.info("");

            lastLogTime = chunkEndTime;
        } else {
            log.debug("Chunk #{} 완료 - {}ms, TPS: {}/초",
                chunkNumber,
                chunkDuration,
                String.format("%.1f", currentTps));
        }
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        long chunkEndTime = System.currentTimeMillis();
        long chunkDuration = chunkEndTime - chunkStartTime;
        int chunkNumber = totalChunks.get() + 1;

        log.error("❌ Chunk #{} 실패 - 처리 시간: {}ms", chunkNumber, chunkDuration);
    }

    @Override
    public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {
        long stepEndTime = System.currentTimeMillis();
        long totalDuration = stepEndTime - stepStartTime;

        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long commitCount = stepExecution.getCommitCount();
        int chunkCount = totalChunks.get();

        if (readCount == 0) {
            log.info("[Performance] 처리 대상 0건 (소요: {}ms)", totalDuration);
            return org.springframework.batch.core.ExitStatus.COMPLETED;
        }

        double totalSeconds = totalDuration / 1000.0;
        double avgChunkTime = chunkCount > 0 ? totalChunkTime.get() / (double) chunkCount : 0;

        double finalTps = totalSeconds > 0 ? readCount / totalSeconds : 0;
        double finalTpm = finalTps * 60;

        double estimatedReaderTime = totalDuration * 0.2;
        double estimatedProcessorTime = totalDuration * 0.5;
        double estimatedWriterTime = totalDuration * 0.3;

        log.info("");
        log.info("================================================================================");
        log.info("배치 성능 측정 완료");
        log.info("================================================================================");
        log.info("");
        log.info("[ 전체 처리 결과 ]");
        log.info("  Read  (읽기)            : {} 건", readCount);
        log.info("  Write (쓰기)            : {} 건", writeCount);
        log.info("  Commit                 : {} 회", commitCount);
        log.info("  Chunk                  : {} 회", chunkCount);
        log.info("");
        log.info("[ 소요 시간 ]");
        log.info("  Total Duration         : {}초", String.format("%.3f", totalSeconds));
        log.info("  평균 Chunk 시간         : {}ms", String.format("%.1f", avgChunkTime));
        log.info("  최소 Chunk 시간         : {}ms", minChunkTime != Long.MAX_VALUE ? minChunkTime : 0);
        log.info("  최대 Chunk 시간         : {}ms", maxChunkTime);
        log.info("");
        long readTime = totalReadTime.get();
        long processTime = totalProcessTime.get();
        long writeTime = totalWriteTime.get();

        if (readTime > 0 || processTime > 0 || writeTime > 0) {
            log.info("[ 컴포넌트별 시간 (실측값) ]");
            log.info("  READ   (Payment 조회)   : {}초", String.format("%.3f", readTime / 1000.0));
            log.info("  PROCESS (건별 계산)     : {}초", String.format("%.3f", processTime / 1000.0));
            log.info("  WRITE  (건별 저장)      : {}초", String.format("%.3f", writeTime / 1000.0));
            log.info("  합계                   : {}초", String.format("%.3f", (readTime + processTime + writeTime) / 1000.0));
        } else {
            log.info("[ 컴포넌트별 시간 (추정치) ]");
            log.info("  주의: 아래 시간은 경험적 비율 기반 추정치입니다.");
            log.info("  Reader  (읽기 20%)      : {}초", String.format("%.3f", estimatedReaderTime / 1000));
            log.info("  Processor (처리 50%)    : {}초", String.format("%.3f", estimatedProcessorTime / 1000));
            log.info("  Writer  (쓰기 30%)      : {}초", String.format("%.3f", estimatedWriterTime / 1000));
        }
        log.info("");
        log.info("[ 처리 속도 ]");
        log.info("  TPS (초당 처리)         : {}/초", String.format("%.1f", finalTps));
        log.info("  TPM (분당 처리)         : {}/분", String.format("%.0f", finalTpm));
        log.info("  평균 Chunk 처리 속도     : {}/초",
            String.format("%.1f", avgChunkTime > 0 ? (readCount / commitCount) / (avgChunkTime / 1000.0) : 0));
        log.info("");
        log.info("[ 목표 대비 ]");
        log.info("  목표                   : 50,000/분 (833.3/초)");
        log.info("  실제                   : {}/분 ({}/초)",
            String.format("%.0f", finalTpm),
            String.format("%.1f", finalTps));
        double achievement = finalTpm > 0 ? (finalTpm / 50000.0) * 100 : 0;
        log.info("  달성률                 : {}%", String.format("%.1f", achievement));
        log.info("");
        log.info("[ Thread Pool 상태 (근거 있는 측정치) ]");
        log.info("  활성 스레드 수          : {}", Thread.activeCount());
        log.info("  현재 스레드 이름         : {}", Thread.currentThread().getName());
        log.info("");
        log.info("================================================================================");
        log.info("");

        return org.springframework.batch.core.ExitStatus.COMPLETED;
    }
}
