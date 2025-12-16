package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.domain.model.SettlementMonthly;
import com.mudosa.musinsa.settlement.domain.model.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface SettlementMonthlyRepositoryCustom {

    
    Page<SettlementMonthly> findAllWithFilters(
        Long brandId,
        Integer year,
        Integer month,
        SettlementStatus status,
        String searchTerm,
        Pageable pageable
    );

    
    Map<String, Object> sumAllByYearAndMonth(int year, int month);
}
