package com.mudosa.musinsa.settlement.presentation.controller;

import com.mudosa.musinsa.brand.domain.repository.BrandMemberRepository;
import com.mudosa.musinsa.common.dto.ApiResponse;
import com.mudosa.musinsa.common.vo.Money;
import com.mudosa.musinsa.payment.domain.model.Payment;
import com.mudosa.musinsa.payment.domain.repository.PaymentRepository;
import com.mudosa.musinsa.settlement.domain.model.SettlementPerTransaction;
import com.mudosa.musinsa.settlement.domain.model.TransactionType;
import com.mudosa.musinsa.settlement.domain.repository.SettlementPerTransactionRepository;
import com.mudosa.musinsa.settlement.domain.service.PgFeeCalculator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Tag(name = "Settlement Test", description = "정산 테스트 API (테스트 환경 전용)")
@RestController
@RequestMapping("/api/test/settlements")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "test.endpoints.enabled", havingValue = "true")
public class SettlementTestController {

    private final SettlementPerTransactionRepository settlementRepository;
    private final PaymentRepository paymentRepository;
    private final BrandMemberRepository brandMemberRepository;
    private final PgFeeCalculator pgFeeCalculator;
    private final com.mudosa.musinsa.batch.settlement.service.SettlementAggregationService settlementAggregationService;

    private volatile List<Long> cachedBrandIds = new ArrayList<>();
    private AtomicInteger brandIndex = new AtomicInteger(0);

    @Transactional(readOnly = true)
    private Long getNextBrandId() {
        if (cachedBrandIds.isEmpty()) {
            synchronized (this) {
                if (cachedBrandIds.isEmpty()) {
                    initBrandIds();
                }
            }
        }

        if (cachedBrandIds.isEmpty()) {
            throw new IllegalStateException("사용 가능한 Brand가 없습니다. DB에 데이터를 확인하세요.");
        }

        int index = brandIndex.getAndIncrement() % cachedBrandIds.size();
        return cachedBrandIds.get(index);
    }

    @Transactional(readOnly = true)
    private void initBrandIds() {
        try {
            List<Long> brandIds = brandMemberRepository.findAll()
                .stream()
                .map(bm -> bm.getBrand().getBrandId())
                .distinct()
                .limit(4000)
                .toList();

            cachedBrandIds = new ArrayList<>(brandIds);

            if (cachedBrandIds.isEmpty()) {
                log.warn("⚠️ 사용 가능한 Brand가 없습니다. 테스트 Settlement 생성이 실패할 수 있습니다.");
            } else {
                log.info("✅ Brand ID 캐시 초기화 완료 - {}개 브랜드 ID 로드됨", cachedBrandIds.size());
            }
        } catch (Exception e) {
            log.error("❌ Brand ID 캐시 초기화 실패", e);
            cachedBrandIds = new ArrayList<>();
        }
    }

    @Operation(
        summary = "테스트용 SettlementPerTransaction 대량 생성",
        description = "배치 성능 테스트를 위해 SettlementPerTransaction을 대량 생성합니다. " +
                "실제 Payment와 연결되지 않으며, 순수 배치 처리 성능 측정용입니다."
    )
    @PostMapping("/create-bulk")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBulkSettlements(
        @RequestParam(defaultValue = "1000") int count,
        @RequestParam(defaultValue = "10000") Long amount,
        @RequestParam(defaultValue = "10.5") BigDecimal commissionRate
    ) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("===============================================================================");
            log.info("대량 Settlement 생성 시작");
            log.info("===============================================================================");
            log.info("[ 생성 설정 ]");
            log.info("  생성 개수              : {}건", count);
            log.info("  거래 금액              : {}원", amount);
            log.info("  수수료율              : {}%", commissionRate);
            log.info("");

            int successCount = 0;
            int failCount = 0;
            List<Long> sampleIds = new ArrayList<>();

            List<SettlementPerTransaction> settlements = new ArrayList<>();

            Money transactionAmount = new Money(BigDecimal.valueOf(amount));
            Money pgFeeAmount = pgFeeCalculator.calculate("TOSS", "CARD", transactionAmount);
            long baseTimestamp = System.currentTimeMillis();
            String baseUuid = UUID.randomUUID().toString().substring(0, 24);

            for (int i = 0; i < count; i++) {
                try {
                    Long brandId = getNextBrandId();

                    Long fakePaymentId = baseTimestamp + i;
                    String pgTransactionId = "test_settlement_" + baseUuid + "_" + i;

                    SettlementPerTransaction settlement = SettlementPerTransaction.createTransaction(
                        brandId,
                        fakePaymentId,
                        pgTransactionId,
                        transactionAmount,
                        commissionRate,
                        pgFeeAmount,
                        TransactionType.ORDER,
                        "Asia/Seoul"
                    );

                    settlements.add(settlement);
                    successCount++;

                    if (settlements.size() >= 1000) {
                        settlementRepository.saveAll(settlements);
                        if (sampleIds.size() < 10) {
                            settlements.stream()
                                .limit(10 - sampleIds.size())
                                .forEach(s -> {
                                    if (s.getId() != null) sampleIds.add(s.getId());
                                });
                        }
                        settlements.clear();
                    }

                } catch (Exception e) {
                    log.error("Settlement 생성 실패 ({}번째)", i + 1, e);
                    failCount++;
                }
            }

            if (!settlements.isEmpty()) {
                settlementRepository.saveAll(settlements);
                if (sampleIds.size() < 10) {
                    settlements.stream()
                        .limit(10 - sampleIds.size())
                        .forEach(s -> {
                            if (s.getId() != null) sampleIds.add(s.getId());
                        });
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            double durationSeconds = duration / 1000.0;
            double perSecond = durationSeconds > 0 ? successCount / durationSeconds : 0;

            log.info("");
            log.info("[ 처리 결과 ]");
            log.info("  생성 성공              : {}건", successCount);
            log.info("  생성 실패              : {}건", failCount);
            log.info("");
            log.info("[ 소요 시간 ]");
            log.info("  Total Duration         : {}초", String.format("%.3f", durationSeconds));
            log.info("");
            log.info("[ 처리 속도 ]");
            log.info("  생성 속도              : {}/초", String.format("%.1f", perSecond));
            log.info("");
            log.info("===============================================================================");
            log.info("");

            Map<String, Object> response = new HashMap<>();
            response.put("requestCount", count);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("durationSeconds", String.format("%.3f", durationSeconds));
            response.put("throughputPerSecond", String.format("%.1f", perSecond));
            response.put("sampleIds", sampleIds);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("❌ 대량 Settlement 생성 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.failure("SETTLEMENT_BULK_CREATE_FAILED",
                    "Settlement 생성 실패: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "테스트 Settlement 정리",
        description = "생성된 테스트 Settlement를 모두 삭제합니다."
    )
    @DeleteMapping("/cleanup")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupTestSettlements() {
        try {
            List<SettlementPerTransaction> testSettlements = settlementRepository.findAll()
                .stream()
                .filter(s -> s.getPgTransactionId().startsWith("test_settlement_"))
                .toList();

            int deleteCount = testSettlements.size();
            settlementRepository.deleteAll(testSettlements);

            log.info("✅ 테스트 Settlement {}건 삭제 완료", deleteCount);

            Map<String, Object> response = new HashMap<>();
            response.put("deletedCount", deleteCount);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("❌ 테스트 Settlement 정리 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.failure("SETTLEMENT_CLEANUP_FAILED",
                    "Settlement 정리 실패: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "NOT_AGGREGATED 건수 조회",
        description = "배치 처리 대기 중인 SettlementPerTransaction 건수를 반환합니다. " +
                "K6 성능 테스트에서 배치 진행 상황 모니터링용으로 사용됩니다."
    )
    @GetMapping("/not-aggregated-count")
    public ResponseEntity<Long> getNotAggregatedCount() {
        try {
            long count = settlementRepository.countByAggregationStatus(
                com.mudosa.musinsa.settlement.domain.model.AggregationStatus.NOT_AGGREGATED
            );
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("❌ NOT_AGGREGATED 건수 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "수동 분 단위 집계 트리거 (테스트용)",
        description = "스케줄러를 기다리지 않고 즉시 분 단위 집계를 실행합니다. " +
                "NOT_AGGREGATED 상태의 SettlementPerTransaction을 집계하여 Daily/Weekly/Monthly/Yearly로 변환합니다."
    )
    @PostMapping("/trigger-aggregation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerMinuteAggregation() {
        long startTime = System.currentTimeMillis();

        try {
            log.info("===============================================================================");
            log.info("수동 분 단위 집계 트리거 시작");
            log.info("===============================================================================");

            Map<String, Integer> result = settlementAggregationService.aggregateIncremental();

            long duration = System.currentTimeMillis() - startTime;
            double durationSeconds = duration / 1000.0;

            int insertCount = result.get("insertCount");
            int updateCount = result.get("updateCount");
            int totalCount = insertCount + updateCount;

            log.info("");
            log.info("[ 처리 결과 ]");
            log.info("  신규 Daily 생성         : {}건", insertCount);
            log.info("  기존 Daily 업데이트      : {}건", updateCount);
            log.info("  총 처리                 : {}건", totalCount);
            log.info("");
            log.info("[ 소요 시간 ]");
            log.info("  Total Duration          : {}초", String.format("%.3f", durationSeconds));
            log.info("");
            log.info("===============================================================================");
            log.info("");

            Map<String, Object> response = new HashMap<>();
            response.put("insertCount", insertCount);
            response.put("updateCount", updateCount);
            response.put("totalCount", totalCount);
            response.put("durationSeconds", String.format("%.3f", durationSeconds));
            response.put("durationMs", duration);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("❌ 수동 집계 트리거 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.failure("AGGREGATION_TRIGGER_FAILED",
                    "집계 트리거 실패: " + e.getMessage()));
        }
    }
}
