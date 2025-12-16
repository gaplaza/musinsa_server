package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.application.dto.YearlyAggregationDto;
import com.mudosa.musinsa.settlement.domain.model.SettlementMonthly;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SettlementMonthlyMapper {

    void batchInsert(@Param("list") List<SettlementMonthly> list);

    List<YearlyAggregationDto> aggregateByYearly(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
