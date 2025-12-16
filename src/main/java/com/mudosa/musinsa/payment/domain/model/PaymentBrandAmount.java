package com.mudosa.musinsa.payment.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제별 브랜드 금액 (정산 배치 성능 최적화용)
 * - 결제 시점에 브랜드별 금액을 미리 계산해서 저장
 * - 정산 배치에서 JOIN 5개 → 1개로 감소
 */
@Entity
@Table(name = "payment_brand_amount")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentBrandAmount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public PaymentBrandAmount(Long paymentId, Long brandId, BigDecimal amount, BigDecimal commissionRate) {
        this.paymentId = paymentId;
        this.brandId = brandId;
        this.amount = amount;
        this.commissionRate = commissionRate;
    }

    public static PaymentBrandAmount of(Long paymentId, Long brandId, BigDecimal amount, BigDecimal commissionRate) {
        return PaymentBrandAmount.builder()
                .paymentId(paymentId)
                .brandId(brandId)
                .amount(amount)
                .commissionRate(commissionRate)
                .build();
    }
}