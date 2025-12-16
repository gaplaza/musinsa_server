package com.mudosa.musinsa.settlement.application.dto;

import com.mudosa.musinsa.common.vo.Money;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@SuppressWarnings("unused")
public class DailyAggregationDto {

    private Long brandId;
    private LocalDate settlementDate;
    private Long totalOrderCount;
    private Money totalSalesAmount;
    private Money totalCommissionAmount;
    private Money totalTaxAmount;
    private Money totalPgFeeAmount;

    public DailyAggregationDto(
        Long brandId,
        LocalDate settlementDate,
        Long totalOrderCount,
        BigDecimal totalSalesAmount,
        BigDecimal totalCommissionAmount,
        BigDecimal totalTaxAmount,
        BigDecimal totalPgFeeAmount
    ) {
        this.brandId = brandId;
        this.settlementDate = settlementDate;
        this.totalOrderCount = totalOrderCount;
        this.totalSalesAmount = new Money(totalSalesAmount);
        this.totalCommissionAmount = new Money(totalCommissionAmount);
        this.totalTaxAmount = new Money(totalTaxAmount);
        this.totalPgFeeAmount = new Money(totalPgFeeAmount);
    }

    public DailyAggregationDto(
        Long brandId,
        LocalDate settlementDate,
        Long totalOrderCount,
        Money totalSalesAmount,
        Money totalCommissionAmount,
        Money totalTaxAmount,
        Money totalPgFeeAmount
    ) {
        this.brandId = brandId;
        this.settlementDate = settlementDate;
        this.totalOrderCount = totalOrderCount;
        this.totalSalesAmount = totalSalesAmount;
        this.totalCommissionAmount = totalCommissionAmount;
        this.totalTaxAmount = totalTaxAmount;
        this.totalPgFeeAmount = totalPgFeeAmount;
    }
}
