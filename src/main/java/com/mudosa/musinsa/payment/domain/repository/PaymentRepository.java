package com.mudosa.musinsa.payment.domain.repository;

import com.mudosa.musinsa.payment.domain.model.Payment;
import com.mudosa.musinsa.payment.domain.model.PaymentStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Payment findByPgTransactionId(String paymentTransactionId);

    /**
     * 정산 미생성 + 승인 완료 결제 조회 (Batch Polling용)
     * settledAt이 null이면 정산 미처리 상태
     * FOR UPDATE SKIP LOCKED로 동시 처리 시 충돌 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")}) // SKIP LOCKED
    @Query("SELECT p FROM Payment p WHERE p.settledAt IS NULL AND p.status = :status ORDER BY p.id ASC")
    Page<Payment> findPendingSettlementPayments(@Param("status") PaymentStatus status, Pageable pageable);

    /**
     * 정산 미생성 결제 수 조회
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.settledAt IS NULL AND p.status = :status")
    long countPendingSettlementPayments(@Param("status") PaymentStatus status);

    /**
     * 미정산 결제 건수 조회 (테스트용)
     * TODO: 성능 테스트 완료 후 삭제 필요
     */
    Long countBySettledAtIsNull();
}
