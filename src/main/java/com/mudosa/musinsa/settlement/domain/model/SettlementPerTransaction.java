package com.mudosa.musinsa.settlement.domain.model;

import com.mudosa.musinsa.common.domain.model.CreatedOnlyEntity;
import com.mudosa.musinsa.common.vo.Money;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "settlements_per_transaction")
@Getter
@Builder(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class SettlementPerTransaction extends CreatedOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_transaction_id")
    private Long id;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "transaction_date_local", nullable = false)
    private LocalDate transactionDateLocal;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType = TransactionType.ORDER;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "transaction_amount", nullable = false, precision = 15, scale = 2))
    private Money transactionAmount;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2))
    private Money commissionAmount;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2))
    private Money taxAmount;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "pg_fee_amount", nullable = false, precision = 15, scale = 2))
    private Money pgFeeAmount;

    @Column(name = "timezone_offset", nullable = false, length = 10)
    private String timezoneOffset = "+09:00";

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_status", nullable = false, length = 20)
    @Builder.Default
    private AggregationStatus aggregationStatus = AggregationStatus.NOT_AGGREGATED;

    
    public static SettlementPerTransaction createTransaction(
        Long brandId,
        Long paymentId,
        String pgTransactionId,
        Money transactionAmount,
        BigDecimal commissionRate,
        Money pgFeeAmount,
        TransactionType transactionType,
        String timezoneOffset
    ) {

        ZonedDateTime utcNow = ZonedDateTime.now(ZoneId.of("UTC"));

        String validatedTimezone;
        ZoneId userZoneId;
        try {
            userZoneId = ZoneId.of(timezoneOffset);
            validatedTimezone = timezoneOffset;
        } catch (DateTimeException e) {
            log.warn("유효하지 않은 타임존 오프셋: {}. UTC로 기본 설정합니다.", timezoneOffset, e);
            validatedTimezone = "UTC";
            userZoneId = ZoneId.of("UTC");
        }

        Money commissionAmount = transactionAmount
            .multiply(commissionRate)
            .divide(100)
            .roundToWon();

        Money taxAmount = commissionAmount
            .multiply(BigDecimal.valueOf(10))
            .divide(100)
            .roundToWon();

        return builder()
            .brandId(brandId)
            .paymentId(paymentId)
            .pgTransactionId(pgTransactionId)
            .transactionAmount(transactionAmount)
            .commissionRate(commissionRate)
            .commissionAmount(commissionAmount)
            .taxAmount(taxAmount)
            .pgFeeAmount(pgFeeAmount)
            .transactionType(transactionType)
            .transactionDate(utcNow.toLocalDateTime())
            .transactionDateLocal(utcNow.withZoneSameInstant(userZoneId).toLocalDate())
            .timezoneOffset(validatedTimezone)
            .aggregationStatus(AggregationStatus.NOT_AGGREGATED)
            .build();
    }

    
    public static SettlementPerTransactionBuilder testBuilder() {
        return builder()
            .transactionType(TransactionType.ORDER)
            .timezoneOffset("UTC");
    }

    
    public Money calculateFinalSettlementAmount() {
        return transactionAmount
            .subtract(commissionAmount)
            .subtract(taxAmount)
            .subtract(pgFeeAmount);
    }

    
    public void markAsAggregated() {
        this.aggregationStatus = AggregationStatus.AGGREGATED;
    }
}
