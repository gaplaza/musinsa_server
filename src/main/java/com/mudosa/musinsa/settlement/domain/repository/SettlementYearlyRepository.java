package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.domain.model.SettlementYearly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface SettlementYearlyRepository extends JpaRepository<SettlementYearly, Long> {

    
    List<SettlementYearly> findByBrandIdOrderBySettlementYearDesc(Long brandId);

    
    java.util.Optional<SettlementYearly> findByBrandIdAndSettlementYear(
        Long brandId,
        Integer settlementYear
    );

    
    List<SettlementYearly> findByBrandId(Long brandId);

    
    @Query("SELECT COALESCE(SUM(y.totalSalesAmount.amount), 0) as totalSalesAmount, " +
           "COALESCE(SUM(y.totalOrderCount), 0) as totalOrderCount " +
           "FROM SettlementYearly y " +
           "WHERE y.settlementYear = :year")
    Map<String, Object> sumAllByYear(@Param("year") int year);

    
    @Query("SELECT COALESCE(SUM(y.totalSalesAmount.amount), 0) as totalSalesAmount, " +
           "COALESCE(SUM(y.totalOrderCount), 0) as totalOrderCount " +
           "FROM SettlementYearly y")
    Map<String, Object> sumAll();
}
