package com.mudosa.musinsa.payment.controller;

import com.mudosa.musinsa.common.dto.ApiResponse;
import com.mudosa.musinsa.common.vo.Money;
import com.mudosa.musinsa.order.domain.model.Order;
import com.mudosa.musinsa.order.domain.model.OrderProduct;
import com.mudosa.musinsa.order.domain.model.OrderStatus;
import com.mudosa.musinsa.order.domain.repository.OrderRepository;
import com.mudosa.musinsa.payment.domain.model.Payment;
import com.mudosa.musinsa.payment.domain.model.PaymentStatus;
import com.mudosa.musinsa.payment.domain.model.PgProvider;
import com.mudosa.musinsa.payment.domain.repository.PaymentRepository;
import com.mudosa.musinsa.product.domain.model.ProductOption;
import com.mudosa.musinsa.product.domain.repository.ProductOptionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Payment 테스트용 컨트롤러
 *
 * ⚠️ 테스트 환경에서만 활성화됩니다!
 * application.yml에서 test.endpoints.enabled=true 설정 필요
 *
 * 토스 목서버 없이 Payment를 직접 생성하여 Settlement 테스트 가능
 *
 * TODO: 성능 테스트 완료 후 삭제 필요
 *       - PaymentTestController.java 파일 전체 삭제
 *       - application.yml의 test.endpoints.enabled 설정 제거
 *       - 프로덕션 배포 전 반드시 제거할 것!
 */
@Slf4j
@Tag(name = "Payment Test", description = "결제 테스트 API (테스트 환경 전용)")
@RestController
@RequestMapping("/api/test/payments")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "test.endpoints.enabled", havingValue = "true")
public class PaymentTestController {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductOptionRepository productOptionRepository;

    // ProductOption 캐시 (성능 개선)
    private volatile List<Long> cachedProductOptionIds = new ArrayList<>();
    private AtomicInteger productOptionIndex = new AtomicInteger(0);

    /**
     * 캐시된 ProductOption ID를 Round-robin 방식으로 반환하고 조회
     * @Transactional 안에서 실행되어 Lazy Loading 문제 해결
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    private ProductOption getNextProductOption() {
        // Lazy initialization: 첫 요청 시 한 번만 초기화
        if (cachedProductOptionIds.isEmpty()) {
            synchronized (this) {
                if (cachedProductOptionIds.isEmpty()) {
                    initProductOptionIds();
                }
            }
        }

        if (cachedProductOptionIds.isEmpty()) {
            throw new IllegalStateException("사용 가능한 ProductOption이 없습니다. DB에 데이터를 확인하세요.");
        }

        // Round-robin으로 ID 선택
        int index = productOptionIndex.getAndIncrement() % cachedProductOptionIds.size();
        Long productOptionId = cachedProductOptionIds.get(index);

        // 트랜잭션 내에서 JOIN FETCH로 조회 (Lazy Loading 문제 해결)
        return productOptionRepository.findByIdWithProductAndInventory(productOptionId)
            .orElseThrow(() -> new IllegalStateException("ProductOption을 찾을 수 없습니다: " + productOptionId));
    }

    /**
     * ProductOption ID 목록을 초기화 (최대 100개만)
     * 전체 63만개를 로딩하는 대신 일부만 사용
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    private void initProductOptionIds() {
        try {
            // 네이티브 쿼리로 재고가 있는 ProductOption ID를 100개만 조회
            List<ProductOption> options = productOptionRepository.findAll()
                .stream()
                .limit(100)
                .toList();

            cachedProductOptionIds = options.stream()
                .filter(po -> {
                    try {
                        return po.getInventory() != null
                            && po.getInventory().getStockQuantity() != null
                            && po.getInventory().getStockQuantity().getValue() > 0;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(ProductOption::getProductOptionId)
                .toList();

            if (cachedProductOptionIds.isEmpty()) {
                log.warn("⚠️ 재고가 있는 ProductOption이 없습니다. 테스트 Payment 생성이 실패할 수 있습니다.");
            } else {
                log.info("✅ ProductOption ID 캐시 초기화 완료 - {}개 상품 ID 로드됨", cachedProductOptionIds.size());
            }
        } catch (Exception e) {
            log.error("❌ ProductOption ID 캐시 초기화 실패", e);
            cachedProductOptionIds = new ArrayList<>();
        }
    }

    @Operation(
        summary = "테스트용 Payment 생성 (완전한 Order 구조)",
        description = "실제 프로덕션과 동일한 Order 구조로 Payment를 생성합니다. Settlement 테스트용."
    )
    @PostMapping("/create-direct")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTestPayment(
        @RequestParam(defaultValue = "10000") Long amount,
        @RequestParam(defaultValue = "CARD") String method,
        @RequestParam(required = false) Long orderId,
        @RequestParam(required = false) Long userId
    ) {
        try {
            // 1. Order 조회 또는 생성
            Order order;
            if (orderId != null) {
                // 기존 Order 사용
                order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

                log.info("✅ 기존 Order 사용 - OrderId: {}", order.getId());
            } else {
                // 완전한 Order 생성 (실제 프로덕션과 동일)
                if (userId == null) {
                    userId = 1L; // 기본 userId
                }

                // 1-1. 캐시에서 ProductOption 조회 (성능 개선: DB 조회 제거)
                ProductOption productOption = getNextProductOption();

                // 1-2. Order 생성 (UUID 추가로 중복 방지)
                String orderNo = "TEST_ORD_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
                order = Order.builder()
                    .userId(userId)
                    .orderNo(orderNo)
                    .status(OrderStatus.PENDING)
                    .totalPrice(new Money(BigDecimal.valueOf(amount)))
                    .totalDiscount(Money.ZERO)
                    .build();

                // 1-3. OrderProduct 생성 및 연결 (핵심!)
                OrderProduct orderProduct = OrderProduct.builder()
                    .productOption(productOption)
                    .productPrice(new Money(BigDecimal.valueOf(amount)))
                    .productQuantity(1)
                    .build();

                // 양방향 관계 설정 (테스트용 setter 사용)
                orderProduct.setOrderForTest(order);
                order.getOrderProducts().add(orderProduct);

                // 1-4. Order 저장 (OrderProduct도 함께 저장됨 - CascadeType.ALL)
                order = orderRepository.save(order);

                log.info("✅ 완전한 Order 생성 완료 - OrderId: {}, OrderProducts: {}개, ProductOptionId: {}",
                    order.getId(),
                    order.getOrderProducts().size(),
                    productOption.getProductOptionId());
            }

            // 2. Payment 생성
            String pgTransactionId = "test_pg_" + UUID.randomUUID().toString();

            Payment payment = Payment.create(
                order.getId(),
                BigDecimal.valueOf(amount),
                PgProvider.TOSS,
                order.getUserId()
            );

            // 3. Payment 승인 처리
            payment.approve(
                pgTransactionId,
                order.getUserId(),
                LocalDateTime.now(),
                method
            );

            // 4. 저장
            paymentRepository.save(payment);

            log.info("✅ 테스트 Payment 생성 완료 - PaymentId: {}, Amount: {}, OrderId: {}",
                payment.getId(), amount, order.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", payment.getId());
            response.put("orderId", order.getId());
            response.put("amount", amount);
            response.put("pgTransactionId", pgTransactionId);
            response.put("status", "APPROVED");
            response.put("hasOrderProducts", !order.getOrderProducts().isEmpty());
            response.put("orderProductCount", order.getOrderProducts().size());

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("❌ 테스트 Payment 생성 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.failure("PAYMENT_CREATE_FAILED", "Payment 생성 실패: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "대량 테스트 Payment 생성",
        description = "Settlement 성능 테스트를 위해 여러 Payment를 한번에 생성합니다."
    )
    @PostMapping("/create-bulk")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBulkTestPayments(
        @RequestParam(defaultValue = "100") int count,
        @RequestParam(required = true) Long orderId
    ) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < count; i++) {
                try {
                    String pgTransactionId = "test_pg_bulk_" + UUID.randomUUID().toString();

                    Payment payment = Payment.create(
                        order.getId(),
                        order.getTotalPrice().getAmount(),
                        PgProvider.TOSS,
                        order.getUserId()
                    );

                    payment.approve(
                        pgTransactionId,
                        order.getUserId(),
                        LocalDateTime.now(),
                        "CARD"
                    );

                    paymentRepository.save(payment);
                    successCount++;

                } catch (Exception e) {
                    log.error("Payment 생성 실패 ({}번째)", i + 1, e);
                    failCount++;
                }
            }

            log.info("✅ 대량 Payment 생성 완료 - 성공: {}건, 실패: {}건", successCount, failCount);

            Map<String, Object> response = new HashMap<>();
            response.put("requestCount", count);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("orderId", orderId);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("❌ 대량 Payment 생성 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.failure("PAYMENT_BULK_CREATE_FAILED", "대량 Payment 생성 실패: " + e.getMessage()));
        }
    }
}