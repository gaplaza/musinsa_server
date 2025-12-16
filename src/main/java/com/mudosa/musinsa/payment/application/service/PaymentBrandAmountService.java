package com.mudosa.musinsa.payment.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PaymentBrandAmount 관리 서비스
 * - 결제 승인 시 브랜드별 금액을 미리 계산하여 저장
 * - 정산 배치 성능 최적화용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentBrandAmountService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 결제 승인 시 브랜드별 금액 저장
     * - 복잡한 JOIN을 미리 계산해서 저장
     * - 정산 배치에서 빠른 조회 가능
     *
     * @param paymentId 결제 ID
     * @param orderId   주문 ID
     */
    @Transactional
    public void saveForPayment(Long paymentId, Long orderId) {
        log.info("[PaymentBrandAmount] 저장 시작 - paymentId: {}, orderId: {}", paymentId, orderId);

        String sql = """
                INSERT INTO payment_brand_amount (payment_id, brand_id, amount, commission_rate)
                SELECT
                    ? as payment_id,
                    p.brand_id,
                    SUM(op.product_price * op.product_quantity) as amount,
                    b.commission_rate
                FROM orders o
                JOIN order_product op ON o.order_id = op.order_id
                JOIN product_option po ON op.product_option_id = po.product_option_id
                JOIN product p ON po.product_id = p.product_id
                JOIN brand b ON p.brand_id = b.brand_id
                WHERE o.order_id = ?
                GROUP BY p.brand_id, b.commission_rate
                """;

        try {
            int inserted = jdbcTemplate.update(sql, paymentId, orderId);
            log.info("[PaymentBrandAmount] 저장 완료 - paymentId: {}, 브랜드 수: {}", paymentId, inserted);
        } catch (Exception e) {
            log.error("[PaymentBrandAmount] 저장 실패 - paymentId: {}, orderId: {}", paymentId, orderId, e);
            throw new RuntimeException("결제 브랜드별 금액 저장 실패", e);
        }
    }

    /**
     * 이미 저장되어 있는지 확인
     */
    public boolean existsForPayment(Long paymentId) {
        String sql = "SELECT COUNT(*) FROM payment_brand_amount WHERE payment_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, paymentId);
        return count != null && count > 0;
    }
}