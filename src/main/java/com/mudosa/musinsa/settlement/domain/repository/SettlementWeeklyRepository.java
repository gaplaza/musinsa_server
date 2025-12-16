package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.domain.model.SettlementStatus;
import com.mudosa.musinsa.settlement.domain.model.SettlementWeekly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface SettlementWeeklyRepository extends JpaRepository<SettlementWeekly, Long> {

    
    List<SettlementWeekly> findByBrandIdOrderByWeekStartDateDesc(Long brandId);

    
    java.util.Optional<SettlementWeekly> findByBrandIdAndSettlementYearAndSettlementMonthAndWeekOfMonth(
        Long brandId,
        Integer year,
        Integer month,
        Integer weekOfMonth
    );

    
    List<SettlementWeekly> findByBrandIdAndWeekStartDateBetween(
        Long brandId,
        LocalDate startDate,
        LocalDate endDate
    );

    
    @Query("SELECT COALESCE(SUM(w.totalSalesAmount.amount), 0) as totalSalesAmount, " +
           "COALESCE(SUM(w.totalOrderCount), 0) as totalOrderCount " +
           "FROM SettlementWeekly w " +
           "WHERE w.weekStartDate >= :startDate AND w.weekStartDate <= :endDate")
    Map<String, Object> sumAllByWeekStartDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    
    @Query("SELECT COALESCE(SUM(w.totalSalesAmount.amount), 0) as totalSalesAmount, " +
           "COALESCE(SUM(w.totalOrderCount), 0) as totalOrderCount " +
           "FROM SettlementWeekly w " +
           "WHERE w.settlementYear = :year AND w.settlementMonth = :month AND w.weekOfMonth = :weekOfMonth")
    Map<String, Object> sumAllByYearAndMonthAndWeekOfMonth(
        @Param("year") int year,
        @Param("month") int month,
        @Param("weekOfMonth") int weekOfMonth
    );

    
    List<SettlementWeekly> findBySettlementStatusAndWeekEndDateBefore(
        SettlementStatus status,
        LocalDate cutoffDate
    );
}
