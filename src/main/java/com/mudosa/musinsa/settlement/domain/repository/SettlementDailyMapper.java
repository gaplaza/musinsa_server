package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.application.dto.MonthlyAggregationDto;
import com.mudosa.musinsa.settlement.application.dto.WeeklyAggregationDto;
import com.mudosa.musinsa.settlement.domain.model.SettlementDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SettlementDailyMapper {

    void batchInsert(@Param("list") List<SettlementDaily> list);

    List<WeeklyAggregationDto> aggregateByWeekly(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    List<MonthlyAggregationDto> aggregateByMonthly(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
