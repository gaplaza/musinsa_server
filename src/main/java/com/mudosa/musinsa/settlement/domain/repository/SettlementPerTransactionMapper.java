package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.application.dto.DailyAggregationDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SettlementPerTransactionMapper {

    List<DailyAggregationDto> aggregateByDaily(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    List<DailyAggregationDto> aggregateNotAggregated();

    List<DailyAggregationDto> aggregateProcessing();

    int updateStatusToProcessing();

    int updateProcessingToAggregated();

    int resetProcessingToNotAggregated();

    int countProcessing();

    int updateAggregationStatusToAggregated();
}
