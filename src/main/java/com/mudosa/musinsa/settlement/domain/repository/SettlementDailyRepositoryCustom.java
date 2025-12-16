package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.domain.model.SettlementDaily;
import com.mudosa.musinsa.settlement.domain.model.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface SettlementDailyRepositoryCustom {

    
    Page<SettlementDaily> findAllWithFilters(
        Long brandId,
        LocalDate startDate,
        LocalDate endDate,
        SettlementStatus status,
        String searchTerm,
        Pageable pageable
    );

    
    Map<String, Object> sumAllBySettlementDate(LocalDate settlementDate);
}
