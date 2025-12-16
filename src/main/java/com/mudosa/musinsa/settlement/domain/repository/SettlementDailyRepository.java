package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.domain.model.SettlementDaily;
import com.mudosa.musinsa.settlement.domain.model.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SettlementDailyRepository extends JpaRepository<SettlementDaily, Long>, SettlementDailyRepositoryCustom {

    
    java.util.Optional<SettlementDaily> findByBrandIdAndSettlementDate(
        Long brandId,
        LocalDate settlementDate
    );

    
    List<SettlementDaily> findBySettlementStatusAndSettlementDateBefore(
        SettlementStatus status,
        LocalDate date
    );
}
