package com.mudosa.musinsa.settlement.application.dto;

import com.mudosa.musinsa.common.vo.Money;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@SuppressWarnings("unused")
public class WeeklyAggregationDto {

    private Long brandId;
    private Integer year;
    private Integer month;
    private Integer weekOfMonth;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private Long totalOrderCount;
    private Money totalSalesAmount;
    private Money totalCommissionAmount;
    private Money totalTaxAmount;
    private Money totalPgFeeAmount;

    public WeeklyAggregationDto() {
    }

    public WeeklyAggregationDto(
        Long brandId,
        Integer year,
        Integer month,
        Integer weekOfMonth,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        Long totalOrderCount,
        BigDecimal totalSalesAmount,
        BigDecimal totalCommissionAmount,
        BigDecimal totalTaxAmount,
        BigDecimal totalPgFeeAmount
    ) {
        this.brandId = brandId;
        this.year = year;
        this.month = month;
        this.weekOfMonth = weekOfMonth;
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.totalOrderCount = totalOrderCount;
        this.totalSalesAmount = new Money(totalSalesAmount);
        this.totalCommissionAmount = new Money(totalCommissionAmount);
        this.totalTaxAmount = new Money(totalTaxAmount);
        this.totalPgFeeAmount = new Money(totalPgFeeAmount);
    }

    public WeeklyAggregationDto(
        Long brandId,
        Integer year,
        Integer month,
        Integer weekOfMonth,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        Long totalOrderCount,
        Money totalSalesAmount,
        Money totalCommissionAmount,
        Money totalTaxAmount,
        Money totalPgFeeAmount
    ) {
        this.brandId = brandId;
        this.year = year;
        this.month = month;
        this.weekOfMonth = weekOfMonth;
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.totalOrderCount = totalOrderCount;
        this.totalSalesAmount = totalSalesAmount;
        this.totalCommissionAmount = totalCommissionAmount;
        this.totalTaxAmount = totalTaxAmount;
        this.totalPgFeeAmount = totalPgFeeAmount;
    }
}
