package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.domain.model.SettlementMonthly;
import com.mudosa.musinsa.settlement.domain.model.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SettlementMonthlyRepository extends JpaRepository<SettlementMonthly, Long>, SettlementMonthlyRepositoryCustom {

    
    java.util.Optional<SettlementMonthly> findByBrandIdAndSettlementYearAndSettlementMonth(
        Long brandId,
        Integer settlementYear,
        Integer settlementMonth
    );

    
    @Query("SELECT m FROM SettlementMonthly m " +
           "WHERE m.settlementStatus = :status " +
           "AND FUNCTION('LAST_DAY', FUNCTION('DATE', CONCAT(m.settlementYear, '-', m.settlementMonth, '-01'))) < :cutoffDate")
    List<SettlementMonthly> findBySettlementStatusAndMonthEndDateBefore(
        @Param("status") SettlementStatus status,
        @Param("cutoffDate") LocalDate cutoffDate
    );
}
