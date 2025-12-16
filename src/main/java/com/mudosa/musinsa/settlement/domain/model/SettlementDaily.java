package com.mudosa.musinsa.settlement.domain.model;

import com.mudosa.musinsa.common.domain.model.BaseEntity;
import com.mudosa.musinsa.common.vo.Money;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(
    name = "settlements_daily",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"settlement_number"}),
        @UniqueConstraint(columnNames = {"brand_id", "settlement_date"})
    },
    indexes = {
        @Index(name = "idx_daily_settlement_date", columnList = "settlement_date"),
        @Index(name = "idx_daily_brand_id", columnList = "brand_id"),
        @Index(name = "idx_daily_settlement_status", columnList = "settlement_status")
    }
)
@Getter
@Builder(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
@SuppressWarnings("lombok")
public class SettlementDaily extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_settlement_id")
    private Long id;

    @Column(name = "settlement_number", nullable = false, unique = true, length = 50)
    private String settlementNumber;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "settlement_timezone", nullable = false, length = 50)
    private String settlementTimezone;

    @Column(name = "total_order_count", nullable = false)
    private Integer totalOrderCount = 0;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_sales_amount", nullable = false, precision = 15, scale = 2))
    private Money totalSalesAmount = Money.ZERO;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_commission_amount", nullable = false, precision = 15, scale = 2))
    private Money totalCommissionAmount = Money.ZERO;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_tax_amount", nullable = false, precision = 15, scale = 2))
    private Money totalTaxAmount = Money.ZERO;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_pg_fee_amount", nullable = false, precision = 15, scale = 2))
    private Money totalPgFeeAmount = Money.ZERO;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "final_settlement_amount", nullable = false, precision = 15, scale = 2))
    private Money finalSettlementAmount = Money.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false, length = 20)
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    @Column(name = "aggregated_at")
    private LocalDateTime aggregatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    
    public static SettlementDaily createDailySettlement(
        Long brandId,
        LocalDate settlementDate,
        String settlementNumber,
        String timezone
    ) {
        return builder()
            .brandId(brandId)
            .settlementDate(settlementDate)
            .settlementNumber(settlementNumber)
            .settlementTimezone(validateTimezone(timezone))
            .settlementStatus(SettlementStatus.PENDING)
            .totalOrderCount(0)
            .totalSalesAmount(Money.ZERO)
            .totalCommissionAmount(Money.ZERO)
            .totalTaxAmount(Money.ZERO)
            .totalPgFeeAmount(Money.ZERO)
            .finalSettlementAmount(Money.ZERO)
            .build();
    }

    
    public static SettlementDaily createFromAggregation(
        Long brandId,
        LocalDate settlementDate,
        String settlementNumber,
        String timezone,
        int totalOrderCount,
        Money totalSalesAmount,
        Money totalCommissionAmount,
        Money totalTaxAmount,
        Money totalPgFeeAmount
    ) {
        Money finalAmount = totalSalesAmount
            .subtract(totalCommissionAmount)
            .subtract(totalTaxAmount)
            .subtract(totalPgFeeAmount);

        return builder()
            .brandId(brandId)
            .settlementDate(settlementDate)
            .settlementNumber(settlementNumber)
            .settlementTimezone(validateTimezone(timezone))
            .totalOrderCount(totalOrderCount)
            .totalSalesAmount(totalSalesAmount)
            .totalCommissionAmount(totalCommissionAmount)
            .totalTaxAmount(totalTaxAmount)
            .totalPgFeeAmount(totalPgFeeAmount)
            .finalSettlementAmount(finalAmount)
            .settlementStatus(SettlementStatus.PENDING)
            .build();
    }

    private static String validateTimezone(String timezone) {
        try {
            return ZoneId.of(timezone).getId();
        } catch (DateTimeException e) {
            log.warn("유효하지 않은 타임존: {}. UTC로 기본 설정합니다.", timezone, e);
            return "UTC";
        }
    }

    
    public static SettlementDailyBuilder testBuilder() {
        return builder()
            .settlementStatus(SettlementStatus.PENDING)
            .settlementTimezone("UTC")
            .totalOrderCount(0)
            .totalSalesAmount(Money.ZERO)
            .totalCommissionAmount(Money.ZERO)
            .totalTaxAmount(Money.ZERO)
            .totalPgFeeAmount(Money.ZERO)
            .finalSettlementAmount(Money.ZERO);
    }

    public void setAggregatedData(
        int totalOrderCount,
        Money totalSalesAmount,
        Money totalCommissionAmount,
        Money totalTaxAmount,
        Money totalPgFeeAmount
    ) {
        this.totalOrderCount = totalOrderCount;
        this.totalSalesAmount = totalSalesAmount;
        this.totalCommissionAmount = totalCommissionAmount;
        this.totalTaxAmount = totalTaxAmount;
        this.totalPgFeeAmount = totalPgFeeAmount;
        this.finalSettlementAmount = calculateFinalAmount();
    }

    
    public void addAggregatedData(
        int orderCount,
        Money salesAmount,
        Money commissionAmount,
        Money taxAmount,
        Money pgFeeAmount
    ) {
        this.totalOrderCount += orderCount;
        this.totalSalesAmount = this.totalSalesAmount.add(salesAmount);
        this.totalCommissionAmount = this.totalCommissionAmount.add(commissionAmount);
        this.totalTaxAmount = this.totalTaxAmount.add(taxAmount);
        this.totalPgFeeAmount = this.totalPgFeeAmount.add(pgFeeAmount);
        this.finalSettlementAmount = calculateFinalAmount();
    }

    private Money calculateFinalAmount() {
        return totalSalesAmount
            .subtract(totalCommissionAmount)
            .subtract(totalTaxAmount)
            .subtract(totalPgFeeAmount);
    }

    public void startProcessing() {
        this.settlementStatus = SettlementStatus.PENDING;
        this.aggregatedAt = LocalDateTime.now(ZoneId.of("UTC"));
    }

    public void confirm() {
        this.settlementStatus = SettlementStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now(ZoneId.of("UTC"));
    }

    public void complete() {
        this.settlementStatus = SettlementStatus.COMPLETED;
        this.completedAt = LocalDateTime.now(ZoneId.of("UTC"));
    }
}
