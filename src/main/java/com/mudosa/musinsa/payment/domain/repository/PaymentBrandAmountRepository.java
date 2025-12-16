package com.mudosa.musinsa.payment.domain.repository;

import com.mudosa.musinsa.payment.domain.model.PaymentBrandAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentBrandAmountRepository extends JpaRepository<PaymentBrandAmount, Long> {

    /**
     * 특정 결제의 브랜드별 금액 조회
     */
    List<PaymentBrandAmount> findByPaymentId(Long paymentId);

    /**
     * 특정 결제의 브랜드별 금액 존재 여부
     */
    boolean existsByPaymentId(Long paymentId);
}
