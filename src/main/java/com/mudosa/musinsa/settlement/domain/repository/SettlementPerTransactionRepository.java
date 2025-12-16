package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.domain.model.AggregationStatus;
import com.mudosa.musinsa.settlement.domain.model.SettlementPerTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SettlementPerTransactionRepository
    extends JpaRepository<SettlementPerTransaction, Long> {

    Page<SettlementPerTransaction> findByBrandIdAndTransactionDateLocalBetween(
        Long brandId,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
    );

    List<SettlementPerTransaction> findByAggregationStatus(AggregationStatus status);

    long countByAggregationStatus(AggregationStatus status);

    boolean existsByPaymentId(Long paymentId);

    int countByPaymentId(Long paymentId);

    List<SettlementPerTransaction> findByPaymentId(Long paymentId);
}
