package com.mudosa.musinsa.settlement.domain.model;

import com.mudosa.musinsa.common.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "pg_fee_policies",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_pg_fee_policy",
            columnNames = {"pg_provider", "payment_method", "effective_from"}
        )
    },
    indexes = {
        @Index(name = "idx_pg_fee_policy_lookup", columnList = "pg_provider, payment_method, effective_from, effective_to")
    }
)
@Getter
@Builder(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PgFeePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @Column(name = "pg_provider", nullable = false, length = 50)
    private String pgProvider;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 20)
    private FeeType feeType;

    @Column(name = "fee_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal feeValue;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    
    public static PgFeePolicy createPolicy(
        String pgProvider,
        String paymentMethod,
        FeeType feeType,
        BigDecimal feeValue,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String description
    ) {
        return builder()
            .pgProvider(pgProvider)
            .paymentMethod(paymentMethod)
            .feeType(feeType)
            .feeValue(feeValue)
            .effectiveFrom(effectiveFrom)
            .effectiveTo(effectiveTo)
            .description(description)
            .active(true)
            .build();
    }

    @SuppressWarnings("unused")
    public static PgFeePolicyBuilder testBuilder() {
        return builder()
            .active(true);
    }

    
    public void deactivate() {
        this.active = false;
    }

    
    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }
}
