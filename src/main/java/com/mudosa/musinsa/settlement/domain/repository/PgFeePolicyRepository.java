package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.domain.model.PgFeePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PgFeePolicyRepository extends JpaRepository<PgFeePolicy, Long> {

    
    @Query("""
        SELECT p FROM PgFeePolicy p
        WHERE p.pgProvider = :pgProvider
          AND p.paymentMethod = :paymentMethod
          AND p.active = true
          AND p.effectiveFrom <= :targetDate
          AND (p.effectiveTo IS NULL OR p.effectiveTo >= :targetDate)
        ORDER BY p.effectiveFrom DESC
        LIMIT 1
        """)
    Optional<PgFeePolicy> findEffectivePolicy(
        @Param("pgProvider") String pgProvider,
        @Param("paymentMethod") String paymentMethod,
        @Param("targetDate") LocalDate targetDate
    );
}
